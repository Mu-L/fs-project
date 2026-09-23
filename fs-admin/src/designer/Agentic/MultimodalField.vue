<script setup lang="ts">
/**
 * 多模态输入参数 - 参数名、输入类型（图片/音频/视频/文件）与变量；
 * 默认关闭，开启后才展开配置内容，字段说明放在标题后的帮助图标里。
 *
 * @v-model {Array} 参数数组
 * @v-model:enabled {Boolean} 是否启用多模态输入
 * @prop {*} instance   - 画布实例（X6Container 暴露的 flow）
 * @prop {*} activeItem - 当前激活的节点，用于排除自身
 */
import { computed } from 'vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import FieldSlice from './FieldSlice.vue'
import SectionSlice from './SectionSlice.vue'

const model: any = defineModel<any[]>({ required: true })
const enabled = defineModel<boolean>('enabled', { default: false })
const { instance, activeItem = {} } = defineProps<{
  instance?: any,
  activeItem?: any,
}>()

const columns = computed(() => [{
  prop: 'name', label: '参数名', icon: 'PriceTag', placeholder: '参数名，如 image', default: '',
}, {
  prop: 'type', type: 'select', options: 'multimodalTypes', icon: 'Grid', default: 'image', placeholder: '输入类型',
}, {
  prop: 'variable', type: 'variable', label: '变量', icon: 'Aim', placeholder: '请选择图片/音频/文件变量', default: '',
}])
</script>

<template>
  <SectionSlice title="多模态">
    <template #title>
      多模态
      <LayoutHelp text="无多模态输入时可保持关闭；需要图片、音频、视频或文件输入时开启并添加参数" />
    </template>
    <template #actions>
      <el-switch v-model="enabled" title="是否启用多模态输入" />
    </template>
    <el-form-item label="" v-if="enabled">
      <FieldSlice
        v-model="model"
        :columns="columns"
        :instance="instance"
        :active-item="activeItem"
        collapsible
        add-text="添加输入参数" />
    </el-form-item>
  </SectionSlice>
</template>

<style lang="scss" scoped>
</style>
