<template>
  <div class="table-actions" @click.stop>
    <el-popover
      v-if="mode === 'menu'"
      v-model:visible="menuVisible"
      :width="menuWidth"
      placement="bottom-end"
      trigger="click"
    >
      <template #reference>
        <el-button class="table-actions__more" link type="primary" @click.stop>
          {{ menuLabel || t('common.operation') }}
          <Icon class="ml-2px" icon="ep:arrow-down" />
        </el-button>
      </template>
      <div class="table-actions__menu" @click="menuVisible = false">
        <slot></slot>
      </div>
    </el-popover>
    <template v-else>
      <slot></slot>
      <el-dropdown v-if="$slots.more && showMore" trigger="click">
        <el-button class="table-actions__more" link type="primary" @click.stop>
          {{ moreLabel || t('common.more') }}
          <Icon class="ml-2px" icon="ep:arrow-down" />
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <slot name="more"></slot>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    mode?: 'inline' | 'menu'
    showMore?: boolean
    moreLabel?: string
    menuLabel?: string
    menuWidth?: number
  }>(),
  { mode: 'inline', showMore: true, moreLabel: '', menuLabel: '', menuWidth: 160 }
)

const { t } = useI18n()
const menuVisible = ref(false)
</script>

<style scoped>
.table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-width: max-content;
  white-space: nowrap;
}

.table-actions :deep(.el-button),
.table-actions__more {
  flex: 0 0 auto;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.table-actions__menu {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.table-actions__menu :deep(.el-button) {
  justify-content: flex-start;
  width: 100%;
  min-height: 32px;
  margin: 0;
  padding: 4px 8px;
}
</style>
