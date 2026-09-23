<script setup lang="ts">
/**
 * 聊天电梯导航（滚动导航）：
 * 为消息列表里的「用户消息」在右侧生成一列横条，悬停/聚焦时展开成内容列表，点击平滑定位到对应气泡。
 * 与具体页面解耦：父页面传入滚动容器、命中消息的选择器与摘要文本选择器即可复用。
 *
 * - 摆放：组件自身绝对定位（贴容器右侧垂直居中），父级消息区需为 `position: relative`，
 *   且组件要放在滚动容器之外，这样导航不随消息一起滚动；
 * - 采集：内容版本号（一般传消息条数）变化时重新扫描容器，无需父页面提供数据；
 * - 滚动：组件自己监听容器的 scroll 事件同步「当前所在条」，父页面不用重复通知。
 *
 * @prop {HTMLElement} target   消息列表所在的可滚动元素
 * @prop {String} selector      需要生成导航的消息选择器，默认 .message.is-user
 * @prop {String} textSelector  取摘要文本的子选择器；不传时取命中元素自身的文本
 * @prop {Number} min           达到该条数才显示，默认 2（只有一条时没有定位意义）
 * @prop {Number} revision      内容版本号（一般传消息条数），变化时重新采集导航条目
 * @example
 * <chat-elevator :target="chatRef" :revision="messages.length" selector=".chat-message.is-user" text-selector=".chat-bubble" />
 */
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const {
  target = null,
  selector = '.message.is-user',
  textSelector = '',
  min = 2,
  revision = 0,
} = defineProps<{
  target?: HTMLElement | null
  selector?: string
  textSelector?: string
  min?: number
  revision?: number
}>()

/** 导航条目：一条用户消息一条，摘要最多 40 字 */
const marks = ref<any[]>([])
/** 当前所在条：最下面一条已越过容器顶部的消息 */
const activeMark = ref('')
/** 悬停/聚焦的条：文字与横条共用同一个状态，保证两侧变化同步 */
const hoverMark = ref('')
/** 是否展开内容列表 */
const open = ref(false)
const rootRef = ref<HTMLDivElement>()

/** 当前所在条：取最下面一条已越过容器顶部的消息 */
const syncActive = () => {
  const element = target
  if (!element) return
  const base = element.getBoundingClientRect().top
  let current = ''
  element.querySelectorAll(selector).forEach((row: any, index: number) => {
    if (row.getBoundingClientRect().top - base <= 28) current = `item-${index}`
  })
  activeMark.value = current
}

/** 采集导航条目：从已渲染的消息里取文本摘要（序号即消息在列表中的顺序） */
const collect = () => {
  const element = target
  if (!element) {
    marks.value = []
    return
  }
  marks.value = Array.from(element.querySelectorAll(selector)).map((row: any, index: number) => {
    const source: any = textSelector ? row.querySelector(textSelector) : row
    const text = String(source?.textContent ?? '').replace(/\s+/g, ' ').trim()
    return {
      key: `item-${index}`,
      index,
      text: `${index + 1}. ${text.slice(0, 40) || '（附件消息）'}`,
    }
  })
  syncActive()
}

/** 定位：按容器内偏移量平滑滚动，不调用 scrollIntoView（避免连带滚动外层布局） */
const scrollTo = (mark: any) => {
  const element = target
  const rows: any[] = element ? Array.from(element.querySelectorAll(selector)) : []
  const row: any = rows[mark.index]
  if (!element || !row) return
  const offset = row.getBoundingClientRect().top - element.getBoundingClientRect().top
  element.scrollTo({ top: element.scrollTop + offset - 12, behavior: 'smooth' })
  activeMark.value = mark.key
}

/** 展开或当前条变化时，把对应的一条滚进可视区（横条列与内容列表都可滚动） */
const revealActive = () => {
  const element: any = rootRef.value
  const row: any = element?.querySelector('.elevator-item.is-active')
  if (!element || !row) return
  const top = row.offsetTop
  if (top >= element.scrollTop && top + row.offsetHeight <= element.scrollTop + element.clientHeight) return
  element.scrollTo({ top: Math.max(0, top - element.clientHeight / 2 + row.offsetHeight / 2), behavior: 'smooth' })
}

// 容器或内容变化：重新采集条目
watch(() => [target, revision], () => nextTick(collect), { immediate: true })
// 容器滚动：组件自己同步当前条
watch(() => target, (element, previous) => {
  previous?.removeEventListener('scroll', syncActive)
  element?.addEventListener('scroll', syncActive, { passive: true })
  nextTick(syncActive)
}, { immediate: true })
// 展开或当前位置变化：把对应条目滚进可视区
watch([open, activeMark], () => nextTick(revealActive))
onBeforeUnmount(() => target?.removeEventListener('scroll', syncActive))
</script>

<template>
  <!-- 横条列：收起只显示横条，悬停/聚焦展开成内容列表，文字与行尾横条同行对齐 -->
  <div
    class="elevator"
    :class="{ 'is-open': open }"
    ref="rootRef"
    v-if="marks.length >= min"
    @mouseenter="open = true"
    @mouseleave="open = false"
    @focusin="open = true"
    @focusout="open = false">
    <div
      class="elevator-item"
      :class="{ 'is-hover': mark.key === hoverMark, 'is-active': mark.key === activeMark }"
      :key="mark.key"
      v-for="mark in marks"
      @mouseenter="hoverMark = mark.key"
      @mouseleave="hoverMark = ''"
      @click="scrollTo(mark)">
      <span class="elevator-text">{{ mark.text }}</span>
      <span
        class="elevator-bar"
        :tabindex="0"
        @focus="hoverMark = mark.key"
        @blur="hoverMark = ''"
        @keydown.enter.prevent="scrollTo(mark)"
        @keydown.space.prevent="scrollTo(mark)"></span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/**
 * 电梯导航：贴容器右侧垂直居中。每条消息一行（文字 + 行尾横条），
 * 横条跟着自己那一行走，所以文字与横条永远对齐（参考 DeepSeek 的滚动导航）。
 * 收起只留横条；展开后整块共用同一个底板，视觉上是一个整体。
 */
.elevator {
  position: absolute;
  top: 50%;
  right: 28px;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  /* 行距统一：收起与展开都按 19px 一行排，展开时横条不会上下跳 */
  gap: 0;
  max-height: 60vh;
  z-index: 1;
  /**
   * 条目过多时自身可滚动（很细的横条列上放滚动条很难看，收起态直接隐藏滚动条）；
   * 展开成列表后保留项目自带的细滚动条
   */
  &:not(.is-open) {
    overflow-x: hidden;
    overflow-y: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      width: 0;
      height: 0;
    }
  }
  /* 展开：文字与横条同处一块底板；右移 11px（内边距 10 + 描边 1）让横条位置保持不变 */
  &.is-open {
    right: 17px;
    align-items: stretch;
    gap: 0;
    /* 面板宽度收在固定区间内：短消息不被撑小，长消息也不会把面板撑宽（文字单行省略） */
    min-width: 220px;
    max-width: 320px;
    padding: 8px 10px;
    border-radius: 10px;
    border: solid 1px var(--el-border-color-lighter);
    background: var(--el-bg-color);
    box-shadow: 0 4px 16px rgb(0 0 0 / 8%);
    /* 列表过长时整体滚动：文字与横条一起滚，保持一一对应 */
    overflow-y: auto;
  }
  /* 一行：文字（可伸缩、超长省略）+ 行尾横条 */
  .elevator-item {
    /* 行高固定：收起时这一行只有横条，展开后同一行放文字，两态行位置一致 */
    height: 19px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    cursor: pointer;
    .elevator-text {
      flex: 1 1 auto;
      min-width: 0;
      font-size: 12px;
      line-height: 19px;
      color: var(--el-text-color-regular);
      transition: color 0.2s;
      @include text-wrap();
    }
    /* hover：文字与横条同时变中性黑 */
    &.is-hover {
      .elevator-text {
        color: var(--el-text-color-primary);
      }
      .elevator-bar {
        background: var(--el-text-color-primary);
      }
    }
    /* 当前所在位置：文字用主色，横条比其它条长一点点 */
    &.is-active {
      .elevator-text {
        color: var(--el-color-primary);
      }
      .elevator-bar {
        width: 20px;
        background: var(--el-color-primary);
      }
    }
  }
  .elevator-bar {
    flex: none;
    width: 16px;
    height: 3px;
    border-radius: 2px;
    /* 默认灰 → hover 黑 → 选中主色，三档区分 */
    background: var(--el-text-color-placeholder);
    transition: background-color 0.2s;
  }
}
/* 收起状态：隐藏文字，只留一列横条 */
.elevator:not(.is-open) .elevator-text {
  display: none;
}
</style>
