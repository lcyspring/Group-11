<template>
  <Dialog v-model="visible" :title="data.no || t('workOrder.title')" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item :label="t('workOrder.title')">{{ data.title }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.status')">{{ statusLabel(data.status) }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.customer')">{{ data.customerName }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.group')">{{ data.groupName || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.handler')">{{ data.handlerUserName || t('workOrder.unassigned') }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.dispatchMode')">{{ dispatchModeLabel(data.dispatchMode) }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.ccUsers')" :span="2">{{ data.ccUserNames?.join('、') || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.description')" :span="2">{{ data.description }}</el-descriptions-item>
      <el-descriptions-item v-if="data.returnReason" :label="t('workOrder.returnReason')" :span="2">{{ data.returnReason }}</el-descriptions-item>
      <el-descriptions-item v-if="data.solution" :label="t('workOrder.solution')" :span="2">{{ data.solution }}</el-descriptions-item>
      <el-descriptions-item v-if="data.sla" :label="t('workOrder.sla')" :span="2">
        {{ data.sla.policyName || data.sla.policyCode || '-' }} · {{ data.sla.resolutionDueTime ? formatDate(data.sla.resolutionDueTime) : '-' }}
        <el-tag v-if="data.sla.paused" type="warning" class="ml-8px">{{ t('workOrder.slaPaused') }}</el-tag>
        <el-tag v-else-if="data.sla.overdue" type="danger" class="ml-8px">{{ t('workOrder.slaOverdue') }}</el-tag>
        <el-tag v-else-if="data.sla.escalatedAt" type="danger" class="ml-8px">{{ t('workOrder.slaEscalated') }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="data.latestCheckIn" :label="t('workOrder.checkIn')" :span="2">
        {{ formatDate(data.latestCheckIn.createTime) }} · {{ data.latestCheckIn.distanceMeters }}m
      </el-descriptions-item>
    </el-descriptions>
    <div class="mt-16px flex gap-8px">
      <el-button v-if="canMobileCheckIn(data)" type="primary" @click="checkIn">{{ t('workOrder.checkIn') }}</el-button>
      <el-button v-if="data.sla && !data.sla.paused && data.status !== 30" @click="pauseSla">{{ t('workOrder.slaPause') }}</el-button>
      <el-button v-if="data.sla?.paused" @click="resumeSla">{{ t('workOrder.slaResume') }}</el-button>
    </div>
    <h3 class="mt-20px mb-12px">{{ t('workOrder.records') }}</h3>
    <el-timeline>
      <el-timeline-item v-for="record in data.records || []" :key="record.id" :timestamp="formatDate(record.createTime)">
        {{ record.operatorUserName }} · {{ actionLabel(record.actionType) }}
        <span v-if="record.remark">：{{ record.remark }}</span>
      </el-timeline-item>
    </el-timeline>
  </Dialog>
</template>
<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import { formatDate } from '@/utils/formatTime'
import { canMobileCheckIn } from './governance'
const { t } = useI18n('crm')
const visible = ref(false)
const data = ref<WorkOrderApi.WorkOrderVO>({} as WorkOrderApi.WorkOrderVO)
const statusLabel = (value?: number) => ({ 10: t('workOrder.statusPending'), 20: t('workOrder.statusProcessing'), 30: t('workOrder.statusCompleted'), 40: t('workOrder.statusReturned') } as Record<number, string>)[value || 0] || value
const actionLabel = (value: number) => ({ 1: t('workOrder.actionCreate'), 2: t('workOrder.actionUpdate'), 3: t('workOrder.start'), 4: t('workOrder.return'), 5: t('workOrder.resubmit'), 6: t('workOrder.complete'), 7: t('workOrder.assign'), 8: t('workOrder.claim'), 9: t('workOrder.ccUpdated'), 10: t('workOrder.checkIn'), 11: t('workOrder.slaPause'), 12: t('workOrder.slaResume'), 13: t('workOrder.slaEscalated') } as Record<number, string>)[value] || value
const dispatchModeLabel = (value?: number) => ({ 0: t('workOrder.dispatchUnassigned'), 1: t('workOrder.dispatchManual'), 2: t('workOrder.dispatchAuto'), 3: t('workOrder.dispatchClaim'), 4: t('workOrder.dispatchReassign') } as Record<number, string>)[value ?? -1] || '-'
const message = useMessage()
const refreshGovernance = async () => {
  if (!data.value.id) return
  const [sla, latestCheckIn] = await Promise.all([
    WorkOrderApi.getWorkOrderSla(data.value.id),
    WorkOrderApi.getLatestCheckIn(data.value.id)
  ])
  data.value.sla = sla
  data.value.latestCheckIn = latestCheckIn
}
const checkIn = () => {
  if (!navigator.geolocation) { message.error(t('workOrder.checkInUnavailable')); return }
  navigator.geolocation.getCurrentPosition(async position => {
    await WorkOrderApi.checkInWorkOrder(data.value.id!, position.coords.latitude, position.coords.longitude, position.coords.accuracy)
    message.success(t('workOrder.checkInSuccess')); await open(data.value.id!)
  }, () => message.error(t('workOrder.checkInUnavailable')), { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })
}
const pauseSla = async () => { await WorkOrderApi.pauseWorkOrderSla(data.value.id!); await open(data.value.id!) }
const resumeSla = async () => { await WorkOrderApi.resumeWorkOrderSla(data.value.id!); await open(data.value.id!) }
const open = async (id: number) => { visible.value = true; data.value = await WorkOrderApi.getWorkOrder(id); await refreshGovernance() }
defineExpose({ open })
</script>
