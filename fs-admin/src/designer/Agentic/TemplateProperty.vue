<script setup lang="ts">
/**
 * 模板转换节点属性 - 用 Jinja2 模板语法把数据渲染成字符串。
 * 模板里不再插入变量占位符：先在「输入变量」里把变量命名，再在模板中按名字引用（如 {{ input }}）。
 */
import { computed, ref } from 'vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const columns = computed(() => [{
  prop: 'name', label: '变量名', placeholder: '模板中的变量名，如 input', default: '',
}, {
  prop: 'variable', type: 'variable', label: '变量', placeholder: '请选择变量', default: '',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="输入变量">
          <template #title>
            输入变量
            <LayoutHelp text="先在这里把变量命名，模板中按名字引用，如 {{ input }}；模板里不能直接引用节点变量" />
          </template>
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.inputs"
              :columns="columns"
              :instance="$props.instance"
              :active-item="model"
              collapsible
              add-text="添加输入变量" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="模板内容（Jinja2）">
          <template #title>
            模板内容（Jinja2）
            <LayoutHelp text="按 Jinja2 语法渲染并输出字符串；没在输入变量里配置的名字，模板中取不到值" />
          </template>
          <el-form-item label="">
            <code-editor
              v-model="model.data.template"
              :height="220"
              resizable
              placeholder="请输入 Jinja2 模板，如：请总结以下内容：{{ input }}" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="输出配置">
          <el-form-item label="输出变量名">
            <el-input v-model="model.data.outputName" placeholder="如 output" />
          </el-form-item>
          <el-form-item label="输出类型">
            <el-select v-model="model.data.outputType" placeholder="请选择">
              <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.types" />
            </el-select>
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
