<template>
  <ContentWrap>
    <el-form :model="queryParams" inline class="-mb-15px">
      <el-form-item :label="t('workOrder.no')"><el-input v-model="queryParams.no" clearable @keyup.enter="query" /></el-form-item>
      <el-form-item :label="t('workOrder.status')"><el-select v-model="queryParams.status" clearable @change="query"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button @click="query">{{ t('common.search') }}</el-button><el-button @click="reset">{{ t('common.reset') }}</el-button><el-button v-hasPermi="['crm:work-order:create']" type="primary" @click="openForm('create')">{{ t('action.add') }}</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column :label="t('workOrder.no')" prop="no" min-width="170"><template #default="{ row }"><el-link type="primary" :underline="false" @click="detailRef.open(row.id)">{{ row.no }}</el-link></template></el-table-column>
      <el-table-column :label="t('workOrder.title')" prop="title" min-width="180" />
      <el-table-column :label="t('workOrder.customer')" prop="customerName" min-width="130" />
      <el-table-column :label="t('workOrder.type')" prop="type" min-width="90"><template #default="{ row }">{{ typeLabel(row.type) }}</template></el-table-column>
      <el-table-column :label="t('workOrder.priority')" prop="priority" min-width="90"><template #default="{ row }">{{ priorityLabel(row.priority) }}</template></el-table-column>
      <el-table-column :label="t('workOrder.status')" prop="status" min-width="100"><template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column :label="t('workOrder.handler')" prop="handlerUserName" min-width="110" />
      <el-table-column :label="t('common.createTime')" prop="createTime" :formatter="dateFormatter" min-width="170" />
      <el-table-column :label="t('common.action')" fixed="right" min-width="280">
        <template #default="{ row }">
          <el-button v-if="[10, 40].includes(row.status) && row.creator === String(userId)" v-hasPermi="['crm:work-order:update']" link type="primary" @click="openForm('update', row.id)">{{ t('common.edit') }}</el-button>
          <el-button v-if="row.status === 10 && row.handlerUserId === userId" v-hasPermi="['crm:work-order:process']" link type="primary" @click="start(row)">{{ t('workOrder.start') }}</el-button>
          <el-button v-if="row.status === 20 && row.handlerUserId === userId" v-hasPermi="['crm:work-order:process']" link type="warning" @click="returnOrder(row)">{{ t('workOrder.return') }}</el-button>
          <el-button v-if="row.status === 20 && row.handlerUserId === userId" v-hasPermi="['crm:work-order:process']" link type="success" @click="complete(row)">{{ t('workOrder.complete') }}</el-button>
          <el-button v-if="row.status === 40 && row.creator === String(userId)" v-hasPermi="['crm:work-order:update']" link type="primary" @click="resubmit(row)">{{ t('workOrder.resubmit') }}</el-button>
          <el-button v-if="row.status === 10 && row.creator === String(userId)" v-hasPermi="['crm:work-order:delete']" link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>
  <WorkOrderForm ref="formRef" @success="getList" />
  <WorkOrderDetail ref="detailRef" />
</template>
<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import WorkOrderForm from './WorkOrderForm.vue'
import WorkOrderDetail from './WorkOrderDetail.vue'
import { dateFormatter } from '@/utils/formatTime'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'CrmWorkOrder' })
const { t } = useI18n('crm')
const message = useMessage()
const userId = useUserStore().getUser.id
const loading = ref(false)
const list = ref<WorkOrderApi.WorkOrderVO[]>([])
const total = ref(0)
const formRef = ref()
const detailRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, no: '', status: undefined as number | undefined })
const statusOptions = [{ value: 10, label: t('workOrder.statusPending') }, { value: 20, label: t('workOrder.statusProcessing') }, { value: 30, label: t('workOrder.statusCompleted') }, { value: 40, label: t('workOrder.statusReturned') }]
const typeLabel = (value: number) => ({ 1: t('workOrder.typeIssue'), 2: t('workOrder.typeDemand'), 3: t('workOrder.typeComplaint'), 4: t('workOrder.typeConsultation') } as Record<number, string>)[value] || value
const priorityLabel = (value: number) => ({ 1: t('workOrder.priorityLow'), 2: t('workOrder.priorityMedium'), 3: t('workOrder.priorityHigh') } as Record<number, string>)[value] || value
const statusLabel = (value: number) => statusOptions.find(item => item.value === value)?.label || value
const statusTag = (value: number) => ({ 10: '', 20: 'warning', 30: 'success', 40: 'danger' }[value] as any)
const getList = async () => { loading.value = true; try { const data = await WorkOrderApi.getWorkOrderPage(queryParams); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const query = () => { queryParams.pageNo = 1; getList() }
const reset = () => { queryParams.no = ''; queryParams.status = undefined; query() }
const openForm = (type: 'create' | 'update', id?: number) => formRef.value.open(type, id)
const start = async (row: WorkOrderApi.WorkOrderVO) => { await WorkOrderApi.startWorkOrder(row.id!); message.success(t('workOrder.startSuccess')); getList() }
const returnOrder = async (row: WorkOrderApi.WorkOrderVO) => { const { value } = await ElMessageBox.prompt(t('workOrder.returnReason'), t('workOrder.return'), { inputValidator: value => !!value || t('workOrder.returnReasonRequired') }); await WorkOrderApi.returnWorkOrder(row.id!, value); getList() }
const complete = async (row: WorkOrderApi.WorkOrderVO) => { const { value } = await ElMessageBox.prompt(t('workOrder.solution'), t('workOrder.complete'), { inputType: 'textarea', inputValidator: value => !!value || t('workOrder.solutionRequired') }); await WorkOrderApi.completeWorkOrder(row.id!, value); getList() }
const resubmit = async (row: WorkOrderApi.WorkOrderVO) => { await WorkOrderApi.resubmitWorkOrder(row.id!); message.success(t('workOrder.resubmitSuccess')); getList() }
const remove = async (row: WorkOrderApi.WorkOrderVO) => { await message.confirm(t('common.delMessage')); await WorkOrderApi.deleteWorkOrder(row.id!); getList() }
onMounted(getList)
onActivated(getList)
</script>
