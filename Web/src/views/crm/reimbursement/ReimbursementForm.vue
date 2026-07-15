<template>
  <Dialog v-model="visible" :title="title" width="1120px">
    <el-alert :closable="false" class="mb-16px" type="info">
      {{ t('reimbursement.totalHint') }}
    </el-alert>
    <el-form ref="formRef" v-loading="loading" :model="formData" :rules="rules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('reimbursement.customer')" prop="customerId">
            <el-select
              v-model="formData.customerId"
              class="w-full"
              clearable
              filterable
              :loading="customerLoading"
              @change="customerChanged"
            >
              <el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('reimbursement.contract')" prop="contractId">
            <el-select
              v-model="formData.contractId"
              class="w-full"
              clearable
              :disabled="!formData.customerId"
              filterable
              :loading="contractLoading"
            >
              <el-option
                v-for="item in contracts"
                :key="item.id"
                :label="`${item.no} · ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('reimbursement.expenseStartDate')" prop="expenseStartDate">
            <el-date-picker v-model="formData.expenseStartDate" class="!w-full" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('reimbursement.expenseEndDate')" prop="expenseEndDate">
            <el-date-picker v-model="formData.expenseEndDate" class="!w-full" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('reimbursement.reason')" prop="reason">
        <el-input v-model="formData.reason" maxlength="500" minlength="5" show-word-limit />
      </el-form-item>
      <el-form-item :label="t('reimbursement.remark')" prop="remark">
        <el-input v-model="formData.remark" maxlength="1000" :rows="2" show-word-limit type="textarea" />
      </el-form-item>

      <div class="mb-10px flex items-center justify-between">
        <span class="font-600">{{ t('reimbursement.items') }}</span>
        <div>
          <span class="mr-16px">{{ t('reimbursement.pageTotal') }}：{{ money(pageTotal) }}</span>
          <el-button type="primary" @click="addItem">{{ t('reimbursement.addItem') }}</el-button>
        </div>
      </div>
      <el-table :data="formData.items" border>
        <el-table-column :label="t('reimbursement.category')" min-width="145">
          <template #default="{ row }">
            <el-select v-model="row.categoryId" class="w-full" filterable>
              <el-option
                v-for="category in enabledCategories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('reimbursement.occurredDate')" min-width="145">
          <template #default="{ row }">
            <el-date-picker v-model="row.occurredDate" class="!w-full" type="date" value-format="YYYY-MM-DD" />
          </template>
        </el-table-column>
        <el-table-column :label="t('reimbursement.amount')" min-width="135">
          <template #default="{ row }">
            <el-input-number v-model="row.amount" class="!w-full" :min="0.000001" :precision="6" />
          </template>
        </el-table-column>
        <el-table-column :label="t('reimbursement.description')" min-width="170">
          <template #default="{ row }"><el-input v-model="row.description" maxlength="500" /></template>
        </el-table-column>
        <el-table-column :label="t('reimbursement.invoiceNo')" min-width="130">
          <template #default="{ row }"><el-input v-model="row.invoiceNo" maxlength="100" /></template>
        </el-table-column>
        <el-table-column :label="t('reimbursement.attachments')" min-width="230">
          <template #default="{ row }">
            <el-upload v-model:file-list="row.files" :auto-upload="false" :limit="10" multiple>
              <el-button>{{ t('common.selectFile') }}</el-button>
            </el-upload>
          </template>
        </el-table-column>
        <el-table-column fixed="right" :label="t('common.action')" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeItem($index)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button :loading="loading" type="primary" @click="submit">{{ t('dialog.confirm') }}</el-button>
      <el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import type { UploadUserFile } from 'element-plus'
import * as ContractApi from '@/api/crm/contract'
import * as CustomerApi from '@/api/crm/customer'
import * as ReimbursementApi from '@/api/crm/reimbursement'
import { calculateReimbursementTotal, isOccurredDateInRange } from './constants'

interface EditableItem extends ReimbursementApi.ReimbursementItemVO {
  files: UploadUserFile[]
}
interface EditableReimbursement extends Omit<ReimbursementApi.ReimbursementVO, 'items'> {
  items: EditableItem[]
}

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const customerLoading = ref(false)
const contractLoading = ref(false)
const mode = ref<'create' | 'update'>('create')
const formRef = ref()
const customers = ref<CustomerApi.CustomerVO[]>([])
const contracts = ref<ContractApi.ContractVO[]>([])
const categories = ref<ReimbursementApi.ExpenseCategoryVO[]>([])
const formData = ref<EditableReimbursement>(emptyForm())
const title = computed(() =>
  mode.value === 'create' ? t('reimbursement.createDraft') : t('reimbursement.updateDraft')
)
const enabledCategories = computed(() =>
  categories.value.filter(
    (item): item is ReimbursementApi.ExpenseCategoryVO & { id: number } =>
      item.status === 0 && item.id !== undefined
  )
)
const pageTotal = computed(() => calculateReimbursementTotal(formData.value.items.map((item) => item.amount)))
const required = (key: string) => ({ required: true, message: t(key), trigger: ['blur', 'change'] })
const rules = reactive({
  expenseStartDate: [required('reimbursement.expenseStartRequired')],
  expenseEndDate: [required('reimbursement.expenseEndRequired')],
  reason: [
    required('reimbursement.reasonRequired'),
    { min: 5, max: 500, message: t('reimbursement.reasonLength'), trigger: 'blur' }
  ]
})

function emptyItem(): EditableItem {
  return {
    categoryId: undefined as unknown as number,
    occurredDate: dayjs().format('YYYY-MM-DD'),
    amount: 0.000001,
    description: '',
    invoiceNo: '',
    attachmentUrls: [],
    files: []
  }
}
function emptyForm(): EditableReimbursement {
  const today = dayjs().format('YYYY-MM-DD')
  return { expenseStartDate: today, expenseEndDate: today, reason: '', remark: '', items: [emptyItem()] }
}
const fileName = (url: string) => {
  const raw = url.split('/').pop()?.split('?')[0] || t('reimbursement.attachment')
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}
const toEditable = (data: ReimbursementApi.ReimbursementVO): EditableReimbursement => ({
  ...data,
  items: data.items.map((item) => ({
    ...item,
    attachmentUrls: item.attachmentUrls || [],
    files: (item.attachmentUrls || []).map((url) => ({ name: fileName(url), url }))
  }))
})
const buildPayload = (): ReimbursementApi.ReimbursementVO => ({
  id: formData.value.id,
  customerId: formData.value.customerId,
  contractId: formData.value.contractId,
  expenseStartDate: formData.value.expenseStartDate,
  expenseEndDate: formData.value.expenseEndDate,
  reason: formData.value.reason,
  remark: formData.value.remark,
  items: formData.value.items.map((item, index) => ({
    categoryId: item.categoryId,
    occurredDate: item.occurredDate,
    amount: item.amount,
    description: item.description,
    invoiceNo: item.invoiceNo || undefined,
    attachmentUrls: item.files.map((file) => file.url).filter((url): url is string => !!url),
    sort: index
  }))
})
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const addItem = () => formData.value.items.push(emptyItem())
const removeItem = (index: number) => {
  if (formData.value.items.length === 1) {
    message.warning(t('reimbursement.itemRequired'))
    return
  }
  formData.value.items.splice(index, 1)
}
const loadCustomers = async (selectedId?: number) => {
  customerLoading.value = true
  try {
    const params = { pageNo: 1, pageSize: 100 }
    const [owned, involved, selected] = await Promise.all([
      CustomerApi.getCustomerPage({ ...params, sceneType: 1 }),
      CustomerApi.getCustomerPage({ ...params, sceneType: 2 }),
      selectedId ? CustomerApi.getCustomer(selectedId) : Promise.resolve(undefined)
    ])
    const candidates = [...(owned.list || []), ...(involved.list || [])]
    if (selected) candidates.unshift(selected)
    customers.value = Array.from(new Map(candidates.map((item) => [item.id, item])).values())
  } finally {
    customerLoading.value = false
  }
}
const loadContracts = async (selectedId?: number) => {
  contracts.value = []
  if (!formData.value.customerId) return
  contractLoading.value = true
  try {
    const [page, selected] = await Promise.all([
      ContractApi.getContractPageByCustomer({
        pageNo: 1,
        pageSize: 100,
        customerId: formData.value.customerId
      }),
      selectedId ? ContractApi.getContract(selectedId) : Promise.resolve(undefined)
    ])
    const candidates = [...(page.list || [])]
    if (selected) candidates.unshift(selected)
    contracts.value = Array.from(new Map(candidates.map((item) => [item.id, item])).values())
  } finally {
    contractLoading.value = false
  }
}
const customerChanged = async () => {
  formData.value.contractId = undefined
  await loadContracts()
}
const validateItems = () => {
  if (formData.value.expenseStartDate > formData.value.expenseEndDate) {
    message.warning(t('reimbursement.dateRangeInvalid'))
    return false
  }
  for (const [index, item] of formData.value.items.entries()) {
    if (!item.categoryId || !item.occurredDate || Number(item.amount) <= 0 || !item.description.trim()) {
      message.warning(t('reimbursement.itemInvalid', { index: index + 1 }))
      return false
    }
    if (!isOccurredDateInRange(item.occurredDate, formData.value.expenseStartDate, formData.value.expenseEndDate)) {
      message.warning(t('reimbursement.itemDateInvalid', { index: index + 1 }))
      return false
    }
  }
  return true
}
const uploadPendingFiles = async (id: number) => {
  for (const item of formData.value.items) {
    for (const file of item.files.filter((candidate) => candidate.raw && !candidate.url)) {
      file.url = await ReimbursementApi.uploadAttachment(id, file.raw as File)
    }
  }
}
const open = async (type: 'create' | 'update', id?: number) => {
  visible.value = true
  mode.value = type
  loading.value = true
  formData.value = emptyForm()
  try {
    const data = id ? await ReimbursementApi.getReimbursement(id) : undefined
    if (data) formData.value = toEditable(data)
    categories.value = await ReimbursementApi.getExpenseCategoryList()
    await Promise.all([loadCustomers(data?.customerId), loadContracts(data?.contractId)])
  } finally {
    loading.value = false
  }
}
const emit = defineEmits<{ (event: 'success'): void }>()
const submit = async () => {
  if (!(await formRef.value.validate()) || !validateItems()) return
  loading.value = true
  try {
    const wasCreate = mode.value === 'create'
    let id = formData.value.id
    if (wasCreate) {
      id = await ReimbursementApi.createReimbursement(buildPayload())
      formData.value.id = id
      mode.value = 'update'
    }
    const hasPendingFiles = formData.value.items.some((item) => item.files.some((file) => file.raw && !file.url))
    if (hasPendingFiles) {
      await uploadPendingFiles(id!)
      await ReimbursementApi.updateReimbursement(buildPayload())
    } else if (!wasCreate) {
      await ReimbursementApi.updateReimbursement(buildPayload())
    }
    message.success(t('reimbursement.saveSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}
defineExpose({ open })
</script>
