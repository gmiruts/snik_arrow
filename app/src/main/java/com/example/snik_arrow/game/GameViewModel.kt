package com.example.snik_arrow.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snik_arrow.game.models.GameState
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.snik_arrow.dataStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(private val application: android.app.Application) : ViewModel() {
    private val engine = GameEngine(viewModelScope)
    
    private val levelKey = intPreferencesKey("current_level")
    
    val gameState: StateFlow<GameState> = engine.gameState

    init {
        viewModelScope.launch {
            val prefs = application.dataStore.data.first()
            val savedLevel = prefs[levelKey] ?: 1
            engine.loadLevel(savedLevel)
        }
    }

    private fun saveLevel(level: Int) {
        viewModelScope.launch {
            application.dataStore.edit { prefs ->
                prefs[levelKey] = level
            }
        }
    }

    fun startLevel() {
        val currentLevel = engine.gameState.value.level.number
        engine.startLevel()
        // If level changed, save it
        if (engine.gameState.value.level.number != currentLevel) {
            saveLevel(engine.gameState.value.level.number)
        }
    }

    fun shoot() {
        engine.shoot()
    }
    
    fun reset() {
        engine.stop()
        engine.loadLevel(gameState.value.level.number)
    }

    fun restartGame() {
        engine.restartGame()
        saveLevel(1)
    }

    fun togglePause() {
        engine.togglePause()
    }

    fun revive() {
        engine.revive()
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }
}
