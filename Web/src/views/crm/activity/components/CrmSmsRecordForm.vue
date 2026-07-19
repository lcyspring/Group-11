<template>
  <Dialog v-model="visible" :title="t('createSms')" width="640px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <el-form-item v-if="bizType === BizTypeEnum.CRM_CUSTOMER" :label="t('contact')">
        <el-select v-model="formData.contactId" class="w-full" clearable filterable @change="fillContactMobile"><el-option v-for="contact in contacts" :key="contact.id" :label="contact.name" :value="contact.id" /></el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('direction')" prop="direction"><el-select v-model="formData.direction" class="w-full" @change="syncStatus"><el-option :label="t('smsOutbound')" :value="1" /><el-option :label="t('smsInbound')" :value="2" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('status')" prop="status"><el-select v-model="formData.status" class="w-full"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('mobile')" prop="mobile"><el-input v-model="formData.mobile" maxlength="32" /></el-form-item>
      <el-form-item :label="t('occurredTime')" prop="occurredTime"><el-date-picker v-model="formData.occurredTime" class="w-full" type="datetime" value-format="x" /></el-form-item>
      <el-form-item :label="t('content')" prop="content"><el-input v-model="formData.content" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item v-if="formData.status === 30" :label="t('failureReason')" prop="failureReason"><el-input v-model="formData.failureReason" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
      <el-form-item :label="t('externalMessageId')"><el-input v-model="formData.externalMessageId" maxlength="128" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="submitting" @click="submit">{{ t('common.ok') }}</el-button><el-button @click="visible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import * as ActivityApi from '@/api/crm/activity'
import * as ContactApi from '@/api/crm/contact'
import { BizTypeEnum } from '@/api/crm/permission'

const props = defineProps<{ bizType: number; bizId: number }>()
const emit = defineEmits<{ (e: 'success'): void }>()
const { t } = useI18n('crm.activity')
const message = useMessage()
const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const contacts = ref<ContactApi.ContactVO[]>([])
const formData = ref<ActivityApi.SmsRecordVO>({} as ActivityApi.SmsRecordVO)
const statusOptions = computed(() => formData.value.direction === 2
  ? [{ value: 40, label: t('smsReceived') }]
  : [{ value: 0, label: t('smsPending') }, { value: 10, label: t('smsSent') }, { value: 20, label: t('smsDelivered') }, { value: 30, label: t('smsFailed') }])
const rules = computed(() => ({
  direction: [{ required: true, message: t('directionRequired'), trigger: 'change' }],
  status: [{ required: true, message: t('statusRequired'), trigger: 'change' }],
  mobile: [{ required: true, message: t('mobileRequired'), trigger: 'blur' }],
  occurredTime: [{ required: true, message: t('occurredTimeRequired'), trigger: 'change' }],
  content: [{ required: true, message: t('contentRequired'), trigger: 'blur' }],
  failureReason: formData.value.status === 30 ? [{ required: true, message: t('failureReasonRequired'), trigger: 'blur' }] : []
}))
const syncStatus = () => { formData.value.status = formData.value.direction === 2 ? 40 : 10 }
const fillContactMobile = (id?: number) => {
  const contact = contacts.value.find(item => item.id === id)
  if (contact) formData.value.mobile = contact.mobile || contact.telephone || ''
}
const open = async () => {
  visible.value = true
  formData.value = { bizType: props.bizType, bizId: props.bizId, direction: 1, status: 10, mobile: '', content: '', occurredTime: Date.now() }
  if (props.bizType === BizTypeEnum.CRM_CUSTOMER && !contacts.value.length) {
    const page = await ContactApi.getContactPageByCustomer({ pageNo: 1, pageSize: 100, customerId: props.bizId })
    contacts.value = page.list
  }
  formRef.value?.resetFields()
}
const submit = async () => {
  if (!(await formRef.value?.validate())) return
  submitting.value = true
  try {
    await ActivityApi.createSmsRecord(formData.value)
    message.success(t('createSuccess'))
    visible.value = false
    emit('success')
  } finally { submitting.value = false }
}
defineExpose({ open })
</script>
