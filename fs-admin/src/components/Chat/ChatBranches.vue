<script setup lang="ts">
/**
 * 消息分支切换：同一条消息存在多个后续分支时显示「◀ 2/3 ▶」。
 *
 * 对话按 parentId 组成消息树，页面只渲染「当前分支尾」所在的那条路径；
 * 切换分支时由父级按当前消息的兄弟节点换一条路径（见 composables/useChatBranch）。
 *
 * @prop {Object|null} branch 当前位置 { index, count }（index 从 1 起）
 * @emits switch 切换分支：+1 下一个、-1 上一个
 */
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const { branch = null } = defineProps<{
  branch?: { index: number, count: number } | null
}>()
const emit = defineEmits(['switch'])
</script>

<template>
  <div class="chat-branches" v-if="branch && branch.count > 1">
    <el-button class="branch-step" link :icon="ArrowLeft" title="上一个分支" @click="emit('switch', -1)" />
    <span class="branch-text">{{ branch.index }} / {{ branch.count }}</span>
    <el-button class="branch-step" link :icon="ArrowRight" title="下一个分支" @click="emit('switch', 1)" />
  </div>
</template>

<style lang="scss" scoped>
.chat-branches {
  @include flex-start();
  align-items: center;
  flex: none;
  gap: 2px;
  height: 18px;
  font-size: 12px;
  line-height: 18px;
  color: var(--el-text-color-placeholder);
  .branch-step {
    @include flex-center();
    flex: none;
    width: 18px;
    height: 18px;
    padding: 0;
    margin: 0;
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-placeholder);
    &:hover {
      color: var(--el-color-primary);
    }
  }
  .branch-text {
    flex: none;
    height: 18px;
    line-height: 18px;
    /* 等宽数字：切换时宽度不跳；文字与两侧箭头同一基线 */
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }
}
</style>
