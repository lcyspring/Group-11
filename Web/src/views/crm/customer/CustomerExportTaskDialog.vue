<template>
  <Dialog v-model="dialogVisible" :title="t('exportTaskTitle')" width="920px" @closed="stopPolling">
    <el-alert :closable="false" class="mb-16px" show-icon type="info">
      <template #title>{{ t('exportTaskGuidance') }}</template>
    </el-alert>

    <div class="mb-12px flex items-center justify-between gap-12px">
      <el-select
        v-model="queryParams.status"
        clearable
        :placeholder="t('exportTaskAllStatuses')"
        class="!w-180px"
        @change="handleStatusChange"
      >
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button :loading="loading" @click="refresh">
        <Icon class="mr-5px" icon="ep:refresh" />
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list" row-key="id" stripe>
      <el-table-column align="center" :label="t('exportTaskId')" prop="id" width="90" />
      <el-table-column align="center" :label="t('exportTaskStatus')" width="120">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('exportTaskRows')" prop="totalCount" width="100" />
      <el-table-column :label="t('exportTaskFile')" min-width="150" prop="fileName">
        <template #default="scope">{{ scope.row.fileName || '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('exportTaskCreatedAt')" min-width="165">
        <template #default="scope">{{ formatTaskTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column :label="t('exportTaskExpiresAt')" min-width="165">
        <template #default="scope">{{ formatTaskTime(scope.row.expiresAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('exportTaskFailure')" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.failureReason || '-' }}</template>
      </el-table-column>
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="110">
        <template #default="scope">
          <el-button
            v-if="scope.row.downloadAvailable"
            :loading="downloadTaskId === scope.row.id"
            link
            type="primary"
            @click="handleDownload(scope.row)"
          >
            {{ t('exportTaskDownload') }}
          </el-button>
          <span v-else class="text-[var(--el-text-color-placeholder)]">-</span>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="refresh"
    />
  </Dialog>
</template>

<script lang="ts" setup>
import * as CustomerApi from '@/api/crm/customer'
import download from '@/utils/download'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CrmCustomerExportTaskDialog' })

const { t } = useI18n('crm.customer')
const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const downloadTaskId = ref<number>()
const list = ref<CustomerApi.ExportTaskVO[]>([])
const total = ref(0)
const queryParams = reactive<{
  pageNo: number
  pageSize: number
  objectType: string
  status?: CustomerApi.ExportTaskStatus
}>({
  pageNo: 1,
  pageSize: 10,
  objectType: 'CUSTOMER',
  status: undefined
})

const statusOptions = computed(() => [
  { value: CustomerApi.ExportTaskStatus.QUEUED, label: t('exportTaskQueued') },
  { value: CustomerApi.ExportTaskStatus.RUNNING, label: t('exportTaskRunning') },
  { value: CustomerApi.ExportTaskStatus.SUCCESS, label: t('exportTaskSuccess') },
  { value: CustomerApi.ExportTaskStatus.FAILED, label: t('exportTaskFailed') },
  { value: CustomerApi.ExportTaskStatus.EXPIRED, label: t('exportTaskExpired') }
])

const statusLabel = (status: CustomerApi.ExportTaskStatus) =>
  statusOptions.value.find((option) => option.value === status)?.label || String(status)

const statusTag = (status: CustomerApi.ExportTaskStatus) => {
  const tags: Record<number, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    [CustomerApi.ExportTaskStatus.QUEUED]: 'info',
    [CustomerApi.ExportTaskStatus.RUNNING]: 'primary',
    [CustomerApi.ExportTaskStatus.SUCCESS]: 'success',
    [CustomerApi.ExportTaskStatus.FAILED]: 'danger',
    [CustomerApi.ExportTaskStatus.EXPIRED]: 'warning'
  }
  return tags[status] || 'info'
}

const formatTaskTime = (value?: string | number) => value ? formatDate(new Date(value)) : '-'

let pollingTimer: number | undefined
const stopPolling = () => {
  if (pollingTimer !== undefined) {
    window.clearTimeout(pollingTimer)
    pollingTimer = undefined
  }
}

const hasActiveTask = () => list.value.some((task) =>
  task.status === CustomerApi.ExportTaskStatus.QUEUED ||
  task.status === CustomerApi.ExportTaskStatus.RUNNING
)

const schedulePolling = () => {
  stopPolling()
  if (!dialogVisible.value || !hasActiveTask()) return
  pollingTimer = window.setTimeout(async () => {
    await getList(true)
  }, 3000)
}

const getList = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const data = await CustomerApi.getExportTaskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    if (!silent) loading.value = false
    schedulePolling()
  }
}

const refresh = () => {
  stopPolling()
  return getList(false)
}

const handleStatusChange = () => {
  queryParams.pageNo = 1
  refresh()
}

const handleDownload = async (task: CustomerApi.ExportTaskVO) => {
  downloadTaskId.value = task.id
  try {
    const token = await CustomerApi.issueExportDownloadToken(task.id)
    const file = await CustomerApi.downloadExportTask(task.id, token.token)
    download.file(file, task.fileName || t('exportFileName') + '.xlsx')
    message.success(t('exportTaskDownloadStarted'))
    await refresh()
  } finally {
    downloadTaskId.value = undefined
  }
}

const open = async () => {
  dialogVisible.value = true
  queryParams.pageNo = 1
  await refresh()
}

defineExpose({ open })
onBeforeUnmount(stopPolling)
</script>
