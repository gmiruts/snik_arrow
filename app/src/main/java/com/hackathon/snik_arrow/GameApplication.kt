package com.hackathon.snik_arrow

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "game_settings")

class GameApplication : Application()
