<template>
  <div>
    <div class="flex items-start justify-between">
      <div>
        <el-col>
          <el-row>
            <span class="text-xl font-bold"
              >{{ t('receivablePlan.period') }} {{ receivablePlan.period }}</span
            >
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
      <el-descriptions-item :label="t('receivablePlan.customerName')">
        {{ receivablePlan.customerName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivablePlan.contractNo')">{{
        receivablePlan.contractNo
      }}</el-descriptions-item>
      <el-descriptions-item :label="t('receivablePlan.price')">
        {{ erpPriceInputFormatter(receivablePlan.price) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivablePlan.returnTime')">
        {{ formatDate(receivablePlan.returnTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('receivablePlan.receivablePrice')">
        {{ erpPriceInputFormatter(receivablePlan.receivedPrice) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('common.status')">
        <el-tag :type="statusType">{{ statusLabel }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>
<script lang="ts" setup>
import * as ReceivablePlanApi from '@/api/crm/receivable/plan'
import { formatDate } from '@/utils/formatTime'
import { erpPriceInputFormatter } from '@/utils'

const { t } = useI18n('crm') // 国际化
const { receivablePlan } = defineProps<{ receivablePlan: ReceivablePlanApi.ReceivablePlanVO }>()
const statusType = computed(() => {
  if (receivablePlan.status === ReceivablePlanApi.ReceivablePlanStatus.RECEIVED) return 'success'
  if (receivablePlan.status === ReceivablePlanApi.ReceivablePlanStatus.OVERDUE) return 'danger'
  return 'warning'
})
const statusLabel = computed(() => {
  if (receivablePlan.status === ReceivablePlanApi.ReceivablePlanStatus.RECEIVED)
    return t('receivablePlan.statusReceived')
  if (receivablePlan.status === ReceivablePlanApi.ReceivablePlanStatus.OVERDUE)
    return t('receivablePlan.statusOverdue')
  return t('receivablePlan.statusPending')
})
</script>
