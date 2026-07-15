<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" class="-mb-15px" inline>
      <el-form-item :label="t('reimbursement.no')" prop="no">
        <el-input v-model="queryParams.no" clearable @keyup.enter="query" />
      </el-form-item>
      <el-form-item :label="t('reimbursement.status')" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-160px" clearable @change="query">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('reimbursement.expenseDate')" prop="expenseDate">
        <el-date-picker
          v-model="queryParams.expenseDate"
          end-placeholder=""
          range-separator="-"
          start-placeholder=""
          type="daterange"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="query">{{ t('common.search') }}</el-button>
        <el-button @click="reset">{{ t('common.reset') }}</el-button>
        <el-button v-hasPermi="['crm:reimbursement:create']" type="primary" @click="formRef.open('create')">
          {{ t('reimbursement.createDraft') }}
        </el-button>
        <el-button v-hasPermi="['crm:expense-category:write']" @click="categoryRef.open()">
          {{ t('reimbursement.categoryManagement') }}
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
      <el-table-column fixed="left" :label="t('reimbursement.no')" min-width="170" prop="no">
        <template #default="{ row }">
          <el-link :underline="false" type="primary" @click="detailRef.open(row.id)">{{ row.no }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('reimbursement.applicant')" min-width="110" prop="applicantUserName" />
      <el-table-column :label="t('reimbursement.customer')" min-width="130" prop="customerName">
        <template #default="{ row }">{{ row.customerName || '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('reimbursement.contract')" min-width="190">
        <template #default="{ row }">
          {{ row.contractId ? `${row.contractNo || ''} · ${row.contractName || ''}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('reimbursement.currency')" min-width="90" prop="currency" />
      <el-table-column align="right" :label="t('reimbursement.totalAmount')" min-width="130">
        <template #default="{ row }">{{ money(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column :label="t('reimbursement.expenseDate')" min-width="190">
        <template #default="{ row }">{{ row.expenseStartDate }} → {{ row.expenseEndDate }}</template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('reimbursement.status')" min-width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.auditStatus)">{{ statusLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('common.action')" min-width="260">
        <template #default="{ row }">
          <el-button
            v-if="canEditReimbursement(row.auditStatus)"
            v-hasPermi="['crm:reimbursement:update']"
            link
            type="primary"
            @click="formRef.open('update', row.id)"
          >
            {{ row.auditStatus === REIMBURSEMENT_STATUS.DRAFT ? t('common.edit') : t('reimbursement.revise') }}
          </el-button>
          <el-button
            v-if="row.auditStatus === REIMBURSEMENT_STATUS.DRAFT"
            v-hasPermi="['crm:reimbursement:update']"
            link
            type="success"
            @click="submit(row)"
          >
            {{ t('reimbursement.submit') }}
          </el-button>
          <el-button v-if="row.processInstanceId" link type="primary" @click="viewProcess(row)">
            {{ t('reimbursement.viewApproval') }}
          </el-button>
          <el-button
            v-if="canDeleteReimbursement(row.auditStatus, row.processInstanceId)"
            v-hasPermi="['crm:reimbursement:delete']"
            link
            type="danger"
            @click="remove(row.id)"
          >
            {{ t('common.delete') }}
          </el-button>
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

  <ReimbursementForm ref="formRef" @success="getList" />
  <ReimbursementDetail ref="detailRef" />
  <ExpenseCategoryDialog ref="categoryRef" />
</template>

<script setup lang="ts">
import * as ReimbursementApi from '@/api/crm/reimbursement'
import ExpenseCategoryDialog from './ExpenseCategoryDialog.vue'
import ReimbursementDetail from './ReimbursementDetail.vue'
import ReimbursementForm from './ReimbursementForm.vue'
import {
  REIMBURSEMENT_STATUS,
  canDeleteReimbursement,
  canEditReimbursement
} from './constants'

defineOptions({ name: 'CrmReimbursement' })
const { t } = useI18n('crm')
const message = useMessage()
const { push } = useRouter()
const loading = ref(false)
const list = ref<ReimbursementApi.ReimbursementVO[]>([])
const total = ref(0)
const formRef = ref()
const detailRef = ref()
const categoryRef = ref()
const queryFormRef = ref()
const sceneType = ref(1)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: '',
  auditStatus: undefined as number | undefined,
  expenseDate: undefined as string[] | undefined
})
const statusOptions = computed(() => [
  { value: REIMBURSEMENT_STATUS.DRAFT, label: t('reimbursement.statusDraft') },
  { value: REIMBURSEMENT_STATUS.PROCESSING, label: t('reimbursement.statusProcessing') },
  { value: REIMBURSEMENT_STATUS.APPROVED, label: t('reimbursement.statusApproved') },
  { value: REIMBURSEMENT_STATUS.REJECTED, label: t('reimbursement.statusRejected') },
  { value: REIMBURSEMENT_STATUS.CANCELED, label: t('reimbursement.statusCanceled') }
])
const statusLabel = (value?: number) =>
  statusOptions.value.find((item) => item.value === value)?.label || String(value ?? '-')
const statusTag = (value?: number) =>
  ({ 0: 'info', 10: 'warning', 20: 'success', 30: 'danger', 40: 'info' })[value ?? -1] as any
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const getList = async () => {
  loading.value = true
  try {
    const data = await ReimbursementApi.getReimbursementPage({ ...queryParams, sceneType: sceneType.value })
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
  Object.assign(queryParams, { pageNo: 1, no: '', auditStatus: undefined, expenseDate: undefined })
  getList()
}
const submit = async (row: ReimbursementApi.ReimbursementVO) => {
  await message.confirm(t('reimbursement.submitConfirm', { no: row.no }))
  await ReimbursementApi.submitReimbursement(row.id!)
  message.success(t('reimbursement.submitSuccess'))
  await getList()
}
const remove = async (id: number) => {
  await message.delConfirm()
  await ReimbursementApi.deleteReimbursement(id)
  message.success(t('common.delSuccess'))
  await getList()
}
const viewProcess = (row: ReimbursementApi.ReimbursementVO) =>
  push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
onMounted(getList)
onActivated(getList)
</script>
