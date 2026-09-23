<script setup lang="ts">
/**
 * 模型对话 - 与「智能体」应用直接对话（流式输出）：
 * 左侧选择智能体、可新建会话，右侧是对话区；发送走 /chat/dialog，模型增量实时上屏。
 *
 * 说明：后端该接口每轮只接收当前输入（没有服务端会话记忆），所以历史消息仅在前端展示，
 * 刷新页面不保留；需要「留历史 + 看执行过程」的场景请用「流程对话」页。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Plus, Promotion, Refresh, Search, VideoPause } from '@element-plus/icons-vue'
import ChatApi from '@/api/agent/ChatApi'
import streams from '@/api/agent/streams'
import ApiUtil from '@/utils/ApiUtil'
import ChatMessage from '@/components/Chat/ChatMessage.vue'
import ChatElevator from '@/components/Chat/ChatElevator.vue'
import useAgenticStream from '@/composables/useAgenticStream'
import useChatScroll from '@/composables/useChatScroll'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
/** 可选智能体：授权给当前登录人的应用列表（/chat/agents） */
const agents = ref<any[]>([])
const agentId = ref<any>('')
const agent = computed<any>(() => agents.value.find((item: any) => String(item.id) === String(agentId.value)) ?? {})

/** 会话消息：仅前端展示（接口不落库、无服务端记忆） */
const messages = ref<any[]>([])
const message = ref('')
const sending = ref(false)
/** 消息区滚动：贴底才跟随新内容（见 composables/useChatScroll） */
const { bodyRef: chatRef, handleScroll, scrollBottom } = useChatScroll()
const keyword = ref('')
const filteredAgents = computed<any[]>(() => {
  const word = String(keyword.value ?? '').trim().toLowerCase()
  if (!word) return agents.value
  return agents.value.filter((item: any) => [item.name, item.description, item.code]
    .some((text: any) => String(text ?? '').toLowerCase().indexOf(word) >= 0))
})

const loadAgents = (warning = false) => ChatApi.agents({ warning }).then((result: any) => {
  const data: any = ApiUtil.data(result)
  agents.value = data?.rows ?? data ?? []
  if (!agentId.value && agents.value.length) agentId.value = agents.value[0].id
}).catch(() => {})

/** 新建会话：清空消息与输入，保留当前选中的智能体 */
const handleNew = () => {
  messages.value = []
  message.value = ''
}

/** 本轮上下文：流式回调按「当前回复」写屏，发送前先登记 */
const turn: { reply: any } = { reply: null }

/**
 * 流式对话：本页接口的报文协议与编排接口不同（`{ action, data }`），
 * 协议适配（parse）登记在 @/api/agent/streams，这里只关心「收到增量怎么画」与收尾。
 */
const stream = useAgenticStream({
  ...streams.chatDialog,
  headers: { 'X-Auth-Token': user.info.token },
  fallback: 'none',
  onDelta: (chunk: any) => {
    const reply: any = turn.reply
    if (!reply) return
    if (chunk.reasoning) reply.reasoning = String(reply.reasoning ?? '') + String(chunk.reasoning)
    if (null != chunk.content) reply.content = String(reply.content ?? '') + String(chunk.content)
    nextTick(() => scrollBottom())
  },
  onError: ({ message }: any) => {
    const reply: any = turn.reply
    if (!reply) return
    reply.streaming = false
    reply.notice = { summary: message || '模型调用失败', detail: '' }
    nextTick(() => scrollBottom())
  },
  onClose: () => {
    if (turn.reply) {
      turn.reply.streaming = false
      turn.reply.createdTime = Date.now()
    }
    sending.value = false
    nextTick(() => scrollBottom())
  },
})

/** 停止生成：中断流式连接，已经输出的内容保留在气泡里（主动中断不算失败，不弹异常提示） */
const handleStop = () => {
  stream.abort()
  if (turn.reply) turn.reply.streaming = false
  sending.value = false
  ElMessage.info('已停止生成')
  nextTick(() => scrollBottom())
}

/** 发送：当前输入交给 /chat/dialog，模型增量实时写进这条回复 */
const handleSend = () => {
  if (sending.value) return
  const text = String(message.value ?? '').trim()
  if (!text) return
  if (!agentId.value) {
    ElMessage.warning('请先选择智能体')
    return
  }
  message.value = ''
  sending.value = true
  messages.value.push({ role: 'user', content: text, createdTime: Date.now() })
  // 助手回复：流式增量直接写在这个对象上（reactive 才能驱动渲染）
  const reply: any = reactive({ role: 'assistant', content: '', reasoning: '', streaming: true, notice: null, createdTime: 0 })
  messages.value.push(reply)
  nextTick(() => scrollBottom())
  // 本轮上下文：发送前登记，回调通过它写屏
  turn.reply = reply
  stream.send({ agentId: agentId.value, input: text })
}

/** 输入框回车：Enter 发送，Shift / Ctrl / Cmd + Enter 换行；中文输入法组词时回车不发送 */
const handleComposerSend = (event: Event | KeyboardEvent) => {
  const key = event as KeyboardEvent
  if (key.isComposing || 229 === key.keyCode) return
  if (key.shiftKey) return
  key.preventDefault()
  handleSend()
}

/** 切换智能体：清空当前会话，避免把上一轮的内容误当成新模型的回答 */
const handleAgentSelect = (item: any) => {
  agentId.value = item.id
  handleNew()
}

onMounted(() => loadAgents())
</script>

<template>
  <el-splitter class="dialog-page">
    <!-- 左侧：智能体选择与新建会话 -->
    <el-splitter-panel class="dialog-aside" size="260px" min="200px" max="460px">
      <el-button class="aside-new" type="primary" plain :icon="Plus" @click="handleNew">新建对话</el-button>
      <div class="aside-filter">
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索智能体" />
      </div>
      <div class="agent-list">
        <div
          class="agent-item"
          :class="{ 'is-active': String(agentId) === String(item.id) }"
          :key="item.id"
          v-for="item in filteredAgents"
          @click="handleAgentSelect(item)">
          <span class="agent-name">{{ item.name }}</span>
          <span class="agent-desc">{{ item.description || item.code || '暂无描述' }}</span>
        </div>
        <el-empty description="没有可用的智能体" :image-size="60" v-if="!filteredAgents.length" />
      </div>
      <el-button class="aside-refresh" link :icon="Refresh" @click="loadAgents(true)">重新载入</el-button>
    </el-splitter-panel>
    <!-- 右侧：对话区 -->
    <el-splitter-panel class="dialog-main">
      <header class="dialog-head">
        <el-avatar class="head-avatar" :icon="MagicStick" />
        <span class="dialog-title">{{ agent.name || '模型对话' }}</span>
        <el-tag class="head-tag" size="small" effect="plain" v-if="agent.code">{{ agent.code }}</el-tag>
      </header>
      <div class="dialog-chat">
        <div class="dialog-body" ref="chatRef" @scroll="handleScroll">
          <ChatMessage
            class="message"
            :key="index"
            v-for="(item, index) in messages"
            :item="item"
            :streaming="item.streaming"
            :feedback="false" />
          <el-empty class="dialog-empty" description="选择智能体后开始对话吧" :image-size="80" v-if="!messages.length" />
        </div>
        <!-- 电梯导航：按用户消息生成右侧横条，点击定位到对应气泡 -->
        <ChatElevator
          :target="chatRef"
          :revision="messages.length"
          selector="[data-chat-role='user']"
          text-selector="[data-chat-text]" />
      </div>
      <footer class="dialog-composer">
        <div class="composer-box">
          <el-input
            class="composer-input"
            v-model="message"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 8 }"
            resize="none"
            placeholder="请输入内容（Enter 发送，Shift+Enter 换行）"
            @keydown.enter="handleComposerSend" />
          <div class="composer-tools">
            <span class="composer-tips">Enter 发送，Shift + Enter 换行</span>
            <el-button
              class="composer-send"
              circle
              :type="sending ? 'danger' : 'primary'"
              :icon="sending ? VideoPause : Promotion"
              :title="sending ? '停止生成' : '发送'"
              :disabled="!sending && !String(message ?? '').trim()"
              @click="sending ? handleStop() : handleSend()" />
          </div>
        </div>
      </footer>
    </el-splitter-panel>
  </el-splitter>
</template>

<style lang="scss" scoped>
/**
 * 布局与「流程对话」页保持同一套口径：左侧智能体列表用浅灰底、右侧对话区白色；
 * 消息列固定宽度居中，气泡与输入框共用同一条竖线。
 */
$dialog-column: 860px;
$dialog-avatar: 26px;
$dialog-gap: 10px;
$dialog-inset: $dialog-avatar + $dialog-gap;
$dialog-min: 600px;
$dialog-max: 788px;
@mixin dialog-column($min, $max) {
  width: 100%;
  min-width: $min;
  max-width: $max;
  margin-left: auto;
  margin-right: auto;
}
.dialog-page {
  height: 100%;
  min-height: 0;
  background: var(--el-bg-color);
  :deep(.el-splitter-panel) {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }
  :deep(.dialog-aside) {
    min-width: 0;
    padding: 16px;
    background: var(--fs-layout-background-color);
  }
  :deep(.dialog-main) {
    min-width: 0;
    background: var(--el-bg-color);
  }
}
/* 左侧：智能体列表与新建会话 */
.dialog-aside {
  .aside-new {
    flex: none;
    width: 100%;
    border-radius: 6px;
  }
  .aside-filter {
    flex: none;
    width: 100%;
    margin-top: 8px;
    .el-input {
      width: 100%;
    }
  }
  .agent-list {
    flex: 1;
    min-height: 0;
    margin-top: 8px;
    overflow-x: hidden;
    overflow-y: auto;
    .agent-item {
      padding: 8px 10px;
      border-radius: 8px;
      border: solid 1px transparent;
      cursor: pointer;
      transition: background-color 0.2s, border-color 0.2s;
      & + .agent-item {
        margin-top: 6px;
      }
      &:hover {
        background: var(--el-bg-color);
      }
      &.is-active {
        background: var(--el-bg-color);
        border-color: var(--el-color-primary-light-7);
        box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
        .agent-name {
          color: var(--el-color-primary);
        }
      }
      .agent-name {
        display: block;
        font-size: 13px;
        color: var(--el-text-color-regular);
        @include text-wrap();
      }
      .agent-desc {
        display: block;
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-placeholder);
        @include text-wrap();
      }
    }
  }
  .aside-refresh {
    flex: none;
    margin-top: 8px;
  }
}
/* 右侧：顶栏 + 消息区 + 底部输入区 */
.dialog-main {
  .dialog-head {
    flex: none;
    @include flex-start();
    gap: 8px;
    padding: 10px 16px;
    border-bottom: solid 1px var(--el-border-color-lighter);
    .head-avatar {
      flex: none;
      width: 26px;
      height: 26px;
      font-size: 14px;
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    .dialog-title {
      min-width: 0;
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      @include text-wrap();
    }
    .head-tag {
      flex: none;
    }
  }
  /* 消息区外层：给右侧电梯导航提供定位基准（导航不随消息滚动） */
  .dialog-chat {
    position: relative;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
  .dialog-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    padding: 16px 40px 8px;
    overflow-x: hidden;
    overflow-y: auto;
    scrollbar-gutter: stable both-edges;
    /* 消息行：结构与通用样式在 components/Chat/ChatMessage.vue，这里只保留本页自己的列宽与观感 */
    .message {
      @include dialog-column($dialog-min + $dialog-inset * 2, $dialog-column);
      flex: none;
      gap: $dialog-gap;
      & + .message {
        margin-top: 16px;
      }
      /* 头像随内容列排列，尺寸按本页列宽给 */
      :deep(.chat-avatar) {
        margin-top: 2px;
        width: $dialog-avatar;
        height: $dialog-avatar;
        font-size: 13px;
      }
      /* 气泡宽度＝内容列宽度，不越过两侧头像那条竖线 */
      :deep(.chat-bubble) {
        flex: 1 1 auto;
        max-width: $dialog-max;
        padding: 8px 12px;
        border-radius: 10px;
        border: none;
        background: var(--el-fill-color-light);
      }
      &.is-user :deep(.chat-bubble) {
        flex: 0 1 auto;
        background: var(--el-color-primary-light-9);
      }
      /* 折叠面板头：默认 48px 对气泡内的过程信息太高，压到 22px */
      :deep(.chat-reasoning.el-collapse) {
        --el-collapse-header-height: 22px;
      }
      /* 思考内容底部留白比通用值更紧，气泡更紧凑 */
      :deep(.chat-reasoning .el-collapse-item__content) {
        padding-bottom: 2px;
      }
      /* 回复工具条：仅靠间距与正文分隔 */
      :deep(.chat-toolbar) {
        margin-top: 8px;
      }
    }
    .dialog-empty {
      margin: auto 0;
    }
  }
  /* 底部输入区：与消息列同宽，聚焦时描边高亮 */
  .dialog-composer {
    flex: none;
    width: 100%;
    padding: 12px 76px 16px;
    background: var(--el-bg-color);
    .composer-box {
      @include dialog-column($dialog-min, $dialog-max);
      padding: 8px 12px;
      border-radius: 10px;
      border: solid 1px var(--el-border-color);
      background: var(--el-bg-color);
      transition: border-color 0.2s, box-shadow 0.2s;
      &:hover {
        border-color: var(--el-border-color-darker);
      }
      &:focus-within {
        border-color: var(--el-color-primary);
        box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
      }
    }
    .composer-input {
      width: 100%;
      :deep(.el-textarea__inner) {
        padding: 0;
        border: none;
        box-shadow: none;
        background: transparent;
        font-size: 13px;
        line-height: 1.7;
      }
    }
    .composer-tools {
      @include flex-between();
      gap: 8px;
      margin-top: 4px;
      .composer-tips {
        min-width: 0;
        font-size: 12px;
        color: var(--el-text-color-placeholder);
        @include text-wrap();
      }
      .composer-send {
        flex: none;
      }
    }
  }
}
</style>
