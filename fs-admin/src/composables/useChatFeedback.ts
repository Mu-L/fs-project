/**
 * 消息反馈：点赞 / 点踩的提交与「提交中」状态（流程对话、对话历史、调试抽屉共用）。
 *
 * - 没有 id 的消息（尚未落库）不发请求，只提示；
 * - 提交成功后把后端算好的情绪 / 标签 / 内容写回消息对象，页面不用关心字段名。
 *
 * @param {String} emptyHint 未落库消息的提示文案（各页措辞略有不同）
 * @returns {{ feeding: Ref<Number>, submit: (item: any, payload: any) => void }}
 * @example
 * const { feeding, submit: handleFeedback } = useChatFeedback()
 * <chat-toolbar :disabled="feeding === item.id" @submit="(payload) => handleFeedback(item, payload)" />
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AgenticApi from '@/api/agent/AgenticApi'
import ApiUtil from '@/utils/ApiUtil'
import type { ChatMessageItem } from '@/types/agent'

export default function useChatFeedback(emptyHint = '消息尚未落库，暂时无法反馈') {
  /** 正在提交反馈的消息标识：避免同一条消息重复提交 */
  const feeding = ref(0)

  const submit = (item: ChatMessageItem, payload: any) => {
    if (!item?.id) {
      ElMessage.warning(emptyHint)
      return
    }
    feeding.value = item.id
    AgenticApi.chatFeedback({ id: item.id, ...payload }, { success: true }).then((result: any) => {
      const data: any = ApiUtil.data(result) ?? {}
      item.feedbackEmotion = data.feedbackEmotion ?? ''
      item.feedbackTag = data.feedbackTag ?? ''
      item.feedbackContent = data.feedbackContent ?? ''
    }).catch(() => {}).finally(() => {
      feeding.value = 0
    })
  }

  return { feeding, submit }
}
