<template>
  <el-descriptions :column="3" border>
    <el-descriptions-item :label="t('reimbursement.applicant')">{{ data.applicantUserName }}</el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.status')">
      <el-tag :type="statusTag(data.auditStatus)">{{ statusLabel(data.auditStatus) }}</el-tag>
    </el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.totalAmount')">
      {{ money(data.totalAmount) }} {{ data.currency }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.customer')">{{ data.customerName || '-' }}</el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.contract')" :span="2">
      {{ data.contractId ? `${data.contractNo || ''} · ${data.contractName || ''}` : '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.expenseDate')" :span="2">
      {{ data.expenseStartDate }} → {{ data.expenseEndDate }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.createTime')">
      {{ data.createTime ? formatDate(new Date(data.createTime)) : '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.reason')" :span="3">{{ data.reason }}</el-descriptions-item>
    <el-descriptions-item :label="t('reimbursement.remark')" :span="3">{{ data.remark || '-' }}</el-descriptions-item>
  </el-descriptions>

  <h3 class="mb-12px mt-20px">{{ t('reimbursement.items') }}</h3>
  <el-table :data="data.items || []" border>
    <el-table-column :label="t('reimbursement.category')" min-width="120" prop="categoryName" />
    <el-table-column :label="t('reimbursement.occurredDate')" min-width="120" prop="occurredDate" />
    <el-table-column align="right" :label="t('reimbursement.amount')" min-width="120">
      <template #default="{ row }">{{ money(row.amount) }}</template>
    </el-table-column>
    <el-table-column :label="t('reimbursement.description')" min-width="180" prop="description" />
    <el-table-column :label="t('reimbursement.invoiceNo')" min-width="120" prop="invoiceNo">
      <template #default="{ row }">{{ row.invoiceNo || '-' }}</template>
    </el-table-column>
    <el-table-column :label="t('reimbursement.attachments')" min-width="170">
      <template #default="{ row }">
        <span v-if="!row.attachmentUrls?.length">-</span>
        <el-link
          v-for="(url, index) in row.attachmentUrls"
          :key="url"
          class="mr-8px"
          :href="url"
          target="_blank"
          type="primary"
        >
          {{ t('reimbursement.attachmentIndex', { index: index + 1 }) }}
        </el-link>
      </template>
    </el-table-column>
  </el-table>

  <h3 class="mb-12px mt-20px">{{ t('reimbursement.actionRecords') }}</h3>
  <el-empty v-if="!records.length" :description="t('reimbursement.noActionRecords')" />
  <el-timeline v-else>
    <el-timeline-item
      v-for="record in records"
      :key="record.id"
      placement="top"
      :timestamp="formatDate(new Date(record.actionTime))"
    >
      <div class="font-600">
        {{ record.operatorUserName || t('reimbursement.systemCallback') }} · {{ actionLabel(record.actionType) }}
      </div>
      <div class="text-13px text-gray-500">
        <span v-if="record.fromStatus !== undefined">{{ statusLabel(record.fromStatus) }}</span>
        <span v-if="record.toStatus !== undefined"> → {{ statusLabel(record.toStatus) }}</span>
        <span> · {{ t('reimbursement.amountSnapshot') }} {{ money(record.amountSnapshot) }}</span>
        <span v-if="record.remark"> · {{ record.remark }}</span>
      </div>
      <div v-if="record.processInstanceId" class="text-12px text-gray-400">{{ record.processInstanceId }}</div>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup lang="ts">
import type { ReimbursementActionVO, ReimbursementVO } from '@/api/crm/reimbursement'
import { formatDate } from '@/utils/formatTime'

defineProps<{ data: ReimbursementVO; records: ReimbursementActionVO[] }>()
const { t } = useI18n('crm')
const statusLabel = (value?: number) =>
  ({
    0: t('reimbursement.statusDraft'),
    10: t('reimbursement.statusProcessing'),
    20: t('reimbursement.statusApproved'),
    30: t('reimbursement.statusRejected'),
    40: t('reimbursement.statusCanceled')
  })[value ?? -1] || String(value ?? '-')
const statusTag = (value?: number) =>
  ({ 0: 'info', 10: 'warning', 20: 'success', 30: 'danger', 40: 'info' })[value ?? -1] as any
const actionLabel = (value: number) =>
  ({
    1: t('reimbursement.actionCreate'),
    2: t('reimbursement.actionUpdate'),
    3: t('reimbursement.actionSubmit'),
    4: t('reimbursement.actionApprove'),
    5: t('reimbursement.actionReject'),
    6: t('reimbursement.actionCancel'),
    7: t('reimbursement.actionDelete')
  })[value] || String(value)
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
</script>

