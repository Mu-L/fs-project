<script setup lang="ts">
/**
 * 模型对比 - 打开工作区的弹窗。
 *
 * 工作区不在工作台上常驻，点「打开」才出来：默认私有，共享后所有人可见；
 * 行本身就是「打开」，共享与删除只出现在自己的行上——
 * 别人共享出来的只能打开查看，可见不等于可改，这一条靠按钮的差别表达。
 *
 * @v-model {Boolean} modelValue - 弹窗开关
 * @prop    {Array}   list       - 可见的工作区（我的在前，每行带 mine 标记）
 * @emits   open                - 打开（载入编辑器）
 * @emits   share               - 共享 / 收回共享（仅属主）
 * @emits   remove              - 删除（仅属主）
 */
import { computed } from 'vue'
import { Delete, Share } from '@element-plus/icons-vue'
import DateUtil from '@/utils/DateUtil'

const visible = defineModel<boolean>({ default: false })
const { list = [] } = defineProps<{ list?: any[] }>()
const emit = defineEmits<{ open: [row: any], share: [row: any], remove: [row: any] }>()

const mine = computed(() => list.filter((item) => item.mine))
const shared = computed(() => list.filter((item) => !item.mine))
</script>

<template>
  <el-dialog v-model="visible" title="打开工作区" width="620px" append-to-body>
    <div class="ws-tip">默认只有本人可见，共享后所有人都能看到</div>
    <el-scrollbar class="ws-list" max-height="52vh">
      <div class="ws-inner">
        <div class="ws-group">我的（{{ mine.length }}）</div>
        <div class="ws-item" :key="item.id" v-for="item in mine" @click="emit('open', item)">
          <div class="item-main">
            <div class="item-title">
              <span class="item-name" :title="item.name">{{ item.name || '未命名工作区' }}</span>
              <el-tag class="item-tag" size="small" effect="plain" type="success" v-if="item.shared">已共享</el-tag>
            </div>
            <div class="item-meta">
              <span>{{ item.modelCount || 0 }} 个模型</span>
              <span>{{ DateUtil.format(item.updatedTime || item.createdTime) }}</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button
              link
              :icon="Share"
              :title="item.shared ? '收回共享' : '共享给所有人'"
              @click.stop="emit('share', item)">{{ item.shared ? '收回' : '共享' }}</el-button>
            <el-button class="item-delete" link :icon="Delete" title="删除" @click.stop="emit('remove', item)">删除</el-button>
          </div>
        </div>
        <el-empty description="还没有工作区，点「保存」即可存下当前配置" :image-size="56" v-if="!mine.length" />

        <template v-if="shared.length">
          <div class="ws-group">他人共享（{{ shared.length }}）</div>
          <div class="ws-item" :key="item.id" v-for="item in shared" @click="emit('open', item)">
            <div class="item-main">
              <div class="item-title">
                <span class="item-name" :title="item.name">{{ item.name || '未命名工作区' }}</span>
                <el-tag class="item-tag" size="small" effect="plain" type="info">共享</el-tag>
              </div>
              <div class="item-meta">
                <span>{{ item.createdUserInfo?.name || '其他用户' }}</span>
                <span>{{ item.modelCount || 0 }} 个模型</span>
                <span>{{ DateUtil.format(item.updatedTime || item.createdTime) }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-scrollbar>
  </el-dialog>
</template>

<style lang="scss" scoped>
.ws-tip {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
/* 工作区多了也不把弹窗撑长：滚动交给 el-scrollbar，常显滚动条 */
.ws-list {
  :deep(.el-scrollbar__view) {
    padding-right: 2px;
  }
}
.ws-group {
  padding: 6px 2px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.ws-item {
  @include flex-between();
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  border: solid 1px transparent;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
  & + .ws-item {
    margin-top: 4px;
  }
  &:hover {
    background: var(--el-fill-color-light);
    border-color: var(--el-border-color-lighter);
  }
  .item-main {
    min-width: 0;
    .item-title {
      @include flex-start();
      gap: 6px;
      .item-name {
        min-width: 0;
        font-size: 13px;
        color: var(--el-text-color-primary);
        @include text-wrap();
      }
      .item-tag {
        flex: none;
      }
    }
    .item-meta {
      @include flex-wrap();
      gap: 2px 12px;
      margin-top: 4px;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }
  .item-actions {
    flex: none;
    opacity: 0;
    transition: opacity 0.2s;
  }
  /* 删除默认保持中性，鼠标划过才转红（与历史列表、反馈的「踩」同一个口径） */
  .item-delete:hover {
    color: var(--el-color-danger);
  }
  &:hover .item-actions {
    opacity: 1;
  }
}
</style>
