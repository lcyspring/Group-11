<template>
  <el-tabs v-model="sceneType" @tab-change="query">
    <el-tab-pane :label="t('customer.myResponsible')" :name="1" />
    <el-tab-pane :label="t('customer.myInvolved')" :name="2" />
    <el-tab-pane :label="t('customer.subordinateResponsible')" :name="3" />
  </el-tabs>
  <el-table v-loading="loading" :data="list" stripe :show-overflow-tooltip="true">
    <el-table-column :label="t('refund.no')" min-width="170" prop="no">
      <template #default="{ row }">
        <el-link :underline="false" type="primary" @click="detailRef?.open(row.id)">
          {{ row.no }}
        </el-link>
      </template>
    </el-table-column>
    <el-table-column :label="t('refund.sourceReceivable')" min-width="170" prop="receivableNo" />
    <el-table-column :label="t('refund.contract')" min-width="190">
      <template #default="{ row }">{{ row.contractNo }} · {{ row.contractName }}</template>
    </el-table-column>
    <el-table-column :label="t('refund.type')" min-width="120">
      <template #default="{ row }">
        <el-tag :type="row.type === 2 ? 'danger' : 'warning'">{{ typeLabel(row.type) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column align="right" :label="t('refund.amount')" min-width="120">
      <template #default="{ row }">{{ money(row.amount) }}</template>
    </el-table-column>
    <el-table-column
      :formatter="dateFormatter"
      :label="t('refund.refundTime')"
      min-width="170"
      prop="refundTime"
    />
    <el-table-column :label="t('refund.auditStatus')" min-width="120">
      <template #default="{ row }">
        <el-tag :type="statusTag(row.auditStatus)">{{ statusLabel(row.auditStatus) }}</el-tag>
      </template>
    </el-table-column>
  </el-table>
  <Pagination
    v-model:limit="queryParams.pageSize"
    v-model:page="queryParams.pageNo"
    :total="total"
    @pagination="loadData"
  />
  <RefundDetail ref="detailRef" />
</template>

<script setup lang="ts">
import * as RefundApi from '@/api/crm/refund'
import { dateFormatter } from '@/utils/formatTime'
import RefundDetail from '../RefundDetail.vue'

defineOptions({ name: 'CrmCustomerRefundList' })
const props = defineProps<{ customerId: number }>()
const { t } = useI18n('crm')
const loading = ref(false)
const total = ref(0)
const list = ref<RefundApi.ReceivableRefundVO[]>([])
const detailRef = ref<InstanceType<typeof RefundDetail>>()
const sceneType = ref(1)
const queryParams = reactive({ pageNo: 1, pageSize: 10 })
const statusOptions = computed(() => [
  { value: 0, label: t('refund.statusDraft') },
  { value: 10, label: t('refund.statusProcessing') },
  { value: 20, label: t('refund.statusApproved') },
  { value: 30, label: t('refund.statusRejected') },
  { value: 40, label: t('refund.statusCanceled') }
])
const statusLabel = (value?: number) =>
  statusOptions.value.find((item) => item.value === value)?.label || String(value ?? '-')
const statusTag = (value?: number) =>
  ({ 0: 'info', 10: 'warning', 20: 'success', 30: 'danger', 40: 'info' })[value ?? -1] as any
const typeLabel = (value: number) =>
  value === 2 ? t('refund.typeBusinessReversal') : t('refund.typeCustomerRefund')
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6
  })
const loadData = async () => {
  loading.value = true
  try {
    const data = await RefundApi.getRefundPage({
      ...queryParams,
      customerId: props.customerId,
      sceneType: sceneType.value
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const query = () => {
  queryParams.pageNo = 1
  loadData()
}
watch(() => props.customerId, query, { immediate: true })
defineExpose({ loadData })
</script>
