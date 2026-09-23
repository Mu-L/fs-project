<script setup lang="ts">
/**
 * 复制按钮 - 统一的复制交互：复制成功后图标切换为对号，3.5 秒后自动恢复。
 * 复制内容可以直接传字符串，也可以传复制回调（如变量编辑器的复制方法）。
 * @prop {String}   content - 要复制的文本
 * @prop {Function} copy    - 自定义复制逻辑，返回 boolean 或 Promise<boolean>（false 视为未复制）
 * @prop {String}   title   - 悬浮提示，默认「复制」，复制成功后显示「已复制」
 * @prop {String}   text    - 按钮文案，传了则渲染为「图标 + 文案」
 * @prop {String}   size    - 按钮尺寸，默认 small
 * @prop {Boolean}  link    - 是否使用 link 样式，默认 true
 * @emits copied - 复制成功
 * @example
 * <ButtonCopy :content="item.content" title="复制回复内容" />
 * <ButtonCopy :copy="editorRef?.copy" />
 */
import { onBeforeUnmount, ref } from 'vue'
import { Check, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FormUtil from '@/utils/FormUtil'

// 复制成功图标展示时长：与其它模块保持一致，3.5 秒后自动恢复
const CopiedDuration = 3500
const {
  content = '',
  copy,
  title = '复制',
  text = '',
  size = 'small',
  link = true,
} = defineProps<{
  content?: string,
  copy?: Function,
  title?: string,
  text?: string,
  size?: string,
  link?: boolean,
}>()
const emit = defineEmits(['copied'])

const copied = ref(false)
let copiedTimer: any = null

/** 触发复制：失败只提示不改变图标状态，成功则展示 3.5 秒对号 */
const handleCopy = () => {
  const action = 'function' === typeof copy
    ? Promise.resolve(copy())
    : FormUtil.copyToClipboard(String(content ?? ''))
  action.then((result: any) => {
    if (false === result) return
    copied.value = true
    emit('copied')
    window.clearTimeout(copiedTimer)
    copiedTimer = window.setTimeout(() => { copied.value = false }, CopiedDuration)
  }).catch(() => {
    ElMessage.warning('复制失败，请手动选择复制')
  })
}

onBeforeUnmount(() => window.clearTimeout(copiedTimer))
</script>

<template>
  <el-button
    class="button-copy"
    :class="{ 'is-copied': copied }"
    :link="link"
    :size="size as any"
    :icon="copied ? Check : DocumentCopy"
    :title="copied ? '已复制' : title"
    @click="handleCopy">
    <span v-if="text">{{ text }}</span>
  </el-button>
</template>

<style lang="scss" scoped>
/* 复制成功后图标变绿，3.5 秒后自动恢复为复制图标 */
.button-copy.is-copied {
  color: var(--el-color-success);
}
</style>
