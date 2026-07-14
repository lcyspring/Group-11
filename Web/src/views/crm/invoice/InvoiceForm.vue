<template>
  <Dialog v-model="visible" :title="title" width="840px">
    <el-alert type="info" :closable="false" class="mb-16px">
      {{ t('invoice.snapshotHint') }}
    </el-alert>
    <el-form ref="formRef" v-loading="loading" :model="formData" :rules="rules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('invoice.contract')" prop="contractId">
            <el-select v-model="formData.contractId" class="w-full" filterable :disabled="mode === 'update'" @change="loadSummary">
              <el-option v-for="item in contracts" :key="item.id" :label="`${item.no} · ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('invoice.handler')" prop="handlerUserId">
            <el-select v-model="formData.handlerUserId" class="w-full" filterable>
              <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('invoice.type')" prop="type">
            <el-select v-model="formData.type" class="w-full">
              <el-option :label="t('invoice.typeOrdinary')" :value="INVOICE_TYPE.VAT_ORDINARY" />
              <el-option :label="t('invoice.typeSpecial')" :value="INVOICE_TYPE.VAT_SPECIAL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('invoice.amount')" prop="amount">
            <el-input-number v-model="formData.amount" :min="0.01" :precision="2" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-card v-if="summary" shadow="never" class="mb-16px">
        <el-row :gutter="12" class="text-center">
          <el-col v-for="item in summaryItems" :key="item.label" :span="Math.floor(24 / summaryItems.length)">
            <div class="text-13px text-gray-500">{{ item.label }}</div>
            <div class="mt-4px font-600">{{ formatAmount(item.value) }}</div>
          </el-col>
        </el-row>
      </el-card>
      <el-form-item :label="t('invoice.title')" prop="title"><el-input v-model="formData.title" maxlength="200" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item :label="t('invoice.taxNo')" prop="taxNo"><el-input v-model="formData.taxNo" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('invoice.email')" prop="email"><el-input v-model="formData.email" /></el-form-item></el-col>
      </el-row>
      <template v-if="formData.type === INVOICE_TYPE.VAT_SPECIAL">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item :label="t('invoice.registeredAddress')" prop="registeredAddress"><el-input v-model="formData.registeredAddress" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('invoice.registeredPhone')" prop="registeredPhone"><el-input v-model="formData.registeredPhone" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item :label="t('invoice.bankName')" prop="bankName"><el-input v-model="formData.bankName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('invoice.bankAccount')" prop="bankAccount"><el-input v-model="formData.bankAccount" /></el-form-item></el-col>
        </el-row>
      </template>
      <el-form-item :label="t('invoice.content')" prop="content"><el-input v-model="formData.content" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      <el-form-item :label="t('invoice.remark')" prop="remark"><el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="submit">{{ t('dialog.confirm') }}</el-button>
      <el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import * as ContractApi from '@/api/crm/contract'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { INVOICE_TYPE } from './constants'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const mode = ref<'create' | 'update'>('create')
const formRef = ref()
const users = ref<UserApi.UserVO[]>([])
const contracts = ref<ContractApi.ContractVO[]>([])
const summary = ref<InvoiceApi.InvoiceSummaryVO>()
const formData = ref<InvoiceApi.InvoiceVO>({} as InvoiceApi.InvoiceVO)
const title = computed(() => mode.value === 'create' ? t('invoice.createDraft') : t('invoice.updateDraft'))
const required = (key: string) => ({ required: true, message: t(key), trigger: ['blur', 'change'] })
const specialRequired = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (formData.value.type === INVOICE_TYPE.VAT_SPECIAL && !value?.trim()) callback(new Error(t('invoice.specialInfoRequired')))
  else callback()
}
const rules = reactive({
  contractId: [required('invoice.contractRequired')], handlerUserId: [required('invoice.handlerRequired')],
  type: [required('invoice.typeRequired')], amount: [required('invoice.amountRequired')],
  title: [required('invoice.titleRequired')], content: [required('invoice.contentRequired')],
  taxNo: [{ validator: specialRequired, trigger: 'blur' }], registeredAddress: [{ validator: specialRequired, trigger: 'blur' }],
  registeredPhone: [{ validator: specialRequired, trigger: 'blur' }], bankName: [{ validator: specialRequired, trigger: 'blur' }],
  bankAccount: [{ validator: specialRequired, trigger: 'blur' }],
  email: [{ type: 'email', message: t('invoice.emailInvalid'), trigger: 'blur' }]
})
const summaryItems = computed(() => summary.value ? [
  { label: t('invoice.contractAmount'), value: summary.value.contractAmount },
  { label: t('invoice.blueAmount'), value: summary.value.blueAmount },
  { label: t('invoice.redAmount'), value: summary.value.redAmount },
  { label: t('invoice.netAmount'), value: summary.value.netAmount },
  { label: t('invoice.availableAmount'), value: summary.value.availableAmount }
] : [])
const formatAmount = (value: number) => Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const draftFields = () => ({
  handlerUserId: formData.value.handlerUserId, type: formData.value.type, amount: formData.value.amount,
  title: formData.value.title, taxNo: formData.value.taxNo, registeredAddress: formData.value.registeredAddress,
  registeredPhone: formData.value.registeredPhone, bankName: formData.value.bankName,
  bankAccount: formData.value.bankAccount, email: formData.value.email,
  content: formData.value.content, remark: formData.value.remark
})
const loadSummary = async () => {
  summary.value = formData.value.contractId ? await InvoiceApi.getContractSummary(formData.value.contractId) : undefined
}
const open = async (type: 'create' | 'update', id?: number) => {
  visible.value = true; mode.value = type; summary.value = undefined
  loading.value = true
  try {
    const [contractPage, userList] = await Promise.all([
      ContractApi.getContractPage({ pageNo: 1, pageSize: 100, auditStatus: 20 }), UserApi.getSimpleUserList()
    ])
    contracts.value = contractPage.list || []; users.value = userList
    if (id) formData.value = await InvoiceApi.getInvoice(id)
    else formData.value = { contractId: undefined, handlerUserId: useUserStore().getUser.id,
      type: INVOICE_TYPE.VAT_ORDINARY, amount: 0.01, title: '', content: '' } as unknown as InvoiceApi.InvoiceVO
    await loadSummary()
  } finally { loading.value = false }
}
const emit = defineEmits<{ (e: 'success'): void }>()
const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    if (mode.value === 'create') await InvoiceApi.createInvoice({ contractId: formData.value.contractId, ...draftFields() })
    else await InvoiceApi.updateInvoice({ id: formData.value.id!, ...draftFields() })
    message.success(mode.value === 'create' ? t('common.createSuccess') : t('common.updateSuccess'))
    visible.value = false; emit('success')
  } finally { loading.value = false }
}
defineExpose({ open })
</script>
