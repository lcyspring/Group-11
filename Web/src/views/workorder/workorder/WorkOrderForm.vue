<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="工单标题" prop="title">
            <el-input v-model="formData.title" placeholder="请输入工单标题" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工单类型" prop="typeId">
            <el-select v-model="formData.typeId" placeholder="请选择工单类型" class="w-full">
              <el-option
                v-for="item in typeList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="formData.priority" placeholder="请选择优先级" class="w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.WO_WORK_ORDER_PRIORITY)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="处理人" prop="handlerUserId">
            <el-input-number v-model="formData.handlerUserId" :min="0" placeholder="请输入处理人ID" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预计完成时间" prop="expectedFinishTime">
            <el-date-picker
              v-model="formData.expectedFinishTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择预计完成时间"
              class="w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="关联客户" prop="customerId">
            <el-input-number v-model="formData.customerId" :min="0" placeholder="请输入客户ID" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联商机" prop="businessId">
            <el-input-number v-model="formData.businessId" :min="0" placeholder="请输入商机ID" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="工单内容" prop="content">
            <el-input v-model="formData.content" placeholder="请输入工单内容/描述" type="textarea" :rows="5" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" placeholder="请输入备注" type="textarea" :rows="2" />
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
import * as WorkOrderTypeApi from '@/api/workorder/workOrderType'

defineOptions({ name: 'WorkOrderForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const typeList = ref<WorkOrderTypeApi.WorkOrderTypeVO[]>([])
const formData = ref({
  id: undefined,
  title: '',
  content: '',
  typeId: undefined,
  priority: 0,
  handlerUserId: undefined,
  expectedFinishTime: undefined,
  customerId: undefined,
  businessId: undefined,
  remark: ''
})
const formRules = reactive({
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择工单类型', trigger: 'change' }]
})
const formRef = ref()

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增工单' : '编辑工单'
  formType.value = type
  resetForm()
  // 加载工单类型列表
  try {
    typeList.value = await WorkOrderTypeApi.getEnableWorkOrderTypeList()
  } catch {}
  // 修改时设置数据
  if (id) {
    formLoading.value = true
    try {
      const res = await WorkOrderApi.getWorkOrder(id)
      formData.value = res
    } finally {
      formLoading.value = false
    }
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
    const data = formData.value as unknown as WorkOrderApi.WorkOrderVO
    if (formType.value === 'create') {
      await WorkOrderApi.createWorkOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await WorkOrderApi.updateWorkOrder(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    title: '',
    content: '',
    typeId: undefined,
    priority: 0,
    handlerUserId: undefined,
    expectedFinishTime: undefined,
    customerId: undefined,
    businessId: undefined,
    remark: ''
  }
  formRef.value?.resetFields()
}
</script>
