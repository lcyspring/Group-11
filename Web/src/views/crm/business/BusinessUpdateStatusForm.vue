<template>
  <Dialog :title="t('crm.business.changeStatus')" v-model="dialogVisible" width="400">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="auto"
      v-loading="formLoading"
    >
      <el-form-item :label="t('crm.business.statusName')" prop="status">
        <el-select
          v-model="formData.status"
          :placeholder="t('crm.business.statusPlaceholder')"
          class="w-1/1"
        >
          <el-option
            v-for="item in availableStatusList"
            :key="item.id"
            :label="item.name + '(' + t('crm.business.winRate') + '：' + item.percent + '%)'"
            :value="item.id"
          />
          <el-option
            v-for="item in BusinessStatusApi.DEFAULT_STATUSES"
            :key="item.endStatus"
            :label="item.name + '(' + t('crm.business.winRate') + '：' + item.percent + '%)'"
            :value="-item.endStatus"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="requiresStatusRemark"
        :label="t('crm.business.statusRemark')"
        prop="statusRemark"
      >
        <el-input
          v-model="formData.statusRemark"
          :placeholder="t('crm.business.statusRemarkPlaceholder')"
          :rows="3"
          maxlength="500"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
      <el-form-item v-if="requiresEndRemark" :label="t('crm.business.endRemark')" prop="endRemark">
        <el-input
          v-model="formData.endRemark"
          :placeholder="t('crm.business.endRemarkPlaceholder')"
          :rows="3"
          maxlength="500"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">{{
        t('common.confirm')
      }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import type { FormRules } from 'element-plus'
import * as BusinessApi from '@/api/crm/business'
import * as BusinessStatusApi from '@/api/crm/business/status'

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const formData = ref({
  id: undefined,
  statusId: undefined,
  endStatus: undefined,
  status: undefined,
  statusRemark: undefined as string | undefined,
  endRemark: undefined as string | undefined
})
const formRules = reactive<FormRules>({
  status: [{ required: true, message: t('crm.business.statusRequired'), trigger: 'blur' }],
  statusRemark: [
    { required: true, message: t('crm.business.statusRemarkRequired'), trigger: 'blur' },
    { max: 500, message: t('crm.business.statusRemarkLength'), trigger: 'blur' }
  ],
  endRemark: [
    { required: true, message: t('crm.business.endRemarkRequired'), trigger: 'blur' },
    { min: 10, max: 500, message: t('crm.business.endRemarkLength'), trigger: 'blur' }
  ]
})
const formRef = ref() // 表单 Ref
const statusList = ref<BusinessStatusApi.BusinessStatusVO[]>([]) // 商机状态列表
const currentStatusId = ref<number>()
const availableStatusList = computed(() => {
  const currentSort = statusList.value.find((item) => item.id === currentStatusId.value)?.sort
  return currentSort == null
    ? statusList.value
    : statusList.value.filter((item) => item.sort > currentSort)
})
const requiresStatusRemark = computed(() => Number(formData.value.status) > 0)
const requiresEndRemark = computed(
  () => formData.value.status === -2 || formData.value.status === -3
)

/** 打开弹窗 */
const open = async (business: BusinessApi.BusinessVO) => {
  dialogVisible.value = true
  resetForm()
  formData.value = {
    id: business.id,
    statusId: business.statusId,
    endStatus: business.endStatus,
    status: business.endStatus != null ? -business.endStatus : undefined,
    statusRemark: undefined,
    endRemark: business.endRemark
  }
  currentStatusId.value = business.statusId
  // 加载状态列表
  formLoading.value = true
  try {
    statusList.value = await BusinessStatusApi.getBusinessStatusSimpleList(business.statusTypeId)
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    await BusinessApi.updateBusinessStatus({
      id: formData.value.id,
      statusId: formData.value.status > 0 ? formData.value.status : undefined,
      endStatus: formData.value.status < 0 ? -formData.value.status : undefined,
      statusRemark: requiresStatusRemark.value ? formData.value.statusRemark : undefined,
      endRemark: requiresEndRemark.value ? formData.value.endRemark : undefined
    })
    message.success(t('crm.business.updateStatusSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    statusId: undefined,
    endStatus: undefined,
    status: undefined,
    statusRemark: undefined,
    endRemark: undefined
  }
  currentStatusId.value = undefined
  formRef.value?.resetFields()
}
</script>
