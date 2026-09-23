<script setup lang="ts">
/**
 * 条件编辑 - 维护一组比较条件，支持全部/任一逻辑，供条件分支、循环终止、列表过滤复用。
 * 每个条件默认收起，仅展示变量与运算符摘要，点击标题行展开编辑。
 *
 * @v-model  {Object} 条件对象 `{ logic: 'and'|'or', conditions: [{ variable, operator, value }] }`
 * @prop     {Boolean} field - 条件的「变量」是列表项字段名（列表过滤用），默认 false 时用画布变量选择器
 */
import { Plus } from '@element-plus/icons-vue'
import { watch } from 'vue'
import CollapseItem from './CollapseItem.vue'
import { useCollapse } from './collapse'
import config from './config'
import VariableSelect from './VariableSelect.vue'

const model: any = defineModel<any>({ required: true })
const props = defineProps<{
  instance?: any,
  activeItem?: any,
  emptyText?: string,
  field?: boolean,
}>()

/**
 * 兼容历史数据缺少条件字段的情况。
 * 用 watch 而不是 setup 里只跑一次：属性面板实例会在同类型节点之间复用，切换节点时同样要兜底。
 */
watch(model, (value: any) => {
  if (!value) return
  if (!Array.isArray(value.conditions)) value.conditions = []
  if (!value.logic) value.logic = 'and'
}, { immediate: true })

// 仅一条条件时默认展开，多条默认收起
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === model.value.conditions.length)

const handleAdd = () => {
  model.value.conditions.push({ variable: '', operator: 'eq', value: '' })
  open(model.value.conditions.length - 1)
}

const handleRemove = (index: number) => {
  model.value.conditions.splice(index, 1)
  remove(index)
}

const needValue = (operator: string) => config.noValueOperators.indexOf(operator) < 0

// 收起时的摘要：变量名与运算符
const summaryTags = (condition: any) => {
  const variable = String(condition?.variable ?? '')
  const operator: any = config.operators.find((item: any) => item.value === condition?.operator)
  return [
    variable ? variable.split('.').pop() : '未选变量',
    operator ? operator.label : condition?.operator,
  ].filter((text: any) => text)
}
</script>

<template>
  <div class="condition-slice">
    <div class="logic" v-if="model.conditions.length > 1">
      <span>满足</span>
      <el-select v-model="model.logic">
        <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in config.logicOperators" />
      </el-select>
      <span>条件</span>
    </div>
    <CollapseItem
      :key="index"
      v-for="(condition, index) in model.conditions"
      :title="'条件 ' + (index + 1)"
      :tags="summaryTags(condition)"
      :expanded="isOpen(index)"
      @toggle="toggle(index)"
      @delete="handleRemove(index)">
      <!-- 列表过滤：条件针对列表里的每一项，这里填的是列表项里的字段名，不是画布变量 -->
      <el-input
        v-if="props.field"
        v-model="condition.variable"
        placeholder="字段名，如 name，支持 a.b" />
      <VariableSelect
        v-else
        v-model="condition.variable"
        :instance="instance"
        :active-item="activeItem"
        allow-create
        placeholder="请选择变量" />
      <el-select v-model="condition.operator" placeholder="请选择运算符">
        <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in config.operators" />
      </el-select>
      <el-input
        v-if="needValue(condition.operator)"
        v-model="condition.value"
        placeholder="请输入比较值" />
    </CollapseItem>
    <el-button link type="primary" :icon="Plus" @click="handleAdd">添加条件</el-button>
  </div>
</template>

<style lang="scss" scoped>
.condition-slice {
  width: 100%;
  .logic {
    margin-bottom: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    @include flex-start();
    gap: 6px;
    .el-select {
      width: 80px;
    }
  }
  .collapse-item {
    .el-select, .el-input {
      width: 100%;
    }
    .el-select + .el-select, .el-select + .el-input {
      margin-top: 6px;
    }
  }
}
</style>
