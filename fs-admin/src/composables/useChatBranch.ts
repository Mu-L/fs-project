/**
 * 对话分支：消息按 parentId 组成消息树，页面只渲染「当前分支尾」所在的那条路径。
 *
 * - 后端 run/invoke 的 parentId 决定新消息接在哪条消息之后，reuseQuestion 表示重新生成；
 * - chatInfo 返回每条消息的 parentId 与会话的 leafId（当前分支尾）；
 * - 本组合式函数按「兄弟节点」计算当前位置，并提供切换分支的方法；
 *   没有位置信息的消息（尚未落库的流式消息、迁移前的历史数据）始终可见，
 *   避免发送过程中气泡被过滤掉；
 * - 尚未落库的新提问由 setPending 一并登记：编辑第一条提问重新发送时它接在会话起点，
 *   不登记就会沿 parentId 回溯不到消息而被当成「没有分支位置」，退回整条会话都显示。
 *
 * @example
 * const branch = useChatBranch()
 * branch.setMessages(info.messages, info.leafId)
 * <chat-message v-for="item in branch.visible.value" :branch="branch.branchOf(item)" @switch="branch.switchBranch(item, $event)" />
 */
import { computed, ref } from 'vue'
import type { ChatMessageItem } from '@/types/agent'

export default function useChatBranch() {
  const messages = ref<ChatMessageItem[]>([])
  /** 当前分支尾消息标识：路径 = 该消息沿 parentId 到根的祖先链 */
  const activeLeaf = ref<any>(0)
  /**
   * 尚未落库的回复：刚点发送/重新生成时这条消息还没有标识，
   * 先把它当作分支尾，重新生成时上一次的输出就会立刻隐藏（可用分支切换找回）
   */
  const pending = ref<any>(null)
  /**
   * 与 pending 一起登记的本地提问：编辑第一条提问重新发送时，新提问接在会话起点（parentId 为 0），
   * 沿 parentId 回溯不到已落库的消息，只认 pending 会被判成「没有分支位置」而退回整条会话都显示
   */
  const pendingQuestion = ref<any>(null)

  const byId = computed<Record<string, ChatMessageItem>>(() => {
    const map: Record<string, ChatMessageItem> = {}
    messages.value.forEach((item: any) => {
      if (item?.id) map[String(item.id)] = item
    })
    return map
  })

  /** 兄弟关系：parentId → 子消息（按消息标识正序，与后端一致） */
  const childMap = computed<Record<string, ChatMessageItem[]>>(() => {
    const map: Record<string, ChatMessageItem[]> = {}
    messages.value.forEach((item: any) => {
      if (!item?.id) return
      const key = String(item.parentId ?? 0)
      if (!map[key]) map[key] = []
      map[key].push(item)
    })
    Object.keys(map).forEach((key: string) => map[key].sort((a: any, b: any) => Number(a.id) - Number(b.id)))
    return map
  })

  const siblingsOf = (item: any, parentId?: any) => {
    const key = String(null == parentId ? (item?.parentId ?? 0) : parentId)
    return childMap.value[key] ?? []
  }

  /** 某个分支的尾部：一直取最新的子消息，直到没有后续（切换分支时展示该分支最近的状态） */
  const tailOf = (id: any): any => {
    let current = id
    const visited = new Set()
    while (current && !visited.has(String(current))) {
      visited.add(String(current))
      const children = childMap.value[String(current)] ?? []
      if (!children.length) return current
      current = children[children.length - 1]?.id
    }
    return current
  }

  /** 当前分支路径：分支尾沿 parentId 回溯到根，按时间正序 */
  const path = computed<ChatMessageItem[]>(() => {
    const list: ChatMessageItem[] = []
    const visited = new Set()
    const local: any = pending.value
    const question: any = pendingQuestion.value
    let current: any = question ? question.parentId : (local ? local.parentId : activeLeaf.value)
    while (current && !visited.has(String(current))) {
      visited.add(String(current))
      const item: any = byId.value[String(current)]
      if (!item) break
      list.unshift(item)
      current = item.parentId ?? 0
    }
    if (question) list.push(question)
    if (local) list.push(local)
    return list
  })

  const pathIds = computed<Set<string>>(() => new Set(path.value.map((item: any) => String(item.id))))

  /** 当前分支的持久化尾节点：有未落库回复时，它的父消息才是分支尾 */
  const tailId = computed<any>(() => pending.value ? (pending.value.parentId ?? 0) : activeLeaf.value)

  /** 页面渲染的消息：当前分支上的消息 + 挂在分支尾上尚未落库的提问/回复 */
  const visible = computed<ChatMessageItem[]>(() => {
    if (!path.value.length) return messages.value
    return messages.value.filter((item: any) => {
      if (item === pending.value) return true
      if (item?.id) return pathIds.value.has(String(item.id))
      return String(item?.parentId ?? 0) === String(tailId.value ?? 0)
    })
  })

  /** 消息的分支位置：同父节点下排第几（1 起），用于显示「◀ 2/3 ▶」 */
  const branchOf = (item: any) => {
    const siblings = siblingsOf(item)
    if (siblings.length < 2) return null
    const index = siblings.findIndex((row: any) => String(row?.id) === String(item?.id))
    return index < 0 ? null : { index: index + 1, count: siblings.length }
  }

  /** 切换分支：按兄弟节点前后移动，并把分支尾设为该分支的末端 */
  const switchBranch = (item: any, step: number) => {
    const siblings = siblingsOf(item)
    if (siblings.length < 2) return
    const index = siblings.findIndex((row: any) => String(row?.id) === String(item?.id))
    if (index < 0) return
    const next = siblings[(index + step + siblings.length) % siblings.length]
    // 手动切换分支：放弃尚未落库的那条本地视图
    pending.value = null
    pendingQuestion.value = null
    activeLeaf.value = tailOf(next?.id)
  }

  /** 登记尚未落库的回复与本轮新提问：分支视图立即切到它，上一次的输出随之隐藏 */
  const setPending = (item: any, question: any = null) => {
    pending.value = item
    pendingQuestion.value = question
  }

  /**
   * 回复落库后收尾：仍在看这条回复时把分支尾切到它；
   * 用户已经切到别的分支时不抢视角，只记分支尾。
   */
  const commit = (item: any, leafId: any = 0) => {
    const leaf = Number(leafId ?? 0) || Number(item?.id ?? 0) || 0
    if (leaf) activeLeaf.value = leaf
    if (pending.value === item) {
      pending.value = null
      pendingQuestion.value = null
    }
  }

  /** 装载会话：leafId 为空（历史数据）时取最后一条消息作为分支尾 */
  const setMessages = (list: any[], leafId: any = 0) => {
    pending.value = null
    pendingQuestion.value = null
    messages.value = Array.isArray(list) ? list : []
    const leaf = Number(leafId ?? 0) || 0
    if (leaf && byId.value[String(leaf)]) {
      activeLeaf.value = leaf
      return
    }
    const last: any = messages.value[messages.value.length - 1]
    activeLeaf.value = last?.id ? tailOf(last.id) : 0
  }

  const reset = () => {
    messages.value = []
    activeLeaf.value = 0
    pending.value = null
    pendingQuestion.value = null
  }

  return { messages, activeLeaf, pending, pendingQuestion, visible, path, branchOf, switchBranch, setPending, commit, setMessages, tailOf, reset }
}
