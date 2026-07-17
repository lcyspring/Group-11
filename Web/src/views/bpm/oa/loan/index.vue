<template>
  <ContentWrap>
    <el-form ref="queryRef" :model="query" class="-mb-15px" label-width="auto">
      <el-row :gutter="16">
        <el-col :span="7"><el-form-item :label="t('oa.loan.type')" prop="type"><el-input v-model="query.type" clearable @keyup.enter="search" /></el-form-item></el-col>
        <el-col :span="7"><el-form-item :label="t('oa.loan.status')" prop="status"><el-select v-model="query.status" clearable><el-option v-for="item in getIntDictOptions(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS)" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="10"><el-form-item><el-button @click="search"><Icon icon="ep:search" />{{ t('common.search') }}</el-button><el-button @click="reset"><Icon icon="ep:refresh" />{{ t('common.reset') }}</el-button><el-button v-hasPermi="['bpm:oa-loan:create']" type="primary" @click="create">{{ t('oa.loan.create') }}</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column prop="id" :label="t('oa.loan.id')" />
      <el-table-column prop="type" :label="t('oa.loan.type')" />
      <el-table-column prop="amount" :label="t('oa.loan.amount')" />
      <el-table-column prop="approvalLimit" :label="t('oa.loan.limit')" />
      <el-table-column prop="escalatedApproval" :label="t('oa.loan.escalated')"><template #default="{ row }"><el-tag :type="row.escalatedApproval ? 'warning' : 'success'">{{ row.escalatedApproval ? t('common.yes') : t('common.no') }}</el-tag></template></el-table-column>
      <el-table-column prop="outstandingAmount" :label="t('oa.loan.outstanding')" />
      <el-table-column prop="status" :label="t('oa.loan.status')"><template #default="{ row }"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="row.status" /></template></el-table-column>
      <el-table-column fixed="right" :label="t('common.operation')" min-width="150"><template #default="{ row }"><TableActions mode="menu"><el-button link type="primary" @click="detail(row)">{{ t('common.detail') }}</el-button><el-button v-if="row.processInstanceId" link type="primary" @click="progress(row)">{{ t('oa.loan.progress') }}</el-button><el-button v-if="row.repaymentStatus === 1" v-hasPermi="['bpm:oa-loan:update']" link type="success" @click="repay(row)">{{ t('oa.loan.repay') }}</el-button></TableActions></template></el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </ContentWrap>
  <Dialog v-model="repaymentVisible" :title="t('oa.loan.repay')" width="520px">
    <el-form ref="repaymentRef" :model="repayment" :rules="repaymentRules" label-width="110px">
      <el-alert class="mb-16px" :closable="false" type="info">{{ t('oa.loan.outstandingHint', { amount: selectedLoan?.outstandingAmount }) }}</el-alert>
      <el-form-item :label="t('oa.loan.repaymentAmount')" prop="amount"><el-input-number v-model="repayment.amount" :min="0.01" :max="selectedLoan?.outstandingAmount" :precision="2" /></el-form-item>
      <el-form-item :label="t('oa.loan.repaidAt')"><el-date-picker v-model="repayment.repaidAt" type="datetime" value-format="x" /></el-form-item>
      <el-form-item :label="t('oa.loan.referenceNo')"><el-input v-model="repayment.referenceNo" maxlength="100" /></el-form-item>
      <el-form-item :label="t('oa.loan.remark')"><el-input v-model="repayment.remark" type="textarea" maxlength="500" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="repaymentLoading" @click="submitRepayment">{{ t('common.confirm') }}</el-button><el-button @click="repaymentVisible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as LoanApi from '@/api/bpm/loan'
defineOptions({ name: 'BpmOALoan' })
const { t } = useI18n('bpm')
const message = useMessage()
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<LoanApi.LoanVO[]>([])
const queryRef = ref()
const query = reactive({ pageNo: 1, pageSize: 10, type: undefined, status: undefined })
const load = async () => { loading.value = true; try { const data = await LoanApi.getLoanPage(query); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const search = () => { query.pageNo = 1; load() }
const reset = () => { queryRef.value?.resetFields(); search() }
const create = () => router.push({ name: 'OALoanCreate' })
const detail = (row: LoanApi.LoanVO) => router.push({ name: 'OALoanDetail', query: { id: row.id } })
const progress = (row: LoanApi.LoanVO) => router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
const repaymentVisible = ref(false)
const repaymentLoading = ref(false)
const repaymentRef = ref()
const selectedLoan = ref<LoanApi.LoanVO>()
const repayment = ref<LoanApi.LoanRepaymentVO>({ loanId: 0, amount: 0 })
const repaymentRules = { amount: [{ required: true, message: t('oa.loan.repaymentAmountRequired'), trigger: 'change' }] }
const repay = (row: LoanApi.LoanVO) => { selectedLoan.value = row; repayment.value = { loanId: row.id!, amount: Number(row.outstandingAmount), repaidAt: Date.now() }; repaymentVisible.value = true }
const submitRepayment = async () => { if (!await repaymentRef.value?.validate()) return; repaymentLoading.value = true; try { await LoanApi.createRepayment(repayment.value); message.success(t('oa.loan.repaymentSuccess')); repaymentVisible.value = false; await load() } finally { repaymentLoading.value = false } }
let initialized = false
onMounted(async () => { await load(); initialized = true })
onActivated(() => { if (initialized) load() })
</script>
