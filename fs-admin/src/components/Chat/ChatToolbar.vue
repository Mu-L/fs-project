<script setup lang="ts">
/**
 * 助手回复的工具条：复制正文 + 点赞/点踩 + 反馈标签 + 时间。
 *
 * - 对话页、对话历史详情、调试抽屉共用，避免三套「复制/反馈/时间」写法；
 * - 只负责展示与交互，落库由调用方处理：反馈提交时 emit submit；
 * - 时间按 `createdTime` 决定显隐（调试抽屉的消息没有该字段，自然不显示时间）；
 * - `feedback` 为 false 时只留复制与时间（消息不落库、无法反馈的场景，如「模型对话」页）；
 * - 间距/颜色由调用方通过 class 传入（各页面与正文的距离不同）。
 *
 * @prop {Object} item      助手消息（取 content / feedbackEmotion / feedbackTag / feedbackContent / createdTime）
 * @prop {Boolean} disabled 反馈提交中（避免重复点击）
 * @prop {Boolean} feedback 是否显示点赞/点踩，默认显示
 * @emits submit {emotion, tag, content} 反馈提交；emotion 传 cancel 表示取消
 * @example
 * <chat-toolbar class="foot" :item="item" :disabled="feeding === item.id" @submit="(payload) => handleFeedback(item, payload)" />
 */
import ButtonCopy from '@/components/Button/ButtonCopy.vue'
import ChatFeedback from '@/components/Chat/ChatFeedback.vue'
import AgenticUtil from '@/utils/AgenticUtil'
import DateUtil from '@/utils/DateUtil'

const {
  item = {},
  disabled = false,
  feedback = true,
} = defineProps<{
  item?: any
  disabled?: boolean
  feedback?: boolean
}>()
const emit = defineEmits(['submit'])
</script>

<template>
  <div class="chat-toolbar">
    <!-- 复制的是原始输出内容（含换行的原文），不是渲染后的富文本 -->
    <ButtonCopy :content="AgenticUtil.answerText(item.content)" title="复制回复内容" />
    <ChatFeedback
      v-if="feedback"
      :emotion="item.feedbackEmotion"
      :tag="item.feedbackTag"
      :content="item.feedbackContent"
      :disabled="disabled"
      @submit="(payload: any) => emit('submit', payload)" />
    <!-- 点踩原因：回显在工具条上，一眼能看出这条回复被反馈过什么 -->
    <el-tag class="toolbar-tag" type="danger" size="small" effect="plain" v-if="'negative' === item.feedbackEmotion && item.feedbackTag">
      {{ item.feedbackTag }}
    </el-tag>
    <span class="toolbar-time" v-if="item.createdTime">{{ DateUtil.format(item.createdTime) }}</span>
  </div>
</template>

<style lang="scss" scoped>
.chat-toolbar {
  @include flex-start();
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  .toolbar-tag {
    flex: none;
  }
  /* 时间靠右：反馈标签与时间之间自动留白 */
  .toolbar-time {
    margin-left: auto;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
  }
}
</style>
