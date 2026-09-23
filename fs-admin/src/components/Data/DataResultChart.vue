<script setup lang="ts">
/**
 * 图表展示 - 按「输出图表」节点给出的图表定义渲染：表格 / 柱状图 / 折线图 / 饼图。
 * 图表定义由模型按内置提示词归纳：type 指定图表类型，categories 为分类轴取值，series 为数值系列（最多两个）。
 * 默认视图取定义里的 type；用户切换视图后保持用户选择（数据变化时才回到定义的默认值）。
 *
 * @prop {String} type       - bar 柱状 / line 折线 / pie 饼图 / table 表格
 * @prop {String} title      - 图表标题
 * @prop {String} source     - 数据来源（展示在标题下方）
 * @prop {Array}  categories - 分类轴取值
 * @prop {Array}  series     - 系列：{ name, data }，data 与 categories 一一对应
 * @prop {Number} limit      - 表格视图最多渲染的行数
 */
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const {
  type = 'bar',
  title = '',
  source = '',
  categories = [],
  series = [],
  limit = 500,
} = defineProps<{
  type?: string,
  title?: string,
  source?: string,
  categories?: any[],
  series?: any[],
  limit?: number,
}>()

const modes = ['table', 'bar', 'line', 'pie']
const mode = ref<string>(modes.indexOf(type) < 0 ? 'table' : type)
/** 用户是否手动切换过视图：手动选择后不再跟随图表定义里的类型 */
const touched = ref(false)
/** 手动切换视图：记住用户选择，后续数据变化不再自动切换 */
const handleModeChange = () => {
  touched.value = true
}
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let observer: ResizeObserver | null = null

const labels = computed<string[]>(() => (categories ?? []).map((item: any) => String(null === item || undefined === item ? '' : item)))
/** 系列名与数值：缺失的系列名按序号补，取值统一转成数字 */
const names = computed<string[]>(() => (series ?? []).map((item: any, index: number) => String(item?.name || `系列${index + 1}`)))
const values = computed<number[][]>(() => (series ?? []).map((item: any) =>
  labels.value.map((_label: string, index: number) => Number(item?.data?.[index] ?? 0))))
/** 值是否可绘制：至少一个系列且取值都是数字 */
const drawable = computed(() => values.value.length > 0 && values.value.every((data: number[]) => data.every((value: number) => !isNaN(value))))
/** 表格视图：分类列 + 各系列列 */
const tableColumns = computed<any[]>(() => [{
  prop: '__category__', label: '分类',
}].concat(names.value.map((name: string, index: number) => ({ prop: `value${index}`, label: name }))))
/** 表格数据：分类 + 各系列取值，超过上限只渲染前若干行 */
const tableRows = computed<any[]>(() => labels.value.slice(0, Math.max(1, limit)).map((label: string, index: number) => {
  const row: any = { __category__: label }
  names.value.forEach((name: string, at: number) => { row[`value${at}`] = values.value[at]?.[index] ?? 0 })
  return row
}))

/** 图表配置：按当前视图生成 */
const option = () => {
  if (!drawable.value) return null
  // 图例（色块描述）统一放在图表底部：顶部只留图形，避免和标题、内容挤在一起
  // （饼图的图例是各分类的占比映射，柱状/折线是系列名，都在底部横向排列，窄面板自动滚动）
  const legend: any = { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 } }
  if ('pie' === mode.value) {
    return {
      tooltip: { trigger: 'item' },
      legend: legend,
      series: [{
        type: 'pie',
        // 略微收小并上移：给底部图例留出空间
        radius: ['38%', '60%'],
        center: ['50%', '46%'],
        // 分类名交给底部图例，扇区上只留占比，避免两处重复文字挤在一起
        label: { fontSize: 11, formatter: '{d}%' },
        data: labels.value.map((label: string, index: number) => ({
          name: label || String(index + 1),
          value: values.value[0][index] ?? 0,
        })),
      }],
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: legend,
    // 底部留出图例一行的高度
    grid: { left: 4, right: 8, top: 8, bottom: 26, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels.value,
      axisLabel: { fontSize: 11, interval: 0, rotate: labels.value.length > 6 ? 30 : 0, hideOverlap: true },
    },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: names.value.map((name: string, index: number) => ({
      name: name,
      type: mode.value,
      smooth: true,
      barMaxWidth: 32,
      data: values.value[index] ?? [],
    })),
  }
}

/** 渲染图表：非图表视图或没有数值列时销毁实例 */
const render = () => {
  if ('table' === mode.value || !chartRef.value) {
    dispose()
    return
  }
  const config: any = option()
  if (null == config) {
    dispose()
    return
  }
  if (null == chart) {
    chart = echarts.init(chartRef.value)
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartRef.value)
  }
  chart.setOption(config, true)
}

const dispose = () => {
  observer?.disconnect()
  observer = null
  chart?.dispose()
  chart = null
}

/** 下载文件名：取图表标题，去掉文件名里不允许的字符 */
const fileName = computed<string>(() => String(title || '图表').replace(/[\\/:*?"<>|]/g, '').trim() || '图表')
/** 导出图片的像素比与版式：页面标题是 HTML 不进画布，导出时单独在图片顶部留出标题行 */
const EXPORT_RATIO = 2
const EXPORT_PADDING = 12
const EXPORT_TITLE_HEIGHT = 18
const EXPORT_FONT = 'system-ui, -apple-system, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'

const downloadUrl = (url: string) => {
  const link = document.createElement('a')
  link.href = url
  link.download = `${fileName.value}.png`
  link.click()
}

/**
 * 导出 PNG：图表本身不带标题（页面标题是 HTML），这里把图表画布与标题拼到一张新画布上，
 * 标题独占顶部一行、上下留白，图表整体下移，避免标题与图例/图形挤在一起
 */
const handleDownload = () => {
  // 表格视图没有画布：先提示切换，避免点了没反应
  if ('table' === mode.value) {
    ElMessage.info('请在图表视图（柱状 / 折线 / 饼图）中下载图片')
    return
  }
  if (null == chart) {
    ElMessage.warning('图表还未渲染完成，请稍后再试')
    return
  }
  const source = chart.getDataURL({ type: 'png', pixelRatio: EXPORT_RATIO, backgroundColor: '#fff' })
  const image = new Image()
  // 图片装载失败时至少把图表本身导出，不因为加标题而丢掉下载
  image.onerror = () => downloadUrl(source)
  image.onload = () => {
    const padding = EXPORT_PADDING * EXPORT_RATIO
    const titleHeight = EXPORT_TITLE_HEIGHT * EXPORT_RATIO
    const canvas = document.createElement('canvas')
    canvas.width = image.width
    canvas.height = image.height + titleHeight + padding * 2
    const context = canvas.getContext('2d')
    if (null == context) {
      downloadUrl(source)
      return
    }
    context.fillStyle = '#fff'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.fillStyle = '#303133'
    // 字号按像素比放大，导出图片里的字号与页面一致
    context.font = `${13 * EXPORT_RATIO}px ${EXPORT_FONT}`
    context.textAlign = 'center'
    context.textBaseline = 'top'
    // 标题过长时按实际宽度截断（挤压字号会看不清），完整标题仍在文件名里
    const maxWidth = canvas.width - padding * 2
    let label = fileName.value
    if (context.measureText(label).width > maxWidth) {
      let count = label.length
      while (count > 1 && context.measureText(`${label.slice(0, count)}…`).width > maxWidth) count--
      label = `${label.slice(0, count)}…`
    }
    context.fillText(label, canvas.width / 2, padding)
    context.drawImage(image, 0, padding + titleHeight)
    downloadUrl(canvas.toDataURL('image/png'))
  }
  image.src = source
}

watch(() => [mode.value, categories, series], () => {
  nextTick(() => render())
}, { deep: true })

// 图表定义（type）变化时回到定义里的默认视图；用户手动切换后保持用户选择
watch(() => type, (value: string) => {
  if (touched.value) return
  mode.value = modes.indexOf(value) < 0 ? 'table' : value
})

onMounted(() => nextTick(() => render()))
onBeforeUnmount(() => dispose())
</script>

<template>
  <div class="data-result">
    <!-- 图表标题与数据来源：来自图表定义（模型归纳时给出） -->
    <div class="result-title" v-if="title">{{ title }}</div>
    <div class="result-caption" v-if="source">数据来源：{{ source }}</div>
    <div class="result-head">
      <el-radio-group v-model="mode" size="small" @change="handleModeChange">
        <el-radio-button value="table">表格</el-radio-button>
        <el-radio-button value="bar" :disabled="!drawable">柱状</el-radio-button>
        <el-radio-button value="line" :disabled="!drawable">折线</el-radio-button>
        <el-radio-button value="pie" :disabled="!drawable">饼图</el-radio-button>
      </el-radio-group>
      <span class="result-actions">
        <span class="result-count" v-if="'table' === mode">共 {{ labels.length }} 项</span>
        <!-- 导出图片：表格视图没有画布，先切到图表视图再导出 -->
        <el-tooltip :content="'table' === mode ? '切换到图表视图后可下载图片' : '下载 PNG 图片'" placement="top">
          <el-button class="result-download" link size="small" :icon="Download" @click="handleDownload" />
        </el-tooltip>
      </span>
    </div>
    <!-- 表格视图：分类列 + 各系列列 -->
    <el-table class="result-table" :data="tableRows" size="small" border max-height="260" v-if="'table' === mode">
      <el-table-column
        :key="column.prop"
        :label="column.label"
        :prop="column.prop"
        min-width="110"
        show-overflow-tooltip
        v-for="column in tableColumns">
      </el-table-column>
    </el-table>
    <!-- 图表视图：按分类轴与数值列渲染 -->
    <div class="result-chart" ref="chartRef" v-else></div>
    <div class="result-tips" v-if="'table' !== mode && !drawable">图表数据不可用，已切换到表格</div>
  </div>
</template>

<style lang="scss" scoped>
.data-result {
  /* 气泡是 inline-block 收缩宽度：给个最小宽度，避免回复文字很短时把图表挤成一条线 */
  min-width: 320px;
  /* 图表标题：模型归纳时给出的统计口径 */
  .result-title {
    font-size: 13px;
    font-weight: 500;
    line-height: 1.6;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }
  /* 图表说明：数据来源，较长时允许折行 */
  .result-caption {
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
    word-break: break-all;
  }
  /* 顶部一行：左侧视图切换，右侧表格行数（仅表格视图展示） */
  .result-head {
    @include flex-between();
    /* 面板很窄时行数换行显示，避免撑出横向滚动条 */
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 4px;
    /* 行数与下载按钮：靠右排列 */
    .result-actions {
      flex: none;
      @include flex-start();
      gap: 4px;
    }
    .result-count {
      flex: none;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
    .result-download {
      flex: none;
      padding: 0 2px;
      color: var(--el-text-color-secondary);
    }
  }
  .result-table {
    margin-top: 6px;
  }
  /* 图表容器固定高度，随宽度自适应（ResizeObserver 触发 resize） */
  .result-chart {
    width: 100%;
    height: 240px;
    margin-top: 6px;
  }
  .result-tips {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
</style>
