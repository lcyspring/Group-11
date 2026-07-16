<template>
  <Dialog v-model="dialogVisible" title="修改优先级" width="450px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="当前优先级">
            <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_PRIORITY" :value="currentPriority" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="新优先级" prop="priority">
            <el-select v-model="formData.priority" placeholder="请选择优先级" class="w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.WO_WORK_ORDER_PRIORITY)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
                :disabled="dict.value === currentPriority"
              />
            </el-select>
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

defineOptions({ name: 'WorkOrderPriorityForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const currentPriority = ref(0)
const formData = ref({
  id: undefined as number | undefined,
  priority: undefined as number | undefined
})
const formRules = reactive({
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
})
const formRef = ref()

/** 打开弹窗 */
const open = async (id: number, currentPriorityValue: number) => {
  dialogVisible.value = true
  formLoading.value = true
  try {
    formData.value.id = id
    currentPriority.value = currentPriorityValue
  } finally {
    formLoading.value = false
    formData.value.priority = undefined
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
    await WorkOrderApi.updateWorkOrderPriority({
      id: formData.value.id!,
      priority: formData.value.priority!
    })
    message.success('优先级更新成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
