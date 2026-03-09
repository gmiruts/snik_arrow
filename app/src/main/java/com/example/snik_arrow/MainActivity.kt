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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: GameViewModel by viewModels()
    private var mInterstitialAd: InterstitialAd? = null

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun showInterstitialAd(onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd() // Pre-load next ad
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            mInterstitialAd?.show(this)
        } else {
            // Ad not ready, just continue
            onAdDismissed()
            // Optionally try loading it again
            loadInterstitialAd()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the Google Mobile Ads SDK on a background thread
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }
        
        loadInterstitialAd()

        setContent {
            SnikarrowTheme {
                val gameState by viewModel.gameState.collectAsState()
                
                GameScreen(
                    gameState = gameState,
                    onStart = { viewModel.startLevel() },
                    onShoot = { viewModel.shoot() },
                    onReset = { viewModel.reset() },
                    onRestartGame = { viewModel.restartGame() },
                    onNextLevel = {
                        showInterstitialAd {
                            viewModel.startLevel()
                        }
                    }
                )
            }
        }
    }
}
