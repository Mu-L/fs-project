<script setup lang="ts">
/**
 * 异常提示图标 - 气泡旁边的红色图标，点击弹出异常详情（摘要 + 详情文本）。
 * 传 notice 为空时不渲染，调用方无需额外判断。
 * @prop {Object} notice      - `{ summary, detail }`
 * @prop {String} popperClass - 弹出层类名，默认 chat-notice-popper
 */
import { CircleCloseFilled } from '@element-plus/icons-vue'

const { notice, popperClass = 'chat-notice-popper' } = defineProps<{
  notice?: any,
  popperClass?: string,
}>()
</script>

<template>
  <el-popover v-if="notice" trigger="click" :width="420" placement="top" :popper-class="popperClass">
    <template #reference>
      <el-icon class="notice-icon"><CircleCloseFilled /></el-icon>
    </template>
    <div class="notice-detail">
      <div class="notice-detail-head">{{ notice.summary }}</div>
      <pre v-if="notice.detail">{{ notice.detail }}</pre>
    </div>
  </el-popover>
</template>

<style lang="scss" scoped>
/* 气泡旁的异常图标：不参与伸缩，垂直居中于气泡 */
.notice-icon {
  align-self: center;
  flex: none;
  width: 22px;
  height: 22px;
  font-size: 16px;
  color: var(--el-color-danger);
  cursor: pointer;
}
</style>

<style lang="scss">
/* 详情层挂在 body 上，样式需全局作用域（类名已加前缀避免污染） */
.chat-notice-popper .notice-detail {
  .notice-detail-head {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }
  pre {
    margin: 6px 0 0;
    max-height: 40vh;
    overflow: auto;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
