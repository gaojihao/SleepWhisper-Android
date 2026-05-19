package com.lizhi1026.sleepwhisper.features.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    private val _dobMs = MutableLiveData<Long?>(null)
    val dobMs: LiveData<Long?> get() = _dobMs

    private val _gender = MutableLiveData(Baby.BabyGender.UNKNOWN)
    val gender: LiveData<Baby.BabyGender> get() = _gender

    val isValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        val update = {
            value = Baby.isValidName(_name.value.orEmpty()) &&
                _dobMs.value != null &&
                (_dobMs.value ?: Long.MAX_VALUE) <= System.currentTimeMillis()
        }
        addSource(_name) { update() }
        addSource(_dobMs) { update() }
        update()
    }

    fun onName(v: String) { _name.value = v }
    fun onDob(epochMs: Long?) { _dobMs.value = epochMs }
    fun onGender(g: Baby.BabyGender) { _gender.value = g }

    fun submit() {
        val n = _name.value?.trim().orEmpty()
        val d = _dobMs.value ?: return
        if (!Baby.isValidName(n)) return
        val now = System.currentTimeMillis()
        val baby = Baby(
            id = UUID.randomUUID().toString(),
            name = n,
            gender = _gender.value ?: Baby.BabyGender.UNKNOWN,
            dateOfBirth = d,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            app.saveBaby(baby)
            // Persist activeBabyId in settings
            val cur = app.settings.value ?: UserSettings.DEFAULT
            app.saveSettings(cur.copy(activeBabyId = baby.id))
        }
    }
}
