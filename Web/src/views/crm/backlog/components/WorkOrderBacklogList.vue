<template>
  <ContentWrap>
    <div class="pb-5 text-xl">{{ t('backlog.workOrder') }}</div>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column :label="t('workOrder.no')" prop="no" min-width="170" />
      <el-table-column :label="t('workOrder.title')" prop="title" min-width="180" />
      <el-table-column :label="t('workOrder.customer')" prop="customerName" min-width="130" />
      <el-table-column :label="t('workOrder.handler')" prop="handlerUserName" min-width="110" />
      <el-table-column :label="t('workOrder.status')" min-width="100">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" prop="createTime" :formatter="dateFormatter" min-width="170" />
      <el-table-column :label="t('common.action')" width="100">
        <template #default><el-button link type="primary" @click="openWorkOrders">{{ t('common.view') }}</el-button></template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="params.pageNo" v-model:limit="params.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import { dateFormatter } from '@/utils/formatTime'
const { t } = useI18n('crm')
const router = useRouter()
const loading = ref(false)
const list = ref<WorkOrderApi.WorkOrderVO[]>([])
const total = ref(0)
const params = reactive({ pageNo: 1, pageSize: 10, backlog: true })
const statusLabel = (value: number) => ({ 10: t('workOrder.statusPending'), 20: t('workOrder.statusProcessing'), 40: t('workOrder.statusReturned') } as Record<number, string>)[value] || value
const getList = async () => { loading.value = true; try { const data = await WorkOrderApi.getWorkOrderPage(params); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const openWorkOrders = () => router.push({ name: 'CrmWorkOrder' })
onMounted(getList)
onActivated(getList)
</script>
