<template>
  <Dialog v-model="dialogVisible" title="分配工单" width="450px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="当前处理人">
            {{ currentHandlerName || '未分配' }}
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="新处理人ID" prop="handlerUserId">
            <el-input-number
              v-model="formData.handlerUserId"
              :min="1"
              placeholder="请输入处理人用户编号"
              class="w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as WorkOrderApi from '@/api/workorder/workOrder'

defineOptions({ name: 'WorkOrderAssignForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const currentHandlerName = ref('')
const formData = ref({
  id: undefined as number | undefined,
  handlerUserId: undefined as number | undefined
})
const formRules = reactive({
  handlerUserId: [{ required: true, message: '请输入处理人用户编号', trigger: 'blur' }]
})
const formRef = ref()

/** 打开弹窗 */
const open = async (id: number, currentHandlerNameValue: string) => {
  dialogVisible.value = true
  formLoading.value = true
  try {
    formData.value.id = id
    currentHandlerName.value = currentHandlerNameValue
  } finally {
    formLoading.value = false
    formData.value.handlerUserId = undefined
    formRef.value?.resetFields()
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    await WorkOrderApi.assignWorkOrder({
      id: formData.value.id!,
      handlerUserId: formData.value.handlerUserId!
    })
    message.success('工单分配成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
