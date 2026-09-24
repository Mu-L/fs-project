import DesignUtil from '@/utils/DesignUtil'
import config from './config'

export interface VariableItem {
  value: string
  name: string
  label?: string
  type: string
  description?: string
  /** 可写入标记：容器内的元素/索引/循环变量可由「变量赋值」节点覆盖取值 */
  writable?: boolean
}

export interface VariableGroup {
  label: string
  variables: VariableItem[]
}

/**
 * 系统变量：只放无法从节点取到的运行上下文。
 * 用户输入（query）与用户文件（files）属于开始节点自身，直接引用开始节点的输出即可，不在这里重复列出。
 */
const sysVariables: VariableItem[] = [
  { value: 'sys.appId', name: 'appId', label: '应用标识', type: 'String', description: '当前应用标识' },
  { value: 'sys.userId', name: 'userId', label: '用户标识', type: 'String', description: '当前用户标识' },
  { value: 'sys.userName', name: 'userName', label: '用户名称', type: 'String', description: '当前用户名称' },
  { value: 'sys.conversationId', name: 'conversationId', label: '会话标识', type: 'String', description: '当前会话标识' },
  { value: 'sys.datetime', name: 'datetime', label: '当前时间', type: 'String', description: '当前时间（yyyy-MM-dd HH:mm:ss，东八区）' },
  { value: 'sys.date', name: 'date', label: '当前日期', type: 'String', description: '当前日期（yyyy-MM-dd，东八区）' },
]

/** 变量的展示名称：标题名称优先，未配置时回落到变量名称 */
export const variableTitle = (item: any) => String(item?.label || item?.name || '')

/**
 * 变量占位符实际值 - 采用 `{{#节点标识.变量英文名称#}}`，如 `{{#n1.query#}}`，用于保存与后端解析
 */
export const variableToken = (reference: string) => `{{#${reference}#}}`

/**
 * 变量占位符反向解析 - 整串就是一个占位符时返回其中引用的变量（`节点标识.变量名`），否则返回空串。
 * 用于只关心「引用了哪个变量」的展示与校验场景（下拉选择器的取值、条件摘要等）
 */
export const referenceOfToken = (value: any) => {
  const matched = String(value ?? '').trim().match(/^\{\{#([^#{}]+)#\}\}$/)
  return matched ? matched[1] : ''
}

/** 手工输入的变量引用：系统变量 `sys.xxx` 与会话变量 `conversation.xxx`（画布变量由 variableGroups 提供） */
const NamedReference = /^(?:sys|conversation)\.[A-Za-z_$][\w$]*$/

/**
 * 识别取值里的变量引用 - 画布中已有的变量（`节点标识.变量名`）或手工输入的 `sys.xxx` / `conversation.xxx`。
 * 识别到即返回引用本身，否则返回空串（固定文本、已是占位符）。
 * 变量引用按统一规范写成占位符 `{{#节点标识.变量名#}}`，后端只解析这种形式
 */
export const detectReference = (value: any, references?: Set<string>) => {
  const text = String(value ?? '').trim()
  if (!text || referenceOfToken(text)) return ''
  if (references?.has(text)) return text
  return NamedReference.test(text) ? text : ''
}

/**
 * 变量占位符的展示名称 - `节点名称.变量中文名称`，如 `开始.用户输入`
 */
export const variableLabel = (group: string, item: any) => `${group}.${variableTitle(item)}`

/**
 * 变量的插入信息映射：变量引用（`节点ID.变量名` 或 `sys.变量名`）→ `{ token, label }`
 * token 为实际写入文本的占位符，label 为编辑器中展示的名称标签
 */
export const variableTokens = (groups: VariableGroup[]) => {
  const result: Record<string, { token: string, label: string }> = {}
  groups.forEach((group) => {
    group.variables.forEach((item: any) => {
      result[item.value] = {
        token: variableToken(item.value),
        label: variableLabel(group.label, item),
      }
    })
  })
  return result
}

/**
 * 按关键字过滤变量分组，匹配变量中文名称、变量名称与节点名称，便于插入变量时快速查找
 */
export const filterVariableGroups = (groups: VariableGroup[], keyword: string) => {
  const word = String(keyword ?? '').trim().toUpperCase()
  if (!word) return groups
  return groups.map((group) => ({
    label: group.label,
    variables: group.variables.filter((item: any) => [
      variableTitle(item), item.name, group.label,
    ].some((text: any) => String(text ?? '').toUpperCase().indexOf(word) >= 0)),
  })).filter((group) => group.variables.length)
}

/**
 * 解析斜线触发：从一行文本与光标位置解析出触发字符后的查询词
 * @returns {Object|null} `{ from, word }`，from 为触发字符所在下标，未触发时返回 null
 */
export const parseTriggerWord = (line: string, ch: number, trigger = '/') => {
  const text = String(line ?? '')
  const start = text.lastIndexOf(trigger, ch - 1)
  if (start < 0) return null
  const word = text.slice(start + trigger.length, ch)
  // 触发字符后出现空白视为普通输入，不再提示
  if (/\s/.test(word)) return null
  return { from: start, word }
}

/**
 * 汇总画布中可被引用的变量，用于下游节点选择上游节点的输出
 * 迭代/循环的容器内变量（元素、索引、循环变量）仅对容器自身与其内部的节点可见
 * @param instance 画布实例（X6Container 暴露的 flow）
 * @param activeItem 当前激活的节点，用于排除自身
 * @param inner 只列出当前节点内部的节点变量（迭代/循环容器收集输出时用），默认 false
 */
export const variableGroups = (instance: any, activeItem: any = {}, inner = false): VariableGroup[] => {
  const cell: any = instance?.flow?.graph?.getCellById?.(activeItem?.id)
  // 容器作用域链（由内到外），用于判断容器内变量的可见性；节点尚未进入画布时不做限制
  const scopes: any = (() => {
    if (!cell) return null
    const ids: string[] = []
    let parent: any = cell.getParent?.()
    while (parent) {
      if ('flow-subprocess' === parent.shape) ids.push(parent.id)
      parent = parent.getParent?.()
    }
    return ids
  })()
  // 只列出容器内部（含多层嵌套）的节点变量；节点尚未进入画布时不做限制
  const onlyInner = Boolean(inner && cell)
  const innerIds: string[] = (() => {
    if (!onlyInner) return []
    const ids: string[] = []
    const walk = (parent: any) => (parent.getChildren?.() ?? []).forEach((child: any) => {
      ids.push(child.id)
      walk(child)
    })
    walk(cell)
    return ids
  })()
  // 容器只收集内部节点的输出，系统变量不参与
  const result: VariableGroup[] = onlyInner ? [] : [{
    label: '系统变量',
    variables: sysVariables.map(item => Object.assign({}, item)),
  }]
  const nodes: any[] = instance?.flow?.graph?.getNodes?.() ?? []
  nodes.forEach((node: any) => {
    if (onlyInner && innerIds.indexOf(node.id) < 0) return
    const data = node.getData() ?? {}
    if (!DesignUtil.widgetByType(data.type, config)) return
    const self = node.id === activeItem?.id
    const items: any[] = config.outputs?.[data.type]?.(data) ?? []
    const variables = items.filter((item: any) => {
      if (!item?.name) return false
      // 容器内变量（元素、索引、循环变量）只对容器自身与其内部的节点可见
      if (item.scope) return !scopes || self || scopes.indexOf(node.id) !== -1
      return !self
    }).map((item: any) => ({
      value: `${node.id}.${item.name}`,
      name: item.name,
      // 标题名称用于展示与占位符，为空时回落到变量名称
      label: item.label || item.name,
      type: item.type ?? 'String',
      description: item.description,
      // 容器内的变量（元素、索引、循环变量）是容器节点的可写入变量
      writable: Boolean(item.scope),
    }))
    if (!variables.length) return
    result.push({ label: data.name ?? node.id, variables })
  })
  return result
}

/**
 * 画布中全部可被引用的变量（含系统变量）- `节点标识.变量名`。
 * 供历史数据升级使用：要与画布现有变量比对，手输的会话变量等不在其中
 */
export const variableReferences = (cells: any[]) => {
  const result = new Set<string>(sysVariables.map((item: VariableItem) => item.value))
  ;(cells ?? []).forEach((cell: any) => {
    const data = cell?.data ?? {}
    if (!DesignUtil.widgetByType(data.type, config)) return
    const items: any[] = config.outputs?.[data.type]?.(data) ?? []
    items.forEach((item: any) => {
      if (item?.name) result.add(`${cell.id}.${item.name}`)
    })
  })
  return result
}

/**
 * 历史数据升级 - 递归把整串裸引用换成占位符（`节点标识.变量名` → `{{#节点标识.变量名#}}`）。
 * 早期下拉选择器保存的是裸引用，而运行时只解析占位符，会让问题分类器、知识检索这类节点
 * 取不到上游入参（把标识原样发给模型）；已是占位符或画布中不存在（如手输的会话变量）的取值保持原样
 */
export const upgradeVariables = (value: any, references: Set<string>): any => {
  if ('string' === typeof value) {
    const reference = detectReference(value, references)
    return reference ? variableToken(reference) : value
  }
  if (Array.isArray(value)) return value.map((item: any) => upgradeVariables(item, references))
  if (value && 'object' === typeof value) {
    const result: Record<string, any> = {}
    Object.keys(value).forEach((key: string) => {
      result[key] = upgradeVariables(value[key], references)
    })
    return result
  }
  return value
}

export default {
  variableGroups, variableTokens, variableToken, referenceOfToken, detectReference, variableLabel, variableTitle,
  variableReferences, upgradeVariables, filterVariableGroups, parseTriggerWord,
}
