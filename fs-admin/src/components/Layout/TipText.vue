<script setup lang="ts">
/**
 * 提示文案 - 属性面板里分组下方的说明文字，统一字号、行高与颜色。
 *
 * 各节点面板原先各写一套 `.xxx-tips` 样式，内容完全一致，抽到这里统一维护。
 *
 * @prop {String} text - 纯文本提示内容；需要富文本（如强调片段）时用默认插槽
 * @slot default - 提示内容，内部可用 <em> 标记强调片段（渲染为主色）
 */
defineProps<{
  text?: string,
}>()
</script>

<template>
  <div class="tip-text">
    <slot>{{ text }}</slot>
  </div>
</template>

<style lang="scss" scoped>
.tip-text {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
  em {
    font-style: normal;
    color: var(--el-color-primary);
  }
}
</style>
