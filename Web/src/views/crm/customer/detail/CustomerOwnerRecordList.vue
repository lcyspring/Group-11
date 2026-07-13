<template>
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      :show-overflow-tooltip="true"
      :stripe="true"
      table-layout="auto"
    >
      <el-table-column align="center" :label="t('ownerChangeType')" min-width="130">
        <template #default="scope">
          {{ getTypeLabel(scope.row.type) }}
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('previousOwner')" min-width="140">
        <template #default="scope">
          {{ getUserLabel(scope.row.previousOwnerUserName, scope.row.previousOwnerUserId) }}
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('newOwner')" min-width="140">
        <template #default="scope">
          {{ getUserLabel(scope.row.newOwnerUserName, scope.row.newOwnerUserId) }}
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('operator')" min-width="140">
        <template #default="scope">
          {{ getUserLabel(scope.row.operatorUserName, scope.row.operatorUserId) }}
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('changeTime')" min-width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as CustomerApi from '@/api/crm/customer'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CrmCustomerOwnerRecordList' })

const props = defineProps<{
  customerId: number
}>()
const { t } = useI18n('crm.customer')
const loading = ref(false)
const list = ref<CustomerApi.CustomerOwnerRecordVO[]>([])

const typeLabelKeys: Record<number, string> = {
  1: 'ownerChangePutPool',
  2: 'ownerChangeTakePool',
  3: 'ownerChangeInitialAssign',
  4: 'ownerChangeTransfer'
}

const getTypeLabel = (type: number) => {
  const key = typeLabelKeys[type]
  return key ? t(key) : t('ownerChangeUnknown', { type })
}

const getUserLabel = (name?: string, id?: number) => {
  if (name) return name
  if (id) return `#${id}`
  return t('unassignedOwner')
}

const getList = async () => {
  loading.value = true
  try {
    list.value = await CustomerApi.getCustomerOwnerRecordList(props.customerId)
  } finally {
    loading.value = false
  }
}

defineExpose({ getList })
onMounted(getList)
</script>
