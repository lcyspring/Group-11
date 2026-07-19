<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1000px">
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column align="center" :label="t('portrait.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('customer.customerName')" min-width="200">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openCustomer(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('portrait.areaName')"
        min-width="220"
        prop="areaName"
      />
      <el-table-column align="center" :label="t('portrait.dealStatusName')" min-width="120">
        <template #default="scope">
          {{
            scope.row.dealStatus
              ? t('portrait.dealStatusDeal')
              : t('portrait.dealStatusUndeal')
          }}
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('customer.ownerUserName')"
        min-width="140"
        prop="ownerUserName"
      />
      <el-table-column
        align="center"
        :formatter="dateFormatter"
        :label="t('portrait.createTime')"
        min-width="180"
        prop="createTime"
      />
    </el-table>
    <Pagination
      v-model:limit="pageParams.pageSize"
      v-model:page="pageParams.pageNo"
      :total="total"
      @pagination="loadPage"
    />
  </Dialog>
</template>

<script lang="ts" setup>
import type { CustomerVO } from '@/api/crm/customer'
import { StatisticsPortraitApi } from '@/api/crm/statistics/portrait'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'PortraitCustomerRegionDetail' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const dialogVisible = ref(false)
const loading = ref(false)
const list = ref<CustomerVO[]>([])
const total = ref(0)
const pageParams = reactive({ pageNo: 1, pageSize: 10 })
const selected = reactive({ areaId: 0, areaName: '', areaType: 3 as 1 | 2 | 3 })
const dialogTitle = computed(() => t('portrait.areaCustomers', { area: selected.areaName }))

const loadPage = async () => {
  loading.value = true
  try {
    const data = await StatisticsPortraitApi.getCustomerPageByArea({
      ...props.queryParams,
      areaId: selected.areaId,
      areaType: selected.areaType,
      pageNo: pageParams.pageNo,
      pageSize: pageParams.pageSize
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const open = async (areaId: number, areaName: string, areaType: 1 | 2 | 3) => {
  selected.areaId = areaId
  selected.areaName = areaName
  selected.areaType = areaType
  pageParams.pageNo = 1
  dialogVisible.value = true
  await loadPage()
}

const { push } = useRouter()
const openCustomer = (id: number) => {
  dialogVisible.value = false
  push({ name: 'CrmCustomerDetail', params: { id } })
}

defineExpose({ open })
</script>
