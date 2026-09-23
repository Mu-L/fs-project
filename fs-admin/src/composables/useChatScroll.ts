/**
 * 对话消息区滚动：贴近底部时才跟随新内容，向上翻阅历史时不被强行拉回。
 *
 * 各对话页原来各写一份（流程对话带「贴底」判断，模型对话与调试抽屉只有强制到底），
 * 统一到这里后行为一致：向上看历史时新消息不再把人拽回底部。
 *
 * @param {Number} bottomGap 距底部多少像素内算「贴底」，默认 60
 * @returns {{ bodyRef, pinned, handleScroll, scrollBottom }}
 * @example
 * const { bodyRef: chatRef, handleScroll, scrollBottom } = useChatScroll()
 * <div class="dialog-body" ref="chatRef" @scroll="handleScroll">…</div>
 * scrollBottom(true)   // 切会话 / 发送后强制到底
 */
import { ref } from 'vue'

export default function useChatScroll(bottomGap = 60) {
  /** 消息列表容器（模板 ref 用） */
  const bodyRef = ref<HTMLDivElement>()
  /** 是否停留在底部附近 */
  const pinned = ref(true)

  const handleScroll = () => {
    const element = bodyRef.value
    if (!element) return
    pinned.value = element.scrollHeight - element.scrollTop - element.clientHeight < bottomGap
  }

  const scrollBottom = (force = false) => {
    const element = bodyRef.value
    if (!element) return
    if (!force && !pinned.value) return
    element.scrollTop = element.scrollHeight
    pinned.value = true
  }

  return { bodyRef, pinned, handleScroll, scrollBottom }
}
