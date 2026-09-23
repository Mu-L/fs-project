import DesignUtil from '@/utils/DesignUtil'
import { Node, Graph, type NodeMetadata } from '@antv/x6'
import {
  applyContainerHeader,
  containerAttrs,
  containerMarkup,
  toggleContainerCollapse,
} from './container'

/**
 * 分组容器（任务编排的分组）：头部样式与子流程容器保持一致——
 * 「图标 + 名称」居中、展开收起按钮固定在左侧、三者同一水平中线，
 * 收起态（150×32）整行垂直居中，名称超长时按可用宽度显示省略号。
 */
export default class FlowGroup extends Node {

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

  addTransientEdge (graph: Graph) {
    const node = this
    const incoming = graph.getConnectedEdges(node, { deep: true, incoming: true })
    const outgoing = graph.getConnectedEdges(node, { deep: true, outgoing: true })
    incoming.forEach((item: any) => {
      if (!item.source.cell || !item.source.port) return
      graph.addEdge({
        shape: 'flow-edge',
        source: { cell: item.source.cell, port: item.source.port },
        target: node,
        data: { name: '', description: '' }
      })
    })
    outgoing.forEach((item: any) => {
      if (!item.target.cell || !item.target.port) return
      graph.addEdge({
        shape: 'flow-edge',
        source: node,
        target: { cell: item.target.cell, port: item.target.port },
        data: { name: '', description: '' }
      })
    })
  }

  removeTransientEdge (graph: Graph) {
    const node = this
    const edges = graph.getConnectedEdges(node)
    edges.forEach(edge => {
      graph.removeEdge(edge)
    })
  }

  toggleCollapse (collapsed: any = null) {
    toggleContainerCollapse(this, collapsed)
  }
}

FlowGroup.config({
  markup: containerMarkup({ tagName: 'rect', selector: 'body' }),
  attrs: containerAttrs({
    rx: 10,
    ry: 10,
    refWidth: '100%',
    refHeight: '100%',
    stroke: 'rgb(34, 36, 42)',
    strokeWidth: '1px',
    fill: '#ffffff',
    fillOpacity: 0.3,
    strokeDasharray: '8, 3, 1, 3',
  }),
})
