<template>
  <ContentWrap class="statistics-lineage-panel">
    <div class="flex flex-wrap items-center justify-between gap-12px">
      <div class="flex flex-wrap items-center gap-8px text-14px">
        <span class="font-600">{{ t('lineage.title') }}</span>
        <el-tag type="success">{{ refreshModeLabel }}</el-tag>
        <span class="text-[var(--el-text-color-secondary)]">
          {{ t('lineage.lastRefreshedAt') }}：{{ lastRefreshedAtLabel }}
        </span>
      </div>
      <div class="flex gap-8px">
        <el-button :loading="refreshing" type="primary" @click="refresh">
          <Icon class="mr-5px" icon="ep:refresh" />
          {{ t('lineage.refreshNow') }}
        </el-button>
        <el-button :loading="metadataLoading" @click="drawerVisible = true">
          <Icon class="mr-5px" icon="ep:document" />
          {{ t('lineage.viewCatalog') }}
        </el-button>
      </div>
    </div>
    <el-alert
      v-if="metadataError"
      :closable="false"
      class="mt-12px"
      :title="t('lineage.loadFailed')"
      type="warning"
    />
  </ContentWrap>

  <el-drawer v-model="drawerVisible" :size="drawerSize" :title="t('lineage.catalogTitle')">
    <el-skeleton :loading="metadataLoading" animated :rows="6">
      <template v-if="catalog">
        <el-descriptions :column="1" border class="mb-16px">
          <el-descriptions-item :label="t('lineage.refreshMode')">
            {{ refreshModeLabel }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('lineage.permissionMode')">
            {{ catalog.permissionMode }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('lineage.historyRecalculation')">
            {{ catalog.historyRecalculation }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('lineage.reconciliation')">
            {{ catalog.reconciliation }}
          </el-descriptions-item>
        </el-descriptions>

        <el-table :data="catalog.metrics" row-key="code">
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-descriptions :column="1" border class="m-12px">
                <el-descriptions-item :label="t('lineage.sourceTables')">
                  {{ row.sourceTables.join(', ') }}
                </el-descriptions-item>
                <el-descriptions-item :label="t('lineage.sourceFields')">
                  {{ row.sourceFields.join(', ') }}
                </el-descriptions-item>
                <el-descriptions-item :label="t('lineage.formula')">
                  {{ row.formula }}
                </el-descriptions-item>
                <el-descriptions-item :label="t('lineage.filters')">
                  <div v-for="filter in row.filters" :key="filter">{{ filter }}</div>
                </el-descriptions-item>
                <el-descriptions-item :label="t('lineage.permission')">
                  {{ row.permission }}
                </el-descriptions-item>
              </el-descriptions>
            </template>
          </el-table-column>
          <el-table-column min-width="180" prop="name" :label="t('lineage.metricName')" />
          <el-table-column min-width="220" prop="code" :label="t('lineage.metricCode')" />
          <el-table-column
            min-width="200"
            prop="businessTime"
            :label="t('lineage.businessTime')"
          />
        </el-table>
      </template>
    </el-skeleton>
  </el-drawer>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import {
  CrmStatisticsMetadataCatalog,
  CrmStatisticsScope,
  getStatisticsMetadataCatalog
} from '@/api/crm/statistics/metadata'

defineOptions({ name: 'CrmStatisticsLineagePanel' })

const props = defineProps<{
  scope: CrmStatisticsScope
  onRefresh: () => Promise<unknown> | unknown
}>()

const { t } = useI18n('crm.statistics')
const catalog = ref<CrmStatisticsMetadataCatalog>()
const metadataLoading = ref(false)
const metadataError = ref(false)
const refreshing = ref(false)
const drawerVisible = ref(false)
const lastRefreshedAt = ref<Date>()
const drawerSize = computed(() => (window.innerWidth < 768 ? '95%' : '75%'))
const refreshModeLabel = computed(() =>
  catalog.value?.refreshMode === 'REALTIME_QUERY'
    ? t('lineage.realtimeQuery')
    : catalog.value?.refreshMode || t('lineage.unknownMode')
)
const lastRefreshedAtLabel = computed(() =>
  lastRefreshedAt.value
    ? dayjs(lastRefreshedAt.value).format('YYYY-MM-DD HH:mm:ss')
    : t('lineage.notRefreshed')
)

const loadCatalog = async () => {
  metadataLoading.value = true
  metadataError.value = false
  try {
    catalog.value = await getStatisticsMetadataCatalog(props.scope)
  } catch {
    metadataError.value = true
  } finally {
    metadataLoading.value = false
  }
}

const markRefreshed = () => {
  lastRefreshedAt.value = new Date()
}

const refresh = async () => {
  refreshing.value = true
  try {
    await props.onRefresh()
    markRefreshed()
  } finally {
    refreshing.value = false
  }
}

onMounted(loadCatalog)
defineExpose({ markRefreshed, loadCatalog })
</script>
