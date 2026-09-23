<script setup lang="ts">
/**
 * 执行步骤（排查视图）：逐节点展示「名称 / 类型 / 容器次数 / 状态 · 耗时」，
 * 节点内部复用 StepDetail（解析后入参 + ReAct 调用链），可选展示节点的最终输出与原始输出。
 *
 * - 对话历史详情与调试抽屉共用，避免各页面重复一套步骤渲染；
 * - 步骤数据由调用方提供（历史会话按 logId 懒加载），展开时 emit open 通知调用方加载；
 * - 运行级的额外内容（如原始入参/输出）用默认插槽补充，组件不关心数据来源。
 *
 * @prop {Array} steps     步骤数组（运行结果或运行日志里的 steps）
 * @prop {String} title    折叠面板标题（各页面文案不同，由调用方传入）
 * @prop {Boolean} loading 步骤是否加载中
 * @prop {Boolean} output  是否额外展示节点的最终模型输出与原始输出（调试排查需要）
 * @emits open 展开时触发
 * @example
 * <agentic-steps :steps="stepsOf(item)" :title="`执行过程（${stepsOf(item).length} 个节点）`"
 *   :loading="stepLoading[String(item.logId)]" @open="loadSteps(item)">
 *   <el-collapse>…运行入参 / 运行输出…</el-collapse>
 * </agentic-steps>
 */
import { ref } from 'vue'
import AgenticUtil from '@/utils/AgenticUtil'
import StepDetail from '@/components/Agentic/StepDetail.vue'
import ChatTextBlock from '@/components/Chat/ChatTextBlock.vue'

const {
  steps = [],
  title = '',
  loading = false,
  output = false,
} = defineProps<{
  steps?: any[]
  title?: string
  loading?: boolean
  output?: boolean
}>()
const emit = defineEmits(['open'])

/** 展开状态由组件自己持有：展开时才通知调用方按需加载步骤 */
const open = ref(false)
const handleChange = (names: any) => {
  const list: any[] = Array.isArray(names) ? names : [names]
  open.value = list.indexOf('steps') >= 0
  if (open.value) emit('open')
}

/** 步骤输出：日志里以 JSON 字符串记录，取值口径与 AgenticUtil.stepOutput 一致 */
const stepOutput = (step: any) => AgenticUtil.stepOutput(step)
/** 输出文本：能解析成 JSON 时缩进展示，否则原样展示（避免多出一对引号） */
const stepText = (value: any) => {
  const text = String(value ?? '')
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (error) {
    return text
  }
}
</script>

<template>
  <el-collapse class="steps" :model-value="open ? ['steps'] : []" @change="handleChange">
    <el-collapse-item name="steps" :title="title">
      <div class="steps-body" v-loading="loading">
        <div class="step" :key="step.id ?? index" v-for="(step, index) in steps">
          <div class="step-head">
            <span class="step-name">{{ step.name }}</span>
            <span class="step-type">{{ step.type }}</span>
            <!-- 容器内节点：标明属于哪一次迭代/循环 -->
            <el-tag class="step-container" size="small" effect="plain" v-if="step.container">
              第 {{ Number(step.iteration ?? 0) + 1 }} 次
            </el-tag>
            <el-tag class="step-status" :type="2 === step.status ? 'danger' : 'success'" size="small" effect="plain">
              {{ 2 === step.status ? '失败' : '成功' }} · {{ step.duration }}ms
            </el-tag>
          </div>
          <!-- 解析后入参 + ReAct 调用链：与运行日志详情页同一套展示 -->
          <StepDetail :step="step" />
          <!-- 节点级失败原因：与运行日志详情页一致，直接展示不必展开调用链找 -->
          <el-alert class="step-alert" type="error" show-icon :closable="false" :title="step.error" v-if="step.error" />
          <!-- 调试排查：节点的最终模型输出与完整输出 -->
          <template v-if="output">
            <ChatTextBlock label="最终模型输出" :text="stepOutput(step).text" v-if="stepOutput(step).text" />
            <ChatTextBlock label="节点输出" :json="step.output" />
          </template>
        </div>
        <!-- 运行级附加内容（原始入参/输出等）由调用方补充 -->
        <slot />
      </div>
    </el-collapse-item>
  </el-collapse>
</template>

<style lang="scss" scoped>
/* 折叠面板：只留标题一行，边框与底色交给上层容器（气泡/抽屉） */
.steps.el-collapse {
  --el-collapse-header-height: 28px;
  border-top: none;
  border-bottom: none;
  :deep(.el-collapse-item__header) {
    padding: 0;
    font-size: 12px;
    border-bottom: none;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 0;
  }
}
/* 一条步骤：标题行 + 明细；步骤之间用虚线分隔 */
.step {
  & + .step {
    margin-top: 10px;
    padding-top: 10px;
    border-top: dashed 1px var(--el-border-color-lighter);
  }
  .step-head {
    @include flex-start();
    flex-wrap: wrap;
    gap: 6px;
    font-size: 12px;
    .step-name {
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
    .step-type,
    .step-container,
    .step-status {
      color: var(--el-text-color-placeholder);
    }
    .step-status {
      margin-left: auto;
    }
  }
  .step-label {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  .step-alert {
    margin-top: 6px;
  }
}
</style>
