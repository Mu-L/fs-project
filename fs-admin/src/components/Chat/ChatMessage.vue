<script setup lang="ts">
/**
 * 消息气泡 - 对话三页（流程对话 / 对话历史详情 / 模型对话）共用的一行消息：
 * 头像 + 气泡（思考过程、正文与图表、空回复占位、输出状态、工具条）+ 气泡外的异常图标。
 *
 * 页面差异走插槽：`#steps`（执行过程：时间线 / 执行明细）、`#extra`（页面自有的补充内容，如模型的原始输出）；
 * 视觉差异走页面覆盖：气泡尺寸与配色、头像尺寸等由页面用 `:deep()` 覆盖，组件这里只给统一底座。
 *
 * @prop {Object}  item      消息对象（role / content / reasoning / notice / streaming / progress / ...）
 * @prop {Boolean} streaming 是否正在流式输出；不传时取 item.streaming（历史数据没有该字段时为 false）
 * @prop {Boolean} avatar    是否展示头像（对话历史详情页不需要，传 false）
 * @prop {Boolean} disabled  反馈按钮是否禁用（反馈正在提交）
 * @prop {Boolean} feedback  是否显示点赞/点踩，默认显示（接口不落库的页面传 false）
 * @prop {Boolean} time      是否在用户气泡里显示时间，默认不显示（助手的时间在工具条里）
 * @attr {String} data-chat-role 消息角色（挂在行根元素上，供电梯导航等按角色定位）
 * @attr {String} data-chat-text 用户消息正文（同上，供取摘要文本）
 * @slot steps 气泡顶部：执行过程（流程对话的时间线、其它页的执行明细）
 * @slot extra 气泡底部：页面自有的补充内容（模型对话的原始输出等）
 * @emits submit 反馈提交：{ emotion, tag, content }
 */
import { computed } from 'vue'
import { MagicStick, UserFilled } from '@element-plus/icons-vue'
import ChatLoading from '@/components/Chat/ChatLoading.vue'
import ChatNotice from '@/components/Chat/ChatNotice.vue'
import ChatToolbar from '@/components/Chat/ChatToolbar.vue'
import DataResultChart from '@/components/Data/DataResultChart.vue'
import MarkdownEditor from '@/components/Editor/MarkdownEditor.vue'
import AgenticUtil from '@/utils/AgenticUtil'
import DateUtil from '@/utils/DateUtil'

const props = withDefaults(defineProps<{
  item: any,
  streaming?: boolean,
  avatar?: boolean,
  disabled?: boolean,
  feedback?: boolean,
  time?: boolean,
}>(), {
  streaming: undefined,
  avatar: true,
  disabled: false,
  feedback: true,
  time: false,
})

const emit = defineEmits<{ submit: [payload: any] }>()

const isUser = computed(() => 'user' === props.item?.role)
/** 用户消息时间：口径与工具条里的助手时间一致（createdTime 决定显隐） */
const timeText = computed(() => (props.item?.createdTime ? DateUtil.format(props.item.createdTime) : ''))
const isAssistant = computed(() => 'assistant' === props.item?.role)
/** 独立异常行：没有对应消息时的整行提示 */
const isError = computed(() => 'error' === props.item?.role)
const isStreaming = computed(() => props.streaming ?? !!props.item?.streaming)
/** 回复分段：文本走 Markdown，图表按「图表占位符」落到指定位置（未引用的追加在末尾） */
const parts = computed<any[]>(() => AgenticUtil.replyParts(props.item) ?? [])
/** 空回复：没有正文也没有图表，给个占位避免只剩一个空气泡 */
const empty = computed(() => isAssistant.value && !isStreaming.value && !props.item?.content && !parts.value.length)

/**
 * 流式状态文案：有节点在执行时显示节点名，
 * 否则按内容判断「正在思考」还是「正在输出」。
 */
const streamingText = computed(() => {
  const running: any = (props.item?.progress ?? []).find((step: any) => 'running' === step?.state)
  if (running) return `正在执行：${running.name || running.id}`
  return props.item?.content ? '正在输出…' : '正在思考…'
})
</script>

<template>
  <div class="chat-message" :class="['is-' + item?.role, { 'is-error': isError }]" :data-chat-role="item?.role">
    <!-- 独立异常提示（没有对应消息时）：图标 + 告警气泡 -->
    <template v-if="isError">
      <ChatNotice :notice="item.notice" />
      <div class="chat-bubble notice-bubble">{{ item.notice?.summary }}</div>
    </template>
    <template v-else>
      <el-avatar class="chat-avatar" v-if="avatar" :icon="isUser ? UserFilled : MagicStick" />
      <div class="chat-bubble">
        <!-- 执行过程（页面自定义）：流程对话的时间线置顶，其它页的执行明细按各自位置传入 -->
        <slot name="steps" />
        <!-- 思考过程：流式时直接展示，输出完成后收进折叠面板（与调试面板一致） -->
        <div class="chat-reasoning-text" v-if="isAssistant && item.reasoning && isStreaming">{{ item.reasoning }}</div>
        <el-collapse class="chat-reasoning" v-else-if="isAssistant && item.reasoning">
          <el-collapse-item title="思考过程">
            <div class="chat-reasoning-text">{{ item.reasoning }}</div>
          </el-collapse-item>
        </el-collapse>
        <!-- 用户提问保持原文 -->
        <div class="chat-content" v-if="isUser" data-chat-text>{{ item.content }}</div>
        <!-- 用户消息时间：与助手时间一样贴在气泡底部（可选，按页面口径打开） -->
        <div class="chat-time" v-if="isUser && time && timeText">{{ timeText }}</div>
        <!-- 助手回复按 Markdown 渲染
             （显式判断角色：上面插了用户时间后，这里若继续用 v-else 就会串到时间那个 v-if 上，
              非历史页会因此把用户消息再按助手回复渲染一遍） -->
        <template v-if="isAssistant">
          <template :key="partIndex" v-for="(part, partIndex) in parts">
            <MarkdownEditor class="chat-markdown" v-if="'text' === part.type" :model-value="part.text" readonly />
            <DataResultChart
              class="chat-chart"
              v-else
              :type="part.chart.type"
              :title="part.chart.title"
              :source="part.chart.source"
              :categories="part.chart.categories"
              :series="part.chart.series" />
          </template>
        </template>
        <!-- 没有正文的异常轮次（如角色授权被撤销）：失败原因直接显示在气泡里，不必点图标才知道原因 -->
        <div class="chat-notice-text" v-if="isAssistant && item.notice && !item.content && !isStreaming">
          {{ item.notice.summary }}
        </div>
        <!-- 空回复：给出占位，避免只剩一个空气泡 -->
        <div class="chat-content" v-else-if="empty">（无回复内容）</div>
        <!-- 流式输出中：内容下方显示输出状态 -->
        <div class="chat-streaming" v-if="isAssistant && isStreaming">
          <ChatLoading />
          <span>{{ streamingText }}</span>
        </div>
        <!-- 回复工具条：复制 / 反馈 / 时间（空回复与流式中不展示） -->
        <ChatToolbar
          class="chat-toolbar"
          v-if="isAssistant && !isStreaming && (item.content || parts.length)"
          :item="item"
          :disabled="disabled"
          :feedback="feedback"
          @submit="(payload: any) => emit('submit', payload)" />
        <!-- 页面自有的补充内容 -->
        <slot name="extra" />
      </div>
      <!-- 节点回复的异常图标：排在回复气泡右侧 -->
      <ChatNotice v-if="!isUser" :notice="item.notice" />
      <!-- 请求级异常（必填缺失等）：用户消息行方向反转，DOM 在后即视觉在气泡左侧 -->
      <ChatNotice v-if="isUser" :notice="item.notice" />
    </template>
  </div>
</template>

<style lang="scss" scoped>
/* 消息行：头像与气泡同排、间距一致；用户消息整行反向（头像在右、气泡贴右） */
.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  & + .chat-message {
    margin-top: 10px;
  }
  &.is-user {
    flex-direction: row-reverse;
  }
  /* 独立异常提示（没有对应消息时）：气泡用告警色 */
  &.is-error {
    align-items: center;
    gap: 6px;
    .notice-bubble {
      color: var(--el-color-danger);
      border-color: var(--el-color-danger-light-5);
      background: var(--el-color-danger-light-9);
      font-size: 12px;
    }
  }
}
/* 头像：尺寸与配色给出默认值，页面可按自己的列宽覆盖 */
.chat-message.is-assistant .chat-avatar {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.chat-message.is-user .chat-avatar {
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-dark);
}
.chat-avatar {
  flex: none;
  margin-top: 2px;
  width: 26px;
  height: 26px;
  font-size: 13px;
}
/* 气泡：尺寸与配色随页面（对话页扁平、历史/模型页带边框），这里只给统一的内边距与排版 */
.chat-bubble {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 6px;
  text-align: left;
  border: solid 1px var(--el-border-color-lighter);
  background: var(--el-bg-color);
}
.chat-message.is-user .chat-bubble {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}
.chat-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
/* 用户消息时间：靠右、浅灰，与工具条里的时间同一观感 */
.chat-time {
  margin-top: 4px;
  text-align: right;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
/* 没有正文的异常轮次：气泡里直接给出失败原因 */
.chat-notice-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-color-danger);
  word-break: break-word;
}
/* 思考过程：折叠面板不带上下边框，流式时直接展示文本 */
.chat-reasoning {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  &.el-collapse {
    border-top: none;
    border-bottom: none;
    /* 气泡底色上折叠头不再铺白底，避免出现横向色块 */
    --el-collapse-header-bg-color: transparent;
    --el-collapse-content-bg-color: transparent;
  }
  :deep(.el-collapse-item__header) {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    border-bottom: none;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 4px;
  }
}
.chat-reasoning-text {
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  /* 思考过程与回复内容之间留一点间距 */
  & + .chat-markdown,
  & + .chat-content {
    margin-top: 6px;
  }
}
/* 助手回复：Markdown 渲染后字体与气泡正文保持一致 */
.chat-markdown {
  font-size: 13px;
  :deep(.fs-markdown-preview) {
    padding: 0;
    font-size: 13px;
  }
}
/* 最终回复里的图表：与回复内容之间留白 */
.chat-chart {
  margin-top: 8px;
}
/* 流式输出状态：跟在内容下方，与正文留出间距；内容为空时作为首行不加上边距 */
.chat-streaming {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);
  &:first-child {
    margin-top: 0;
  }
}
/* 回复工具条：只靠间距与正文分隔，时间靠右 */
.chat-toolbar {
  margin-top: 6px;
}
</style>
