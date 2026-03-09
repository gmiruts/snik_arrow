package com.example.snik_arrow.game

import com.example.snik_arrow.game.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs

const val TARGET_RADIUS = 120f
const val ARROW_START_Y = 600f
const val ARROW_SPEED = 1800f // pixels per second
const val COLLISION_THRESHOLD_DEGREES = 12f

class GameEngine(private val coroutineScope: CoroutineScope) {

    private val _gameState = MutableStateFlow(
        GameState(
            level = LevelRepository.getLevel(1),
            attachedArrows = LevelRepository.getLevel(1).initialArrowsAngles.map { AttachedArrow(it) },
            arrowsLeftToShoot = LevelRepository.getLevel(1).arrowsToShoot
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lastUpdateTime = 0L

    fun loadLevel(levelNumber: Int) {
        val level = LevelRepository.getLevel(levelNumber)
        _gameState.update { currentState ->
            GameState(
                status = GameStatus.IDLE,
                level = level,
                currentScore = currentState.currentScore,
                targetRotation = 0f,
                attachedArrows = level.initialArrowsAngles.map { AttachedArrow(it) },
                shootingArrows = emptyList(),
                arrowsLeftToShoot = level.arrowsToShoot
            )
        }
    }

    fun restartGame() {
        stop()
        val level = LevelRepository.getLevel(1)
        _gameState.update {
            GameState(
                status = GameStatus.IDLE,
                level = level,
                currentScore = 0,
                targetRotation = 0f,
                attachedArrows = level.initialArrowsAngles.map { AttachedArrow(it) },
                shootingArrows = emptyList(),
                arrowsLeftToShoot = level.arrowsToShoot
            )
        }
    }

    fun startLevel() {
        if (_gameState.value.status == GameStatus.PLAYING) return
        
        // If it was game over or complete, re-init the level state first
        if (_gameState.value.status == GameStatus.LEVEL_COMPLETE) {
            val nextLevelNum = _gameState.value.level.number + 1
            if (nextLevelNum > LevelRepository.totalLevels) {
                _gameState.update { it.copy(status = GameStatus.GAME_WON) }
                return
            } else {
                loadLevel(nextLevelNum)
            }
        } else if (_gameState.value.status == GameStatus.GAME_OVER) {
            loadLevel(_gameState.value.level.number)
        }

        _gameState.update { it.copy(status = GameStatus.PLAYING) }
        startGameLoop()
    }

    fun shoot() {
        val currentState = _gameState.value
        if (currentState.status != GameStatus.PLAYING || currentState.arrowsLeftToShoot <= 0) return

        _gameState.update { state ->
            state.copy(
                arrowsLeftToShoot = state.arrowsLeftToShoot - 1,
                shootingArrows = state.shootingArrows + ShootingArrow(ARROW_START_Y)
            )
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lastUpdateTime = System.currentTimeMillis()
        gameLoopJob = coroutineScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val dt = (currentTime - lastUpdateTime) / 1000f
                lastUpdateTime = currentTime

                update(dt)

                delay(16) // roughly 60 FPS
            }
        }
    }

    private fun update(dt: Float) {
        val currentState = _gameState.value
        if (currentState.status != GameStatus.PLAYING) return

        val level = currentState.level
        val currentRotation = currentState.targetRotation
        // Update target rotation
        val newRotation = (currentRotation + (level.targetSpeed * level.rotationDirection) * dt) % 360f

        val newShootingArrows = mutableListOf<ShootingArrow>()
        val newAttachedArrows = currentState.attachedArrows.toMutableList()
        var newStatus = currentState.status
        var newScore = currentState.currentScore

        for (arrow in currentState.shootingArrows) {
            val newY = arrow.y - ARROW_SPEED * dt
            if (newY <= TARGET_RADIUS) {
                // Arrow hit the target
                // Convert hit angle to relative angle on the target
                val rawHitAngle = 90f - newRotation
                val hitAngle = normalizeAngle(rawHitAngle)

                // Collision detection against attached arrows
                val isCollision = newAttachedArrows.any { attached ->
                    val diff = abs(normalizeAngle(attached.angle - hitAngle))
                    val shortestDiff = if (diff > 180f) 360f - diff else diff
                    shortestDiff < COLLISION_THRESHOLD_DEGREES
                }

                if (isCollision) {
                    newStatus = GameStatus.GAME_OVER
                    newAttachedArrows.add(AttachedArrow(hitAngle, isCollided = true))
                } else {
                    newAttachedArrows.add(AttachedArrow(hitAngle))
                    newScore += 10
                    
                    // Award bonus for completing level
                    if (newShootingArrows.isEmpty() && currentState.arrowsLeftToShoot <= 0 && currentState.shootingArrows.size == 1) {
                         newScore += level.number * 50 // Level completion bonus
                    }
                }
            } else {
                newShootingArrows.add(ShootingArrow(newY))
            }
        }

        // Check if level is complete
        if (newStatus == GameStatus.PLAYING && newShootingArrows.isEmpty() && currentState.arrowsLeftToShoot <= 0) {
            newStatus = GameStatus.LEVEL_COMPLETE
        }

        _gameState.update { state ->
            state.copy(
                status = newStatus,
                targetRotation = normalizeAngle(newRotation),
                shootingArrows = newShootingArrows,
                attachedArrows = newAttachedArrows,
                currentScore = newScore
            )
        }

        if (newStatus != GameStatus.PLAYING) {
            gameLoopJob?.cancel()
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < 0) result += 360f
        return result
    }
    
    fun stop() {
        gameLoopJob?.cancel()
    }
}