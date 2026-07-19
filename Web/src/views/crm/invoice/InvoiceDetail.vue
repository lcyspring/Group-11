<template>
  <Dialog v-model="visible" :title="data.no || t('invoice.detail')" width="900px">
    <el-descriptions :column="3" border>
      <el-descriptions-item :label="t('invoice.contract')" :span="2">{{ data.contractNo }} · {{ data.contractName }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.customer')">{{ data.customerName }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.direction')"><el-tag :type="data.direction === -1 ? 'danger' : 'primary'">{{ directionLabel(data.direction) }}</el-tag></el-descriptions-item>
      <el-descriptions-item :label="t('invoice.status')"><el-tag :type="statusTag(data.status)">{{ statusLabel(data.status) }}</el-tag></el-descriptions-item>
      <el-descriptions-item :label="t('invoice.type')">{{ typeLabel(data.type) }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.amount')">{{ money(data.amount) }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.redAmount')">{{ money(data.redAmount) }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.invoiceNo')">{{ data.invoiceNo || '-' }}</el-descriptions-item>
      <el-descriptions-item v-if="data.originalInvoiceId" :label="t('invoice.originalInvoice')">{{ data.originalInvoiceNo || data.originalInvoiceId }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.invoiceDate')">{{ data.invoiceDate ? formatDate(new Date(data.invoiceDate)) : '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.handler')">{{ data.handlerUserName }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.owner')">{{ data.ownerUserName }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.title')" :span="2">{{ data.title }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.taxNo')">{{ data.taxNo || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.registeredAddress')" :span="2">{{ data.registeredAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.registeredPhone')">{{ data.registeredPhone || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.bankName')">{{ data.bankName || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.bankAccount')">{{ data.bankAccount || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.email')">{{ data.email || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.content')" :span="3">{{ data.content }}</el-descriptions-item>
      <el-descriptions-item v-if="data.issueRemark" :label="t('invoice.issueRemark')" :span="3">{{ data.issueRemark }}</el-descriptions-item>
      <el-descriptions-item v-if="data.externalProvider" :label="t('invoice.provider')">{{ data.externalProvider }}</el-descriptions-item>
      <el-descriptions-item v-if="data.externalRequestId" :label="t('invoice.providerRequest')" :span="2">{{ data.externalRequestId }}</el-descriptions-item>
    </el-descriptions>
    <h3 class="mt-20px mb-12px">{{ t('invoice.actionRecords') }}</h3>
    <el-empty v-if="!data.actionRecords?.length" :description="t('invoice.noActionRecords')" />
    <el-timeline v-else>
      <el-timeline-item v-for="record in data.actionRecords" :key="record.id" :timestamp="formatDate(record.actionTime)" placement="top">
        <div class="font-600">{{ record.operatorUserName || record.operatorUserId }} · {{ actionLabel(record.actionType) }}</div>
        <div class="text-13px text-gray-500">
          <span v-if="record.fromStatus !== undefined">{{ statusLabel(record.fromStatus) }}</span>
          <span v-if="record.toStatus !== undefined"> → {{ statusLabel(record.toStatus) }}</span>
          <span v-if="record.remark"> · {{ record.remark }}</span>
        </div>
        <div v-if="record.providerRequestId" class="text-12px text-gray-400">{{ record.providerRequestId }}</div>
      </el-timeline-item>
    </el-timeline>
  </Dialog>
</template>
<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import { formatDate } from '@/utils/formatTime'
const { t } = useI18n('crm'); const visible = ref(false); const data = ref<InvoiceApi.InvoiceVO>({} as InvoiceApi.InvoiceVO)
const statusLabel = (value?: number) => ({ 0: t('invoice.statusDraft'), 10: t('invoice.statusIssued'), 20: t('invoice.statusPartiallyRed'), 30: t('invoice.statusFullyRed'), 40: t('invoice.statusVoided') } as Record<number, string>)[value ?? -999] || String(value ?? '-')
const statusTag = (value?: number) => ({ 0: 'info', 10: 'success', 20: 'warning', 30: 'danger', 40: 'info' } as Record<number, any>)[value ?? -999]
const directionLabel = (value?: number) => value === -1 ? t('invoice.directionRed') : t('invoice.directionBlue')
const typeLabel = (value?: number) => value === 2 ? t('invoice.typeSpecial') : t('invoice.typeOrdinary')
const actionLabel = (value: number) => ({ 1: t('invoice.actionCreate'), 2: t('invoice.actionUpdate'), 3: t('invoice.actionIssue'), 4: t('invoice.actionVoid'), 5: t('invoice.actionRedFlush'), 6: t('invoice.actionVoidRed'), 7: t('invoice.actionDelete') } as Record<number, string>)[value] || String(value)
const money = (value?: number) => Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const open = async (id: number) => { visible.value = true; data.value = await InvoiceApi.getInvoice(id) }
defineExpose({ open })
</script>
