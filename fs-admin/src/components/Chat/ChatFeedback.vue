<script setup lang="ts">
/**
 * 消息反馈 - 助手回复的点赞/点踩（点踩时可附原因标签与补充说明）。
 * 组件只负责收集反馈，落库与回显由调用方通过 submit 事件接管。
 * @prop {String} emotion  当前情绪：positive-赞，negative-踩，空表示未反馈
 * @prop {String} tag      当前反馈标签（点踩原因）
 * @prop {String} content  当前反馈补充说明
 * @prop {Boolean} disabled 禁用交互（如请求中）
 * @prop {Array} options   点踩原因候选，默认使用通用原因
 * @emits submit {emotion, tag, content} 提交反馈；emotion 传 cancel 表示取消
 */
import { ref } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const {
  emotion = '',
  tag = '',
  content = '',
  disabled = false,
  // 默认值必须是字面量：解构默认值会被提升到 setup 之外，不能引用本地变量
  options = ['答非所问', '内容有误', '信息不全', '格式混乱', '其他'],
} = defineProps<{
  emotion?: string
  tag?: string
  content?: string
  disabled?: boolean
  options?: string[]
}>()
const emit = defineEmits<{ submit: [payload: { emotion: string, tag: string, content: string }] }>()

// 点踩原因面板：由 Element Plus 的弹出层承载，展开时把已保存的反馈回填到表单
const panel = ref(false)
const form = ref({ tag: '', content: '' })
const handleShow = () => {
  form.value = { tag: tag || '', content: content || '' }
}
// 原因用标签直接点选：再点一次取消选中，也不会像下拉那样把弹出层一起关掉
const handleTag = (value: string) => {
  form.value.tag = form.value.tag === value ? '' : value
}

// 点赞：已赞时再次点击表示取消
const handleLike = () => {
  if (disabled) return
  emit('submit', { emotion: 'positive' === emotion ? 'cancel' : 'positive', tag: '', content: '' })
}
const handleSubmit = () => {
  panel.value = false
  emit('submit', { emotion: 'negative', tag: form.value.tag, content: form.value.content })
}
// 取消反馈：仅点踩后可见，撤回已提交的反馈
const handleCancelFeedback = () => {
  panel.value = false
  emit('submit', { emotion: 'cancel', tag: '', content: '' })
}
</script>

<template>
  <div class="chat-feedback">
    <layout-icon
      class="feedback-icon"
      :class="{ 'is-positive': 'positive' === emotion }"
      name="chat.thumbUp"
      :title="'positive' === emotion ? '取消点赞' : '点赞'"
      @click="handleLike" />
    <el-popover v-model:visible="panel" trigger="click" :width="280" placement="top-end" :teleported="true" :disabled="disabled" @show="handleShow">
      <template #reference>
        <span class="feedback-trigger">
          <layout-icon
            class="feedback-icon"
            :class="{ 'is-negative': 'negative' === emotion }"
            name="chat.thumbDown"
            :title="'negative' === emotion ? '已反馈，可修改' : '没帮助'" />
        </span>
      </template>
      <div class="chat-feedback-panel">
        <div class="feedback-tips">选择原因（选填）</div>
        <div class="feedback-tags">
          <el-tag
            class="feedback-tag"
            :class="{ 'is-active': form.tag === item }"
            :effect="form.tag === item ? 'dark' : 'plain'"
            :key="item"
            size="small"
            v-for="item in options"
            @click="handleTag(item)">{{ item }}</el-tag>
        </div>
        <el-input class="feedback-content" v-model="form.content" type="textarea" :rows="2" size="small" placeholder="补充说明（选填）" />
        <div class="feedback-actions">
          <el-button size="small" link type="danger" v-if="'negative' === emotion" @click="handleCancelFeedback">取消反馈</el-button>
          <el-button size="small" @click="panel = false">取消</el-button>
          <el-button size="small" type="primary" @click="handleSubmit">提交</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<style lang="scss" scoped>
.chat-feedback {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  line-height: 1;
  vertical-align: middle;
  .feedback-trigger {
    display: inline-flex;
  }
  .feedback-icon {
    color: var(--el-text-color-placeholder);
    cursor: pointer;
    &:hover {
      color: var(--el-color-primary);
    }
    &.is-positive {
      color: var(--el-color-success);
    }
    &.is-negative {
      color: var(--el-color-danger);
    }
  }
}
</style>

<style lang="scss">
/* 弹出层内容挂载在 body 上，样式需全局作用域（类名已加前缀避免污染） */
.chat-feedback-panel {
  .feedback-tips {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  /* 原因标签：点选切换，选中态为深色实心 */
  .feedback-tags {
    @include flex-wrap();
    gap: 6px;
    margin-top: 6px;
    .feedback-tag {
      cursor: pointer;
      user-select: none;
    }
  }
  .feedback-content {
    margin-top: 8px;
  }
  .feedback-actions {
    margin-top: 8px;
    text-align: right;
  }
}
</style>
