<template>
  <el-alert
    v-if="!loading && list.length > 0 && !hasConfiguredStages"
    class="mb-16px"
    :closable="false"
    :title="t('funnel.statusGroupMissingStages')"
    type="warning"
    show-icon
  />
  <el-card shadow="never">
    <el-skeleton :loading="loading" animated>
      <Echart :height="500" :options="echartsOption" />
    </el-skeleton>
  </el-card>

  <el-card class="mt-16px" shadow="never">
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column align="center" :label="t('customer.index')" type="index" width="80" />
      <el-table-column align="center" :label="t('funnel.stage')" min-width="180">
        <template #default="scope">
          <dict-tag
            v-if="scope.row.endStatus"
            :type="DICT_TYPE.CRM_BUSINESS_END_STATUS_TYPE"
            :value="scope.row.endStatus"
          />
          <span v-else>{{ scope.row.statusName }}</span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('funnel.businessCount')"
        min-width="140"
        prop="businessCount"
      />
      <el-table-column
        align="center"
        :label="t('funnel.businessTotalPrice')"
        min-width="180"
        prop="totalPrice"
      />
      <el-table-column align="center" :label="t('funnel.conversionOrOutcomeRate')" min-width="180">
        <template #default="scope">{{ scope.row.conversionRate }}%</template>
      </el-table-column>
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="100">
        <template #default="scope">
          <el-button
            v-hasPermi="['crm:business:query']"
            link
            type="primary"
            @click="openStageDetails(scope.row)"
          >
            {{ t('common.detail') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <Dialog v-model="detailsVisible" :title="detailsTitle" width="1100px">
    <el-table v-loading="detailsLoading" :data="businessList" table-layout="auto">
      <el-table-column align="center" :label="t('funnel.businessName')" min-width="180">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openBusinessDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('funnel.customerName')" min-width="160">
        <template #default="scope">
          <el-link
            :underline="false"
            type="primary"
            @click="openCustomerDetail(scope.row.customerId)"
          >
            {{ scope.row.customerName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="right"
        :formatter="erpPriceTableColumnFormatter"
        :label="t('funnel.totalPrice')"
        min-width="150"
        prop="totalPrice"
      />
      <el-table-column
        align="center"
        :formatter="dateFormatter"
        :label="t('funnel.dealTime')"
        min-width="180"
        prop="dealTime"
      />
      <el-table-column
        align="center"
        :label="t('funnel.statusName')"
        min-width="140"
        prop="statusName"
      />
      <el-table-column
        align="center"
        :label="t('funnel.ownerUserName')"
        min-width="140"
        prop="ownerUserName"
      />
    </el-table>
    <Pagination
      v-model:limit="detailsPage.pageSize"
      v-model:page="detailsPage.pageNo"
      :total="detailsTotal"
      @pagination="loadStageDetails"
    />
  </Dialog>
</template>

<script lang="ts" setup>
import {
  CrmStatisticsBusinessStageSummaryRespVO,
  StatisticFunnelApi
} from '@/api/crm/statistics/funnel'
import { DICT_TYPE, getDictLabel } from '@/utils/dict'
import { EChartsOption } from 'echarts'
import type { BusinessVO } from '@/api/crm/business'
import { erpPriceTableColumnFormatter } from '@/utils'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'FunnelBusiness' })

const { t } = useI18n('crm.statistics')
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const list = ref<CrmStatisticsBusinessStageSummaryRespVO[]>([])
const detailsVisible = ref(false)
const detailsLoading = ref(false)
const selectedStage = ref<CrmStatisticsBusinessStageSummaryRespVO>()
const businessList = ref<BusinessVO[]>([])
const detailsTotal = ref(0)
const detailsPage = reactive({ pageNo: 1, pageSize: 10 })
const hasConfiguredStages = computed(() => list.value.some((row) => !row.endStatus && row.statusId))

const echartsOption = reactive<EChartsOption>({
  title: { text: t('funnel.stageAndOutcome') },
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      const row = params.data.raw as CrmStatisticsBusinessStageSummaryRespVO
      return `${params.name}<br/>${t('funnel.businessTotalPrice')}: ${row.totalPrice}<br/>${t(
        'funnel.conversionOrOutcomeRate'
      )}: ${row.conversionRate}%`
    }
  },
  toolbox: {
    feature: {
      dataView: { readOnly: true },
      restore: {},
      saveAsImage: {}
    }
  },
  series: [
    {
      name: t('funnel.funnel'),
      type: 'funnel',
      left: '4%',
      top: 60,
      bottom: 40,
      width: '58%',
      min: 0,
      minSize: '0%',
      maxSize: '100%',
      sort: 'none',
      gap: 2,
      label: { show: true, position: 'inside' },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { label: { fontSize: 18 } },
      data: []
    },
    {
      name: t('funnel.outcomeDistribution'),
      type: 'pie',
      center: ['82%', '38%'],
      radius: ['22%', '38%'],
      label: { formatter: '{b}\n{c}' },
      data: []
    }
  ]
}) as EChartsOption

const stageLabel = (row: CrmStatisticsBusinessStageSummaryRespVO) =>
  row.endStatus
    ? getDictLabel(DICT_TYPE.CRM_BUSINESS_END_STATUS_TYPE, row.endStatus) || '-'
    : row.statusName || '-'

const detailsTitle = computed(() =>
  selectedStage.value
    ? `${stageLabel(selectedStage.value)} - ${t('funnel.stageDetails')}`
    : t('funnel.stageDetails')
)

const loadStageDetails = async () => {
  if (!selectedStage.value) return
  detailsLoading.value = true
  try {
    const params = {
      ...props.queryParams,
      pageNo: detailsPage.pageNo,
      pageSize: detailsPage.pageSize
    }
    const data = selectedStage.value.endStatus
      ? await StatisticFunnelApi.getBusinessOutcomePage({
          ...params,
          endStatus: selectedStage.value.endStatus
        })
      : await StatisticFunnelApi.getBusinessStagePage({
          ...params,
          statusId: selectedStage.value.statusId
        })
    businessList.value = data.list
    detailsTotal.value = data.total
  } finally {
    detailsLoading.value = false
  }
}

const openStageDetails = async (row: CrmStatisticsBusinessStageSummaryRespVO) => {
  selectedStage.value = row
  detailsPage.pageNo = 1
  detailsVisible.value = true
  await loadStageDetails()
}

const clearData = () => {
  list.value = []
  if (echartsOption.series?.[0]) {
    echartsOption.series[0]['data'] = []
  }
  if (echartsOption.series?.[1]) echartsOption.series[1]['data'] = []
}

const loadData = async () => {
  if (!props.queryParams.statusTypeId) {
    clearData()
    return
  }
  detailsVisible.value = false
  loading.value = true
  try {
    const data = (await StatisticFunnelApi.getBusinessStageSummary(
      props.queryParams
    )) as CrmStatisticsBusinessStageSummaryRespVO[]
    list.value = data
    if (echartsOption.series?.[0]) {
      echartsOption.series[0]['data'] = data.filter((row) => !row.endStatus).map((row) => ({
        value: row.businessCount,
        name: `${stageLabel(row)}-${row.businessCount}${t('funnel.unit')}`,
        raw: row
      }))
    }
    if (echartsOption.series?.[1]) {
      echartsOption.series[1]['data'] = data.filter((row) => row.endStatus).map((row) => ({
        value: row.businessCount,
        name: stageLabel(row),
        raw: row
      }))
    }
  } finally {
    loading.value = false
  }
}

const { push } = useRouter()
const openBusinessDetail = (id: number) => push({ name: 'CrmBusinessDetail', params: { id } })
const openCustomerDetail = (id: number) => push({ name: 'CrmCustomerDetail', params: { id } })

defineExpose({ loadData })
onMounted(loadData)
</script>
