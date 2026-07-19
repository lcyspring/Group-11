<template>
  <el-card shadow="never">
    <template #header>
      <!-- 标题 -->
      <div class="flex flex-row items-center justify-between">
        <CardTitle :title="t('rank')" />
        <!-- 查询条件 -->
        <ShortcutDateRangePicker ref="shortcutDateRangePicker" @change="handleDateRangeChange" />
      </div>
    </template>
    <!-- 排行列表 -->
    <el-table v-loading="loading" :data="list" @sort-change="handleSortChange" :table-layout="'auto'">
      <el-table-column :label="t('spuId')" prop="spuId" min-width="70" />
      <el-table-column :label="t('picUrl')" align="center" prop="picUrl" min-width="80">
        <template #default="{ row }">
          <el-image
            :src="row.picUrl"
            :preview-src-list="[row.picUrl]"
            class="h-30px w-30px"
            preview-teleported
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('name')" prop="name" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column :label="t('browseCountLabel')" prop="browseCount" min-width="90" sortable="custom" />
      <el-table-column :label="t('browseUserCountLabel')" prop="browseUserCount" min-width="90" sortable="custom" />
      <el-table-column :label="t('cartCount')" prop="cartCount" min-width="105" sortable="custom" />
      <el-table-column :label="t('orderCount')" prop="orderCount" min-width="105" sortable="custom" />
      <el-table-column :label="t('orderPayCountLabel')" prop="orderPayCount" min-width="105" sortable="custom" />
      <el-table-column
        :label="t('orderPayPriceLabel')"
        prop="orderPayPrice"
        min-width="105"
        sortable="custom"
        :formatter="fenToYuanFormat"
      />
      <el-table-column :label="t('favoriteCount')" prop="favoriteCount" min-width="90" sortable="custom" />
      <el-table-column
        :label="t('browseConvertPercent')"
        prop="browseConvertPercent"
        min-width="180"
        sortable="custom"
        :formatter="formatConvertRate"
      />
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getSpuList"
    />
  </el-card>
</template>
<script lang="ts" setup>
import { ProductStatisticsApi, ProductStatisticsVO } from '@/api/mall/statistics/product'
import { CardTitle } from '@/components/Card'
import { buildSortingField } from '@/utils'
import { fenToYuanFormat } from '@/utils/formatter'
import { useI18n } from '@/hooks/web/useI18n'

/** 商品排行 */
defineOptions({ name: 'ProductRank' })

const { t } = useI18n('mall.statistics.product')

// 格式化：访客到支付的转化率
const formatConvertRate = (row: ProductStatisticsVO) => {
  return `${row.browseConvertPercent}%`
}

const handleSortChange = (params: any) => {
  queryParams.sortingFields = [buildSortingField(params)]
  getSpuList()
}

const handleDateRangeChange = (times: any[]) => {
  queryParams.times = times as []
  getSpuList()
}

const shortcutDateRangePicker = ref()
// 查询参数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  times: [],
  sortingFields: {}
})
const loading = ref(false) // 列表的加载中
const total = ref(0) // 总记录数
const list = ref<ProductStatisticsVO[]>([]) // 排行数据

/** 查询商品列表 */
const getSpuList = async () => {
  loading.value = true
  try {
    const data = await ProductStatisticsApi.getProductStatisticsRankPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 初始化排行数据 */
onMounted(async () => {
  await getSpuList()
})
</script>
<style lang="scss" scoped></style>
