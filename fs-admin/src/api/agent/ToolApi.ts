import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/tool/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/tool/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/tool/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/tool/save', param, tips)
  },
  mcpSync (param: any, tips = {}) {
    return base.post('/tool/mcpSync', param, tips)
  },
  /**
   * 工具方法清单：后端解析后落库缓存，支持单个 id 或批量 ids
   */
  methods (param: any, tips = {}) {
    return base.post('/tool/methods', param, tips)
  },
  /**
   * 方法检索：工具与方法都可能有大量数据，选择器按关键词分页检索（行内含工具名与参数明细）
   */
  methodList (param: any, tips = {}) {
    return base.post('/tool/methodList', param, tips)
  },
  /**
   * 解析预览：解析未保存的工具配置，返回方法清单但不落库
   */
  parse (param: any, tips = {}) {
    return base.post('/tool/parse', param, tips)
  },
  /**
   * 重新解析：按已保存工具的配置重解析并落库
   */
  parseSource (param: any, tips = {}) {
    return base.post('/tool/parseSource', param, tips)
  },
  /**
   * 方法测试：按工具配置真正发起一次调用（MCP tools/call 或 OpenAPI 请求）
   */
  test (param: any, tips = {}) {
    return base.post('/tool/test', param, tips)
  },
  /**
   * 方法维护：启用 / 停用与排序（描述与参数由解析结果决定）
   */
  methodSave (param: any, tips = {}) {
    return base.post('/tool/methodSave', param, tips)
  },
}
