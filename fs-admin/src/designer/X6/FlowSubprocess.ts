import DesignUtil from '@/utils/DesignUtil'
import { Node, type NodeMetadata } from '@antv/x6'
import {
  applyContainerHeader,
  containerAttrs,
  containerMarkup,
  toggleContainerCollapse,
} from './container'

// 容器节点边框与画布内其它节点卡片保持一致（浅灰细边框 + 圆角）
export const SubprocessStroke = '#d5dae0'

/**
 * 子流程容器（智能体编排的迭代/循环、任务编排的子流程）：
 * 头部为「图标 + 名称」居中、展开收起按钮固定在左侧，三者同一水平中线，
 * 收起态（150×32）整行垂直居中，名称超长时按可用宽度显示省略号。
 */
export default class FlowSubprocess extends Node {

  meta: NodeMetadata
  collapsed: Boolean = false

  constructor (metadata?: NodeMetadata) {
    super(metadata)
    this.meta = metadata ?? {}
    applyContainerHeader(this, this.meta.data, false)
  }

  postprocess () {
    this.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
      Object.assign(this.meta, { data: current })
      applyContainerHeader(this, this.meta.data, Boolean(this.collapsed))
    }))
    // 尺寸变化（改宽、收起、展开）后重算居中与省略
    this.on('change:size', () => applyContainerHeader(this, this.meta?.data, Boolean(this.collapsed)))
    toggleContainerCollapse(this, false)
  }

  isCollapsed () {
    return this.collapsed
  }

  toggleCollapse (collapsed: any = null) {
    toggleContainerCollapse(this, collapsed)
  }
}

FlowSubprocess.config({
  markup: containerMarkup({ tagName: 'rect', selector: 'body' }),
  attrs: containerAttrs({
    rx: 8,
    ry: 8,
    refWidth: '100%',
    refHeight: '100%',
    stroke: SubprocessStroke,
    strokeWidth: 1,
    fill: '#ffffff',
    fillOpacity: 0.5,
  }),
})
