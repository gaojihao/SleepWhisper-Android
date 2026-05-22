/**
 * OnboardingViewModel.kt — 宝宝信息录入界面的 ViewModel（features/onboarding）
 *
 * 用途：持有 Onboarding 表单的瞬时 UI 状态，并在提交时将宝宝档案写入持久层。
 * 表单验证：[isValid] 由 MediatorLiveData 聚合 _name、_dobMs 两个源，规则为
 *           Baby.isValidName(name) && dobMs != null && dobMs <= System.currentTimeMillis()。
 * 提交流程：[submit] → app.saveBaby(baby) + app.saveSettings(activeBabyId = baby.id)；
 *           提交后 AppStateContainer.baby 变为非 null，上层路由自动跳转至 WelcomeRitualScreen。
 */
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

/**
 * Onboarding 表单的 ViewModel。
 *
 * 持有宝宝名字、生日、性别三个可变状态，以及由 MediatorLiveData 聚合的表单有效性。
 * 提交后 AppStateContainer.baby 变化，上层路由自动跳转至 WelcomeRitualScreen。
 *
 * @param app 全局状态容器，提供 saveBaby / saveSettings 等持久化接口（Hilt 注入）
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {

    private val _name = MutableLiveData("")
    /** 宝宝名字（双向绑定到 BasicTextField） */
    val name: LiveData<String> get() = _name

    private val _dobMs = MutableLiveData<Long?>(null)
    /** 出生日期（毫秒时间戳），null 表示用户尚未选择 */
    val dobMs: LiveData<Long?> get() = _dobMs

    private val _gender = MutableLiveData(Baby.BabyGender.UNKNOWN)
    /** 宝宝性别，默认为 UNKNOWN（可选字段） */
    val gender: LiveData<Baby.BabyGender> get() = _gender

    /**
     * 表单有效性：[MediatorLiveData] 聚合 _name 与 _dobMs。
     * 规则：Baby.isValidName(name) && dobMs != null && dobMs <= 当前时间
     */
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

    /** 更新宝宝名字，触发 isValid 重新求值 */
    fun onName(v: String) { _name.value = v }
    /** 更新出生日期（来自 DatePickerDialog 的毫秒时间戳），触发 isValid 重新求值 */
    fun onDob(epochMs: Long?) { _dobMs.value = epochMs }
    /** 更新性别选择 */
    fun onGender(g: Baby.BabyGender) { _gender.value = g }

    /**
     * 提交表单：校验通过后构建 [Baby] 对象并持久化。
     *
     * 流程：app.saveBaby(baby) → app.saveSettings(activeBabyId = baby.id)
     * 之后 AppStateContainer.baby 变为非 null，导航由上层观察驱动。
     */
    fun submit() {
        val n = _name.value?.trim().orEmpty()
        val d = _dobMs.value ?: return  // dobMs 为 null 时直接返回，防御性检查
        if (!Baby.isValidName(n)) return  // 二次校验名字合法性
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
            // 将新宝宝设为激活档案，驱动后续所有功能使用此宝宝数据
            val cur = app.settings.value ?: UserSettings.DEFAULT
            app.saveSettings(cur.copy(activeBabyId = baby.id))
        }
    }
}
