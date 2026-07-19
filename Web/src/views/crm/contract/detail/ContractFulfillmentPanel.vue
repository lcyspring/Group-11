<template>
  <ContentWrap v-loading="loading">
    <div class="mb-12px flex flex-wrap items-center gap-8px">
      <el-button
        v-hasPermi="['crm:erp-fulfillment:create']"
        type="primary"
        :disabled="!canCreateOrRetryFulfillment(data?.eligible || false, data?.record?.status)"
        :loading="submitting"
        @click="createOrRetry"
      >
        {{ data?.record?.status === FULFILLMENT_STATUS.FAILED ? t('crm.erpFulfillment.retry') : t('crm.erpFulfillment.create') }}
      </el-button>
      <el-button
        v-hasPermi="['crm:erp-fulfillment:refresh']"
        :disabled="!canRefreshFulfillment(data?.record?.status, data?.record?.erpOrderId)"
        :loading="refreshing"
        @click="refresh"
      >
        {{ t('crm.erpFulfillment.refresh') }}
      </el-button>
      <el-tag type="info">{{ t('crm.erpFulfillment.policyVersion') }} {{ data?.policyVersion || '-' }}</el-tag>
      <el-tag type="info">{{ data?.currencyMode || '-' }} · {{ data?.erpCurrencyCode || '-' }}</el-tag>
    </div>

    <el-alert
      v-if="data?.sourceInvalidated"
      :title="t('crm.erpFulfillment.sourceInvalidated')"
      type="warning"
      :closable="false"
      class="mb-12px"
    />
    <el-alert
      v-if="data?.blockers?.length"
      :title="t('crm.erpFulfillment.notReady')"
      type="warning"
      :closable="false"
      class="mb-12px"
    >
      <div class="flex flex-wrap gap-6px">
        <el-tag v-for="blocker in data.blockers" :key="blocker" type="warning">
          {{ blockerLabel(blocker) }}
        </el-tag>
      </div>
    </el-alert>

    <el-descriptions :title="t('crm.erpFulfillment.mappingReadiness')" :column="3" border class="mb-16px">
      <el-descriptions-item :label="t('crm.erpFulfillment.crmCustomer')">
        {{ data?.crmCustomerName || data?.crmCustomerId || '-' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.erpFulfillment.erpCustomer')">
        {{ data?.erpCustomerName || data?.erpCustomerId || t('crm.erpFulfillment.unmapped') }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.erpFulfillment.contractVersion')">
        V{{ data?.contractVersion || '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-table :data="data?.productMappings || []" border class="mb-16px">
      <el-table-column :label="t('crm.erpFulfillment.crmProduct')" min-width="200">
        <template #default="{ row }">{{ row.crmProductNo }} · {{ row.crmProductName }}</template>
      </el-table-column>
      <el-table-column :label="t('crm.erpFulfillment.erpProduct')" min-width="200">
        <template #default="{ row }">
          {{ row.erpProductName || row.erpProductId || t('crm.erpFulfillment.unmapped') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('crm.erpFulfillment.mappingStatus')" width="120">
        <template #default="{ row }">
          <el-tag :type="row.mapped ? 'success' : 'danger'">
            {{ row.mapped ? t('crm.erpFulfillment.mapped') : t('crm.erpFulfillment.unmapped') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <template v-if="data?.record">
      <el-descriptions :title="t('crm.erpFulfillment.fulfillmentRecord')" :column="3" border class="mb-16px">
        <el-descriptions-item :label="t('crm.erpFulfillment.bridgeStatus')">
          <el-tag :type="fulfillmentStatusType(data.record.status)">{{ statusLabel(data.record.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.erpOrderNo')">
          {{ data.record.erpOrderNo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.erpAuditStatus')">
          {{ erpAuditLabel(data.record.erpOrderStatus) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.requestId')">
          {{ data.record.requestId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.attemptCount')">
          {{ data.record.attemptCount }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.lastSyncTime')">
          {{ data.record.lastSyncTime ? formatDate(new Date(data.record.lastSyncTime)) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.currencyAndRate')">
          {{ data.record.sourceCurrencyCode }} → {{ data.record.erpCurrencyCode }} × {{ data.record.exchangeRate }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.sourceAmount')">
          {{ data.record.sourceGrossAmount }} {{ data.record.sourceCurrencyCode }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('crm.erpFulfillment.erpAmount')">
          {{ data.record.erpTotalAmount ?? '-' }} {{ data.record.erpCurrencyCode }}
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="data.record.lastErrorMessage"
        :title="`${data.record.lastErrorCode || ''} ${data.record.lastErrorMessage}`"
        type="error"
        :closable="false"
        class="mb-16px"
      />
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div class="mb-6px">{{ t('crm.erpFulfillment.outProgress') }}</div>
          <el-progress :percentage="progressPercent(data.record.outCount, data.record.totalCount)" />
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="mb-6px">{{ t('crm.erpFulfillment.returnProgress') }}</div>
          <el-progress status="warning" :percentage="progressPercent(data.record.returnCount, data.record.totalCount)" />
        </el-col>
      </el-row>
    </template>
  </ContentWrap>
</template>

<script setup lang="ts">
import * as FulfillmentApi from '@/api/crm/fulfillment'
import { formatDate } from '@/utils/formatTime'
import {
  ERP_AUDIT_STATUS,
  FULFILLMENT_STATUS,
  canCreateOrRetryFulfillment,
  canRefreshFulfillment,
  fulfillmentStatusType,
  progressPercent
} from '@/views/crm/erpFulfillment/constants'

const props = defineProps<{ contractId: number }>()
const { t } = useI18n()
const message = useMessage()
const loading = ref(false)
const submitting = ref(false)
const refreshing = ref(false)
const data = ref<FulfillmentApi.ContractFulfillmentVO>()
const load = async () => {
  if (!props.contractId) return
  loading.value = true
  try {
    data.value = await FulfillmentApi.getContractFulfillment(props.contractId)
  } finally {
    loading.value = false
  }
}
const createOrRetry = async () => {
  await message.confirm(t('crm.erpFulfillment.createConfirm'))
  submitting.value = true
  try {
    data.value = await FulfillmentApi.createOrRetryContractFulfillment(props.contractId)
    message.success(t('crm.erpFulfillment.createSuccess'))
  } finally {
    submitting.value = false
  }
}
const refresh = async () => {
  refreshing.value = true
  try {
    data.value = await FulfillmentApi.refreshContractFulfillment(props.contractId)
    message.success(t('crm.erpFulfillment.refreshSuccess'))
  } finally {
    refreshing.value = false
  }
}
const statusLabel = (status?: number) =>
  status === FULFILLMENT_STATUS.CREATED
    ? t('crm.erpFulfillment.created')
    : status === FULFILLMENT_STATUS.FAILED
      ? t('crm.erpFulfillment.failed')
      : t('crm.erpFulfillment.creating')
const erpAuditLabel = (status?: number) =>
  status === ERP_AUDIT_STATUS.APPROVED
    ? t('crm.erpFulfillment.erpApproved')
    : status === ERP_AUDIT_STATUS.PROCESSING
      ? t('crm.erpFulfillment.erpProcessing')
      : '-'
const blockerLabel = (code: string) => {
  const key = `crm.erpFulfillment.blockers.${code}`
  const translated = t(key)
  return translated === key ? code : translated
}
watch(() => props.contractId, load, { immediate: true })
</script>
