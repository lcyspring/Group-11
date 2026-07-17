<template>
  <el-row :gutter="20">
    <el-col :span="16">
      <ContentWrap :title="t('oa.trip.create')">
        <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item :label="t('oa.trip.startTime')" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="x" :disabled-date="disablePastDate" class="w-full" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="t('oa.trip.endTime')" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="x" :disabled-date="disablePastDate" class="w-full" /></el-form-item></el-col>
          </el-row>
          <el-form-item :label="t('oa.trip.days')"><el-input :model-value="tripDays" disabled /></el-form-item>
          <el-form-item :label="t('oa.trip.destination')" prop="destination"><el-input v-model="form.destination" maxlength="200" show-word-limit /></el-form-item>
          <el-form-item :label="t('oa.trip.reason')" prop="reason"><el-input v-model="form.reason" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
          <el-form-item :label="t('oa.trip.estimatedExpense')" prop="estimatedExpense"><el-input-number v-model="form.estimatedExpense" :min="0" :precision="2" :step="100" controls-position="right" /></el-form-item>
          <el-form-item :label="t('oa.trip.companions')"><el-select v-model="form.companionUserIds" multiple filterable collapse-tags collapse-tags-tooltip :multiple-limit="20" class="w-full"><el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" /></el-select></el-form-item>
          <el-form-item :label="t('oa.trip.attachments')"><UploadFile v-model="form.attachmentUrls" :limit="10" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="loading" @click="submit">{{ t('common.confirm') }}</el-button><el-button @click="cancel">{{ t('common.cancel') }}</el-button></el-form-item>
        </el-form>
      </ContentWrap>
    </el-col>
    <el-col :span="8">
      <ContentWrap :title="t('process.instance.flowDiagram')" :body-style="{ padding: '0 20px' }">
        <ProcessInstanceTimeline :activity-nodes="activityNodes" :show-status-icon="false" @select-user-confirm="selectUserConfirm" />
      </ContentWrap>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import * as TripApi from '@/api/bpm/trip'
import * as UserApi from '@/api/system/user'
import * as DefinitionApi from '@/api/bpm/definition'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import ProcessInstanceTimeline from '@/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue'
import { CandidateStrategy, NodeId } from '@/components/SimpleProcessDesignerV2/src/consts'
import type { ApprovalNodeInfo } from '@/api/bpm/processInstance'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { calculateTripDays, isFutureTrip } from './tripDuration.mjs'

defineOptions({ name: 'BpmOATripCreate' })
const { t } = useI18n('bpm')
const message = useMessage()
const router = useRouter()
const route = useRoute()
const { delView } = useTagsViewStore()
const loading = ref(false)
const formRef = ref()
const users = ref<any[]>([])
const form = ref<TripApi.TripVO>({ startTime: '', endTime: '', destination: '', reason: '', estimatedExpense: 0, companionUserIds: [], attachmentUrls: [] })
const tripDays = computed(() => calculateTripDays(form.value.startTime, form.value.endTime))
const validateStart = (_: unknown, value: string | number, done: (error?: Error) => void) => done(!value || isFutureTrip(value) ? undefined : new Error(t('oa.trip.startFuture')))
const validateEnd = (_: unknown, value: string | number, done: (error?: Error) => void) => done(!value || Number(value) > Number(form.value.startTime) ? undefined : new Error(t('oa.trip.endAfterStart')))
const rules = reactive({
  startTime: [{ required: true, message: t('oa.trip.startRequired'), trigger: 'change' }, { validator: validateStart, trigger: 'change' }],
  endTime: [{ required: true, message: t('oa.trip.endRequired'), trigger: 'change' }, { validator: validateEnd, trigger: 'change' }],
  destination: [{ required: true, message: t('oa.trip.destinationRequired'), trigger: 'blur' }],
  reason: [{ required: true, message: t('oa.trip.reasonRequired'), trigger: 'blur' }, { min: 5, max: 1000, message: t('oa.trip.reasonLength'), trigger: 'blur' }]
})
const processDefinitionId = ref('')
const activityNodes = ref<ApprovalNodeInfo[]>([])
const startUserSelectTasks = ref<any[]>([])
const startUserSelectAssignees = ref<Record<string, number[]>>({})
const disablePastDate = (date: Date) => date.getTime() < new Date().setHours(0, 0, 0, 0)
const selectUserConfirm = (id: string, selected: any[]) => { startUserSelectAssignees.value[id] = selected.map((item) => item.id) }
const loadApproval = async () => {
  const data = await ProcessInstanceApi.getApprovalDetail({ processDefinitionId: processDefinitionId.value, activityId: NodeId.START_USER_NODE_ID, processVariablesStr: JSON.stringify({ days: tripDays.value, estimatedExpense: form.value.estimatedExpense || 0, destination: form.value.destination }) })
  activityNodes.value = data?.activityNodes || []
  startUserSelectTasks.value = activityNodes.value.filter((node) => node.candidateStrategy === CandidateStrategy.START_USER_SELECT)
  startUserSelectTasks.value.forEach((node) => { startUserSelectAssignees.value[node.id] ||= [] })
}
const submit = async () => {
  if (!await formRef.value?.validate()) return
  for (const task of startUserSelectTasks.value) if (!startUserSelectAssignees.value[task.id]?.length) return message.warning(t('process.instance.selectCandidate', { name: task.name }))
  loading.value = true
  try { await TripApi.createTrip({ ...form.value, startUserSelectAssignees: startUserSelectAssignees.value }); message.success(t('process.instance.startSuccess')); delView(unref(router.currentRoute)); await router.push({ name: 'BpmOATrip' }) } finally { loading.value = false }
}
const cancel = async () => { delView(unref(router.currentRoute)); await router.push({ name: 'BpmOATrip' }) }
onMounted(async () => {
  users.value = await UserApi.getSimpleUserList()
  const definition = await DefinitionApi.getProcessDefinition(undefined, 'oa_trip')
  if (!definition) return message.error(t('oa.trip.processModelNotFound'))
  processDefinitionId.value = definition.id
  if (route.query.id) form.value = { ...(await TripApi.getTrip(Number(route.query.id))), id: undefined, status: undefined, processInstanceId: undefined }
  await loadApproval()
})
watch(() => [form.value.startTime, form.value.endTime, form.value.estimatedExpense, form.value.destination], () => { if (processDefinitionId.value) loadApproval() })
</script>
