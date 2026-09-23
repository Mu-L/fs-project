<script setup lang="ts">
/**
 * 长文本块 - 执行过程里的入参 / 调用链 / 节点输出与排查用的原始数据统一展示方式：
 * - 短文本照旧原样展示（不改变现有观感）；
 * - 超长文本只给几行摘要 + 「复制 / 展开」，全文放到可关闭的独立浮层里查看，
 *   既不会把执行过程撑成几万行，也不引入页面内的嵌套滚动条。
 *
 * @prop {String} label   块标题（渲染在文本上方，与排查视图的其它小标题一致）
 * @prop {Any}    text    纯文本
 * @prop {Any}    json    JSON 字符串：能解析就缩进美化，否则原样展示
 * @prop {Any}    value   对象 / 数组：按 JSON 缩进展示
 * @prop {Number} rows    超过该行数即折叠，默认 12
 * @prop {Number} chars   超过该字符数即折叠，默认 1000
 * @prop {Number} preview 摘要展示的行数，默认 3
 * @example
 * <chat-text-block label="解析后入参" :value="step.request" />
 * <chat-text-block label="调用参数" :json="call.args" />
 */
import { computed, ref } from 'vue'
import { View } from '@element-plus/icons-vue'
import ButtonCopy from '@/components/Button/ButtonCopy.vue'

const {
  label = '',
  text = '',
  json = '',
  value = undefined,
  rows = 12,
  chars = 1000,
  preview = 3,
} = defineProps<{
  label?: string
  text?: any
  json?: any
  value?: any
  rows?: number
  chars?: number
  preview?: number
}>()

/** 展开的浮层是否打开 */
const open = ref(false)

/** 值 → 文本：字符串原样；对象 / 数组按 JSON 缩进（避免直接 String() 出 [object Object]） */
const asText = (input: any) => {
  if (null === input || undefined === input) return ''
  if ('string' === typeof input) return input
  if ('object' === typeof input) return JSON.stringify(input, null, 2)
  return String(input)
}

/** JSON 美化：能解析就缩进展示，解析失败（普通文本）原样返回 */
const pretty = (raw: any) => {
  const source = asText(raw)
  if (!source) return ''
  try {
    return JSON.stringify(JSON.parse(source), null, 2)
  } catch (error) {
    return source
  }
}

/** 正文：纯文本优先，其次 JSON（字符串或对象），最后对象/数组（都为空时为空串） */
const body = computed(() => {
  if (undefined !== text && '' !== text && null !== text) return asText(text)
  if (undefined !== json && '' !== json && null !== json) return pretty(json)
  if (undefined !== value && null !== value) return JSON.stringify(value, null, 2)
  return ''
})

/** 完整内容：复制与浮层都用它，保证复制到的是原文（标题在文本区域外面） */
const full = computed(() => body.value)
const lineCount = computed(() => (full.value ? full.value.split('\n').length : 0))
const charCount = computed(() => full.value.length)
/** 是否算「超长」：超过行数或字符数阈值就收成摘要 */
const long = computed(() => lineCount.value > rows || charCount.value > chars)

/** 摘要：取前几行有内容的行（默认 3 行）并限长，多出的部分由样式截断 */
const brief = computed(() => {
  const lines = full.value.split('\n').filter((row: string) => !!row.trim())
  const head = lines.slice(0, Math.max(1, preview)).join('\n')
  const text = head.length > 240 ? head.slice(0, 240) : head
  return lines.length > Math.max(1, preview) || head.length > 240 ? `${text}…` : text
})
</script>

<template>
  <div class="text-block">
    <div class="text-label" v-if="label">{{ label }}</div>
    <!-- 短文本：与原来一致，直接展示 -->
    <pre class="text-body" v-if="!long">{{ full }}</pre>
    <!-- 超长文本：顶部一行统计与操作 + 几行摘要，全文进独立浮层 -->
    <div class="text-summary" :style="{ '--text-preview-lines': preview }" v-else>
      <div class="text-summary-head">
        <span class="text-meta">{{ lineCount }} 行 · {{ charCount }} 字符</span>
        <span class="text-actions">
          <ButtonCopy :content="full" title="复制完整内容" />
          <el-button link size="small" :icon="View" @click="open = true">展开</el-button>
        </span>
      </div>
      <div class="text-brief">{{ brief }}</div>
    </div>
    <!-- 独立查看区：可关闭，全文在这里滚动，不占用执行过程的版面 -->
    <el-dialog
      class="text-dialog"
      v-model="open"
      :title="label || '完整内容'"
      width="76%"
      top="6vh"
      append-to-body
      destroy-on-close>
      <div class="text-dialog-head">
        <span class="text-meta">{{ lineCount }} 行 · {{ charCount }} 字符</span>
        <ButtonCopy :content="full" title="复制完整内容" text="复制全文" />
      </div>
      <pre class="text-full">{{ full }}</pre>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
/* 小标题：与排查视图里其它 step-label 同一观感 */
.text-label {
  margin: 6px 0 2px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
/* 短文本：沿用执行过程里原来的文本块样式 */
.text-body {
  margin: 2px 0 0;
  padding: 10px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  /* 超长无空格串（压缩 JSON / Base64）也能换行，且不会切碎英文单词 */
  overflow-wrap: anywhere;
}
/* 超长文本：顶部一行统计与操作，下面是几行摘要（与展开浮层的头部保持一致） */
.text-summary {
  position: relative;
  /* 让底部渐变贴合圆角 */
  overflow: hidden;
  margin-top: 2px;
  padding: 6px 10px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 12px;
  line-height: 1.6;
  /* 底部阴影遮罩：颜色与块底色一致，渐隐出「下面还有内容」的观感 */
  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 22px;
    pointer-events: none;
    background: linear-gradient(to bottom, transparent, var(--el-fill-color-light));
  }
  .text-brief {
    /* 摘要展示前几行（行数由 --text-preview-lines 决定），多出的部分截断 */
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: var(--text-preview-lines, 3);
    overflow: hidden;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    color: var(--el-text-color-regular);
  }
  .text-summary-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 4px;
    .text-meta {
      color: var(--el-text-color-placeholder);
    }
    .text-actions {
      flex: none;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}
.text-meta {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
/* 浮层内：全文自己滚动，等宽字体保留缩进 */
.text-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.text-full {
  margin: 0;
  padding: 10px;
  max-height: 70vh;
  overflow: auto;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  tab-size: 2;
}
</style>
