<template>
  <Dialog
    v-model="visible"
    :title="t('receivable.writeOffLedgerTitle', { no: receivable?.no || '-' })"
    width="1040px"
  >
    <div v-loading="loading">
      <el-descriptions :column="3" border class="mb-20px">
        <el-descriptions-item :label="t('receivable.receivableAmount')">
          {{ money(receivable?.price) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('receivable.writtenOffAmount')">
          {{ money(writtenOffAmount) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('receivable.writeOffRemaining')">
          <span :class="remainingAmount > 0 ? 'text-primary' : 'text-success'">
            {{ money(remainingAmount) }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="remainingAmount <= 0"
        :closable="false"
        :title="t('receivable.writeOffCompleted')"
        type="success"
        show-icon
        class="mb-20px"
      />

      <el-form
        v-else
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
        class="mb-20px"
      >
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="t('receivable.writeOffAmount')" prop="amount">
              <el-input-number
                v-model="formData.amount"
                :max="remainingAmount"
                :min="0.01"
                :precision="6"
                controls-position="right"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('receivable.writeOffTime')" prop="writeOffTime">
              <el-date-picker
                v-model="formData.writeOffTime"
                type="datetime"
                value-format="x"
                :disabled-date="disableFutureDate"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('receivable.writeOffSource')" prop="sourceType">
              <el-select v-model="formData.sourceType" class="!w-full">
                <el-option :label="t('receivable.writeOffManual')" :value="1" />
                <el-option :label="t('receivable.writeOffBank')" :value="2" />
                <el-option :label="t('receivable.writeOffImport')" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('receivable.writeOffReference')" prop="referenceNo">
              <el-input
                v-model="formData.referenceNo"
                clearable
                maxlength="128"
                :placeholder="t('receivable.writeOffReferencePlaceholder')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('receivable.remark')" prop="remark">
              <el-input v-model="formData.remark" clearable maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="flex justify-end gap-8px">
          <el-button @click="fillRemainingAmount">
            {{ t('receivable.writeOffAllRemaining') }}
          </el-button>
          <el-button type="primary" :loading="saving" @click="saveWriteOff">
            {{ t('receivable.confirmWriteOff') }}
          </el-button>
        </div>
      </el-form>

      <div class="mb-10px font-600">{{ t('receivable.writeOffLedger') }}</div>
      <el-table :data="records" border stripe max-height="360">
        <el-table-column :label="t('receivable.writeOffTime')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.writeOffTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('receivable.writeOffAmount')" min-width="130">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="t('receivable.writeOffSource')" min-width="120">
          <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column
          :label="t('receivable.writeOffReference')"
          prop="referenceNo"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.referenceNo || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? t('receivable.writeOffActive') : t('receivable.writeOffReversed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('receivable.remark')" prop="remark" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('common.action')" fixed="right" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['crm:receivable:write-off']"
              link
              type="danger"
              :loading="reversingId === row.id"
              @click="reverseWriteOff(row)"
            >
              {{ t('receivable.reverseWriteOff') }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty>{{ t('receivable.noWriteOffRecords') }}</template>
      </el-table>
    </div>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import * as ReceivableApi from '@/api/crm/receivable'
import { resolveDialogAction } from '@/utils/dialogAction'
import { formatDate } from '@/utils/formatTime'

const emit = defineEmits<{ success: [] }>()
const message = useMessage()
const { t } = useI18n('crm')
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const reversingId = ref<number>()
const formRef = ref<FormInstance>()
const receivable = ref<ReceivableApi.ReceivableVO>()
const records = ref<ReceivableApi.ReceivableWriteOffVO[]>([])
const formData = reactive<ReceivableApi.ReceivableWriteOffCreateReqVO>({
  receivableId: 0,
  amount: 0.01,
  writeOffTime: Date.now(),
  sourceType: 1,
  referenceNo: '',
  remark: ''
})

const writtenOffAmount = computed(() =>
  records.value
    .filter((item) => item.status === 0)
    .reduce((sum, item) => sum + Number(item.amount), 0)
)
const remainingAmount = computed(() =>
  Math.max(0, Number(receivable.value?.price || 0) - writtenOffAmount.value)
)
const validateAmount = (_rule: unknown, value: number, callback: (error?: Error) => void) => {
  if (!Number.isFinite(value) || value < 0.01) {
    callback(new Error(t('receivable.writeOffAmountRequired')))
  } else if (value > remainingAmount.value) {
    callback(new Error(t('receivable.writeOffAmountExceeded', { amount: money(remainingAmount.value) })))
  } else {
    callback()
  }
}
const formRules: FormRules = {
  amount: [{ required: true, validator: validateAmount, trigger: 'change' }],
  writeOffTime: [{ required: true, message: t('receivable.writeOffTimeRequired'), trigger: 'change' }],
  sourceType: [{ required: true, message: t('receivable.writeOffSourceRequired'), trigger: 'change' }]
}

const money = (value?: number) =>
  Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const sourceLabel = (value: number) =>
  ({
    1: t('receivable.writeOffManual'),
    2: t('receivable.writeOffBank'),
    3: t('receivable.writeOffImport')
  } as Record<number, string>)[value] || String(value)
const disableFutureDate = (date: Date) => date.getTime() > Date.now()
const resetForm = () => {
  Object.assign(formData, {
    receivableId: receivable.value?.id || 0,
    amount: Math.min(remainingAmount.value, 0.01),
    writeOffTime: Date.now(),
    sourceType: 1,
    referenceNo: '',
    remark: ''
  })
  formRef.value?.clearValidate()
}
const refreshRecords = async () => {
  if (!receivable.value) return
  records.value = await ReceivableApi.getReceivableWriteOffList(receivable.value.id)
}
const fillRemainingAmount = () => {
  formData.amount = remainingAmount.value
  formRef.value?.validateField('amount')
}
const saveWriteOff = async () => {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    await ReceivableApi.createReceivableWriteOff({
      ...formData,
      referenceNo: formData.referenceNo?.trim() || undefined,
      remark: formData.remark?.trim() || undefined
    })
    await refreshRecords()
    resetForm()
    emit('success')
    message.success(t('receivable.writeOffSuccess'))
  } finally {
    saving.value = false
  }
}
const reverseWriteOff = async (record: ReceivableApi.ReceivableWriteOffVO) => {
  if (!receivable.value) return
  if (!(await resolveDialogAction(message.confirm(t('receivable.reverseWriteOffConfirm'))))) return
  reversingId.value = record.id
  try {
    await ReceivableApi.reverseReceivableWriteOff(receivable.value.id, record.id)
    await refreshRecords()
    resetForm()
    emit('success')
    message.success(t('receivable.reverseWriteOffSuccess'))
  } finally {
    reversingId.value = undefined
  }
}
const open = async (row: ReceivableApi.ReceivableVO) => {
  receivable.value = row
  visible.value = true
  loading.value = true
  try {
    await refreshRecords()
    resetForm()
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
