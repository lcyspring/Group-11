<template>
  <Dialog v-model="visible" :title="t('invoice.void')" width="520px">
    <el-alert type="warning" :closable="false" class="mb-16px">{{
      t('invoice.voidHint')
    }}</el-alert>
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item :label="t('invoice.invoiceNo')"
        ><el-input :model-value="invoice.invoiceNo" disabled
      /></el-form-item>
      <el-form-item :label="t('invoice.voidReason')" prop="reason"
        ><el-input
          v-model="formData.reason"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button type="danger" :loading="loading" @click="submit">{{
        t('invoice.confirmVoid')
      }}</el-button
      ><el-button @click="visible = false">{{ t('common.cancel') }}</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const invoice = ref<InvoiceApi.InvoiceVO>({} as InvoiceApi.InvoiceVO)
const formData = reactive({ reason: '' })
const rules = {
  reason: [{ required: true, message: t('invoice.voidReasonRequired'), trigger: 'blur' }]
}
const open = (row: InvoiceApi.InvoiceVO) => {
  visible.value = true
  invoice.value = row
  formData.reason = ''
}
const emit = defineEmits<{ (e: 'success'): void }>()
const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    await InvoiceApi.voidInvoice(invoice.value.id!, formData.reason)
    message.success(t('invoice.voidSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}
defineExpose({ open })
</script>
