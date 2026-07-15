<template>
  <ContentWrap class="mb-12px">
    <div class="mb-12px flex items-center justify-between">
      <span class="text-16px font-600">{{ t('summaryTitle') }}</span>
      <el-tag type="info">{{ t('summaryReadOnly') }}</el-tag>
    </div>
    <el-skeleton :loading="loading" animated>
      <el-row :gutter="12">
        <el-col v-for="item in amountItems" :key="item.key" :xs="12" :sm="8" :lg="4">
          <el-statistic :title="item.label" :value="item.value" :precision="2" />
        </el-col>
      </el-row>
      <el-divider />
      <el-space wrap>
        <el-tag v-for="item in countItems" :key="item.key" effect="plain">
          {{ item.label }}：{{ item.value }}
        </el-tag>
        <el-tooltip v-if="summary && !summary.taskSupported" :content="t('taskUnavailableTip')">
          <el-tag type="info" effect="plain">{{ t('taskUnavailable') }}</el-tag>
        </el-tooltip>
      </el-space>
    </el-skeleton>
  </ContentWrap>
</template>

<script setup lang="ts">
import * as CustomerApi from '@/api/crm/customer'

defineOptions({ name: 'CrmCustomer360Summary' })
const props = defineProps<{ customerId: number }>()
const { t } = useI18n('crm.customer')
const loading = ref(false)
const summary = ref<CustomerApi.Customer360SummaryVO>()

const amountItems = computed(() => [
  { key: 'contract', label: t('summaryContractAmount'), value: Number(summary.value?.contractAmount || 0) },
  { key: 'receivable', label: t('summaryReceivableAmount'), value: Number(summary.value?.approvedReceivableAmount || 0) },
  { key: 'refund', label: t('summaryRefundAmount'), value: Number(summary.value?.approvedRefundAmount || 0) },
  { key: 'netReceivable', label: t('summaryNetReceivableAmount'), value: Number(summary.value?.netReceivableAmount || 0) },
  { key: 'outstanding', label: t('summaryOutstandingAmount'), value: Number(summary.value?.outstandingReceivableAmount || 0) },
  { key: 'invoice', label: t('summaryInvoiceAmount'), value: Number(summary.value?.effectiveInvoiceAmount || 0) },
  { key: 'uninvoiced', label: t('summaryUninvoicedAmount'), value: Number(summary.value?.uninvoicedAmount || 0) }
])
const countItems = computed(() => [
  { key: 'contact', label: t('summaryContacts'), value: summary.value?.contactCount || 0 },
  { key: 'business', label: t('summaryBusinesses'), value: summary.value?.businessCount || 0 },
  { key: 'order', label: t('summaryMappedOrders'), value: summary.value?.mappedOrderCount || 0 },
  { key: 'plan', label: t('summaryPlans'), value: summary.value?.receivablePlanCount || 0 },
  { key: 'receivable', label: t('summaryReceivables'), value: summary.value?.receivableCount || 0 },
  { key: 'refund', label: t('summaryRefunds'), value: summary.value?.refundCount || 0 },
  { key: 'invoice', label: t('summaryInvoices'), value: summary.value?.invoiceCount || 0 },
  { key: 'workOrder', label: t('summaryWorkOrders'), value: summary.value?.workOrderCount || 0 },
  { key: 'attachment', label: t('summaryContractAttachments'), value: summary.value?.contractAttachmentCount || 0 }
])

const loadData = async () => {
  if (!props.customerId) return
  loading.value = true
  try {
    summary.value = await CustomerApi.getCustomer360Summary(props.customerId)
  } finally {
    loading.value = false
  }
}

watch(() => props.customerId, loadData, { immediate: true })
defineExpose({ loadData })
</script>
