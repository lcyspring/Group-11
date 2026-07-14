<template>
  <Dialog v-model="visible" :title="data.no || t('refund.detail')" width="900px">
    <el-descriptions :column="3" border>
      <el-descriptions-item :label="t('refund.sourceReceivable')">
        {{ data.receivableNo }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.sourceAmount')">
        {{ money(data.receivablePrice) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.auditStatus')">
        <el-tag :type="statusTag(data.auditStatus)">{{ statusLabel(data.auditStatus) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.contract')" :span="2">
        {{ data.contractNo }} · {{ data.contractName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.customer')">{{ data.customerName }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.type')">{{ typeLabel(data.type) }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.amount')">{{ money(data.amount) }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.refundTime')">
        {{ data.refundTime ? formatDate(new Date(data.refundTime)) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.owner')">{{ data.ownerUserName }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.creator')">{{ data.creatorName }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.createTime')">
        {{ data.createTime ? formatDate(new Date(data.createTime)) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('refund.reason')" :span="3">{{ data.reason }}</el-descriptions-item>
      <el-descriptions-item :label="t('refund.remark')" :span="3">{{ data.remark || '-' }}</el-descriptions-item>
    </el-descriptions>

    <h3 class="mb-12px mt-20px">{{ t('refund.actionRecords') }}</h3>
    <el-empty v-if="!records.length" :description="t('refund.noActionRecords')" />
    <el-timeline v-else>
      <el-timeline-item
        v-for="record in records"
        :key="record.id"
        placement="top"
        :timestamp="formatDate(new Date(record.actionTime))"
      >
        <div class="font-600">
          {{ record.operatorUserName || t('refund.systemCallback') }} · {{ actionLabel(record.actionType) }}
        </div>
        <div class="text-13px text-gray-500">
          <span v-if="record.fromStatus !== undefined">{{ statusLabel(record.fromStatus) }}</span>
          <span v-if="record.toStatus !== undefined"> → {{ statusLabel(record.toStatus) }}</span>
          <span v-if="record.remark"> · {{ record.remark }}</span>
        </div>
        <div v-if="record.processInstanceId" class="text-12px text-gray-400">
          {{ record.processInstanceId }}
        </div>
      </el-timeline-item>
    </el-timeline>
  </Dialog>
</template>

<script setup lang="ts">
import * as RefundApi from '@/api/crm/refund'
import { formatDate } from '@/utils/formatTime'

const { t } = useI18n('crm')
const visible = ref(false)
const data = ref<RefundApi.ReceivableRefundVO>({} as RefundApi.ReceivableRefundVO)
const records = ref<RefundApi.RefundActionVO[]>([])
const statusLabel = (value?: number) =>
  ({
    0: t('refund.statusDraft'),
    10: t('refund.statusProcessing'),
    20: t('refund.statusApproved'),
    30: t('refund.statusRejected'),
    40: t('refund.statusCanceled')
  })[value ?? -1] || String(value ?? '-')
const statusTag = (value?: number) =>
  ({ 0: 'info', 10: 'warning', 20: 'success', 30: 'danger', 40: 'info' })[value ?? -1] as any
const typeLabel = (value?: number) =>
  value === 2 ? t('refund.typeBusinessReversal') : t('refund.typeCustomerRefund')
const actionLabel = (value: number) =>
  ({
    1: t('refund.actionCreate'),
    2: t('refund.actionUpdate'),
    3: t('refund.actionSubmit'),
    4: t('refund.actionApprove'),
    5: t('refund.actionReject'),
    6: t('refund.actionCancel'),
    7: t('refund.actionDelete')
  })[value] || String(value)
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const open = async (id: number) => {
  visible.value = true
  ;[data.value, records.value] = await Promise.all([
    RefundApi.getRefund(id),
    RefundApi.getActionRecords(id)
  ])
}
defineExpose({ open })
</script>
