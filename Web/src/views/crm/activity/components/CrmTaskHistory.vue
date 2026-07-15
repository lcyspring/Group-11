<template>
  <Dialog v-model="visible" :title="t('taskHistory')" width="720px">
    <el-timeline v-loading="loading">
      <el-timeline-item v-for="item in records" :key="item.id" :timestamp="formatDate(item.createTime)" placement="top">
        <div class="font-medium">{{ actionLabel(item.actionType) }}</div>
        <div class="text-sm text-gray-500">{{ statusLabel(item.fromStatus) }} → {{ statusLabel(item.toStatus) }}</div>
        <div v-if="item.operatorUserName" class="text-sm">{{ t('operator') }}：{{ item.operatorUserName }}</div>
        <div v-if="item.remark" class="mt-1">{{ item.remark }}</div>
      </el-timeline-item>
      <el-empty v-if="!loading && !records.length" :description="t('noRecords')" />
    </el-timeline>
  </Dialog>
</template>

<script setup lang="ts">
import * as ActivityApi from '@/api/crm/activity'
import { formatDate } from '@/utils/formatTime'

const { t } = useI18n('crm.activity')
const visible = ref(false)
const loading = ref(false)
const records = ref<ActivityApi.TaskActionRecordVO[]>([])
const actionLabel = (value: number) => t(`action${value}`)
const statusLabel = (value?: number) => value === undefined || value === null ? t('none') : t(`taskStatus${value}`)
const open = async (taskId: number) => {
  visible.value = true
  loading.value = true
  try { records.value = await ActivityApi.getTaskActionRecords(taskId) }
  finally { loading.value = false }
}
defineExpose({ open })
</script>
