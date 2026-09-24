<script setup lang="ts">
/**
 * 远程搜索下拉选择器 - 通过回调函数远程搜索并展示下拉选项，支持单选/多选，自动回显已选值。
 *
 * @v-model  {*}                选中值（双向绑定主值），多选时为数组
 * @prop     {Function}         callback     - 远程搜索回调（必填），签名为 (params) => Promise<{ data: { rows: Row[] } }>
 * @prop     {Row[]}            data         - 最后一次检索结果，通过 v-model:data 传入
 * @prop     {Row|Row[]}        selected     - 选中项对应的完整行数据，通过 v-model:selected 传入
 * @prop     {Boolean}          multiple     - 是否多选，默认 false
 * @prop     {Boolean}          clearable    - 是否可清空，默认 false
 * @prop     {String}           placeholder  - 占位文本，默认"输入关键词进行查找"
 * @prop     {String}           fieldKey     - 用作唯一标识的字段名，默认 'id'，支持 `a.b` 形式路径
 * @prop     {String}           fieldValue   - 用作值的字段名，默认 'id'，支持 `a.b` 形式路径
 * @prop     {String}           fieldLabel   - 用作标签的字段名，默认 'name'，同时作为远程搜索的关键词字段，支持 `a.b` 形式路径
 * @prop     {Function}         labelFormatter - 标签内容格式化函数，签名为 (row, index) => String，未设置时取 fieldLabel 对应字段
 * @prop     {*}                exceptIds    - 排除的记录 ID
 * @prop     {Number}           pageSize     - 分页大小，默认 15
 * @prop     {Function}         parameter    - 扩展查询参数函数，签名为 (query: string) => Object
 * @prop     {String}           groupField   - 分组展示：行数据里作为分组名的字段名，不设置时平铺展示，支持 `a.b` 形式路径
 *
 * @emits    {Function} change - 选中变化，参数：(value, selected, data)
 *                                 value    - 选中值
 *                                 selected - 选中值对应的完整行数据
 *                                 data     - 最后一次检索结果数组
 *
 * 行数据结构 (Row):
 *   { id: any, name: string, ... } — 需包含 fieldKey、fieldValue、fieldLabel 对应的字段
 *   可选 disabled 字段标记该项不可选；设置 groupField 后按该字段的值分组展示
 *
 * @example
 * <form-select
 *   v-model="userId"
 *   v-model:selected="selectedUser"
 *   :callback="UserApi.search"
 *   placeholder="搜索用户"
 * />
 * <form-select
 *   v-model="modelIds"
 *   :callback="ModelApi.list"
 *   :labelFormatter="(row, index) => row.alias ? row.name + '(' + row.alias + ')' : row.name"
 *   multiple
 * />
 */
import DataUtil from '@/utils/DataUtil';
import { computed, ref, watch } from 'vue';

const {
  multiple = false,
  clearable = false,
  placeholder = '输入关键词进行查找',
  fieldKey = 'id',
  fieldValue = 'id',
  fieldLabel = 'name',
  labelFormatter = undefined,
  exceptIds = '',
  pageSize = 15,
  callback,
  parameter = undefined,
  groupField = '',
} = defineProps({
  multiple: { type: Boolean, required: false },
  clearable: { type: Boolean, required: false },
  placeholder: { type: String, required: false },
  fieldKey: { type: String, required: false },
  fieldValue: { type: String, required: false },
  fieldLabel: { type: String, required: false },
  labelFormatter: { type: Function, required: false },
  exceptIds: { required: false },
  pageSize: { type: Number, required: false },
  callback: Function,
  parameter: { type: Function, required: false },
  groupField: { type: String, required: false },
})

const model: any = defineModel()
const data = defineModel<Object[]>('data', { default: () => [] })
const selected = defineModel('selected', { type: [Object, Array<Object>], default: null })
const emit = defineEmits(['change'])
const options: any = ref([])
const loading = ref(false)

/**
 * 取值：字段名支持 `a.b` 形式的路径，嵌套数据可直接用 `xxInfo.name` 取值。
 * fieldKey / fieldValue / fieldLabel / groupField 都走这里，扁平字段与嵌套字段一致处理
 */
const valueOf = (item: any, field: string) => {
  return DataUtil.value(item, field)
}

/** 按取值字段建立索引：行数据可能取嵌套路径，不能直接用 array2map */
const mapByValue = (rows: any[]) => {
  const result: Record<string, any> = {}
  ;(rows ?? []).forEach((item: any) => { result[String(valueOf(item, fieldValue))] = item })
  return result
}

// 标签内容：由调用端通过 labelFormatter 自行组装，未设置时取 fieldLabel 对应字段
const label = (item: any, index: number) => {
  return labelFormatter ? labelFormatter(item, index) : valueOf(item, fieldLabel)
}

const handleCallback = async (params: any) => {
  if (!callback) return
  loading.value = true
  options.value = await callback(params).then((result: any) => {
    data.value = result.data.rows
    if (multiple) {
      // 过滤后最后一次检索结果可能不包含已选中内容
      const map = mapByValue(result.data.rows.concat(selected.value || []))
      selected.value = model.value?.map((v: any) => map[v])
    } else {
      const map = mapByValue(result.data.rows)
      selected.value = map[model.value]
    }
    return result.data.rows.map((item: any, index: number) => {
      // 带上原始行与不可选标记：分组展示按 row[groupField] 归类，disabled 的候选项不可选
      return { key: valueOf(item, fieldKey), value: valueOf(item, fieldValue), label: label(item, index), row: item, disabled: !!item.disabled }
    })
  }).catch(() => []).finally(() => {
    loading.value = false
  })
}

const handleParameter = (params: any, query: string) => {
  return Object.assign({}, { pageSize, exceptIds }, params, parameter && parameter(query))
}

/** 分组展示：按行数据的 groupField 归类；未设置 groupField 时仍是平铺选项 */
const groups = computed(() => {
  const map = new Map<string, any[]>()
  options.value.forEach((item: any) => {
    const name = String(valueOf(item.row, groupField) ?? '')
    if (!map.has(name)) map.set(name, [])
    map.get(name)!.push(item)
  })
  return Array.from(map, ([label, rows]) => ({ label, rows }))
})

watch(model, (value, oldValue) => {
  if (value === oldValue || DataUtil.empty(value)) return
  // 用户从下拉选项中选择时，选中项已存在于当前选项或回显数据中，无需再次远程检索
  const known = new Set<string>()
  options.value.forEach((item: any) => known.add(String(item.value)))
  const selectedRows = DataUtil.isArray(selected.value) ? selected.value : (selected.value ? [selected.value] : [])
  selectedRows.forEach((item: any) => item && known.add(String(valueOf(item, fieldValue))))
  const values = DataUtil.isArray(value) ? value : [value]
  if (values.every((item: any) => known.has(String(item)))) return
  const size = Math.max(pageSize, DataUtil.isArray(model.value) ? model.value.length : 1)
  handleCallback(handleParameter({ [fieldValue]: model.value, pageSize: size }, ''))
}, { immediate: true })

const remoteMethod = (query: string) => {
  handleCallback(handleParameter({ [fieldLabel]: query }, query))
}

const handleChange = (value: any) => {
  if (DataUtil.isArray(value)) {
    // 过滤后最后一次检索结果可能不包含已选中内容
    const map = mapByValue(data.value.concat(selected.value || []))
    selected.value = value.map((v: any) => map[v])
  } else {
    const map = mapByValue(data.value)
    selected.value = map[value]
  }
  emit('change', value, selected.value, data.value)
}
</script>

<template>
  <el-select
    v-model="model"
    autocomplete="off"
    :multiple="multiple"
    filterable
    remote
    :clearable="clearable"
    :placeholder="placeholder"
    remote-show-suffix
    :remote-method="remoteMethod"
    :loading="loading"
    @change="handleChange"
  >
    <template v-if="groupField">
      <el-option-group :key="group.label" :label="group.label" v-for="group in groups">
        <el-option
          :key="item.key"
          :label="item.label"
          :value="item.value"
          :disabled="item.disabled"
          v-for="item in group.rows" />
      </el-option-group>
    </template>
    <template v-else>
      <el-option
        :key="item.key"
        :disabled="item.disabled"
        :label="item.label"
        :value="item.value"
        v-for="item in options" />
    </template>
  </el-select>
</template>

<style lang="scss" scoped>
</style>
