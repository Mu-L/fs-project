<script setup lang="ts">
/**
 * 输出图表节点属性 - 只有模型参数（模型名称、温度、思考模式与强度）与记忆配置。
 * 图表类型、展示数据与图表参数由模型按用户问题与上游上下文自动判断，不需要额外配置：
 * 默认只取本轮记录 —— 开始节点的用户输入 + 上游大语言模型节点的回复文本与工具调用结果（真实数据），
 * 没有大语言模型节点时才退回使用其它上游节点的输出；
 * 多轮对话按记忆窗口把历史轮次加入上下文（开启工具链后历史轮次的工具调用也会带进来）。
 */
import { computed, ref } from 'vue'
import MemoryField from './MemoryField.vue'
import ModelParamsField from './ModelParamsField.vue'
import NodeSlice from './NodeSlice.vue'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

/**
 * 输出变量清单：直接取自节点目录里定义的 outputs（与变量选择器同一份描述），
 * 面板里逐个换行展示，避免两处描述不一致
 */
const outputs = computed<any[]>(() => {
  const factory: any = props.config?.outputs?.Chart
  return 'function' === typeof factory ? factory() : []
})
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <ModelParamsField
          v-model="model.data"
          :config="$props.config"
          :instance="$props.instance"
          :active-item="model"
          system-placeholder="留空为只使用内置图表提示词，可补充统计口径等要求">
          <template #extra>
            <div class="chart-tip">
              图表类型与展示数据由模型按用户问题与大语言模型本轮的记录自动决定，无需配置
            </div>
          </template>
        </ModelParamsField>
        <MemoryField v-model="model.data" />
        <SectionSlice title="输出">
          <el-form-item label="">
            <!-- 每个输出变量一行：变量名（中文名）+ 描述，描述与变量选择器里的完全一致 -->
            <div class="chart-output">
              <div class="chart-output-line" :key="item.name" v-for="item in outputs">
                <span class="chart-output-name">{{ item.name }}（{{ item.label }}）</span>
                <span class="chart-output-desc">{{ item.description }}</span>
              </div>
            </div>
          </el-form-item>
        </SectionSlice>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
/* 参数说明：跟在模型参数后面，不占用单独一行标题 */
.chart-tip,
.chart-output {
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-placeholder);
}
/* 输出变量：每个变量一行，名称在前、描述在后（换行展示，窄面板里自动折行） */
.chart-output {
  width: 100%;
  .chart-output-line {
    display: block;
    & + .chart-output-line {
      margin-top: 4px;
    }
    .chart-output-name {
      display: block;
      color: var(--el-text-color-regular);
    }
    .chart-output-desc {
      /* 描述独占整行、自动换行完整展示（不与变量名左右分栏，也不用省略号） */
      display: block;
      white-space: normal;
      word-break: break-word;
      overflow-wrap: anywhere;
    }
  }
}
</style>
