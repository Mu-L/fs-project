/**
 * 容器节点（分组 flow-group、子流程 flow-subprocess）的公共头部：
 * 图标底块 + 居中标题 + 展开/收起按钮，三者共用同一水平中线；
 * 收起态（150×32）时整行在该高度内垂直居中，展开态固定在卡片头部。
 *
 * 说明：原生 X6 节点无法渲染 Vue 组件，图标取 SVG 路径绘制；
 * 展开/收起按钮固定在左侧，反复点击收起/展开不会移位。
 */
import icons from '@/assets/icons'
import * as ElementPlusIcons from '@element-plus/icons-vue'

// 收起态尺寸
export const ContainerCollapsed = { width: 150, height: 32 }

const IconSize = 22
const IconX = 28
const IconY = 7
const IconGlyph = 14
const IconGap = 8
const ButtonWidth = 16
const ButtonHeight = 14
const LabelFontSize = 12
// 文字视觉微调：包围盒中心略低于字形视觉中心，上移 1px 与图标居中对齐
const LabelOffset = -1
// 头部内容与容器左右边缘的最小留白
const HeaderPadding = 12

// 名称宽度估算：中文按字号计宽，其余字符按 0.6 倍字号
export const textWidth = (text: string) => {
  let width = 0
  for (const char of String(text ?? '')) {
    width += /[\u4e00-\u9fa5]/.test(char) ? LabelFontSize : LabelFontSize * 0.6
  }
  return Math.round(width)
}

/**
 * 按可用宽度截断文字并追加省略号（宽度按字号估算，与居中计算同一套口径）
 * 收起态宽度很小，超长名称靠它避免超出容器
 */
export const ellipsisText = (text: string, width: number) => {
  const value = String(text ?? '')
  if (width <= 0 || textWidth(value) <= width) return value
  const suffix = '…'
  const limit = width - textWidth(suffix)
  let result = ''
  let used = 0
  for (const char of value) {
    const charWidth = textWidth(char)
    if (used + charWidth > limit) break
    result += char
    used += charWidth
  }
  return result ? `${result}${suffix}` : suffix
}

/** 头部行的垂直中线：收起态在窄框内居中，展开态固定在卡片头部 */
export const containerCenterY = (size: any, collapsed: boolean) => {
  const height = Number(size?.height || 0)
  return collapsed && height > 0 ? height / 2 : IconY + IconSize / 2
}

// 取 Element Plus 图标的路径数据
const elementIconPath = (name: string) => {
  const component: any = (ElementPlusIcons as any)?.[name]
  if (!component?.setup) return ''
  const context: any = { attrs: {}, slots: {}, emit: () => {}, expose: () => {} }
  const render: any = component.setup({}, context)
  if ('function' !== typeof render) return ''
  const vnode: any = render({}, [])
  return vnode?.children?.[0]?.props?.d ?? ''
}

/**
 * 解析图标为画布绘制所需的数据：
 * - `flow.loop` 这类带点号的名字取项目自定义 SVG 图标（assets/icons 的 raw 字符串）
 * - 其它按 Element Plus 图标名解析
 */
export const containerIcon = (name: string): { d: string, viewBox: string } | null => {
  if (!name) return null
  if (name.indexOf('.') >= 0) {
    // 安全查找：图标名不存在时不能抛异常，否则节点创建会中断
    const raw = String(name.split('.').reduce((value: any, key: string) => value?.[key], icons) ?? '')
    const paths = Array.from(raw.matchAll(/<path[^>]*\sd="([^"]+)"/g)).map((item) => item[1])
    if (!paths.length) return null
    return {
      d: paths.join(' '),
      viewBox: (raw.match(/viewBox="([^"]+)"/) ?? [])[1] ?? '0 0 1024 1024',
    }
  }
  const d = elementIconPath(name)
  return d ? { d, viewBox: '0 0 1024 1024' } : null
}

// 诊断用：图标名存在但取不到路径时在控制台提示一次，便于排查画布图标不显示的问题
const warnedIcons: string[] = []
const resolveIcon = (name: string) => {
  if (!name) return null
  const icon = containerIcon(name)
  if (!icon && warnedIcons.indexOf(name) < 0) {
    warnedIcons.push(name)
    console.warn(`[agentic] 未取到图标路径：${name}`)
  }
  return icon
}

/** 图标绘制变换：按图标自身 viewBox 等比缩放到 IconGlyph 尺寸，并在图标底块内居中 */
export const iconTransform = (viewBox: string, left: number, top: number) => {
  const [x, y, w, h] = String(viewBox).split(/[\s,]+/).map(Number)
  const width = w || 1024
  const height = h || 1024
  const scale = IconGlyph / Math.max(width, height)
  const dx = left + (IconSize - width * scale) / 2 - (x || 0) * scale
  const dy = top + (IconSize - height * scale) / 2 - (y || 0) * scale
  return `translate(${dx}, ${dy}) scale(${scale})`
}

/**
 * 展开/收起按钮里的 ± 符号：refX/refY 是叠加位移，取「按钮中心 - 符号包围盒中心」，
 * 因此符号不论路径坐标如何都落在 16×14 按钮的正中
 */
export const buttonSignAttrs = (collapsed: boolean) => collapsed
  ? { d: 'M 0 4 8 4 M 4 0 4 8', refX: (ButtonWidth - 8) / 2, refY: (ButtonHeight - 8) / 2 }
  : { d: 'M 0 0 6 0', refX: (ButtonWidth - 6) / 2, refY: ButtonHeight / 2 }

/**
 * 渲染容器头部：「图标 + 名称」整体水平居中，展开/收起按钮固定在左侧，
 * 三者纵向共用同一中线；无图标时名称单独居中
 */
export const applyContainerHeader = (node: any, data: any = {}, collapsed = false) => {
  const size: any = node.size?.() ?? node.getSize?.() ?? { width: 0, height: 0 }
  const name = String(data?.name ?? '')
  const centerY = containerCenterY(size, collapsed)
  const top = centerY - IconSize / 2
  node.attr('buttonGroup', { refX: 8, refY: centerY - ButtonHeight / 2 })
  node.attr('buttonSign', buttonSignAttrs(collapsed))
  const icon = resolveIcon(String(data?.icon ?? ''))
  // 名称按可用宽度截断（收起态尤其明显）：按图标最靠左时的位置算，保证任何宽度下都不溢出，
  // 截断后的宽度再参与居中计算
  const available = Number(size.width || 0) - (IconX + IconSize + IconGap) - HeaderPadding
  const title = ellipsisText(name, available)
  node.attr('label/text', title)
  if (!icon) {
    node.attr('iconBg/display', 'none')
    node.attr('iconPath', { display: 'none', d: '' })
    node.attr('label', { refX: '50%', refY: centerY + LabelOffset, yAlign: 'middle', textAnchor: 'middle' })
    return
  }
  // 「图标 + 名称」作为一组整体居中，且不让图标压到左侧按钮
  const group = IconSize + IconGap + textWidth(title)
  const left = Math.max(IconX, Math.round((Number(size.width || 0) - group) / 2))
  node.attr('iconBg', { display: '', refX: left, refY: top })
  node.attr('iconPath', {
    display: '',
    d: icon.d,
    transform: iconTransform(icon.viewBox, left, top),
  })
  node.attr('label', {
    refX: left + IconSize + IconGap,
    refY: centerY + LabelOffset,
    yAlign: 'middle',
    textAnchor: 'start',
  })
}

/** 收起/展开容器：先更新状态再 resize，使 change:size 能按新状态重算居中 */
export const toggleContainerCollapse = (node: any, collapsed: any = null) => {
  const target = null === collapsed ? !node.collapsed : collapsed
  node.collapsed = target
  if (target) {
    Object.assign(node.meta, node.getSize())
    node.resize(ContainerCollapsed.width, ContainerCollapsed.height)
  } else if (node.meta) {
    node.resize(node.meta.width || 0, node.meta.height || 0)
  }
  applyContainerHeader(node, node.meta?.data, target)
}

// 公共头部 markup 与样式：分组、子流程共用，避免两处实现走样
const headerMarkup = [
  { tagName: 'text', selector: 'label' },
  { tagName: 'rect', selector: 'iconBg' },
  { tagName: 'path', selector: 'iconPath' },
  {
    tagName: 'g',
    selector: 'buttonGroup',
    children: [
      { tagName: 'rect', selector: 'button', attrs: { 'pointer-events': 'visiblePainted' } },
      { tagName: 'path', selector: 'buttonSign', attrs: { fill: 'none', 'pointer-events': 'none' } },
    ],
  },
]

const headerAttrs = {
  buttonGroup: { refX: 8, refY: IconY + IconSize / 2 - ButtonHeight / 2 },
  button: {
    height: ButtonHeight,
    width: ButtonWidth,
    rx: 2,
    ry: 2,
    fill: '#f5f5f5',
    stroke: '#ccc',
    cursor: 'pointer',
    event: 'node:collapse',
  },
  // ± 符号必须挂在 buttonSign 选择器下（曾因摊平到顶层导致 stroke 丢失、符号不可见）
  buttonSign: Object.assign({ stroke: '#808080' }, buttonSignAttrs(false)),
  label: {
    fontSize: LabelFontSize,
    fill: '#333333',
    refX: IconX + IconSize + IconGap,
    refY: IconY + IconSize / 2,
    yAlign: 'middle',
    textAnchor: 'start',
  },
  iconBg: {
    rx: 5,
    ry: 5,
    width: IconSize,
    height: IconSize,
    refX: IconX,
    refY: IconY,
    fill: '#eef4ff',
    stroke: 'none',
  },
  iconPath: { fill: '#3b7cff', stroke: 'none' },
}

/** 生成容器 markup（body + 公共头部） */
export const containerMarkup = (body: any) => [body].concat(JSON.parse(JSON.stringify(headerMarkup)))

/** 生成容器 attrs（自定义 body + 公共头部），每个类各自一份，避免相互影响 */
export const containerAttrs = (body: any) => JSON.parse(JSON.stringify(Object.assign({}, headerAttrs, { body })))

export default {
  ContainerCollapsed,
  applyContainerHeader,
  toggleContainerCollapse,
  containerMarkup,
  containerAttrs,
}
