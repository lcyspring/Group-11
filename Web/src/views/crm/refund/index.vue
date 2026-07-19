<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" class="-mb-15px" inline>
      <el-form-item :label="t('refund.no')" prop="no">
        <el-input v-model="queryParams.no" clearable @keyup.enter="query" />
      </el-form-item>
      <el-form-item :label="t('refund.type')" prop="type">
        <el-select v-model="queryParams.type" class="!w-160px" clearable @change="query">
          <el-option :label="t('refund.typeCustomerRefund')" :value="1" />
          <el-option :label="t('refund.typeBusinessReversal')" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('refund.auditStatus')" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-160px" clearable @change="query">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('refund.refundTime')" prop="refundTime">
        <el-date-picker
          v-model="queryParams.refundTime"
          end-placeholder=""
          range-separator="-"
          start-placeholder=""
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="query">{{ t('common.search') }}</el-button>
        <el-button @click="reset">{{ t('common.reset') }}</el-button>
        <el-button
          v-hasPermi="['crm:receivable-refund:create']"
          type="primary"
          @click="formRef.open('create')"
        >
          {{ t('refund.createDraft') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="sceneType" @tab-change="query">
      <el-tab-pane :label="t('customer.myResponsible')" :name="1" />
      <el-tab-pane :label="t('customer.myInvolved')" :name="2" />
      <el-tab-pane :label="t('customer.subordinateResponsible')" :name="3" />
      <el-tab-pane :label="t('customer.organizationScope')" :name="4" />
    </el-tabs>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column fixed="left" :label="t('refund.no')" min-width="170" prop="no">
        <template #default="{ row }">
          <el-link :underline="false" type="primary" @click="detailRef.open(row.id)">{{
            row.no
          }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('refund.sourceReceivable')" min-width="170" prop="receivableNo" />
      <el-table-column :label="t('refund.customer')" min-width="120" prop="customerName" />
      <el-table-column :label="t('refund.contract')" min-width="190">
        <template #default="{ row }">{{ row.contractNo }} · {{ row.contractName }}</template>
      </el-table-column>
      <el-table-column :label="t('refund.type')" min-width="110">
        <template #default="{ row }">
          <el-tag :type="row.type === 2 ? 'danger' : 'warning'">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="right" :label="t('refund.amount')" min-width="120" prop="amount">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter"
        :label="t('refund.refundTime')"
        min-width="170"
        prop="refundTime"
      />
      <el-table-column :label="t('refund.owner')" min-width="100" prop="ownerUserName" />
      <el-table-column fixed="right" :label="t('refund.auditStatus')" min-width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.auditStatus)">{{ statusLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('common.action')" width="340">
        <template #default="{ row }">
          <TableActions>
            <el-button
              v-if="[0, 30, 40].includes(row.auditStatus)"
              v-hasPermi="['crm:receivable-refund:update']"
              link
              type="primary"
              @click="formRef.open('update', row.id)"
            >
              {{ row.auditStatus === 0 ? t('common.edit') : t('refund.revise') }}
            </el-button>
            <el-button
              v-if="row.auditStatus === 0"
              v-hasPermi="['crm:receivable-refund:update']"
              link
              type="success"
              @click="submit(row)"
            >
              {{ t('refund.submit') }}
            </el-button>
            <el-button v-if="row.processInstanceId" link type="primary" @click="viewProcess(row)">
              {{ t('refund.viewApproval') }}
            </el-button>
            <el-button
              v-if="row.auditStatus === 0 && !row.processInstanceId"
              v-hasPermi="['crm:receivable-refund:delete']"
              link
              type="danger"
              @click="remove(row.id)"
            >
              {{ t('common.delete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <RefundForm ref="formRef" @success="getList" />
  <RefundDetail ref="detailRef" />
</template>

<script setup lang="ts">
import * as RefundApi from '@/api/crm/refund'
import RefundForm from './RefundForm.vue'
import RefundDetail from './RefundDetail.vue'
import TableActions from '@/components/TableActions/index.vue'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'CrmReceivableRefund' })
const { t } = useI18n('crm')
const message = useMessage()
const { push } = useRouter()
const loading = ref(false)
const list = ref<RefundApi.ReceivableRefundVO[]>([])
const total = ref(0)
const formRef = ref()
const detailRef = ref()
const queryFormRef = ref()
const sceneType = ref(1)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: '',
  type: undefined as number | undefined,
  auditStatus: undefined as number | undefined,
  refundTime: undefined as string[] | undefined
})
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
const getList = async () => {
  loading.value = true
  try {
    const data = await RefundApi.getRefundPage({ ...queryParams, sceneType: sceneType.value })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const query = () => {
  queryParams.pageNo = 1
  getList()
}
const reset = () => {
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, {
    pageNo: 1,
    no: '',
    type: undefined,
    auditStatus: undefined,
    refundTime: undefined
  })
  getList()
}
const submit = async (row: RefundApi.ReceivableRefundVO) => {
  await message.confirm(t('refund.submitConfirm', { no: row.no }))
  await RefundApi.submitRefund(row.id!)
  message.success(t('refund.submitSuccess'))
  await getList()
}
const remove = async (id: number) => {
  await message.delConfirm()
  await RefundApi.deleteRefund(id)
  message.success(t('common.delSuccess'))
  await getList()
}
const viewProcess = (row: RefundApi.ReceivableRefundVO) =>
  push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
onMounted(getList)
onActivated(getList)
</script>
