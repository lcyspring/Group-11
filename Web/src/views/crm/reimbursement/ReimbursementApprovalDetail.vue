<template>
  <div v-loading="loading" class="min-h-180px">
    <ReimbursementDetailContent v-if="data.id" :data="data" :records="records" />
  </div>
</template>

<script setup lang="ts">
import * as ReimbursementApi from '@/api/crm/reimbursement'
import ReimbursementDetailContent from './ReimbursementDetailContent.vue'

const props = defineProps<{ id?: number | string }>()
const loading = ref(false)
const data = ref<ReimbursementApi.ReimbursementVO>({} as ReimbursementApi.ReimbursementVO)
const records = ref<ReimbursementApi.ReimbursementActionVO[]>([])

const load = async () => {
  const id = Number(props.id)
  if (!Number.isSafeInteger(id) || id <= 0) return
  loading.value = true
  try {
    ;[data.value, records.value] = await Promise.all([
      ReimbursementApi.getReimbursement(id),
      ReimbursementApi.getActionRecords(id)
    ])
  } finally {
    loading.value = false
  }
}

watch(() => props.id, load, { immediate: true })
</script>

