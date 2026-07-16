<template>
  <Dialog v-model="dialogVisible" title="完结工单" width="500px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="当前状态">
            <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_STATUS" :value="currentStatus" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="处理结果" prop="result">
            <el-input
              v-model="formData.result"
              placeholder="请输入处理结果（必填）"
              type="textarea"
              :rows="4"
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
import { DICT_TYPE } from '@/utils/dict'
import * as WorkOrderApi from '@/api/workorder/workOrder'

defineOptions({ name: 'WorkOrderCompleteForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const currentStatus = ref(0)
const formData = ref({
  id: undefined as number | undefined,
  result: ''
})
const formRules = reactive({
  result: [{ required: true, message: '请输入处理结果', trigger: 'blur' }]
})
const formRef = ref()

/** 打开弹窗 */
const open = async (id: number, currentStatusValue: number) => {
  dialogVisible.value = true
  formLoading.value = true
  try {
    formData.value.id = id
    currentStatus.value = currentStatusValue
  } finally {
    formLoading.value = false
    formData.value.result = ''
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
    await WorkOrderApi.completeWorkOrder({
      id: formData.value.id!,
      result: formData.value.result
    })
    message.success('工单已完结')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
