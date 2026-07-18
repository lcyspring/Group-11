<template><ContentWrap v-loading="loading" :title="t('customerVisit.detail')"><el-descriptions :column="2" border>
  <el-descriptions-item :label="t('customerVisit.id')">{{ data.id }}</el-descriptions-item><el-descriptions-item :label="t('customerVisit.auditStatus')"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="data.auditStatus" /></el-descriptions-item>
  <el-descriptions-item :label="t('customerVisit.customer')">{{ data.customerName }}</el-descriptions-item><el-descriptions-item :label="t('customerVisit.contact')">{{ data.contactName || '-' }}</el-descriptions-item>
  <el-descriptions-item :label="t('customerVisit.plannedStart')">{{ formatDate(data.plannedStartTime) }}</el-descriptions-item><el-descriptions-item :label="t('customerVisit.plannedEnd')">{{ formatDate(data.plannedEndTime) }}</el-descriptions-item>
  <el-descriptions-item :label="t('customerVisit.location')" :span="2">{{ data.location }}</el-descriptions-item><el-descriptions-item :label="t('customerVisit.purpose')" :span="2">{{ data.purpose }}</el-descriptions-item>
  <el-descriptions-item v-if="data.resultStatus === 1" :label="t('customerVisit.actualStart')">{{ formatDate(data.actualStartTime) }}</el-descriptions-item><el-descriptions-item v-if="data.resultStatus === 1" :label="t('customerVisit.actualEnd')">{{ formatDate(data.actualEndTime) }}</el-descriptions-item>
  <el-descriptions-item v-if="data.resultStatus === 1" :label="t('customerVisit.resultContent')" :span="2">{{ data.resultContent }}</el-descriptions-item><el-descriptions-item v-if="data.followUpRecordId" :label="t('customerVisit.followUpRecord')">{{ data.followUpRecordId }}</el-descriptions-item>
</el-descriptions></ContentWrap></template>
<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as VisitApi from '@/api/crm/customerVisit'
defineOptions({ name: 'CrmCustomerVisitDetail' })
const { t } = useI18n('crm')
const props = defineProps<{ id?: number | string }>()
const route = useRoute()
const loading = ref(false)
const data = ref<VisitApi.CustomerVisitVO>({} as VisitApi.CustomerVisitVO)
onMounted(async () => {
  const id = Number(props.id ?? route.query.id)
  if (!Number.isSafeInteger(id) || id <= 0) return
  loading.value = true
  try { data.value = await VisitApi.getCustomerVisit(id) } finally { loading.value = false }
})
</script>
