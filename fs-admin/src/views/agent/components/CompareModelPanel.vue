<script setup lang="ts">
/**
 * 模型对比 - 单个模型的参数面板（手风琴的一节）。
 *
 * 一次只展开一个：展开时才铺出四项参数，收起时只在标题行给一串参数摘要。
 * 否则几个模型叠在一起就是几十个开关和输入框，看着就乱。
 *
 * 四项参数各带启用开关：不开就不下发，由服务端取默认值。
 * 这样「没配过」与「显式配成 0」在语义上分得开，对比时也才知道差在哪一项。
 *
 * @v-model          {Object}  model        - 该模型的配置对象
 * @v-model:expanded {Boolean} expanded     - 是否展开（由父级做手风琴控制）
 * @prop             {Number}  index        - 序号，仅用于标题旁的角标
 * @prop             {Array}   thinkModes   - 思考模式档位字典
 * @prop             {Array}   thinkEfforts - 思考强度档位字典
 * @emits            remove                 - 从调试列表里移除该模型
 */
import { computed } from 'vue'
import { ArrowDown, ArrowRight, Close } from '@element-plus/icons-vue'
import type { CompareModelConfig } from '@/types/compare'

const model = defineModel<CompareModelConfig>({ required: true })
const expanded = defineModel<boolean>('expanded', { default: true })
const {
  index = 1,
  thinkModes = [],
  thinkEfforts = [],
} = defineProps<{
  index?: number
  thinkModes?: any[]
  thinkEfforts?: any[]
}>()
const emit = defineEmits<{ remove: [] }>()

const label = (options: any[], value: any) => {
  const hit = (options ?? []).find((item: any) => String(item?.value) === String(value))
  return hit?.label ?? String(value ?? '')
}

/**
 * 思考强度是否可配：自身开关开着，且思考模式没被显式关成 off。
 * 模式关闭时强度没有意义，置灰比藏起来更好——用户能看出这两项是联动的。
 */
const effortEnabled = computed(() => {
  if (!model.value?.thinkEffortEnabled) return false
  if (model.value?.thinkModeEnabled && 'off' === model.value?.thinkMode) return false
  return true
})

/** 收起时的摘要：只列真正会下发的项，一眼看出各模型的差别 */
const summary = computed(() => {
  const parts: string[] = []
  if (model.value?.temperatureEnabled) parts.push(`温度 ${model.value.temperature}`)
  if (model.value?.maxTokensEnabled) parts.push(`上限 ${model.value.maxTokens}`)
  if (model.value?.thinkModeEnabled) parts.push(`思考 ${label(thinkModes, model.value.thinkMode)}`)
  if (effortEnabled.value) parts.push(`强度 ${label(thinkEfforts, model.value.thinkEffort)}`)
  return parts
})
</script>

<template>
  <div class="model-panel" :class="{ 'is-expanded': expanded }">
    <div class="panel-head" role="button" tabindex="0"
      @click="expanded = !expanded"
      @keydown.enter.prevent="expanded = !expanded"
      @keydown.space.prevent="expanded = !expanded">
      <el-icon class="panel-caret"><component :is="expanded ? ArrowDown : ArrowRight" /></el-icon>
      <span class="panel-index">{{ index }}</span>
      <span class="panel-model" :class="{ 'is-empty': !model.model }">{{ model.model || '未选择模型' }}</span>
      <el-button class="panel-remove" link :icon="Close" title="移除该模型" @click.stop="emit('remove')" />
    </div>
    <!-- 收起时给摘要；没配过任何参数就说明它跟随服务端默认 -->
    <div class="panel-summary" v-if="!expanded">
      <span class="summary-item" v-for="item in summary" :key="item">{{ item }}</span>
      <span class="summary-item is-muted" v-if="!summary.length">跟随默认</span>
    </div>
    <div class="panel-body" v-show="expanded">
      <div class="param-row">
        <span class="param-name">模型名称</span>
        <el-input class="param-value is-wide" v-model="model.model" clearable placeholder="请选择或输入模型名称" />
      </div>
      <div class="param-row">
        <span class="param-name">温度</span>
        <el-switch v-model="model.temperatureEnabled" />
        <el-input-number
          class="param-value"
          v-model="model.temperature"
          :disabled="!model.temperatureEnabled"
          :precision="2"
          :step="0.1"
          :min="0"
          :max="2"
          :controls="false" />
      </div>
      <div class="param-row">
        <span class="param-name">最大输出</span>
        <el-switch v-model="model.maxTokensEnabled" />
        <el-input-number
          class="param-value"
          v-model="model.maxTokens"
          :disabled="!model.maxTokensEnabled"
          :step="256"
          :min="0"
          :controls="false" />
      </div>
      <div class="param-row">
        <span class="param-name">思考模式</span>
        <el-switch v-model="model.thinkModeEnabled" />
        <el-select class="param-value" v-model="model.thinkMode" :disabled="!model.thinkModeEnabled" placeholder="请选择">
          <el-option v-for="item in thinkModes" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
      </div>
      <div class="param-row">
        <span class="param-name">思考强度</span>
        <el-switch v-model="model.thinkEffortEnabled" :disabled="model.thinkModeEnabled && 'off' === model.thinkMode" />
        <el-select class="param-value" v-model="model.thinkEffort" :disabled="!effortEnabled" placeholder="请选择">
          <el-option v-for="item in thinkEfforts" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.model-panel {
  border-radius: 6px;
  border: solid 1px var(--el-border-color-lighter);
  background: var(--el-bg-color);
  transition: border-color 0.2s;
  & + .model-panel {
    margin-top: 6px;
  }
  /* 展开的那一节边框加深一点，其余保持浅色，视觉重心很清楚 */
  &.is-expanded {
    border-color: var(--el-color-primary-light-5);
  }
}
.panel-head {
  @include flex-start();
  gap: 6px;
  padding: 7px 8px;
  cursor: pointer;
  .panel-caret {
    flex: none;
    color: var(--el-text-color-placeholder);
  }
  /* 序号用角标而不是文字，省得跟模型名抢视线 */
  .panel-index {
    flex: none;
    min-width: 18px;
    height: 18px;
    padding: 0 4px;
    border-radius: 4px;
    background: var(--el-fill-color);
    font-size: 11px;
    line-height: 18px;
    text-align: center;
    color: var(--el-text-color-placeholder);
  }
  .panel-model {
    min-width: 0;
    font-size: 13px;
    color: var(--el-text-color-primary);
    @include text-wrap();
    &.is-empty {
      color: var(--el-text-color-placeholder);
    }
  }
  .panel-remove {
    flex: none;
    margin-left: auto;
  }
}
/* 收起时的参数摘要：小胶囊，比标签组件轻 */
.panel-summary {
  @include flex-wrap();
  gap: 4px;
  padding: 0 8px 7px 38px;
  .summary-item {
    padding: 1px 6px;
    border-radius: 4px;
    background: var(--el-fill-color-light);
    font-size: 11px;
    line-height: 16px;
    color: var(--el-text-color-secondary);
    &.is-muted {
      background: transparent;
      color: var(--el-text-color-placeholder);
    }
  }
}
.panel-body {
  padding: 6px 8px 8px;
  border-top: solid 1px var(--el-border-color-lighter);
}
/* 参数行：左侧名称、中间开关、右侧取值，一行一项，避免占两份行高 */
.param-row {
  @include flex-start();
  gap: 8px;
  min-width: 0;
  & + .param-row {
    margin-top: 6px;
  }
  .param-name {
    flex: 0 0 56px;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
  .el-switch {
    flex: none;
  }
  /* Element Plus 控件不带上本组件的作用域属性，必须 :deep */
  :deep(.param-value) {
    margin-left: auto;
    width: 124px;
    max-width: 100%;
  }
  :deep(.param-value.is-wide) {
    flex: 1;
    width: 100%;
  }
  :deep(.el-input-number .el-input__inner) {
    text-align: right;
  }
}
</style>
