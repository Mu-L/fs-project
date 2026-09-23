/**
 * Maintain 维护接口
 *
 * reindexChunk：重建检索块索引，把数据库中的分块连同文档、分段状态写入检索索引
 * 返回 SSE 接口描述对象，供 FormMaintain 等流式任务组件使用。
 */
export default {
  reindexChunk(params: any = {}) {
    return { app: 'agent', uri: '/maintain/reindexChunk', method: 'POST', params }
  },
}