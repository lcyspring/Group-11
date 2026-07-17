<template><ContentWrap v-loading="loading" :title="t('oa.loan.detail')"><el-descriptions :column="2" border>
  <el-descriptions-item :label="t('oa.loan.id')">{{ loan.id }}</el-descriptions-item><el-descriptions-item :label="t('oa.loan.status')"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="loan.status" /></el-descriptions-item>
  <el-descriptions-item :label="t('oa.loan.type')">{{ loan.type }}</el-descriptions-item><el-descriptions-item :label="t('oa.loan.amount')">{{ loan.amount }}</el-descriptions-item>
  <el-descriptions-item :label="t('oa.loan.level')">{{ loan.employeeLevel }}</el-descriptions-item><el-descriptions-item :label="t('oa.loan.limit')">{{ loan.approvalLimit }}</el-descriptions-item>
  <el-descriptions-item :label="t('oa.loan.escalated')">{{ loan.escalatedApproval ? t('common.yes') : t('common.no') }}</el-descriptions-item><el-descriptions-item :label="t('oa.loan.outstanding')">{{ loan.outstandingAmount }}</el-descriptions-item>
  <el-descriptions-item :label="t('oa.loan.reason')" :span="2">{{ loan.reason }}</el-descriptions-item>
</el-descriptions><el-divider>{{ t('oa.loan.repaymentRecords') }}</el-divider><el-table :data="repayments"><el-table-column prop="amount" :label="t('oa.loan.repaymentAmount')" /><el-table-column prop="repaidAt" :label="t('oa.loan.repaidAt')" /><el-table-column prop="referenceNo" :label="t('oa.loan.referenceNo')" /><el-table-column prop="remark" :label="t('oa.loan.remark')" /></el-table></ContentWrap></template>
<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import * as LoanApi from '@/api/bpm/loan'
defineOptions({ name: 'BpmOALoanDetail' })
const { t } = useI18n('bpm')
const message = useMessage()
const route = useRoute()
const loading = ref(false)
const loan = ref<LoanApi.LoanVO>({} as LoanApi.LoanVO)
const repayments = ref<LoanApi.LoanRepaymentVO[]>([])
onMounted(async () => {
  const rawId = route.query.loanId ?? route.query.id
  const id = Number(rawId)
  if (!Number.isSafeInteger(id) || id <= 0) {
    message.error(t('oa.loan.invalidId'))
    return
  }
  loading.value = true
  try { [loan.value, repayments.value] = await Promise.all([LoanApi.getLoan(id), LoanApi.getRepayments(id)]) } finally { loading.value = false }
})
</script>
