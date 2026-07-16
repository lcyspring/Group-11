<template>
  <Dialog v-model="visible" :title="title" width="760px">
    <el-alert :closable="false" class="mb-16px" type="warning">
      {{ t('refund.boundaryHint') }}
    </el-alert>
    <el-form ref="formRef" v-loading="loading" :model="formData" :rules="rules" label-width="130px">
      <el-form-item :label="t('refund.sourceReceivable')" prop="receivableId">
        <el-select
          v-model="formData.receivableId"
          class="w-full"
          :disabled="mode === 'update'"
          filterable
          :loading="receivableLoading"
          remote
          :remote-method="loadReceivables"
          @change="loadSummary"
        >
          <el-option
            v-for="item in receivables"
            :key="item.id"
            :label="`${item.no} · ${item.customerName || ''} · ${money(item.price)}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-card v-if="summary" class="mb-16px" shadow="never">
        <el-row :gutter="12" class="text-center">
          <el-col v-for="item in summaryItems" :key="item.label" :span="8">
            <div class="text-13px text-gray-500">{{ item.label }}</div>
            <div class="mt-4px font-600">{{ money(item.value) }}</div>
          </el-col>
        </el-row>
      </el-card>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('refund.type')" prop="type">
            <el-select v-model="formData.type" class="w-full">
              <el-option :label="t('refund.typeCustomerRefund')" :value="1" />
              <el-option :label="t('refund.typeBusinessReversal')" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('refund.amount')" prop="amount">
            <el-input-number
              v-model="formData.amount"
              class="!w-full"
              :max="Number(summary?.remainingRefundableAmount || Number.MAX_SAFE_INTEGER)"
              :min="0.01"
              :precision="2"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('refund.refundTime')" prop="refundTime">
        <el-date-picker
          v-model="formData.refundTime"
          class="!w-full"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item :label="t('refund.reason')" prop="reason">
        <el-input
          v-model="formData.reason"
          maxlength="500"
          minlength="10"
          :rows="4"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
      <el-form-item :label="t('refund.remark')" prop="remark">
        <el-input
          v-model="formData.remark"
          maxlength="1000"
          :rows="2"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :loading="loading" type="primary" @click="submit">{{
        t('common.confirm')
      }}</el-button>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import * as RefundApi from '@/api/crm/refund'
import * as ReceivableApi from '@/api/crm/receivable'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const receivableLoading = ref(false)
let receivableRequestSequence = 0
const mode = ref<'create' | 'update'>('create')
const formRef = ref()
const receivables = ref<ReceivableApi.ReceivableVO[]>([])
const summary = ref<RefundApi.RefundSourceSummaryVO>()
const formData = ref<RefundApi.ReceivableRefundVO>({} as RefundApi.ReceivableRefundVO)
const title = computed(() =>
  mode.value === 'create' ? t('refund.createDraft') : t('refund.updateDraft')
)
const required = (messageKey: string) => ({
  required: true,
  message: t(messageKey),
  trigger: ['blur', 'change']
})
const rules = reactive({
  receivableId: [required('refund.sourceRequired')],
  type: [required('refund.typeRequired')],
  refundTime: [required('refund.refundTimeRequired')],
  amount: [required('refund.amountRequired')],
  reason: [
    required('refund.reasonRequired'),
    { min: 10, max: 500, message: t('refund.reasonLength'), trigger: 'blur' }
  ]
})
const summaryItems = computed(() =>
  summary.value
    ? [
        { label: t('refund.sourceAmount'), value: summary.value.receivableAmount },
        { label: t('refund.reservedAmount'), value: summary.value.reservedRefundAmount },
        { label: t('refund.remainingAmount'), value: summary.value.remainingRefundableAmount }
      ]
    : []
)
const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6
  })
const loadSummary = async () => {
  summary.value = formData.value.receivableId
    ? await RefundApi.getSourceSummary(formData.value.receivableId, formData.value.id)
    : undefined
}
const loadReceivables = async (keyword = '') => {
  const sequence = ++receivableRequestSequence
  receivableLoading.value = true
  try {
    const pageParams = {
      pageNo: 1,
      pageSize: 100,
      auditStatus: 20,
      no: keyword.trim() || undefined
    }
    const selectedId = formData.value.receivableId
    const [ownedPage, involvedPage, selected] = await Promise.all([
      ReceivableApi.getReceivablePage({ ...pageParams, sceneType: 1 }),
      ReceivableApi.getReceivablePage({ ...pageParams, sceneType: 2 }),
      selectedId ? ReceivableApi.getReceivable(selectedId) : Promise.resolve(undefined)
    ])
    if (sequence !== receivableRequestSequence) return
    const candidates = [...(ownedPage.list || []), ...(involvedPage.list || [])]
    if (selected) candidates.unshift(selected)
    receivables.value = Array.from(new Map(candidates.map((item) => [item.id, item])).values())
  } finally {
    if (sequence === receivableRequestSequence) receivableLoading.value = false
  }
}
const open = async (type: 'create' | 'update', id?: number, receivableId?: number) => {
  visible.value = true
  mode.value = type
  loading.value = true
  summary.value = undefined
  try {
    formData.value = id
      ? await RefundApi.getRefund(id)
      : ({
          receivableId,
          type: 1,
          refundTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
          amount: 0.01,
          reason: ''
        } as RefundApi.ReceivableRefundVO)
    await Promise.all([loadReceivables(), loadSummary()])
  } finally {
    loading.value = false
  }
}
const emit = defineEmits<{ (event: 'success'): void }>()
const submit = async () => {
  if (!(await formRef.value.validate())) return
  loading.value = true
  try {
    if (mode.value === 'create') await RefundApi.createRefund(formData.value)
    else await RefundApi.updateRefund(formData.value)
    message.success(mode.value === 'create' ? t('common.createSuccess') : t('common.updateSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}
defineExpose({ open })
</script>
