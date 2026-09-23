/**
 * 工具方法 - 方法清单由后端解析工具配置（OpenAPI 的 JSON/YAML、MCP 同步结果）后落库缓存，
 * 前端只做读取与展示：通过 ToolApi 按工具批量取方法并缓存，供工具页与智能体编排引用。
 *
 * 参数只包含方法级参数（OpenAPI 的 parameters/requestBody、MCP 的 inputSchema），
 * 工具级 url/header/query 只在后端调用时使用，不进入参数清单与模型 tool 定义。
 */
import ToolApi from '@/api/agent/ToolApi'
import ApiUtil from '@/utils/ApiUtil'

export interface ToolMethodParam {
  name: string
  type: string
  required: boolean
  description?: string
  /** 参数位置：query/path/header/body/mcp */
  in?: string
  defaultValue?: any
  enum?: string[]
  /** 参数结构：对象/数组参数的 properties / items，用于生成填写模板与模型 tool 定义 */
  schema?: any
}

export interface ToolMethod {
  id?: number
  toolId?: number
  /** 方法名：给模型与调用使用 */
  name: string
  /** 原始方法名：OpenAPI 的 operationId、MCP 的 tool name */
  originName?: string
  title?: string
  description?: string
  params: ToolMethodParam[]
  invoke?: any
  sort?: number
  /** 1-启用，2-停用 */
  status?: number
  /** 1-最近一次解析存在，0-已失效 */
  present?: number
  parseError?: string
}

/** 方法是否可被模型调用：最近一次解析仍存在且未人工停用 */
export const methodAvailable = (method: any) => {
  return 1 === Number(method?.present ?? 1) && 1 === Number(method?.status ?? 1)
}

/** 方法参数的展示文案：必填参数带 *，如 `location*、unit` */
export const paramText = (method: any) => {
  return (method?.params ?? []).map((item: ToolMethodParam) => item.required ? `${item.name}*` : item.name).join('、')
}

/** 结构化参数的字段名（只取一层）：让列表里也能看出对象该怎么填 */
const schemaKeys = (schema: any) => {
  const properties: any = schema?.properties
  if (!properties) return ''
  return Object.keys(properties).slice(0, 6).join('、')
}

/** 方法参数明细：`body(object{id、name})*、unit(string)` */
export const paramDetailText = (method: any) => {
  return (method?.params ?? [])
    .map((item: ToolMethodParam) => {
      const keys = schemaKeys(item.schema)
      return `${item.name}(${item.type || 'string'}${keys ? `{${keys}}` : ''})${item.required ? '*' : ''}`
    })
    .join('、')
}

/** 参数的悬浮说明：类型 + 字段 + 描述 */
export const paramHint = (item: ToolMethodParam) => {
  const keys = schemaKeys(item.schema)
  return [
    `${item.name}(${item.type || 'string'})${item.required ? ' · 必填' : ''}`,
    keys ? `字段：${keys}` : '',
    item.description ?? '',
  ].filter((text: string) => text).join('\n')
}

/**
 * 按参数结构生成填写模板：对象展开各字段、数组给一项、标量按默认值/枚举/类型给空值。
 * 测试时直接拿它预填 JSON，避免面对空输入框不知道怎么写
 */
export const schemaSample = (schema: any, depth = 0): any => {
  if (!schema || depth > 6) return null
  const type = String(schema.type ?? (schema.properties ? 'object' : ''))
  if ('object' === type || schema.properties) {
    const result: any = {}
    Object.keys(schema.properties ?? {}).forEach((key: string) => {
      result[key] = schemaSample(schema.properties[key], depth + 1)
    })
    return result
  }
  if ('array' === type) return [schemaSample(schema.items, depth + 1)]
  if (undefined !== schema.default) return schema.default
  if (schema.enum && schema.enum.length) return schema.enum[0]
  if ('boolean' === type) return false
  if ('integer' === type || 'number' === type) return null
  return ''
}

/**
 * 对象参数的字段清单：对象参数（如 OpenAPI 的 body）可以按字段分别配置执行变量，
 * 返回 [{ name, type, required, description }]，非对象参数返回空数组
 */
export const paramFields = (parameter: any) => {
  const properties: any = parameter?.schema?.properties
  if (!properties) return []
  const required: string[] = parameter?.schema?.required ?? []
  return Object.keys(properties).map((name: string) => ({
    name,
    type: String(properties[name]?.type ?? 'string'),
    required: required.indexOf(name) >= 0,
    description: properties[name]?.description ?? '',
  }))
}

/**
 * 方法缓存：toolId → 方法清单。
 * 模块级缓存避免同一批工具被反复请求；调用方通过 reactive 包装（ref/reactive）即可获得响应式。
 */
export const toolMethodCache: { data: Record<string, ToolMethod[]> } = { data: {} }

/** 已确认拉取过方法清单的工具（空结果也算拉取过，避免“没有方法”被当成未加载） */
export const toolMethodLoaded: Record<string, boolean> = {}

/** 已缓存的方法清单 */
export const cachedToolMethods = (toolId: any) => {
  return toolId ? toolMethodCache.data[String(toolId)] ?? [] : []
}

/** 指定工具的方法清单是否已拉取过 */
export const toolMethodsLoaded = (toolId: any) => {
  return Boolean(toolId) && true === toolMethodLoaded[String(toolId)]
}

/**
 * 批量载入方法清单：只请求缓存里没有的工具，返回本次拉取到的清单
 * @param toolIds 工具标识集合（重复与空值会被忽略）
 */
export const loadToolMethods = (toolIds: any[]) => {
  const ids: number[] = []
  toolIds.forEach((toolId: any) => {
    const id = Number(toolId)
    // 空结果不进缓存：工具刚保存或重新解析后能立刻拉到方法，不会长期停留在“没有方法”
    if (!id || (toolMethodCache.data[String(id)] ?? []).length) return
    if (ids.indexOf(id) < 0) ids.push(id)
  })
  if (!ids.length) return Promise.resolve(toolMethodCache.data)
  return ToolApi.methods({ ids }).then((result: any) => {
    const rows: any[] = ApiUtil.data(result) ?? []
    ids.forEach((id: number) => { toolMethodCache.data[String(id)] = [] })
    ids.forEach((id: number) => { toolMethodLoaded[String(id)] = true })
    rows.forEach((row: any) => {
      const key = String(row.toolId ?? '')
      if (!key) return
      if (!toolMethodCache.data[key]) toolMethodCache.data[key] = []
      toolMethodCache.data[key].push(row)
    })
    return toolMethodCache.data
  })
}

export default { loadToolMethods, cachedToolMethods, toolMethodCache, methodAvailable, paramText, paramDetailText, paramHint, paramFields, schemaSample }
