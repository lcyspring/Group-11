<template>
  <Dialog v-model="visible" :title="t('workOrder.assign')" width="520px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item :label="t('workOrder.group')" prop="groupId">
        <el-select v-model="formData.groupId" class="w-full" clearable @change="loadCandidates">
          <el-option v-for="item in context.groups" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('workOrder.handler')" prop="handlerUserId">
        <el-select v-model="formData.handlerUserId" class="w-full" filterable>
          <el-option
            v-for="item in context.candidates"
            :key="item.id"
            :label="candidateLabel(item.nickname, item.openCount)"
            :value="item.id"
            :disabled="item.id === currentHandlerUserId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('workOrder.assignRemark')" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="submit">{{ t('common.confirm') }}</el-button>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import { candidateLabel } from './dispatch'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const workOrderType = ref(1)
const context = ref<WorkOrderApi.WorkOrderDispatchContextVO>({
  enabled: true,
  autoAssignOnCreate: true,
  fallbackMode: 'UNASSIGNED_POOL',
  maxCcUsers: 20,
  manualAssignmentAllowed: true,
  groups: [],
  candidates: []
})
const currentHandlerUserId = ref<number>()
const formData = reactive({ id: 0, groupId: undefined as number | undefined, handlerUserId: undefined as number | undefined, remark: '' })
const rules = reactive({
  handlerUserId: [{ required: true, message: t('workOrder.handlerRequired'), trigger: 'change' }]
})
const emit = defineEmits<{ (e: 'success'): void }>()

const open = async (row: WorkOrderApi.WorkOrderVO) => {
  visible.value = true
  formData.id = row.id!
  formData.groupId = row.groupId
  formData.handlerUserId = undefined
  formData.remark = ''
  workOrderType.value = row.type
  currentHandlerUserId.value = row.handlerUserId
  await loadCandidates()
}

const loadCandidates = async () => {
  formData.handlerUserId = undefined
  context.value = await WorkOrderApi.getDispatchContext(workOrderType.value, formData.groupId)
}

const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    await WorkOrderApi.assignWorkOrder(
      formData.id,
      formData.handlerUserId!,
      formData.groupId,
      formData.remark || undefined
    )
    message.success(t('workOrder.assignSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
