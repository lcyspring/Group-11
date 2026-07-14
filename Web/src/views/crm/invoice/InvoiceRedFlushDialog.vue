<template>
  <Dialog v-model="visible" :title="t('invoice.redFlush')" width="600px">
    <el-descriptions :column="2" border class="mb-16px">
      <el-descriptions-item :label="t('invoice.invoiceNo')">{{ invoice.invoiceNo }}</el-descriptions-item>
      <el-descriptions-item :label="t('invoice.remainingRedAmount')">{{ remaining }}</el-descriptions-item>
    </el-descriptions>
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
      <el-form-item :label="t('invoice.redInvoiceNo')" prop="invoiceNo"><el-input v-model="formData.invoiceNo" maxlength="50" /></el-form-item>
      <el-form-item :label="t('invoice.redAmountForm')" prop="amount"><el-input-number v-model="formData.amount" :min="0.01" :max="remaining" :precision="2" class="!w-full" /></el-form-item>
      <el-form-item :label="t('invoice.invoiceDate')" prop="invoiceDate"><el-date-picker v-model="formData.invoiceDate" type="datetime" value-format="x" class="!w-full" /></el-form-item>
      <el-form-item :label="t('invoice.handler')" prop="handlerUserId"><el-select v-model="formData.handlerUserId" class="w-full" filterable><el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.redReason')" prop="reason"><el-input v-model="formData.reason" type="textarea" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <template #footer><el-button type="danger" :loading="loading" @click="submit">{{ t('invoice.confirmRedFlush') }}</el-button><el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button></template>
  </Dialog>
</template>
<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import * as UserApi from '@/api/system/user'
import { remainingRedAmount, toInvoiceEpochMillis } from './constants'
const { t } = useI18n('crm'); const message = useMessage()
const visible = ref(false); const loading = ref(false); const formRef = ref(); const users = ref<UserApi.UserVO[]>([])
const invoice = ref<InvoiceApi.InvoiceVO>({} as InvoiceApi.InvoiceVO)
const remaining = computed(() => remainingRedAmount(invoice.value.amount, invoice.value.redAmount))
type InvoiceRedFlushForm = Omit<InvoiceApi.InvoiceRedFlushReqVO, 'invoiceDate'> & { invoiceDate: string | number }
const formData = ref<InvoiceRedFlushForm>({} as InvoiceRedFlushForm)
const rules = { invoiceNo: [{ required: true, message: t('invoice.invoiceNoRequired'), trigger: 'blur' }], amount: [{ required: true, message: t('invoice.amountRequired'), trigger: 'blur' }], invoiceDate: [{ required: true, message: t('invoice.invoiceDateRequired'), trigger: 'change' }], handlerUserId: [{ required: true, message: t('invoice.handlerRequired'), trigger: 'change' }], reason: [{ required: true, message: t('invoice.redReasonRequired'), trigger: 'blur' }] }
const open = async (row: InvoiceApi.InvoiceVO) => { visible.value = true; invoice.value = row; users.value = await UserApi.getSimpleUserList(); formData.value = { originalInvoiceId: row.id!, amount: remaining.value, invoiceNo: '', invoiceDate: Date.now(), handlerUserId: row.handlerUserId, reason: '' } }
const emit = defineEmits<{ (e: 'success'): void }>()
const submit = async () => { if (!(await formRef.value.validate())) return; loading.value = true; try { await InvoiceApi.redFlushInvoice({ ...formData.value, invoiceDate: toInvoiceEpochMillis(formData.value.invoiceDate) }); message.success(t('invoice.redFlushSuccess')); visible.value = false; emit('success') } finally { loading.value = false } }
defineExpose({ open })
</script>
