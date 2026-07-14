<template>
  <el-table v-loading="loading" :data="list" stripe :show-overflow-tooltip="true">
    <el-table-column :label="t('invoice.applicationNo')" prop="no" min-width="170">
      <template #default="{ row }">
        <el-link type="primary" :underline="false" @click="detailRef?.open(row.id)">{{ row.no }}</el-link>
      </template>
    </el-table-column>
    <el-table-column :label="t('invoice.invoiceNo')" prop="invoiceNo" min-width="150" />
    <el-table-column :label="t('invoice.contract')" min-width="190">
      <template #default="{ row }">{{ row.contractNo }} · {{ row.contractName }}</template>
    </el-table-column>
    <el-table-column :label="t('invoice.direction')" min-width="80">
      <template #default="{ row }">
        <el-tag :type="row.direction === -1 ? 'danger' : 'primary'">
          {{ row.direction === -1 ? t('invoice.directionRed') : t('invoice.directionBlue') }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('invoice.status')" min-width="110">
      <template #default="{ row }">
        <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('invoice.amount')" align="right" min-width="120">
      <template #default="{ row }">{{ money(row.amount) }}</template>
    </el-table-column>
    <el-table-column :label="t('invoice.handler')" prop="handlerUserName" min-width="110" />
    <el-table-column
      :label="t('invoice.invoiceDate')"
      prop="invoiceDate"
      :formatter="dateFormatter"
      min-width="170"
    />
  </el-table>
  <Pagination
    v-model:page="query.pageNo"
    v-model:limit="query.pageSize"
    :total="total"
    @pagination="loadData"
  />
  <InvoiceDetail ref="detailRef" />
</template>

<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import { dateFormatter } from '@/utils/formatTime'
import InvoiceDetail from '../InvoiceDetail.vue'

defineOptions({ name: 'CrmCustomerInvoiceList' })
const props = defineProps<{ customerId: number }>()
const { t } = useI18n('crm')
const loading = ref(false)
const total = ref(0)
const list = ref<InvoiceApi.InvoiceVO[]>([])
const detailRef = ref<InstanceType<typeof InvoiceDetail>>()
const query = reactive({ pageNo: 1, pageSize: 10, customerId: props.customerId })
const statusOptions = computed(() => [
  { value: 0, label: t('invoice.statusDraft') },
  { value: 10, label: t('invoice.statusIssued') },
  { value: 20, label: t('invoice.statusPartiallyRed') },
  { value: 30, label: t('invoice.statusFullyRed') },
  { value: 40, label: t('invoice.statusVoided') }
])
const statusLabel = (value: number) =>
  statusOptions.value.find((item) => item.value === value)?.label || value
const statusTag = (value: number) => {
  const tags: Record<number, 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    10: 'success',
    20: 'warning',
    30: 'danger',
    40: 'info'
  }
  return tags[value] || 'info'
}
const money = (value: number) =>
  Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6
  })
const loadData = async () => {
  loading.value = true
  try {
    query.customerId = props.customerId
    const data = await InvoiceApi.getInvoicePage(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
watch(() => props.customerId, loadData, { immediate: true })
defineExpose({ loadData })
</script>
