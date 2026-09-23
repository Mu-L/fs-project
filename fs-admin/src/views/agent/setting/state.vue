<script setup lang="ts">
/**
 * 智能体运行状态维护
 *
 * 左侧维护项导航，右侧展示当前维护任务的执行面板。
 */
import MaintainApi from '@/api/agent/MaintainApi';
import FormMaintainBox from '@/components/Form/FormMaintainBox.vue';

const items = [
  {
    key: 'reindexChunk',
    title: '检索块重建',
    description: '按知识库把数据库中的分块连同文档、分段状态写入检索索引，修复历史数据缺失结构字段导致的召回失效。',
    api: MaintainApi.reindexChunk,
    params: () => ({}),
  },
]
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="maintain-header">
        <span>智能体运行状态维护</span>
        <span class="maintain-tip">选择左侧维护项执行，执行过程会以 SSE 方式实时展示。</span>
      </div>
    </template>
    <FormMaintainBox :items="items" height="320px" />
  </el-card>
</template>

<style lang="scss" scoped>
.maintain-header {
  @include flex-between();

  .maintain-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: normal;
  }
}
</style>