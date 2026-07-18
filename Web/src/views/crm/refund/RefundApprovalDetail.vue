<template>
  <div v-loading="loading" class="min-h-180px">
    <RefundDetailContent v-if="data.id" :data="data" :records="records" />
  </div>
</template>

<script setup lang="ts">
import * as RefundApi from '@/api/crm/refund'
import RefundDetailContent from './RefundDetailContent.vue'

const props = defineProps<{ id?: number | string }>()
const loading = ref(false)
const data = ref<RefundApi.ReceivableRefundVO>({} as RefundApi.ReceivableRefundVO)
const records = ref<RefundApi.RefundActionVO[]>([])

const load = async () => {
  const id = Number(props.id)
  if (!Number.isSafeInteger(id) || id <= 0) return
  loading.value = true
  try {
    ;[data.value, records.value] = await Promise.all([
      RefundApi.getRefund(id),
      RefundApi.getActionRecords(id)
    ])
  } finally {
    loading.value = false
  }
}

watch(() => props.id, load, { immediate: true })
</script>

