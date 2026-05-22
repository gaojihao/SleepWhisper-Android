package com.lizhi1026.sleepwhisper.core.toast

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局 Toast 事件总线（单例）。
 *
 * 所在层：core/toast — 基础设施层，依赖 AndroidX LiveData，无业务逻辑。
 * 交互对象：发送方（AppStateContainer / ViewModel）调用 [show] 发出事件；
 *           接收方（ToastOverlay Composable）观察 [events] 并在渲染后调用 [consume] 清除。
 *
 * 设计约定：
 *   - LiveData 槽位只保存最新一条事件，新事件覆盖旧事件。
 *   - UI 消费后必须调用 [consume]，否则重组时会重复展示同一条 Toast。
 *   - [show] 使用 postValue 支持子线程调用；[consume] 直接赋 null 清槽。
 */
@Singleton
class ToastCenter @Inject constructor() {

    /** Toast 展示样式，对应不同的视觉语义。 */
    enum class Style { INFO, SUCCESS, WARNING, ERROR }

    /**
     * 单条 Toast 数据模型。
     *
     * @param messageRes  字符串资源 ID，支持本地化。
     * @param args        消息格式化参数列表（对应 String.format 的可变参数）。
     * @param style       Toast 样式，决定图标与颜色。
     * @param undo        可选的撤销回调，非 null 时 UI 应展示"撤销"按钮。
     * @param editAction  可选的编辑回调，非 null 时 UI 应展示"编辑"按钮。
     */
    data class ToastItem(
        @StringRes val messageRes: Int,
        val args: List<Any> = emptyList(),
        val style: Style = Style.INFO,
        val undo: (() -> Unit)? = null,
        val editAction: (() -> Unit)? = null
    )

    // 内部可写 LiveData 槽位，初始值为 null（无待显示事件）
    private val _events = MutableLiveData<ToastItem?>(null)

    /** 对外暴露的只读 LiveData，ToastOverlay 订阅此字段。 */
    val events: LiveData<ToastItem?> get() = _events

    /**
     * 发出一条 Toast 事件；若前一条尚未被消费则直接覆盖。
     * 使用 postValue 以支持在非主线程调用。
     *
     * @param messageRes  字符串资源 ID。
     * @param args        格式化参数。
     * @param style       Toast 样式。
     * @param undo        撤销操作回调（可选）。
     * @param editAction  编辑操作回调（可选）。
     */
    fun show(
        @StringRes messageRes: Int,
        args: List<Any> = emptyList(),
        style: Style = Style.INFO,
        undo: (() -> Unit)? = null,
        editAction: (() -> Unit)? = null
    ) {
        _events.postValue(ToastItem(messageRes, args, style, undo, editAction))
    }

    /** UI 渲染完成后调用，清除当前槽位，防止重组时重复展示。 */
    fun consume() {
        _events.postValue(null)
    }
}
