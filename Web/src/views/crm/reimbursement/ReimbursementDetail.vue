<template>
  <Dialog v-model="visible" :title="data.no || t('reimbursement.detail')" width="1000px">
    <ReimbursementDetailContent :data="data" :records="records" />
  </Dialog>
</template>

<script setup lang="ts">
import * as ReimbursementApi from '@/api/crm/reimbursement'
import ReimbursementDetailContent from './ReimbursementDetailContent.vue'

const { t } = useI18n('crm')
const visible = ref(false)
const data = ref<ReimbursementApi.ReimbursementVO>({} as ReimbursementApi.ReimbursementVO)
const records = ref<ReimbursementApi.ReimbursementActionVO[]>([])
const open = async (id: number) => {
  visible.value = true
  ;[data.value, records.value] = await Promise.all([
    ReimbursementApi.getReimbursement(id),
    ReimbursementApi.getActionRecords(id)
  ])
}
defineExpose({ open })
</script>
