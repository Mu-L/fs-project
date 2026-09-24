<script setup lang="ts">
/**
 * 模型统计 - 调用量与评价。
 *
 * 版式：筛选 → 概览 → 模型（图表行 + 表格行）→ 用户（图表行 + 表格行）。
 * 图表与表格分行而不是并排：并排时表格被压到几列宽反而看不清，分开后
 * 图表看走势、表格往下钻，各占满行宽。
 *
 * 图表直接用 ECharts 画，按各自的数据形态选形式：
 * - 模型调用量：横向条形，按量排序——模型名较长，横着放才读得全；
 * - 模型评价：横向堆叠条，点赞与点踩同一条上看正负比例与评价量；
 * - 用户 × 模型：纵向堆叠柱，每个用户一根柱，柱内按模型分段。
 *
 * 数据来自调用明细表（一次提交里一个模型一次调用），能按时间 / 模型 / 用户筛。
 *
 * 评价来自结果卡片上的赞 / 踩，三个派生量别混：
 * - 整体评价 = 赞 − 踩，看的是口碑好坏（净口碑，可正可负）；
 * - 评价体量 = 赞 + 踩，是评分的分母，只看「被评了几次」；
 * - 评分 = 赞 / 评价体量 × 100。没有任何评价时给「暂无评分」，不用 0 分冒充。
 *
 * 默认统计近一周——一打开就是全量数据的口径没人看。
 * 注意：这里展示的是**全部用户**的汇总，不只是本人的。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Search } from '@element-plus/icons-vue'
import CompareApi from '@/api/agent/CompareApi'
import UserApi from '@/api/member/UserApi'
import ApiUtil from '@/utils/ApiUtil'

const DAY = 24 * 60 * 60 * 1000
/** 图表配色：前几个取 Element Plus 的语义色，与页面其余部分一致 */
const COLORS = ['#409eff', '#67c23a', '#e6a23c', '#909399', '#f56c6c', '#79bbff', '#95d475', '#eebe77', '#b1b3b8', '#f89898']
/** 用户柱状图最多画几个用户、几个模型；超出的模型合并成「其他」，免得堆叠条被切成十几段 */
const USER_LIMIT = 12
const MODEL_LIMIT = 8

/** 默认近一周：含今天共 7 天 */
const defaultRange = (): any[] => {
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  return [new Date(end.getTime() - 6 * DAY), end]
}

const loading = ref(false)
const models = ref<any[]>([])
const users = ref<any[]>([])
const userModels = ref<any[]>([])
const modelOptions = ref<any[]>([])
const filter = ref<any>({ range: defaultRange(), model: '', uid: '' })

/** 日期两端补成当天 00:00:00 / 23:59:59，避免「选到某天却查不到那天的记录」 */
const buildParam = () => {
  const range: any[] = filter.value.range ?? []
  return {
    beginTime: range[0] ? new Date(range[0]).setHours(0, 0, 0, 0) : 0,
    endTime: range[1] ? new Date(range[1]).setHours(23, 59, 59, 999) : Date.now(),
    model: filter.value.model || '',
    uid: Number(filter.value.uid) || 0,
  }
}

const load = (warning = false) => {
  loading.value = true
  CompareApi.statistic(buildParam(), { warning, error: false }).then((result: any) => {
    const data: any = ApiUtil.data(result) ?? {}
    models.value = data.models ?? []
    users.value = data.users ?? []
    userModels.value = data.userModels ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleSearch = () => load(true)
const handleReset = () => {
  filter.value = { range: defaultRange(), model: '', uid: '' }
  load()
}

/* ------------------------------- 显示格式化 ------------------------------- */

const renderUser = (row: any) => row?.createdUserInfo?.name || `#${row?.createdUid ?? 0}`
/** 毫秒转秒：统计里看到的是「平均」量级，留一位小数够了 */
const renderSeconds = (value: any) => {
  const ms = Number(value ?? 0)
  return ms < 1 ? '—' : (ms / 1000).toFixed(1) + 's'
}
/** 净口碑带符号：正数补 +，零就是 0，负数自带 - */
const renderNet = (value: any) => {
  const net = Number(value ?? 0)
  return net > 0 ? `+${net}` : String(net)
}
/** 净口碑的展示类名：正绿负红，零中性 */
const netClass = (value: any) => {
  const net = Number(value ?? 0)
  return net > 0 ? 'is-positive' : (net < 0 ? 'is-negative' : '')
}

/* ------------------------------- 图表 ------------------------------- */

/** 容器用函数 ref 收进来：三张图共用一套渲染与尺寸监听，不必各写一遍 */
const doms = new Map<string, HTMLDivElement>()
/** 实例按「容器元素」索引：ResizeObserver 回调里拿到的就是元素，用它才好反查 */
const instances = new Map<HTMLElement, echarts.ECharts>()
let observer: ResizeObserver | null = null

const bind = (key: string) => (el: any) => {
  if (el) doms.set(key, el as HTMLDivElement)
  else doms.delete(key)
}
const bindModel = bind('model')
const bindRated = bind('rated')
const bindNet = bind('net')
const bindUser = bind('user')

const renderChart = (key: string, option: any) => {
  const el = doms.get(key)
  if (!el || !option) return
  let chart = instances.get(el)
  if (!chart) {
    chart = echarts.init(el)
    instances.set(el, chart)
    observer?.observe(el)
  }
  chart.setOption(option, true)
}

// 柱状图的高度跟着条目数走：模型多时不会被压成一堆细线
const modelChartHeight = computed(() => Math.max(200, models.value.length * 30 + 40) + 'px')
/** 评价体量图底部有图例，多留一行 */
const volumeChartHeight = computed(() => Math.max(220, models.value.length * 30 + 60) + 'px')

/**
 * 三张模型图共用同一套分类与顺序（按调用量升序，横向条从下往上画，量大的在最上面）。
 * 顺序一致是为了行对行地横着看：同一个模型调得多不多、评价多不多、口碑正不正。
 */
const chartRows = computed(() => [...models.value].sort((a: any, b: any) => Number(a.total || 0) - Number(b.total || 0)))
/** 某行评价的三个派生量：体量看着色长度，净口碑看数字 */
const rated = (row: any) => {
  const positive = Number(row?.positive || 0)
  const negative = Number(row?.negative || 0)
  return { positive, negative, volume: positive + negative, net: positive - negative }
}

/** 调用量：横向条形 */
const modelOption = computed(() => {
  const rows = chartRows.value
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: rows.map((item: any) => item.model), axisLabel: { fontSize: 11, width: 130, overflow: 'truncate' } },
    series: [{
      name: '调用量',
      type: 'bar',
      barMaxWidth: 16,
      itemStyle: { color: COLORS[0], borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 11, color: '#909399' },
      data: rows.map((item: any) => Number(item.total || 0)),
    }],
  }
})

/**
 * 模型评价：横向堆叠条同时给出两个量。
 * - 条形总长 = 评价体量（赞 + 踩），绿红两段的比例就是正负之比；
 * - 条尾标注 = 体量与净口碑（赞 − 踩），体量看长度、净口碑看数字，不用来回换算。
 */
const ratedOption = computed(() => {
  const rows = chartRows.value
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any[]) => {
        const row = rows[params?.[0]?.dataIndex ?? 0]
        const item = rated(row)
        return [
          String(row?.model ?? ''),
          `点赞：${item.positive}`,
          `点踩：${item.negative}`,
          `评价体量：${item.volume}`,
        ].join('<br/>')
      },
    },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    // 条尾标出体量：长度已经表达了量级，但具体数字还是要给
    grid: { left: 8, right: 48, top: 8, bottom: 28, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: rows.map((item: any) => item.model), axisLabel: { fontSize: 11, width: 130, overflow: 'truncate' } },
    series: [
      {
        name: '点赞',
        type: 'bar',
        stack: 'rated',
        barMaxWidth: 16,
        itemStyle: { color: COLORS[1] },
        // 点踩为零时右端就是点赞，标签改挂在它上面（见点踩段注释）
        label: {
          show: false,
          position: 'right',
          fontSize: 11,
          color: '#909399',
          formatter: (params: any) => String(rated(rows[params.dataIndex]).volume),
        },
        data: rows.map((item: any) => ({
          value: Number(item.positive || 0),
          label: { show: 0 === Number(item.negative || 0) && Number(item.positive || 0) > 0 },
        })),
      }, {
        name: '点踩',
        type: 'bar',
        stack: 'rated',
        barMaxWidth: 16,
        itemStyle: { color: COLORS[4] },
        // 标签挂在最后一段上：点踩为零时右端是点赞，零长度的段不画形状也就不会出标签，
        // 所以按行挑一段挂
        label: {
          show: false,
          position: 'right',
          fontSize: 11,
          color: '#909399',
          formatter: (params: any) => String(rated(rows[params.dataIndex]).volume),
        },
        data: rows.map((item: any) => ({
          value: Number(item.negative || 0),
          label: { show: Number(item.negative || 0) > 0 },
        })),
      },
    ],
  }
})

/**
 * 整体评价：净口碑 = 赞 − 踩，双向条——正数往右、负数往左，一眼看出正负；
 * 色值跟着正负走，零值只留一条贴轴的空条（说明「有评价但刚好打平」）。
 */
const netOption = computed(() => {
  const rows = chartRows.value
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any[]) => {
        const row = rows[params?.[0]?.dataIndex ?? 0]
        const item = rated(row)
        return [
          String(row?.model ?? ''),
          `整体评价：${renderNet(item.net)}`,
          `（${item.positive} 赞 − ${item.negative} 踩）`,
        ].join('<br/>')
      },
    },
    // 负值的标签落在条左侧，左右都要留出位置
    grid: { left: 40, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: rows.map((item: any) => item.model), axisLabel: { fontSize: 11, width: 130, overflow: 'truncate' } },
    series: [{
      name: '整体评价',
      type: 'bar',
      barMaxWidth: 16,
      data: rows.map((item: any) => {
        const net = rated(item).net
        return {
          value: net,
          itemStyle: { color: net < 0 ? COLORS[4] : COLORS[1], borderRadius: net < 0 ? [3, 0, 0, 3] : [0, 3, 3, 0] },
          // 正数标在条右端、负数标在条左端，文字才不会压在条上或跑出画布
          label: { show: true, position: net < 0 ? 'left' : 'right', fontSize: 11, color: '#909399' },
        }
      }),
      label: { formatter: (params: any) => renderNet(params.value) },
    }],
  }
})

/** 用户 × 模型：纵向堆叠柱，每个用户一根，柱内按模型分段 */
const topUserList = computed(() => users.value.slice(0, USER_LIMIT))
/** 模型按总量排名：前若干进入堆叠，其余合并成「其他」 */
const modelRank = computed(() => {
  const map = new Map<string, number>()
  userModels.value.forEach((row: any) => map.set(row.model, (map.get(row.model) ?? 0) + Number(row.total || 0)))
  return [...map.entries()].sort((a, b) => b[1] - a[1]).map((item) => item[0])
})

const userOption = computed(() => {
  const rows = topUserList.value
  const topNames = modelRank.value.slice(0, MODEL_LIMIT)
  const names = modelRank.value.length > MODEL_LIMIT ? topNames.concat(['其他']) : topNames
  const counts = new Map<string, number>()
  userModels.value.forEach((row: any) => counts.set(`${row.createdUid}|${row.model}`, Number(row.total || 0)))
  const series = names.map((name: string, index: number) => ({
    name,
    type: 'bar',
    stack: 'calls',
    barMaxWidth: 44,
    itemStyle: { color: COLORS[index % COLORS.length] },
    data: rows.map((user: any) => {
      if ('其他' !== name) return counts.get(`${user.createdUid}|${name}`) ?? 0
      // 「其他」= 该用户总量减去已单列的模型之和，保证柱高与总量一致
      const known = topNames.reduce((sum: number, model: string) => sum + (counts.get(`${user.createdUid}|${model}`) ?? 0), 0)
      return Math.max(0, Number(user.total || 0) - known)
    }),
  }))
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      // 标题里带上「评测模型数」，与用户表的口径对上
      formatter: (params: any[]) => {
        const user = rows[params?.[0]?.dataIndex ?? 0]
        const total = (params ?? []).reduce((sum: number, item: any) => sum + Number(item.value || 0), 0)
        const lines = (params ?? [])
          .filter((item: any) => Number(item.value) > 0)
          .map((item: any) => `${item.marker}${item.seriesName}：${item.value}`)
        return [`${renderUser(user)} · ${user?.modelCount ?? 0} 个模型 · 共 ${total} 次`].concat(lines).join('<br/>')
      },
    },
    legend: { type: 'scroll', bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    grid: { left: 8, right: 16, top: 10, bottom: 34, containLabel: true },
    xAxis: {
      type: 'category',
      data: rows.map((user: any) => renderUser(user)),
      axisLabel: { fontSize: 11, interval: 0, rotate: rows.length > 6 ? 30 : 0, hideOverlap: true },
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
    series,
  }
})

const renderAll = () => {
  renderChart('model', modelOption.value)
  renderChart('rated', ratedOption.value)
  renderChart('net', netOption.value)
  renderChart('user', userOption.value)
}

/* ------------------------------- 概览与生命周期 ------------------------------- */

const totalCalls = computed(() => models.value.reduce((sum: number, item: any) => sum + Number(item.total || 0), 0))
const totalFinished = computed(() => models.value.reduce((sum: number, item: any) => sum + Number(item.finished || 0), 0))
/** 整体评价取净口碑：赞减踩，而不是两个数累加（累加只是「被评了几次」，不是评价好坏） */
const totalNet = computed(() => models.value.reduce(
  (sum: number, item: any) => sum + Number(item.positive || 0) - Number(item.negative || 0), 0))
const overview = computed(() => [
  { label: '调用总量', value: totalCalls.value },
  { label: '覆盖模型', value: models.value.length },
  { label: '参与用户', value: users.value.length },
  { label: '整体评价', value: renderNet(totalNet.value) },
  {
    label: '整体完成率',
    value: totalCalls.value < 1 ? '—' : Math.round(totalFinished.value * 100 / totalCalls.value) + '%',
  },
])

watch([models, users, userModels], () => nextTick(renderAll), { deep: true })

onMounted(() => {
  observer = new ResizeObserver((entries) => {
    entries.forEach((entry) => instances.get(entry.target as HTMLElement)?.resize())
  })
  // 模型筛选的候选取自网关可达模型；取不到不影响别的，输入框留空即全部
  CompareApi.models({ warning: false, error: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    modelOptions.value = Array.isArray(data) ? data : (data?.rows ?? [])
  }).catch(() => {})
  load()
  nextTick(renderAll)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  instances.forEach((chart) => chart.dispose())
  instances.clear()
})
</script>

<template>
  <div class="stat-page" v-loading="loading">
    <!-- 筛选：时间 / 模型 / 用户，默认近一周。用列表页统一的 form-search 栅格，
         标签宽度、列宽与换行都由它管，不必再手拼 flex -->
    <el-card class="stat-card fs-table-search" :bordered="false" shadow="never">
      <form-search :model="filter">
        <form-search-item label="时间" prop="range">
          <el-date-picker
            v-model="filter.range"
            type="daterange"
            :clearable="false"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期" />
        </form-search-item>
        <form-search-item label="模型" prop="model">
          <el-select v-model="filter.model" filterable clearable placeholder="全部模型">
            <el-option v-for="item in modelOptions" :key="item.name" :value="item.name" :label="item.name" />
          </el-select>
        </form-search-item>
        <form-search-item label="用户" prop="uid">
          <form-select v-model="filter.uid" :callback="UserApi.list" clearable placeholder="全部用户" />
        </form-search-item>
        <form-search-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </form-search-item>
      </form-search>
    </el-card>

    <!-- 概览 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="overview">
        <div class="overview-item" v-for="item in overview" :key="item.label">
          <span class="overview-value">{{ item.value }}</span>
          <span class="overview-label">{{ item.label }}</span>
        </div>
        <span class="overview-tip">统计全部用户的调用与评价</span>
      </div>
    </el-card>

    <!-- 模型：图表行 → 表格行 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="section-title">模型</div>
      <el-row :gutter="12">
        <el-col :xs="24" :md="8">
          <div class="chart-title">调用量</div>
          <div class="chart" :style="{ height: modelChartHeight }" :ref="bindModel"></div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="chart-title">评价体量 <span class="chart-tip">点赞 + 点踩</span></div>
          <div class="chart" :style="{ height: volumeChartHeight }" :ref="bindRated"></div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="chart-title">整体评价 <span class="chart-tip">点赞 − 点踩，正右负左</span></div>
          <div class="chart" :style="{ height: modelChartHeight }" :ref="bindNet"></div>
        </el-col>
      </el-row>
      <el-table class="section-table" :data="models" size="small" border table-layout="auto">
        <el-table-column prop="model" label="模型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="total" label="调用量" width="90" />
        <el-table-column prop="positive" label="点赞" width="80" />
        <el-table-column prop="negative" label="点踩" width="80" />
        <!-- 整体评价是净口碑（赞 − 踩），与「被评了几次」不是一回事 -->
        <el-table-column label="整体评价" width="110">
          <template #default="scope">
            <span class="muted" v-if="!scope.row.rated">—</span>
            <span class="net" :class="netClass(scope.row.positive - scope.row.negative)" v-else>
              {{ renderNet(scope.row.positive - scope.row.negative) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="110">
          <template #default="scope">
            <span class="muted" v-if="!scope.row.rated">暂无评分</span>
            <span class="score" v-else>{{ scope.row.score }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="完成率" width="90">
          <template #default="scope">{{ scope.row.successRate }}%</template>
        </el-table-column>
        <el-table-column prop="finished" label="完成" width="80" />
        <el-table-column prop="aborted" label="中断" width="80" />
        <el-table-column prop="errored" label="失败" width="80" />
        <el-table-column label="平均耗时" width="100">
          <template #default="scope">{{ renderSeconds(scope.row.avgDuration) }}</template>
        </el-table-column>
        <el-table-column label="平均首字" width="100">
          <template #default="scope">{{ renderSeconds(scope.row.avgFirstToken) }}</template>
        </el-table-column>
      </el-table>
      <el-empty description="该条件下没有调用记录" :image-size="70" v-if="!models.length && !loading" />
    </el-card>

    <!-- 用户：图表行 → 表格行 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="section-title">用户</div>
      <el-row :gutter="12">
        <el-col :span="24">
          <div class="chart-title">
            评测调用量 · 按模型分段
            <span class="chart-tip">最多展示前 {{ USER_LIMIT }} 位用户；模型取前 {{ MODEL_LIMIT }} 个，其余并入「其他」</span>
          </div>
          <div class="chart is-tall" :ref="bindUser"></div>
        </el-col>
      </el-row>
      <el-table class="section-table" :data="users" size="small" border table-layout="auto">
        <el-table-column label="用户" min-width="200">
          <template #default="scope">{{ renderUser(scope.row) }}</template>
        </el-table-column>
        <el-table-column prop="total" label="评测调用量" width="120" />
        <el-table-column prop="modelCount" label="评测模型数" width="120" />
        <el-table-column prop="rated" label="参与评价次数" width="130" />
      </el-table>
      <el-empty description="该条件下没有调用记录" :image-size="70" v-if="!users.length && !loading" />
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.stat-page {
  .stat-card {
    & + .stat-card {
      margin-top: 12px;
    }
    :deep(.el-card__body) {
      padding: 14px 16px;
    }
  }
  /* 筛选卡用的是列表页通用的 fs-table-search：它自带 margin-bottom，
     这里归零，让相邻卡的 margin-top 统一管行距，免得两处叠加 */
  .stat-card.fs-table-search {
    margin-bottom: 0;
  }
  /* 下拉类控件占满栅格；日期区间由 FormSearchItem 自带的规则撑满 */
  :deep(.el-form-item__content > .el-select) {
    width: 100%;
  }
}
/* 图表标题：与表格的 section-title 区分开，字号更小、颜色更浅 */
.chart-title {
  @include flex-start();
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  .chart-tip {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.chart {
  width: 100%;
  height: 240px;
  &.is-tall {
    height: 320px;
  }
}
/* 概览：数字一排，末尾提示靠右 */
.overview {
  @include flex-start();
  flex-wrap: wrap;
  gap: 16px 40px;
  .overview-item {
    @include flex-start-column();
    flex: none;
    gap: 2px;
    .overview-value {
      font-size: 22px;
      font-weight: 600;
      line-height: 1.2;
      color: var(--el-text-color-primary);
      font-variant-numeric: tabular-nums;
    }
    .overview-label {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }
  .overview-tip {
    margin-left: auto;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.section-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
/* 表格单独一行，与上方图表留开 */
.section-table {
  margin-top: 12px;
}
.score {
  font-weight: 500;
  color: var(--el-color-success);
}
/* 净口碑：正绿负红，零保持中性 */
.net {
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  &.is-positive {
    color: var(--el-color-success);
  }
  &.is-negative {
    color: var(--el-color-danger);
  }
}
.muted {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
