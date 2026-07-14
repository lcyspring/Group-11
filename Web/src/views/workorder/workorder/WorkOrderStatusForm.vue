<template>
  <Dialog v-model="dialogVisible" title="工单状态流转" width="500px">
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
          <el-form-item label="目标状态" prop="status">
            <el-select v-model="formData.status" placeholder="请选择目标状态" class="w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.WO_WORK_ORDER_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
                :disabled="!canTransition(dict.value)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="处理结果" prop="result">
            <el-input
              v-model="formData.result"
              placeholder="请输入处理结果/备注信息"
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
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as WorkOrderApi from '@/api/workorder/workOrder'

defineOptions({ name: 'WorkOrderStatusForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const currentStatus = ref(0)
const formData = ref({
  id: undefined,
  status: undefined,
  result: ''
})
const formRules = reactive({
  status: [{ required: true, message: '请选择目标状态', trigger: 'change' }]
})
const formRef = ref()

// 不可流转到当前状态或已完成/已关闭
const canTransition = (targetStatus: number) => {
  if (targetStatus === currentStatus.value) return false
  if (currentStatus.value === 2 || currentStatus.value === 3) return false
  return true
}

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  formLoading.value = true
  try {
    const res = await WorkOrderApi.getWorkOrder(id)
    formData.value.id = res.id
    currentStatus.value = res.status
  } finally {
    formLoading.value = false
    formData.value.status = undefined
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
    await WorkOrderApi.updateWorkOrderStatus({
      id: formData.value.id!,
      status: formData.value.status!,
      result: formData.value.result
    })
    message.success('状态更新成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
