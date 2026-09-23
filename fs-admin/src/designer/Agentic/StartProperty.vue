<script setup lang="ts">
/**
 * 开始节点属性 - 定义工作流的输入：
 * 固定输入（用户输入 query、文件列表 files）默认勾选启用，文件可配置上传数量与文件类型；
 * 自定义参数按文本、段落、数值等类型添加，并可配置是否必填。
 */
import { computed, ref, watch } from 'vue'
import FieldSlice from './FieldSlice.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import config from './config'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

const columns = computed(() => [{
  prop: 'name', label: '变量名称', icon: 'PriceTag', placeholder: '变量名称，如 language', default: '',
}, {
  prop: 'label', label: '标题名称', icon: 'Memo', placeholder: '展示名称（选填），为空时展示变量名称', default: '',
}, {
  prop: 'type', type: 'select', options: 'inputTypes', icon: 'Grid', default: 'String', placeholder: '参数类型',
}, {
  prop: 'description', label: '参数说明', icon: 'InfoFilled', placeholder: '参数说明（选填）', default: '',
}, {
  prop: 'required', type: 'switch', label: '是否必填', default: true,
}])

const fileTypes = computed<any[]>(() => props.config?.fileTypes ?? config.fileTypes ?? [])

/**
 * 兼容历史数据：补齐固定输入配置，避免属性面板读取到空值。
 * 用 immediate 的 watch 而不是 setup 里只跑一次，这样切换节点（组件实例复用）时同样会补齐。
 */
watch(model, (value: any) => {
  const data = value?.data
  if (!data || (data.query && data.files && Array.isArray(data.variables))) return
  config.startRepair?.(data)
}, { immediate: true })
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="固定输入">
          <div class="fixed-slice">
            <div class="fixed-item">
              <div class="fixed-head">
                <div class="fixed-name">
                  <el-switch v-model="model.data.query.enabled" />
                  <span class="name">用户输入</span>
                  <span class="meta">query</span>
                </div>
                <span class="type">String</span>
              </div>
              <el-form-item label="最大长度" v-if="model.data.query.enabled">
                <el-input-number v-model="model.data.query.maxLength" :controls="false">
                  <template #prefix><LayoutIcon name="EditPen" /></template>
                </el-input-number>
              </el-form-item>
            </div>
            <div class="fixed-item">
              <div class="fixed-head">
                <div class="fixed-name">
                  <el-switch v-model="model.data.files.enabled" />
                  <span class="name">文件列表</span>
                  <span class="meta">files</span>
                </div>
                <span class="type">Array&lt;File&gt;</span>
              </div>
              <template v-if="model.data.files.enabled">
                <el-form-item label="上传数量">
                  <el-input-number v-model="model.data.files.maxCount" :min="1" :max="20" :controls="false">
                    <template #prefix><LayoutIcon name="Files" /></template>
                  </el-input-number>
                </el-form-item>
                <el-form-item label="文件类型">
                  <el-select
                    v-model="model.data.files.fileTypes"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    placeholder="不限（默认全部类型）">
                    <template #prefix><LayoutIcon name="CollectionTag" /></template>
                    <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in fileTypes" />
                  </el-select>
                </el-form-item>
                <el-form-item label="">
                  <tip-text text="文件列表为文件信息数组，每项为文件存储服务返回的文件信息（文件ID、文件名、文件类型等），不是前端的 File 对象" />
                </el-form-item>
              </template>
            </div>
          </div>
        </SectionSlice>
        <SectionSlice title="自定义参数">
          <el-form-item label="">
            <FieldSlice v-model="model.data.variables" :columns="columns" collapsible add-text="添加自定义参数" />
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.fixed-slice {
  width: 100%;
  .fixed-item {
    padding: 8px 10px;
    /* 固定输入项：与自定义参数等折叠卡片同一套卡片语言——灰底上的白块 */
    border-radius: 4px;
    background: var(--fs-panel-surface);
    & + .fixed-item {
      margin-top: 6px;
    }
    .fixed-head {
      // 固定变量的名称与类型两端分布
      @include flex-between();
      .fixed-name {
        @include flex-start();
        .name {
          margin-left: 8px;
        }
        .meta {
          margin-left: 6px;
        }
      }
      .name {
        font-size: 13px;
        color: var(--el-text-color-primary);
      }
      .meta {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
      .type {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
    .el-form-item {
      margin: 6px 0 0;
      &:last-child {
        margin-bottom: 0;
      }
    }
    .el-input-number, .el-select {
      width: 100%;
    }
  }
}
</style>
