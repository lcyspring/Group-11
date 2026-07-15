<template>
  <Dialog v-model="visible" :title="t('createCall')" width="640px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <el-form-item v-if="bizType === BizTypeEnum.CRM_CUSTOMER" :label="t('contact')">
        <el-select v-model="formData.contactId" class="w-full" clearable filterable @change="fillContactPhone">
          <el-option v-for="contact in contacts" :key="contact.id" :label="contact.name" :value="contact.id" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('direction')" prop="direction"><el-select v-model="formData.direction" class="w-full"><el-option :label="t('callOutbound')" :value="1" /><el-option :label="t('callInbound')" :value="2" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('status')" prop="status"><el-select v-model="formData.status" class="w-full"><el-option :label="t('callConnected')" :value="10" /><el-option :label="t('callMissed')" :value="20" /><el-option :label="t('callFailed')" :value="30" /></el-select></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('phone')" prop="phone"><el-input v-model="formData.phone" maxlength="32" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('startTime')" prop="startTime"><el-date-picker v-model="formData.startTime" class="w-full" type="datetime" value-format="x" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('endTime')" :prop="formData.status === 10 ? 'endTime' : undefined"><el-date-picker v-model="formData.endTime" class="w-full" clearable type="datetime" value-format="x" /></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('recordingUrl')"><el-input v-model="formData.recordingUrl" :placeholder="t('recordingUrlHint')" /></el-form-item>
      <el-form-item :label="t('summary')"><el-input v-model="formData.summary" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
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
const formData = ref<ActivityApi.CallRecordVO>({} as ActivityApi.CallRecordVO)
const rules = computed(() => ({
  direction: [{ required: true, message: t('directionRequired'), trigger: 'change' }],
  status: [{ required: true, message: t('statusRequired'), trigger: 'change' }],
  phone: [{ required: true, message: t('phoneRequired'), trigger: 'blur' }],
  startTime: [{ required: true, message: t('startTimeRequired'), trigger: 'change' }],
  endTime: formData.value.status === 10 ? [{ required: true, message: t('endTimeRequired'), trigger: 'change' }] : []
}))
const fillContactPhone = (id?: number) => {
  const contact = contacts.value.find(item => item.id === id)
  if (contact) formData.value.phone = contact.mobile || contact.telephone || ''
}
const open = async () => {
  visible.value = true
  formData.value = { bizType: props.bizType, bizId: props.bizId, direction: 1, status: 10, phone: '', startTime: Date.now(), summary: '' }
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
    await ActivityApi.createCallRecord(formData.value)
    message.success(t('createSuccess'))
    visible.value = false
    emit('success')
  } finally { submitting.value = false }
}
defineExpose({ open })
</script>
