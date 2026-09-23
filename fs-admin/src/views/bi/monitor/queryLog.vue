<script setup lang="ts">
/**
 * 数据查询日志 - 记录即席查询（SQL查询）、数据集查询与数据主题查询：
 * 列表展示查询类型、查询对象、查询用户、状态与耗时，详情给出完整查询语句、来源与服务端结果，
 * 便于数据审计与问题排查。
 */
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import DataQueryLogApi from '@/api/bi/DataQueryLogApi'
import UserApi from '@/api/member/UserApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'typeText', label: '查询类型' },
  { prop: 'targetName', label: '查询对象', minWidth: 160, showOverflowTooltip: true },
  { prop: 'createdUserInfo', label: '查询用户', slot: 'user' },
  { prop: 'createdUid', label: '用户ID', hide: true },
  { prop: 'sqlText', label: 'SQL语句', minWidth: 240, slot: 'sqlText' },
  { prop: 'status', label: '状态码', hide: true }, // 列表按可见列裁剪字段，状态标签需要状态值，故显式保留
  { prop: 'statusText', label: '状态', slot: 'status' },
  { prop: 'resultCode', label: '返回码', hide: true },
  { prop: 'message', label: '结果描述', minWidth: 160, slot: 'message' },
  { prop: 'rowCount', label: '返回行数' },
  { prop: 'columnCount', label: '返回列数', hide: true },
  { prop: 'duration', label: '耗时(毫秒)' },
  { prop: 'maxRows', label: '行数限制', hide: true },
  { prop: 'timeout', label: '超时(秒)', hide: true },
  { prop: 'requestIp', label: '来源IP' },
  { prop: 'userAgent', label: '客户端', hide: true },
  { prop: 'requestUrl', label: '请求地址', hide: true },
  { prop: 'createdTime', label: '查询时间', formatter: DateUtil.render },
])
const config: any = ref({ ready: false, types: {}, status: {}, sorts: {} })
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage), {
    columns: TableUtil.columns2query(columns.value, 'status')
  })
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DataQueryLogApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DataQueryLogApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

/* ------------------------------- 详情 ------------------------------- */

const infoVisible = ref(false)
const infoLoading = ref(false)
const info: any = ref({})

const handleShow = (scope: any) => {
  infoVisible.value = true
  infoLoading.value = true
  info.value = Object.assign({}, scope.row)
  DataQueryLogApi.info(scope.row.id).then((result: any) => {
    info.value = ApiUtil.data(result) ?? {}
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DataQueryLogApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

/** 查询用户：按标识填充的用户信息优先，取不到时回落到标识本身 */
const userText = (row: any) => row?.createdUserInfo?.name || (row?.createdUid ? row.createdUid : '系统')
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="查询类型" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="查询用户" prop="createdUid">
        <form-select v-model="filters.createdUid" :callback="UserApi.list" clearable placeholder="输入名称检索用户" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="查询对象" prop="targetName">
          <el-input v-model="filters.targetName" clearable />
        </form-search-item>
        <form-search-item label="日志ID" prop="id">
          <el-input v-model="filters.id" clearable />
        </form-search-item>
        <form-search-item label="查询开始时间" prop="createdTimeBegin">
          <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="查询结束时间" prop="createdTimeEnd">
          <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
        </form-search-item>
      </template>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'bi:dataQueryLog:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" @change="handleRefresh(true, true)" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s">
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #sqlText="scope">
          <span class="cell-ellipsis" :title="scope.row.sqlText">{{ scope.row.sqlText }}</span>
        </template>
        <template #message="scope">
          <span class="cell-ellipsis" :title="scope.row.message">{{ scope.row.message }}</span>
        </template>
        <template #user="scope">
          {{ userText(scope.row) }}
        </template>
        <template #status="scope">
          <el-tag :type="2 === scope.row.status ? 'danger' : 'success'" size="small" effect="plain">
            {{ scope.row.statusText }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:dataQueryLog:'">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'查询日志 - ' + (info.id ?? '')" size="60%">
    <!-- v-loading 必须挂在元素上：el-drawer 的根是 Teleport，指令挂在组件上不会生效 -->
    <div class="log-detail" v-loading="infoLoading">
    <el-alert class="log-alert" type="error" show-icon :closable="false" v-if="2 === info.status"
              :title="info.message || '查询失败'" />
    <layout-heading title="基础信息"></layout-heading>
    <el-descriptions :column="2" label-width="100px" border class="mb-15">
      <el-descriptions-item label="查询类型">{{ info.typeText }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="2 === info.status ? 'danger' : 'success'" size="small" effect="plain">{{ info.statusText }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="查询对象">{{ info.targetName || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="查询用户">{{ userText(info) }}</el-descriptions-item>
      <el-descriptions-item label="查询时间">{{ DateUtil.format(info.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="耗时">{{ info.duration }} 毫秒</el-descriptions-item>
      <el-descriptions-item label="返回行数">{{ info.rowCount }}</el-descriptions-item>
      <el-descriptions-item label="返回列数">{{ info.columnCount }}</el-descriptions-item>
      <el-descriptions-item label="行数限制">{{ info.maxRows }}</el-descriptions-item>
      <el-descriptions-item label="超时(秒)">{{ info.timeout }}</el-descriptions-item>
      <el-descriptions-item label="返回码">{{ info.resultCode }}</el-descriptions-item>
      <el-descriptions-item label="结果描述">{{ info.message || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="来源IP" :span="2">{{ info.requestIp || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="客户端" :span="2">{{ info.userAgent || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="请求地址" :span="2">{{ info.requestUrl || '暂无' }}</el-descriptions-item>
    </el-descriptions>
    <template v-if="info.detail">
      <layout-heading title="详细内容">
        <template #extra>
          <button-copy :content="info.detail" title="复制详细内容" text="复制" />
        </template>
      </layout-heading>
      <pre class="log-text mb-15">{{ info.detail }}</pre>
    </template>
    <layout-heading title="查询语句">
      <template #extra>
        <button-copy :content="info.sqlText" title="复制查询语句" text="复制" v-if="info.sqlText" />
      </template>
    </layout-heading>
    <pre class="log-text" v-if="info.sqlText">{{ info.sqlText }}</pre>
    <el-empty v-else :image-size="60" description="该查询未记录查询语句" />
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
.log-alert {
  margin-bottom: 12px;
}
/* 长文本单元格：单行展示，超出以省略号截断，鼠标悬浮通过 title 查看全文 */
.cell-ellipsis {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.log-text {
  max-height: 320px;
  padding: 10px;
  overflow: auto;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
