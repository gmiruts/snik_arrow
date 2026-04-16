package com.hackathon.snik_arrow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hackathon.snik_arrow.game.GameViewModel
import com.hackathon.snik_arrow.ui.GameScreen
import com.hackathon.snik_arrow.ui.theme.SnikarrowTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: GameViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(application) as T
            }
        }
    }
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

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

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    private fun showRewardedAd(onRewardEarned: () -> Unit) {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    loadRewardedAd() // Pre-load next
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mRewardedAd = null
                    loadRewardedAd()
                }
            }
            mRewardedAd?.show(this) {
                onRewardEarned()
            }
        } else {
            // Ad not ready, maybe show a toast or just load it
            loadRewardedAd()
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
        loadRewardedAd()

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
                        if (gameState.level.number % 3 == 0) {
                            showInterstitialAd {
                                viewModel.startLevel()
                            }
                        } else {
                            viewModel.startLevel()
                        }
                    },
                    onRewardedRetry = {
                        showRewardedAd {
                            viewModel.revive()
                        }
                    },
                    onTogglePause = { viewModel.togglePause() }
                )
            }
        }
    }
}
