<template>
  <el-row :gutter="20">
    <el-col :span="16"><ContentWrap :title="t('oa.loan.create')"><el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="110px">
      <el-form-item :label="t('oa.loan.type')" prop="type"><el-select v-model="form.type" class="w-full"><el-option v-for="item in types" :key="item" :label="item" :value="item" /></el-select></el-form-item>
      <el-form-item :label="t('oa.loan.amount')" prop="amount"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="500" /></el-form-item>
      <el-form-item :label="t('oa.loan.trip')"><el-select v-model="form.tripId" clearable filterable class="w-full"><el-option v-for="trip in trips" :key="trip.id" :label="`${trip.destination} · ${trip.startTime}`" :value="trip.id" /></el-select></el-form-item>
      <el-form-item :label="t('oa.loan.reason')" prop="reason"><el-input v-model="form.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit /></el-form-item>
      <el-alert v-if="amountHint" class="mb-16px" :closable="false" :type="amountHint.type">{{ amountHint.text }}</el-alert>
      <el-form-item><el-button type="primary" :loading="loading" @click="submit">{{ t('common.confirm') }}</el-button><el-button @click="cancel">{{ t('common.cancel') }}</el-button></el-form-item>
    </el-form></ContentWrap></el-col>
    <el-col :span="8"><ContentWrap :title="t('process.instance.flowDiagram')"><ProcessInstanceTimeline :activity-nodes="activityNodes" :show-status-icon="false" @select-user-confirm="selectUserConfirm" /></ContentWrap></el-col>
  </el-row>
</template>

<script setup lang="ts">
import * as LoanApi from '@/api/bpm/loan'
import * as TripApi from '@/api/bpm/trip'
import * as DefinitionApi from '@/api/bpm/definition'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import ProcessInstanceTimeline from '@/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue'
import { CandidateStrategy, NodeId } from '@/components/SimpleProcessDesignerV2/src/consts'
import { useTagsViewStore } from '@/store/modules/tagsView'
defineOptions({ name: 'BpmOALoanCreate' })
const { t } = useI18n('bpm')
const message = useMessage()
const router = useRouter()
const { delView } = useTagsViewStore()
const loading = ref(false)
const formRef = ref()
const form = ref<LoanApi.LoanVO>({ type: '', amount: 0, reason: '' })
const types = computed(() => [t('oa.loan.typeTravel'), t('oa.loan.typePurchase'), t('oa.loan.typeOther')])
const trips = ref<TripApi.TripVO[]>([])
const loanLimit = ref<LoanApi.LoanLimitVO>()
const rules = { type: [{ required: true, message: t('oa.loan.typeRequired'), trigger: 'change' }], amount: [{ required: true, message: t('oa.loan.amountRequired'), trigger: 'change' }], reason: [{ required: true, message: t('oa.loan.reasonRequired'), trigger: 'blur' }, { min: 5, max: 1000, message: t('oa.loan.reasonLength'), trigger: 'blur' }] }
const amountHint = computed(() => loanLimit.value && form.value.amount > loanLimit.value.approvalLimit
  ? { type: 'warning' as const, text: t('oa.loan.limitHint', { limit: loanLimit.value.approvalLimit }) }
  : loanLimit.value ? { type: 'info' as const, text: t('oa.loan.currentLimit', { level: loanLimit.value.employeeLevel, limit: loanLimit.value.approvalLimit }) } : undefined)
const processDefinitionId = ref('')
const activityNodes = ref<any[]>([])
const selectTasks = ref<any[]>([])
const assignees = ref<Record<string, number[]>>({})
const selectUserConfirm = (id: string, users: any[]) => { assignees.value[id] = users.map((user) => user.id) }
const loadApproval = async () => { const data = await ProcessInstanceApi.getApprovalDetail({ processDefinitionId: processDefinitionId.value, activityId: NodeId.START_USER_NODE_ID, processVariablesStr: JSON.stringify({ amount: form.value.amount, escalatedApproval: !!loanLimit.value && form.value.amount > loanLimit.value.approvalLimit }) }); activityNodes.value = data?.activityNodes || []; selectTasks.value = activityNodes.value.filter((node) => node.candidateStrategy === CandidateStrategy.START_USER_SELECT); selectTasks.value.forEach((task) => { assignees.value[task.id] ||= [] }) }
const submit = async () => { if (!await formRef.value?.validate()) return; for (const task of selectTasks.value) if (!assignees.value[task.id]?.length) return message.warning(t('process.instance.selectCandidate', { name: task.name })); loading.value = true; try { await LoanApi.createLoan({ ...form.value, startUserSelectAssignees: assignees.value }); message.success(t('process.instance.startSuccess')); delView(unref(router.currentRoute)); await router.push({ name: 'BpmOALoan' }) } finally { loading.value = false } }
const cancel = async () => { delView(unref(router.currentRoute)); await router.push({ name: 'BpmOALoan' }) }
onMounted(async () => { const [definition, tripPage, limit] = await Promise.all([DefinitionApi.getProcessDefinition(undefined, 'oa_loan'), TripApi.getTripPage({ pageNo: 1, pageSize: 100 }), LoanApi.getMyLimit()]); trips.value = tripPage.list || []; loanLimit.value = limit; if (!definition) return message.error(t('oa.loan.processModelNotFound')); processDefinitionId.value = definition.id; await loadApproval() })
watch(() => form.value.amount, () => { if (processDefinitionId.value) loadApproval() })
</script>
