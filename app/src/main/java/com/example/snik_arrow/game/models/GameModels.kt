package com.example.snik_arrow.game.models

enum class GameStatus {
    IDLE, PLAYING, GAME_OVER, LEVEL_COMPLETE, GAME_WON
}

data class Level(
    val number: Int,
    val targetSpeed: Float, // degrees per second
    val rotationDirection: Int = 1, // 1 for clockwise, -1 for counter-clockwise
    val arrowsToShoot: Int,
    val initialArrowsAngles: List<Float> = emptyList()
)

data class AttachedArrow(
    val angle: Float, // angle relative to target center
    val isCollided: Boolean = false
)

data class ShootingArrow(
    val y: Float // vertical position from the center of the target
)

data class GameState(
    val status: GameStatus = GameStatus.IDLE,
    val level: Level,
    val currentScore: Int = 0,
    val targetRotation: Float = 0f,
    val attachedArrows: List<AttachedArrow> = emptyList(),
    val shootingArrows: List<ShootingArrow> = emptyList(),
    val arrowsLeftToShoot: Int = 0
)
