<template>
  <ContentWrap v-loading="loading">
    <div v-if="selectedQuote" class="flex flex-col gap-16px">
      <div class="flex flex-wrap items-center justify-between gap-12px">
        <div class="flex items-center gap-10px">
          <span class="text-lg font-600">{{ t('crm.business.quoteVersion') }} V{{ selectedQuote.versionNo }}</span>
          <el-tag :type="quoteStatusType(selectedQuote.status)">{{ quoteStatusLabel(selectedQuote.status) }}</el-tag>
          <span class="text-sm text-gray-500">{{ selectedQuote.currencyCode }} → {{ selectedQuote.baseCurrencyCode }} × {{ selectedQuote.exchangeRateToBase }}</span>
        </div>
        <div class="flex items-center gap-8px">
          <el-select v-model="selectedQuoteId" class="w-180px" @change="selectVersion">
            <el-option
              v-for="quote in versions"
              :key="quote.id"
              :label="`V${quote.versionNo} · ${quoteStatusLabel(quote.status)}`"
              :value="quote.id"
            />
          </el-select>
          <el-button
            v-if="isCurrentSelected && selectedQuote.status === QuoteStatus.DRAFT && !businessEnded"
            v-hasPermi="['crm:business:quote:lock']"
            type="primary"
            @click="lockCurrent"
          >
            {{ t('crm.business.lockQuote') }}
          </el-button>
          <el-button
            v-if="isCurrentSelected && selectedQuote.status === QuoteStatus.LOCKED && !businessEnded"
            v-hasPermi="['crm:business:quote:reopen']"
            @click="reopenCurrent"
          >
            {{ t('crm.business.reopenQuote') }}
          </el-button>
        </div>
      </div>

      <el-descriptions :column="4" border>
        <el-descriptions-item :label="t('crm.business.quoteSubtotal')">{{ money(selectedQuote.subtotal) }} {{ selectedQuote.currencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteDiscount')">{{ money(selectedQuote.discountAmount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteTax')">{{ money(selectedQuote.taxAmount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteGross')">{{ money(selectedQuote.grossAmount) }} {{ selectedQuote.currencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteBaseGross')">{{ money(selectedQuote.baseGrossAmount) }} {{ selectedQuote.baseCurrencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.discountPercent')">{{ selectedQuote.discountPercent }}%</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteLockedAt')">{{ selectedQuote.lockedAt ? formatDate(selectedQuote.lockedAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('crm.business.quoteSource')">{{ selectedQuote.sourceQuoteId ? `#${selectedQuote.sourceQuoteId}` : '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="selectedQuote.items" stripe show-overflow-tooltip>
        <el-table-column type="index" width="56" align="center" />
        <el-table-column :label="t('crm.business.product')" prop="productNameSnapshot" min-width="150" />
        <el-table-column :label="t('crm.business.productNo')" prop="productNoSnapshot" min-width="120" />
        <el-table-column :label="t('crm.business.productVersion')" prop="productVersionSnapshot" width="100" align="center" />
        <el-table-column :label="t('crm.business.productPrice')" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.listPrice) }}</template>
        </el-table-column>
        <el-table-column :label="t('crm.business.businessPrice')" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.businessPrice) }}</template>
        </el-table-column>
        <el-table-column :label="t('crm.business.count')" prop="count" width="100" align="right" />
        <el-table-column :label="t('crm.business.taxRate')" min-width="90" align="right">
          <template #default="{ row }">{{ row.taxRatePercent }}%</template>
        </el-table-column>
        <el-table-column :label="t('crm.business.quoteNet')" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.netAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('crm.business.quoteTax')" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.taxAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('crm.business.quoteGross')" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.grossAmount) }}</template>
        </el-table-column>
      </el-table>

      <el-timeline v-if="selectedQuote.actions?.length">
        <el-timeline-item v-for="action in selectedQuote.actions" :key="action.id" :timestamp="formatDate(action.createTime)">
          {{ quoteActionLabel(action.actionType) }} · {{ action.remark }}
        </el-timeline-item>
      </el-timeline>
    </div>
    <el-empty v-else :description="t('crm.business.noQuote')" />
  </ContentWrap>
</template>

<script setup lang="ts">
import * as QuoteApi from '@/api/crm/business/quote'
import { formatDate } from '@/utils/formatTime'
import { erpPriceInputFormatter } from '@/utils'

const props = defineProps<{ businessId: number; businessEnded: boolean }>()
const emit = defineEmits<{ (e: 'changed', quote: QuoteApi.QuoteVO): void }>()
const { t } = useI18n()
const message = useMessage()
const loading = ref(false)
const versions = ref<QuoteApi.QuoteVO[]>([])
const selectedQuoteId = ref<number>()
const selectedQuote = ref<QuoteApi.QuoteVO>()

enum QuoteStatus { DRAFT = 0, LOCKED = 10, SUPERSEDED = 20, TERMINATED = 30 }

const isCurrentSelected = computed(() => versions.value[0]?.id === selectedQuoteId.value)
const money = (value?: number) => erpPriceInputFormatter(value ?? 0)
const quoteStatusLabel = (status: number) => t(`crm.business.quoteStatus${status}`)
const quoteStatusType = (status: number) => status === QuoteStatus.LOCKED
  ? 'success'
  : status === QuoteStatus.SUPERSEDED
    ? 'info'
    : status === QuoteStatus.TERMINATED
      ? 'danger'
      : 'warning'
const quoteActionLabel = (action: number) => t(`crm.business.quoteAction${action}`)

const selectVersion = (id: number) => {
  selectedQuote.value = versions.value.find((quote) => quote.id === id)
}

const load = async () => {
  if (!props.businessId) return
  loading.value = true
  try {
    versions.value = await QuoteApi.getQuoteVersions(props.businessId)
    selectedQuoteId.value = versions.value[0]?.id
    selectedQuote.value = versions.value[0]
    if (selectedQuote.value) emit('changed', selectedQuote.value)
  } finally {
    loading.value = false
  }
}

const promptReason = async (title: string, placeholder: string) => {
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    inputType: 'textarea',
    inputValidator: (input) => !!input?.trim() || t('crm.business.quoteReasonRequired')
  })
  return value.trim()
}

const lockCurrent = async () => {
  const reason = await promptReason(t('crm.business.lockQuote'), t('crm.business.lockQuoteReason'))
  const quote = await QuoteApi.lockQuote(props.businessId, reason)
  message.success(t('crm.business.lockQuoteSuccess'))
  emit('changed', quote)
  await load()
}

const reopenCurrent = async () => {
  const reason = await promptReason(t('crm.business.reopenQuote'), t('crm.business.reopenQuoteReason'))
  const quote = await QuoteApi.reopenQuote(props.businessId, reason)
  message.success(t('crm.business.reopenQuoteSuccess'))
  emit('changed', quote)
  await load()
}

watch(() => props.businessId, load, { immediate: true })
defineExpose({ load })
</script>
