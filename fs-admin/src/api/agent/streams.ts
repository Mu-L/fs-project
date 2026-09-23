/**
 * 智能体流式接口的「端点 + 协议适配」注册表：
 * 页面只说用哪条流，uri 与报文解析都留在这里，协议知识不再散落到视图里。
 *
 * 新增一条流时：在这里补一个端点（uri + parse），页面 `...streams.xxx` 即可复用
 * useAgenticStream 的运行状态、兜底与中断语义。
 *
 * @example
 * const stream = useAgenticStream({ ...streams.agenticInvoke, headers, fallback: 'invoke', ... })
 */
import type { AgenticStreamEndpoint, AgenticStreamEvent } from '@/types/agent'

/** 编排接口的默认协议：`{ type: delta|step|round|done|error, data }` */
const typed = (payload: any): AgenticStreamEvent[] => {
  const types: AgenticStreamEvent['type'][] = ['delta', 'step', 'round', 'done', 'error']
  return types.indexOf(payload?.type) >= 0 ? [payload as AgenticStreamEvent] : []
}

export default {
  /** 发布版流程对话：增量 + 节点进度 + ReAct 轮次（/agentic/invokeStream） */
  agenticInvoke: { uri: '/agentic/invokeStream', parse: typed } as AgenticStreamEndpoint,

  /** 编排页运行抽屉：同一套协议，只有 uri 不同（/agentic/runStream） */
  agenticRun: { uri: '/agentic/runStream', parse: typed } as AgenticStreamEndpoint,

  /** 模型对话：`{ action, data }`，一条事件里可能带多个增量（/chat/dialog） */
  chatDialog: {
    uri: '/chat/dialog',
    parse: (payload: any): AgenticStreamEvent[] => {
      if ('choices.message' === payload?.action) {
        // 思考内容与正文各归各的字段，交给页面按 delta 处理
        return (payload.data ?? []).map((item: any) => {
          const delta: any = item?.delta || item?.message || {}
          return { type: 'delta' as const, data: { reasoning: delta.reasoning_content, content: delta.content } }
        })
      }
      // error.message / error.unknown / error.throwable：本轮到此结束
      if (String(payload?.action ?? '').indexOf('error') === 0) {
        return [{
          type: 'error' as const,
          data: { message: payload.data?.error?.message || payload.data?.message || '模型调用失败' },
        }]
      }
      return []
    },
  } as AgenticStreamEndpoint,
}
