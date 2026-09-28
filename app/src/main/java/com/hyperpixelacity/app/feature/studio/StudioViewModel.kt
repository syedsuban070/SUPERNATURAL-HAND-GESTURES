package com.hyperpixelacity.app.feature.studio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperpixelacity.app.core.model.StudioSettings
import com.hyperpixelacity.app.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
@HiltViewModel class StudioViewModel @Inject constructor(private val repository:SettingsRepository):ViewModel() {
 val settings=repository.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),StudioSettings())
 fun save(s:StudioSettings) { viewModelScope.launch { repository.save(s) } }
}
