<template>
  <Dialog v-model="visible" :title="t('workOrder.assign')" width="520px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item :label="t('workOrder.handler')" prop="handlerUserId">
        <el-select v-model="formData.handlerUserId" class="w-full" filterable>
          <el-option
            v-for="item in users"
            :key="item.id"
            :label="item.nickname"
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
      <el-button type="primary" :loading="loading" @click="submit">{{ t('dialog.confirm') }}</el-button>
      <el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import * as UserApi from '@/api/system/user'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const users = ref<UserApi.UserVO[]>([])
const currentHandlerUserId = ref<number>()
const formData = reactive({ id: 0, handlerUserId: undefined as number | undefined, remark: '' })
const rules = reactive({
  handlerUserId: [{ required: true, message: t('workOrder.handlerRequired'), trigger: 'change' }]
})
const emit = defineEmits<{ (e: 'success'): void }>()

const open = async (row: WorkOrderApi.WorkOrderVO) => {
  visible.value = true
  formData.id = row.id!
  formData.handlerUserId = undefined
  formData.remark = ''
  currentHandlerUserId.value = row.handlerUserId
  users.value = await UserApi.getSimpleUserList()
}

const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    await WorkOrderApi.assignWorkOrder(formData.id, formData.handlerUserId!, formData.remark || undefined)
    message.success(t('workOrder.assignSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
