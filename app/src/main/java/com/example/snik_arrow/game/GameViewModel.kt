package com.example.snik_arrow.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snik_arrow.game.models.GameState
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    private val engine = GameEngine(viewModelScope)
    val gameState: StateFlow<GameState> = engine.gameState

    fun startLevel() {
        engine.startLevel()
    }

    fun shoot() {
        engine.shoot()
    }
    
    fun reset() {
        // Stop current and reload level 1 or current level
        engine.stop()
        engine.loadLevel(gameState.value.level.number)
    }

    fun restartGame() {
        engine.restartGame()
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }
}
