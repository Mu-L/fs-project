<script setup lang="ts">
/**
 * HTTP请求节点属性 - 通过 HTTP 协议发送服务器请求。
 * 录入方式参考数据接口配置（/bi/data/api）：请求方式与地址同一行，
 * 请求体按类型切换成键值表格或 javascript 模式的编辑器。
 */
import { computed, ref } from 'vue'
import MetadataTable from '@/components/Data/MetadataTable.vue'
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

// 请求体类型：表单用键值表格，JSON 用代码编辑器
const formBody = computed(() => ['form-data', 'x-www-form-urlencoded'].indexOf(model.value?.data?.body?.type) >= 0)
// json 与 raw 共用同一个内容字段，都用 javascript 模式的编辑器（与数据接口配置一致）
const contentBody = computed(() => ['json', 'raw'].indexOf(model.value?.data?.body?.type) >= 0)
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="请求配置">
          <el-form-item label="">
            <!-- 请求方式与地址同一行：与数据接口配置的录入方式一致 -->
            <el-input v-model="model.data.url" placeholder="请求地址，可插入变量">
              <template #prepend>
                <el-select v-model="model.data.method" style="width: 86px">
                  <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.httpMethods" />
                </el-select>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="超时时间(秒)">
            <el-input-number v-model="model.data.timeout" :min="1" :max="600" :controls="false" />
          </el-form-item>
          <el-form-item label="SSL校验" class="fs-form-inline">
            <el-switch v-model="model.data.sslVerify" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="请求头">
          <el-form-item label="">
            <metadata-table v-model="model.data.headers" :editable="true" :compact="true" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="请求体">
          <el-form-item label="请求参数">
            <!-- 选项名较长（x-www-form-urlencoded 等），用下拉而不是分段按钮，窄面板下不会换行占高度 -->
            <el-select v-model="model.data.body.type" placeholder="请选择">
              <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.httpBodyTypes" />
            </el-select>
          </el-form-item>
          <el-form-item label="" v-if="formBody">
            <metadata-table v-model="model.data.body.form" :editable="true" :compact="true" />
          </el-form-item>
          <el-form-item label="" v-if="contentBody">
            <code-editor
              v-model="model.data.body.content"
              mode="javascript"
              :height="160"
              resizable
              :placeholder="'json' === model.data.body.type ? '请输入 JSON 内容，可插入变量' : '请输入内容，可插入变量'" />
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
