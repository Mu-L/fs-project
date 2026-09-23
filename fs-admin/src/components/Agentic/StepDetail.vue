<script setup lang="ts">
/**
 * 单个节点的执行明细 - 运行日志详情与对话历史的「执行过程」共用：
 * 节点输入、解析后入参、完整输出，以及 ReAct 调用链（逐轮思考/模型输出/工具调用参数与返回结果）。
 *
 * @prop {Object} step - 运行结果里的一条步骤（含 input/request/output 与 output.rounds）
 */
import ChatTextBlock from '@/components/Chat/ChatTextBlock.vue'

defineProps<{ step: any }>()

/** 步骤输出的结构化结果：解析失败时返回空对象 */
const stepOutput = (step: any) => {
  const text = String(step?.output ?? '')
  if (!text) return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && 'object' === typeof parsed ? parsed : {}
  } catch (error) {
    return {}
  }
}

/** 调用链：ReAct 每轮含模型输出与该轮工具调用；旧记录按一轮平铺展示工具调用 */
const stepChain = (step: any) => {
  const output: any = stepOutput(step)
  const rounds = Array.isArray(output?.rounds) ? output.rounds : []
  if (rounds.length) return rounds
  const calls = Array.isArray(output?.calls) ? output.calls : []
  return calls.length ? [{ round: 1, content: '', reasoning: '', calls }] : []
}
</script>

<template>
  <div class="step-detail">
    <!-- 解析后的实际入参：节点拼好的请求体 / 提示词，或配置里变量解析后的结果（与调试面板一致） -->
    <ChatTextBlock label="解析后入参" :value="step.request" v-if="step.request" />
    <!-- 调用链：ReAct 逐轮展示模型输出与该轮工具调用，最后一轮即最终输出 -->
    <template v-if="stepChain(step).length">
      <div class="step-label">调用链</div>
      <div class="step-round" :key="roundIndex" v-for="(round, roundIndex) in stepChain(step)">
        <div class="step-round-head">
          第 {{ round.round ?? roundIndex + 1 }} 轮
          <span v-if="(round.calls ?? []).length">· 工具调用 {{ round.calls.length }} 次</span>
          <span v-else>· 最终输出</span>
        </div>
        <ChatTextBlock label="思考过程" :text="round.reasoning" v-if="round.reasoning" />
        <ChatTextBlock label="模型输出" :text="round.content" v-if="round.content" />
        <div class="step-call" :key="call.id ?? callIndex" v-for="(call, callIndex) in (round.calls ?? [])">
          <div>工具方法：{{ call.method }} · {{ 2 === call.status ? '失败' : '成功' }}</div>
          <ChatTextBlock label="调用参数" :json="call.args" />
          <ChatTextBlock :label="2 === call.status ? '失败原因' : '返回结果'" :json="2 === call.status ? call.error : call.result" />
        </div>
      </div>
    </template>
    <!-- 最终模型输出：ReAct 跑完后模型给出的回复文本 -->
    <ChatTextBlock label="最终模型输出" :text="stepOutput(step).text" v-if="stepOutput(step).text" />
    <ChatTextBlock label="节点输出" :json="step.output" />
    <!-- 节点配置：节点自身的定义（不是本轮入参），放在最后供对照排查 -->
    <ChatTextBlock label="节点配置" :json="step.input" v-if="step.input" />
  </div>
</template>

<style lang="scss" scoped>
.step-detail {
  .step-label {
    margin: 6px 0 2px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    &:first-child {
      margin-top: 0;
    }
  }
  .step-round {
    margin-top: 6px;
    .step-round-head {
      margin-bottom: 4px;
      font-size: 12px;
      color: var(--el-text-color-regular);
    }
    .step-call {
      margin-top: 6px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
