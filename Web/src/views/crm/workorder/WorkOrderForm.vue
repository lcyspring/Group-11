<template>
  <Dialog v-model="visible" :title="title">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item :label="t('workOrder.title')" prop="title"><el-input v-model="formData.title" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('workOrder.type')" prop="type"><el-select v-model="formData.type" class="w-full" @change="loadDispatch"><el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('workOrder.priority')" prop="priority"><el-select v-model="formData.priority" class="w-full"><el-option v-for="item in priorities" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('workOrder.customer')" prop="customerId"><el-select v-model="formData.customerId" class="w-full" filterable :disabled="mode === 'update'" @change="() => loadSources(false)"><el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('workOrder.group')"><el-select v-model="formData.groupId" class="w-full" clearable :disabled="mode === 'update'" @change="loadDispatch"><el-option v-for="item in dispatchContext.groups" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('workOrder.handler')"><el-select v-model="formData.handlerUserId" class="w-full" clearable filterable :disabled="mode === 'update' || !dispatchContext.manualAssignmentAllowed" :placeholder="t('workOrder.autoAssignHint')"><el-option v-for="item in dispatchContext.candidates" :key="item.id" :label="candidateLabel(item.nickname, item.openCount)" :value="item.id" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('workOrder.ccUsers')"><el-select v-model="formData.ccUserIds" class="w-full" multiple collapse-tags collapse-tags-tooltip filterable :multiple-limit="dispatchContext.maxCcUsers || 20"><el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('workOrder.sourceType')" prop="sourceType"><el-select v-model="formData.sourceType" class="w-full" :disabled="mode === 'update'" @change="() => loadSources(false)"><el-option v-for="item in sourceTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col v-if="formData.sourceType !== 0" :span="12"><el-form-item :label="t('workOrder.source')" prop="sourceId"><el-select v-model="formData.sourceId" class="w-full" filterable :disabled="mode === 'update'"><el-option v-for="item in sources" :key="item.id" :label="item.name || item.no" :value="item.id" /></el-select></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('workOrder.description')" prop="description"><el-input v-model="formData.description" type="textarea" :rows="5" /></el-form-item>
      <el-form-item :label="t('workOrder.attachments')"><el-input v-model="attachmentText" :placeholder="t('workOrder.attachmentsPlaceholder')" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="loading" @click="submit">{{ t('dialog.confirm') }}</el-button><el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import * as CustomerApi from '@/api/crm/customer'
import * as UserApi from '@/api/system/user'
import * as BusinessApi from '@/api/crm/business'
import * as ContractApi from '@/api/crm/contract'
import { candidateLabel, normalizeCcUserIds } from './dispatch'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const mode = ref<'create' | 'update'>('create')
const title = computed(() => mode.value === 'create' ? t('workOrder.create') : t('workOrder.update'))
const formRef = ref()
const formData = ref<WorkOrderApi.WorkOrderVO>({} as WorkOrderApi.WorkOrderVO)
const attachmentText = ref('')
const customers = ref<any[]>([])
const users = ref<any[]>([])
const dispatchContext = ref<WorkOrderApi.WorkOrderDispatchContextVO>({
  enabled: true,
  autoAssignOnCreate: true,
  fallbackMode: 'UNASSIGNED_POOL',
  maxCcUsers: 20,
  manualAssignmentAllowed: false,
  groups: [],
  candidates: []
})
const sources = ref<any[]>([])
const types = [{ value: 1, label: t('workOrder.typeIssue') }, { value: 2, label: t('workOrder.typeDemand') }, { value: 3, label: t('workOrder.typeComplaint') }, { value: 4, label: t('workOrder.typeConsultation') }]
const priorities = [{ value: 1, label: t('workOrder.priorityLow') }, { value: 2, label: t('workOrder.priorityMedium') }, { value: 3, label: t('workOrder.priorityHigh') }]
const sourceTypes = [{ value: 0, label: t('workOrder.sourceCustomer') }, { value: 1, label: t('workOrder.sourceBusiness') }, { value: 2, label: t('workOrder.sourceContract') }]
const rules = reactive({
  title: [{ required: true, message: t('workOrder.titleRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('workOrder.typeRequired'), trigger: 'change' }],
  priority: [{ required: true, message: t('workOrder.priorityRequired'), trigger: 'change' }],
  customerId: [{ required: true, message: t('workOrder.customerRequired'), trigger: 'change' }],
  sourceType: [{ required: true, message: t('workOrder.sourceTypeRequired'), trigger: 'change' }],
  sourceId: [{ required: true, message: t('workOrder.sourceRequired'), trigger: 'change' }],
  description: [
    { required: true, message: t('workOrder.descriptionRequired'), trigger: 'blur' },
    { min: 20, message: t('workOrder.descriptionMinLength'), trigger: 'blur' }
  ]
})
const emit = defineEmits<{ (e: 'success'): void }>()

const loadSources = async (preserveSource = false) => {
  const sourceId = formData.value.sourceId
  sources.value = []
  if (!preserveSource) formData.value.sourceId = undefined
  if (!formData.value.customerId || formData.value.sourceType === 0) return
  if (formData.value.sourceType === 1) {
    const all = await BusinessApi.getSimpleBusinessList()
    sources.value = all.filter((item: any) => item.customerId === formData.value.customerId)
  } else sources.value = await ContractApi.getContractSimpleList(formData.value.customerId)
  if (preserveSource) formData.value.sourceId = sourceId
}
const loadDispatch = async () => {
  if (!formData.value.type || mode.value === 'update') return
  formData.value.handlerUserId = undefined
  dispatchContext.value = await WorkOrderApi.getDispatchContext(formData.value.type, formData.value.groupId)
}
const open = async (type: 'create' | 'update', id?: number) => {
  visible.value = true; mode.value = type; formData.value = {} as WorkOrderApi.WorkOrderVO; attachmentText.value = ''
  customers.value = await CustomerApi.getCustomerSimpleList(); users.value = await UserApi.getSimpleUserList()
  if (id) {
    loading.value = true
    try { formData.value = await WorkOrderApi.getWorkOrder(id); attachmentText.value = formData.value.attachmentUrls?.join(',') || ''; await loadSources(true) }
    finally { loading.value = false }
  } else {
    formData.value = { type: 1, priority: 2, sourceType: 0, title: '', description: '', ccUserIds: [] } as WorkOrderApi.WorkOrderVO
    await loadDispatch()
  }
}
const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    formData.value.attachmentUrls = attachmentText.value.split(',').map(item => item.trim()).filter(Boolean)
    formData.value.ccUserIds = normalizeCcUserIds(formData.value.ccUserIds, dispatchContext.value.maxCcUsers)
    if (mode.value === 'create') await WorkOrderApi.createWorkOrder(formData.value)
    else await WorkOrderApi.updateWorkOrder(formData.value)
    message.success(mode.value === 'create' ? t('common.createSuccess') : t('common.updateSuccess'))
    visible.value = false; emit('success')
  } finally { loading.value = false }
}
defineExpose({ open })
</script>
