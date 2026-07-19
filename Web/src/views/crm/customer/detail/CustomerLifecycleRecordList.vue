<template>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe table-layout="auto">
      <el-table-column align="center" :label="t('lifecycleFromStatus')" min-width="130">
        <template #default="scope">{{ statusLabel(scope.row.fromStatus) }}</template>
      </el-table-column>
      <el-table-column align="center" :label="t('lifecycleToStatus')" min-width="130">
        <template #default="scope">{{ statusLabel(scope.row.toStatus) }}</template>
      </el-table-column>
      <el-table-column align="center" :label="t('lifecycleReason')" prop="reason" min-width="220" />
      <el-table-column align="center" :label="t('operator')" min-width="140">
        <template #default="scope">{{ scope.row.operatorUserName || `#${scope.row.operatorUserId || '-'}` }}</template>
      </el-table-column>
      <el-table-column align="center" :label="t('changeTime')" min-width="180">
        <template #default="scope">{{ formatDate(scope.row.changeTime) }}</template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as CustomerApi from '@/api/crm/customer'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CrmCustomerLifecycleRecordList' })
const props = defineProps<{ customerId: number }>()
const { t } = useI18n('crm.customer')
const loading = ref(false)
const list = ref<CustomerApi.CustomerLifecycleRecordVO[]>([])
const statusLabels = computed<Record<number, string>>(() => ({
  10: t('lifecyclePotential'), 20: t('lifecycleIntentional'),
  30: t('lifecycleDeal'), 40: t('lifecycleLost')
}))
const statusLabel = (status: CustomerApi.CustomerLifecycleStatus) =>
  statusLabels.value[status] || t('lifecycleUnknown')
const getList = async () => {
  loading.value = true
  try { list.value = await CustomerApi.getCustomerLifecycleRecordList(props.customerId) }
  finally { loading.value = false }
}
defineExpose({ getList })
onMounted(getList)
</script>
