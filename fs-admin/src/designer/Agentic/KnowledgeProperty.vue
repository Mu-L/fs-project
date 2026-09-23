<script setup lang="ts">
/**
 * 知识检索节点属性 - 从知识库中查询与用户问题相关的文本内容。
 */
import { ref } from 'vue'
import KnowledgeApi from '@/api/agent/KnowledgeApi'
import MetadataTable from '@/components/Data/MetadataTable.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="检索配置">
          <el-form-item label="查询变量">
            <VariableSelect
              v-model="model.data.query"
              :instance="$props.instance"
              :active-item="model"
              allow-create
              placeholder="请选择查询内容来源" />
          </el-form-item>
          <el-form-item label="知识库">
            <form-select v-model="model.data.knowledgeIds" :callback="KnowledgeApi.list" multiple clearable placeholder="请选择知识库" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="元数据过滤">
          <el-form-item label="">
            <metadata-table v-model="model.data.metadata" :editable="true" :compact="true" />
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
