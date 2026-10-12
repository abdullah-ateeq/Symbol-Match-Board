package com.example.symbolmatchboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.symbolmatchboard.SymbolMatchApp
import com.example.symbolmatchboard.game.ai.SymbolMatchCpuStrategy
import com.example.symbolmatchboard.game.logic.PairResolution
import com.example.symbolmatchboard.game.logic.SelectionOutcome
import com.example.symbolmatchboard.game.logic.SymbolMatchGameEngine
import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.GameResult
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolMatchGameState
import com.example.symbolmatchboard.util.FeedbackCue
import com.example.symbolmatchboard.util.GameTimings
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class EventKind { INFO, HINT, PLAYER_CAPTURE, CPU_CAPTURE, MISMATCH }

/** Short line shown under the board describing what just happened. */
data class GameEvent(val kind: EventKind, val title: String, val detail: String? = null)

data class GameUiState(
    val game: SymbolMatchGameState,
    val event: GameEvent? = null,
    /** Pair currently being linked by the connecting line (before capture). */
    val matchLink: Pair<Int, Int>? = null,
    val pulseIds: Set<Int> = emptySet(),
    val mismatchIds: Set<Int> = emptySet(),
    val lastMatchIds: Set<Int> = emptySet(),
    val cpuThinking: Boolean = false,
    val result: GameResult? = null,
    val showResult: Boolean = false
) {
    val acceptsPlayerInput: Boolean
        get() = game.phase == GamePhase.PLAYER_TURN && !showResult
}

sealed interface GameEffect {
    data class Cue(val cue: FeedbackCue) : GameEffect
    data class Message(val text: String) : GameEffect
}

class SymbolMatchViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepository = (app as SymbolMatchApp).settingsRepository
    private val statisticsRepository = (app as SymbolMatchApp).statisticsRepository
    private val random = Random.Default
    private val engine = SymbolMatchGameEngine(random)
    private val cpuStrategy = SymbolMatchCpuStrategy(random)

    private val _uiState = MutableStateFlow(GameUiState(game = engine.newGame()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<GameEffect>(Channel.BUFFERED)
    val effects: Flow<GameEffect> = effectChannel.receiveAsFlow()

    /** The single coroutine allowed to drive resolution / CPU turns. Guards against rapid taps. */
    private var turnJob: Job? = null
    private var resultRecorded = false

    private val timings: GameTimings
        get() = GameTimings.forAnimations(settingsRepository.settings.value.animations)

    private val game: SymbolMatchGameState get() = _uiState.value.game

    fun startNewGame() {
        turnJob?.cancel()
        turnJob = null
        resultRecorded = false
        val fresh = engine.newGame()
        _uiState.value = GameUiState(
            game = fresh,
            event = GameEvent(EventKind.INFO, "YOUR TURN", fresh.targetSymbol?.let { "Find two ${it.pluralName}." })
        )
    }

    /** Stops any running turn when the player leaves the match. Matches are never persisted. */
    fun abandonGame() {
        turnJob?.cancel()
        turnJob = null
    }

    fun onResultShown() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun onCellTapped(cellId: Int) {
        if (turnJob?.isActive == true || !_uiState.value.acceptsPlayerInput) return
        val target = game.targetSymbol ?: return
        when (val outcome = engine.select(game, cellId, Side.PLAYER)) {
            SelectionOutcome.Ignored -> Unit
            SelectionOutcome.WrongTarget -> {
                setEvent(GameEvent(EventKind.HINT, "FIND THE TARGET", "Tap a ${target.displayName}."))
                send(GameEffect.Message("Find the target symbol."))
            }
            SelectionOutcome.SameCell -> {
                setEvent(GameEvent(EventKind.HINT, "CHOOSE ANOTHER", "Pick a different ${target.displayName}."))
                send(GameEffect.Message("Choose another symbol."))
            }
            is SelectionOutcome.FirstSelected -> {
                _uiState.update { it.copy(game = outcome.state) }
                send(GameEffect.Cue(FeedbackCue.FIRST_SELECTION))
            }
            is SelectionOutcome.PairSelected -> {
                _uiState.update { it.copy(game = outcome.state) }
                send(GameEffect.Cue(FeedbackCue.SECOND_SELECTION))
                turnJob = viewModelScope.launch {
                    resolve(outcome)
                    continueGame()
                }
            }
        }
    }

    private suspend fun continueGame() {
        while (game.phase == GamePhase.CPU_TURN) {
            playCpuStep()
        }
        if (game.phase == GamePhase.GAME_OVER) finishGame()
    }

    private suspend fun playCpuStep() {
        val startState = game
        val wasThinking = _uiState.value.cpuThinking
        _uiState.update {
            it.copy(cpuThinking = true, event = GameEvent(EventKind.INFO, "CPU TURN", "Finding a match..."))
        }
        if (!wasThinking) send(GameEffect.Cue(FeedbackCue.CPU_TURN))
        val t = timings
        delay(random.nextLong(t.cpuThinkMinMs, t.cpuThinkMaxMs + 1))

        val difficulty = settingsRepository.settings.value.difficulty
        val move = cpuStrategy.chooseMove(startState.cells, startState.targetSymbol, difficulty)
        val first = move?.let { engine.select(startState, it.firstId, Side.CPU) } as? SelectionOutcome.FirstSelected
        if (move == null || first == null) {
            // Defensive: never leave the game stuck on the CPU.
            _uiState.update { it.copy(game = startState.copy(phase = GamePhase.PLAYER_TURN, activeSide = Side.PLAYER), cpuThinking = false) }
            return
        }
        _uiState.update { it.copy(game = first.state) }
        delay(t.cpuSecondPickMs)

        val second = engine.select(first.state, move.secondId, Side.CPU) as? SelectionOutcome.PairSelected ?: return
        _uiState.update { it.copy(game = second.state) }
        resolve(second)
        if (game.phase != GamePhase.CPU_TURN) _uiState.update { it.copy(cpuThinking = false) }
    }

    private suspend fun resolve(selection: SelectionOutcome.PairSelected) {
        val t = timings
        val ids = setOf(selection.firstId, selection.secondId)
        if (selection.isMatch) {
            _uiState.update { it.copy(matchLink = selection.firstId to selection.secondId, pulseIds = ids) }
            delay(t.matchRevealMs)
        } else {
            val missedBy = selection.state.activeSide
            setEvent(
                GameEvent(
                    EventKind.MISMATCH,
                    "NOT A MATCH",
                    if (missedBy == Side.PLAYER) "Turn passes to the CPU." else "CPU missed. Your turn."
                )
            )
            _uiState.update { it.copy(mismatchIds = ids) }
            send(GameEffect.Cue(FeedbackCue.MISMATCH))
            if (missedBy == Side.PLAYER) send(GameEffect.Message("Not a match."))
            delay(t.mismatchMs)
        }

        when (val resolution = engine.resolvePair(game) ?: return) {
            is PairResolution.Captured -> {
                val first = resolution.state.cell(resolution.firstId)
                val second = resolution.state.cell(resolution.secondId)
                val where = "${resolution.symbol.pluralName} at ${first?.coordinate?.label} + ${second?.coordinate?.label}"
                val byPlayer = resolution.side == Side.PLAYER
                _uiState.update {
                    it.copy(
                        game = resolution.state,
                        matchLink = null,
                        pulseIds = emptySet(),
                        lastMatchIds = ids,
                        event = GameEvent(
                            if (byPlayer) EventKind.PLAYER_CAPTURE else EventKind.CPU_CAPTURE,
                            if (byPlayer) "PAIR CAPTURED" else "CPU CAPTURED",
                            "$where  ·  +${resolution.points}"
                        )
                    )
                }
                if (byPlayer) {
                    send(GameEffect.Cue(FeedbackCue.MATCH))
                    val gameOver = resolution.state.phase == GamePhase.GAME_OVER
                    send(GameEffect.Message(if (gameOver) "Pair captured!" else "Pair captured! New target."))
                } else {
                    send(GameEffect.Cue(FeedbackCue.CPU_CAPTURE))
                    send(GameEffect.Message("CPU captured a pair."))
                }
                delay(t.lastMatchHighlightMs)
                _uiState.update { it.copy(lastMatchIds = emptySet()) }
            }
            is PairResolution.Mismatch -> {
                _uiState.update { it.copy(game = resolution.state, mismatchIds = emptySet()) }
            }
        }
    }

    private suspend fun finishGame() {
        if (resultRecorded) return
        val result = engine.result(game) ?: return
        resultRecorded = true
        statisticsRepository.record(result)
        _uiState.update {
            it.copy(
                cpuThinking = false,
                result = result,
                event = GameEvent(EventKind.INFO, "BOARD CLEARED", "${result.playerPairs} — ${result.cpuPairs}")
            )
        }
        send(GameEffect.Cue(if (result.outcome == GameOutcome.CPU_WIN) FeedbackCue.DEFEAT else FeedbackCue.VICTORY))
        delay(timings.gameOverDelayMs)
        _uiState.update { it.copy(showResult = true) }
    }

    private fun setEvent(event: GameEvent) {
        _uiState.update { it.copy(event = event) }
    }

    private fun send(effect: GameEffect) {
        effectChannel.trySend(effect)
    }
}
