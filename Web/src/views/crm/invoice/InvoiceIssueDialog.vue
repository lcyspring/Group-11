<template>
  <Dialog v-model="visible" :title="t('invoice.issue')" width="560px">
    <el-alert type="warning" :closable="false" class="mb-16px">{{ t('invoice.issueImmutableHint') }}</el-alert>
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
      <el-form-item :label="t('invoice.invoiceNo')" prop="invoiceNo"><el-input v-model="formData.invoiceNo" maxlength="50" /></el-form-item>
      <el-form-item :label="t('invoice.invoiceDate')" prop="invoiceDate"><el-date-picker v-model="formData.invoiceDate" type="datetime" value-format="x" class="!w-full" /></el-form-item>
      <el-form-item :label="t('invoice.handler')" prop="handlerUserId"><el-select v-model="formData.handlerUserId" class="w-full" filterable><el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.issueRemark')"><el-input v-model="formData.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="loading" @click="submit">{{ t('invoice.confirmIssue') }}</el-button><el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button></template>
  </Dialog>
</template>
<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import * as UserApi from '@/api/system/user'
import { toInvoiceEpochMillis } from './constants'
const { t } = useI18n('crm'); const message = useMessage()
const visible = ref(false); const loading = ref(false); const formRef = ref(); const users = ref<UserApi.UserVO[]>([])
type InvoiceIssueForm = Omit<InvoiceApi.InvoiceIssueReqVO, 'invoiceDate'> & { invoiceDate: string | number }
const formData = ref<InvoiceIssueForm>({} as InvoiceIssueForm)
const rules = { invoiceNo: [{ required: true, message: t('invoice.invoiceNoRequired'), trigger: 'blur' }], invoiceDate: [{ required: true, message: t('invoice.invoiceDateRequired'), trigger: 'change' }], handlerUserId: [{ required: true, message: t('invoice.handlerRequired'), trigger: 'change' }] }
const open = async (row: InvoiceApi.InvoiceVO) => { visible.value = true; users.value = await UserApi.getSimpleUserList(); formData.value = { id: row.id!, invoiceNo: '', invoiceDate: Date.now(), handlerUserId: row.handlerUserId, remark: '' } }
const emit = defineEmits<{ (e: 'success'): void }>()
const submit = async () => { if (!(await formRef.value.validate())) return; loading.value = true; try { await InvoiceApi.issueInvoice({ ...formData.value, invoiceDate: toInvoiceEpochMillis(formData.value.invoiceDate) }); message.success(t('invoice.issueSuccess')); visible.value = false; emit('success') } finally { loading.value = false } }
defineExpose({ open })
</script>
