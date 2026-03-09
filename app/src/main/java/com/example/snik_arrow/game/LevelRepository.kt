package com.example.snik_arrow.game

import com.example.snik_arrow.game.models.Level
import kotlin.math.min

object LevelRepository {
    val totalLevels = 50

    fun getLevel(number: Int): Level {
        val safeNumber = number.coerceIn(1, totalLevels)
        
        // Speed: Starts at 50, caps at around 190 degrees/sec
        val targetSpeed = 50f + (safeNumber * 2.8f)
        
        // Direction: Alternates more frequently as levels progress
        val rotationDirection = if (safeNumber % 3 == 0 || safeNumber % 7 == 0) -1 else 1
        
        // Arrows to shoot: Starts at 5, goes up to 25
        val arrowsToShoot = 5 + (safeNumber / 2)
        
        // Pre-attached arrows: Starts appearing at level 5
        val initialArrowsCount = when {
            safeNumber < 5 -> 0
            safeNumber < 15 -> min(4, safeNumber / 3)
            safeNumber < 30 -> min(8, safeNumber / 3)
            else -> min(14, safeNumber / 3)
        }
        
        val initialArrowsAngles = if (initialArrowsCount > 0) {
            // Evenly distributed pre-attached arrows
            List(initialArrowsCount) { index ->
                (360f / initialArrowsCount) * index
            }
        } else {
            emptyList()
        }

        return Level(
            number = safeNumber,
            targetSpeed = targetSpeed,
            rotationDirection = rotationDirection,
            arrowsToShoot = arrowsToShoot,
            initialArrowsAngles = initialArrowsAngles
        )
    }
}