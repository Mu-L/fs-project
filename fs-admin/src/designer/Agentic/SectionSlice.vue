<script setup lang="ts">
/**
 * 可折叠分组 - 属性面板的分组标题 + 分组内容：点击标题整块收起/展开，收起后只留标题（右侧箭头）。
 * 各节点属性面板的分组统一用它，长面板可以按需收起不常改的配置。
 *
 * @prop {String}  title     - 分组标题
 * @prop {Boolean} collapsed - 初始是否收起，默认展开
 * @slot title  - 标题内容，默认取 title 属性；需要标题后跟帮助图标等附加内容时用它
 * @slot default - 分组内容（若干个表单项/子组件）
 * @slot actions - 标题行右侧的额外操作（如删除按钮），点击不会触发收起
 */
import { ref } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const {
  title = '',
  collapsed = false,
} = defineProps<{
  title?: string,
  collapsed?: boolean,
}>()

const expanded = ref(!collapsed)
</script>

<template>
  <el-form-item
    label=""
    class="title is-collapsible"
    role="button"
    tabindex="0"
    :aria-expanded="expanded"
    @click="expanded = !expanded"
    @keydown.enter.prevent="expanded = !expanded"
    @keydown.space.prevent="expanded = !expanded">
    <LayoutIcon class="section-caret" :name="expanded ? 'ArrowDown' : 'ArrowRight'" />
    <span class="section-title"><slot name="title">{{ title }}</slot></span>
    <span class="section-actions" @click.stop><slot name="actions"></slot></span>
  </el-form-item>
  <div class="section-body" v-show="expanded">
    <slot></slot>
  </div>
</template>

<style lang="scss" scoped>
.section-caret {
  /* 展开/收起按钮放在标题左侧：与面板内列表项（CollapseItem）方向一致，
     也符合「层级展开」的阅读顺序——先看到箭头，再读标题 */
  margin-right: 6px;
  color: var(--el-text-color-placeholder);
}
.section-actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
}
.section-body {
  min-height: 0;
}
</style>
