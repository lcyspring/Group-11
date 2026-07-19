<template>
  <Dialog v-model="visible" :title="data.no || t('refund.detail')" width="900px">
    <RefundDetailContent :data="data" :records="records" />
  </Dialog>
</template>

<script setup lang="ts">
import * as RefundApi from '@/api/crm/refund'
import RefundDetailContent from './RefundDetailContent.vue'

const { t } = useI18n('crm')
const visible = ref(false)
const data = ref<RefundApi.ReceivableRefundVO>({} as RefundApi.ReceivableRefundVO)
const records = ref<RefundApi.RefundActionVO[]>([])
const open = async (id: number) => {
  visible.value = true
  ;[data.value, records.value] = await Promise.all([
    RefundApi.getRefund(id),
    RefundApi.getActionRecords(id)
  ])
}
defineExpose({ open })
</script>
