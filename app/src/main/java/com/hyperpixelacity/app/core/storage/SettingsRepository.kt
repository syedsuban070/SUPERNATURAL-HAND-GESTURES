package com.hyperpixelacity.app.core.storage
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hyperpixelacity.app.core.model.StudioSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import java.io.IOException
private val Context.store by preferencesDataStore("studio")
@Singleton class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
 val settings = context.store.data.catch { if(it is IOException) emit(emptyPreferences()) else throw it }.map { p -> StudioSettings(
 effect=p[intPreferencesKey("effect")]?:0, intensity=p[floatPreferencesKey("intensity")]?:1f,
 size=p[floatPreferencesKey("size")]?:1f, glow=p[floatPreferencesKey("glow")]?:1f,
 density=p[floatPreferencesKey("density")]?:.5f, trailSeconds=p[floatPreferencesKey("trail")]?:1f,
 quality=p[intPreferencesKey("quality")]?:1,mirror=p[booleanPreferencesKey("mirror")]?:true,
 reduceMotion=p[booleanPreferencesKey("reduce")]?:false,debug=p[booleanPreferencesKey("debug")]?:false,
 countdown=p[intPreferencesKey("countdown")]?:0,hue=p[longPreferencesKey("hue")]?:0L,
 onboarded=p[booleanPreferencesKey("onboarded")]?:false,favorites=p[stringSetPreferencesKey("favorites")]?:emptySet()) }
 suspend fun save(s: StudioSettings) { context.store.edit { p ->
 p[intPreferencesKey("effect")]=s.effect.coerceIn(0,9);p[floatPreferencesKey("intensity")]=s.intensity.coerceIn(.2f,2f)
 p[floatPreferencesKey("size")]=s.size.coerceIn(.4f,2f);p[floatPreferencesKey("glow")]=s.glow.coerceIn(0f,2f)
 p[floatPreferencesKey("density")]=s.density.coerceIn(0f,1f);p[floatPreferencesKey("trail")]=s.trailSeconds.coerceIn(.3f,2f)
 p[intPreferencesKey("quality")]=s.quality.coerceIn(0,2);p[booleanPreferencesKey("mirror")]=s.mirror
 p[booleanPreferencesKey("reduce")]=s.reduceMotion;p[booleanPreferencesKey("debug")]=s.debug
 p[intPreferencesKey("countdown")]=s.countdown;p[longPreferencesKey("hue")]=s.hue
 p[booleanPreferencesKey("onboarded")]=s.onboarded;p[stringSetPreferencesKey("favorites")]=s.favorites
 } }
}
