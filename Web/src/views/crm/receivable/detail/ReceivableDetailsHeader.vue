<template>
  <el-alert
    v-if="hasReferenceIssue(receivable.referenceStatus)"
    :title="t(referenceStatusLocaleKey(receivable.referenceStatus))"
    :description="
      t('receivable.referenceArchiveNotice', {
        customerId: receivable.customerId,
        contractId: receivable.contractId
      })
    "
    type="warning"
    :closable="false"
    show-icon
    class="mb-10px"
  />
  <div>
    <div class="flex items-start justify-between">
      <div>
        <el-col>
          <el-row>
            <span class="text-xl font-bold">{{ receivable.no }}</span>
          </el-row>
        </el-col>
      </div>
      <div>
        <!-- 右上：按钮 -->
        <slot></slot>
      </div>
    </div>
  </div>
  <ContentWrap class="mt-10px">
    <el-descriptions :column="5" direction="vertical">
      <el-descriptions-item :label="t('receivable.customerName')">
        {{
          isCustomerReferenceMissing(receivable.referenceStatus)
            ? t('receivable.missingCustomerReference', { id: receivable.customerId })
            : receivable.customerName
        }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivable.contractPrice')">
        {{ erpPriceInputFormatter(receivable.contract?.totalPrice) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivable.returnTime')">
        {{ formatDate(receivable.returnTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivable.price')">
        {{ erpPriceInputFormatter(receivable.price) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivable.ownerUserName')">
        {{ receivable.ownerUserName }}
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>
<script lang="ts" setup>
import * as ReceivableApi from '@/api/crm/receivable'
import { formatDate } from '@/utils/formatTime'
import { erpPriceInputFormatter } from '@/utils'
import {
  hasReferenceIssue,
  isCustomerReferenceMissing,
  referenceStatusLocaleKey
} from '../referenceIntegrity'

const { t } = useI18n('crm') // 国际化
const { receivable } = defineProps<{ receivable: ReceivableApi.ReceivableVO }>()
</script>
