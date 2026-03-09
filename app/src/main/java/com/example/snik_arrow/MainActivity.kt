package com.example.snik_arrow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.snik_arrow.game.GameViewModel
import com.example.snik_arrow.ui.GameScreen
import com.example.snik_arrow.ui.theme.SnikarrowTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the Google Mobile Ads SDK on a background thread
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        setContent {
            SnikarrowTheme {
                val gameState by viewModel.gameState.collectAsState()
                
                GameScreen(
                    gameState = gameState,
                    onStart = { viewModel.startLevel() },
                    onShoot = { viewModel.shoot() },
                    onReset = { viewModel.reset() },
                    onRestartGame = { viewModel.restartGame() }
                )
            }
        }
    }
}
