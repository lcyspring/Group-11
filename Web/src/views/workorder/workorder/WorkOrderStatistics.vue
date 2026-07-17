<template>
  <Dialog v-model="dialogVisible" title="工单统计分析" width="900px">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="工单统计" name="statistics">
        <div v-loading="statsLoading">
          <el-row :gutter="20" class="mb-15px">
            <el-col :span="8">
              <el-card shadow="hover">
                <el-statistic title="工单总数" :value="statistics.totalCount || 0" />
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <el-statistic title="今日新增" :value="statistics.todayNewCount || 0" />
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <el-statistic title="今日完结" :value="statistics.todayCompletedCount || 0" />
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card header="状态分布" shadow="hover">
                <div v-for="(count, name) in statistics.statusDistribution" :key="name" class="flex justify-between py-5px">
                  <span>{{ name }}</span>
                  <el-tag type="info">{{ count }}</el-tag>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card header="优先级分布" shadow="hover">
                <div v-for="(count, name) in statistics.priorityDistribution" :key="name" class="flex justify-between py-5px">
                  <span>{{ name }}</span>
                  <el-tag type="info">{{ count }}</el-tag>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="效率分析" name="efficiency">
        <div v-loading="effLoading">
          <el-row :gutter="20" class="mb-15px">
            <el-col :span="6">
              <el-card shadow="hover">
                <el-statistic title="已完结工单" :value="efficiency.completedCount || 0" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <el-statistic title="平均处理时长" :value="efficiency.avgProcessingHours || 0" suffix="小时" :precision="1" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <el-statistic title="按时完成率" :value="efficiency.onTimeRate || 0" suffix="%" :precision="1" />
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <el-statistic title="超时工单数" :value="efficiency.delayedCount || 0" />
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card header="处理时长统计" shadow="hover">
                <div class="py-5px">最短：{{ efficiency.minProcessingHours || 0 }} 小时</div>
                <div class="py-5px">中位：{{ efficiency.medianProcessingHours || 0 }} 小时</div>
                <div class="py-5px">最长：{{ efficiency.maxProcessingHours || 0 }} 小时</div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card header="各优先级平均时长" shadow="hover">
                <div v-for="(hours, priority) in efficiency.avgProcessingHoursByPriority" :key="priority" class="flex justify-between py-5px">
                  <span>{{ priority }}</span>
                  <el-tag type="info">{{ hours }} 小时</el-tag>
                </div>
                <div v-if="!efficiency.avgProcessingHoursByPriority || Object.keys(efficiency.avgProcessingHoursByPriority).length === 0" class="text-gray-400">
                  暂无数据
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="趋势分析" name="trend">
        <div v-loading="trendLoading">
          <el-card header="近30天工单趋势" shadow="hover">
            <div class="trend-chart">
              <div
                v-for="(item, index) in trendData"
                :key="index"
                class="trend-row flex items-center py-3px"
              >
                <span class="trend-date w-100px text-right pr-10px text-xs">{{ item.date.slice(5) }}</span>
                <div class="trend-bars flex-1 flex items-center gap-2px">
                  <div class="bar-new h-16px rounded-2px" :style="{ width: barWidth(item.newCount, maxCount) }">
                    <span v-if="item.newCount > 0" class="text-10px text-white px-2px">{{ item.newCount }}</span>
                  </div>
                  <div class="bar-completed h-16px rounded-2px" :style="{ width: barWidth(item.completedCount, maxCount) }">
                    <span v-if="item.completedCount > 0" class="text-10px text-white px-2px">{{ item.completedCount }}</span>
                  </div>
                </div>
              </div>
              <div class="trend-legend flex justify-center gap-15px mt-10px text-xs">
                <span><span class="legend-dot inline-block w-10px h-10px rounded-2px bg-blue-500 mr-4px"></span>新增</span>
                <span><span class="legend-dot inline-block w-10px h-10px rounded-2px bg-green-500 mr-4px"></span>完结</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as WorkOrderStatisticsApi from '@/api/workorder/workOrderStatistics'

defineOptions({ name: 'WorkOrderStatistics' })

const dialogVisible = ref(false)
const activeTab = ref('statistics')
const statsLoading = ref(false)
const effLoading = ref(false)
const trendLoading = ref(false)

const statistics = ref<WorkOrderStatisticsApi.WorkOrderStatisticsVO>({
  totalCount: 0,
  todayNewCount: 0,
  todayCompletedCount: 0,
  statusDistribution: {},
  priorityDistribution: {},
  typeDistribution: {}
})

const efficiency = ref<WorkOrderStatisticsApi.WorkOrderEfficiencyVO>({
  avgProcessingHours: 0,
  medianProcessingHours: 0,
  minProcessingHours: 0,
  maxProcessingHours: 0,
  completedCount: 0,
  onTimeCount: 0,
  delayedCount: 0,
  onTimeRate: 0,
  avgProcessingHoursByHandler: {},
  avgProcessingHoursByPriority: {}
})

const trendData = ref<WorkOrderStatisticsApi.TrendItem[]>([])
const maxCount = ref(1)

const barWidth = (count: number, max: number) => {
  if (max === 0) return '0px'
  return Math.max((count / max) * 200, count > 0 ? 20 : 0) + 'px'
}

const loadStatistics = async () => {
  statsLoading.value = true
  try {
    const res = await WorkOrderStatisticsApi.getWorkOrderStatistics()
    statistics.value = res
  } finally {
    statsLoading.value = false
  }
}

const loadEfficiency = async () => {
  effLoading.value = true
  try {
    const res = await WorkOrderStatisticsApi.getWorkOrderEfficiencyAnalysis()
    efficiency.value = res
  } finally {
    effLoading.value = false
  }
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await WorkOrderStatisticsApi.getWorkOrderTrendAnalysis()
    trendData.value = res.dailyTrends || []
    const counts = trendData.value.flatMap((t) => [t.newCount, t.completedCount])
    maxCount.value = Math.max(...counts, 1)
  } finally {
    trendLoading.value = false
  }
}

const open = () => {
  dialogVisible.value = true
  activeTab.value = 'statistics'
  loadStatistics()
  // 预加载后续标签页数据
  loadEfficiency()
  loadTrend()
}
defineExpose({ open })
</script>
<style scoped>
.trend-chart {
  max-height: 500px;
  overflow-y: auto;
}
.bar-new {
  background-color: #409eff;
  min-width: 4px;
  transition: width 0.3s;
}
.bar-completed {
  background-color: #67c23a;
  min-width: 4px;
  transition: width 0.3s;
}
.legend-dot {
  vertical-align: middle;
}
</style>
