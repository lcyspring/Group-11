<template>
  <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true">
    <el-table-column :label="t('workOrder.no')" prop="no" min-width="170">
      <template #default="{ row }"><el-link type="primary" :underline="false" @click="detailRef?.open(row.id)">{{ row.no }}</el-link></template>
    </el-table-column>
    <el-table-column :label="t('workOrder.title')" prop="title" min-width="180" />
    <el-table-column :label="t('workOrder.type')" min-width="90"><template #default="{ row }">{{ typeLabel(row.type) }}</template></el-table-column>
    <el-table-column :label="t('workOrder.status')" min-width="100"><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column>
    <el-table-column :label="t('workOrder.handler')" prop="handlerUserName" min-width="110" />
    <el-table-column :label="t('common.createTime')" prop="createTime" :formatter="dateFormatter" min-width="170" />
  </el-table>
  <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
  <WorkOrderDetail ref="detailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as WorkOrderApi from '@/api/crm/workorder'
import WorkOrderDetail from '../WorkOrderDetail.vue'

defineOptions({ name: 'CrmCustomerWorkOrderList' })
const props = defineProps<{ customerId: number }>()
const { t } = useI18n('crm')
const loading = ref(false)
const total = ref(0)
const list = ref<WorkOrderApi.WorkOrderVO[]>([])
const detailRef = ref<InstanceType<typeof WorkOrderDetail>>()
const query = reactive({ pageNo: 1, pageSize: 10, customerId: props.customerId })
const loadData = async () => {
  loading.value = true
  try {
    query.customerId = props.customerId
    const data = await WorkOrderApi.getWorkOrderPage(query)
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
const statusLabel = (value: number) => ({ 10: t('workOrder.statusPending'), 20: t('workOrder.statusProcessing'), 30: t('workOrder.statusCompleted'), 40: t('workOrder.statusReturned') } as Record<number, string>)[value] || value
const typeLabel = (value: number) => ({ 1: t('workOrder.typeIssue'), 2: t('workOrder.typeDemand'), 3: t('workOrder.typeComplaint'), 4: t('workOrder.typeConsultation') } as Record<number, string>)[value] || value
onMounted(loadData)
defineExpose({ loadData })
</script>
