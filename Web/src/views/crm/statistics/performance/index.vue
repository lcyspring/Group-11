<!-- 数据统计 - 员工业绩分析 -->
<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" label-width="auto">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('selectYear')">
            <el-date-picker
              v-model="selectedYear"
              class="!w-240px"
              type="year"
              value-format="YYYY"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('dept')" prop="deptId">
            <el-tree-select
              v-model="queryParams.deptId"
              class="!w-240px"
              :data="deptList"
              :props="defaultProps"
              check-strictly
              node-key="id"
              :placeholder="t('dept')"
              @change="queryParams.userId = undefined"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('user')" prop="userId">
            <el-select
              v-model="queryParams.userId"
              class="!w-240px"
              :placeholder="t('user')"
              clearable
            >
              <el-option
                v-for="(user, index) in userListByDeptId"
                :label="user.nickname"
                :value="user.id"
                :key="index"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item>
            <el-button @click="handleQuery">
              <Icon icon="ep:search" class="mr-5px" /> {{ t('search') }}
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh" class="mr-5px" /> {{ t('reset') }}
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <!-- 员工业绩统计 -->
  <el-col>
    <el-tabs v-model="activeTab">
      <!-- 员工合同统计 -->
      <el-tab-pane :label="t('performance.contractCount')" name="ContractCountPerformance" lazy>
        <ContractCountPerformance :query-params="queryParams" ref="ContractCountPerformanceRef" />
      </el-tab-pane>
      <!-- 员工合同金额统计 -->
      <el-tab-pane :label="t('performance.contractPrice')" name="ContractPricePerformance" lazy>
        <ContractPricePerformance :query-params="queryParams" ref="ContractPricePerformanceRef" />
      </el-tab-pane>
      <!-- 员工回款金额统计 -->
      <el-tab-pane :label="t('performance.receivablePrice')" name="ReceivablePricePerformance" lazy>
        <ReceivablePricePerformance
          :query-params="queryParams"
          ref="ReceivablePricePerformanceRef"
        />
      </el-tab-pane>
      <el-tab-pane
        :label="t('performance.targetCompletion')"
        name="TargetCompletionPerformance"
        lazy
      >
        <TargetCompletionPerformance
          :query-params="queryParams"
          ref="TargetCompletionPerformanceRef"
        />
      </el-tab-pane>
      <el-tab-pane
        :label="t('performance.targetManagement')"
        name="PerformanceTargetManagement"
        lazy
      >
        <PerformanceTargetManagement
          :query-params="queryParams"
          ref="PerformanceTargetManagementRef"
        />
      </el-tab-pane>
    </el-tabs>
  </el-col>
</template>

<script lang="ts" setup>
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { beginOfDay, endOfDay, formatDate } from '@/utils/formatTime'
import { defaultProps, handleTree } from '@/utils/tree'
import ContractCountPerformance from './components/ContractCountPerformance.vue'
import ContractPricePerformance from './components/ContractPricePerformance.vue'
import ReceivablePricePerformance from './components/ReceivablePricePerformance.vue'
import TargetCompletionPerformance from './components/TargetCompletionPerformance.vue'
import PerformanceTargetManagement from './components/PerformanceTargetManagement.vue'

defineOptions({ name: 'CrmStatisticsPerformance' })

const { t } = useI18n('crm.statistics') // 国际化
const currentYear = new Date().getFullYear()
const selectedYear = ref(String(currentYear))

const queryParams = reactive({
  deptId: useUserStore().getUser.deptId,
  userId: undefined,
  times: [
    formatDate(beginOfDay(new Date(currentYear, 0, 1))),
    formatDate(endOfDay(new Date(currentYear, 11, 31)))
  ]
})

const queryFormRef = ref() // 搜索的表单
const deptList = ref<Tree[]>([]) // 部门树形结构
const userList = ref<UserApi.UserVO[]>([]) // 全量用户清单
// 根据选择的部门筛选员工清单
const userListByDeptId = computed(() =>
  queryParams.deptId
    ? userList.value.filter((u: UserApi.UserVO) => u.deptId === queryParams.deptId)
    : []
)

// 活跃标签
const activeTab = ref('ContractCountPerformance')
const ContractCountPerformanceRef = ref() // 员工合同数量统计
const ContractPricePerformanceRef = ref() // 员工合同金额统计
const ReceivablePricePerformanceRef = ref() // 员工回款金额统计
const TargetCompletionPerformanceRef = ref() // 业绩目标完成度
const PerformanceTargetManagementRef = ref() // 业绩目标维护

/** 搜索按钮操作 */
const handleQuery = () => {
  if (!selectedYear.value) {
    return
  }
  const selectYear = Number(selectedYear.value)
  queryParams.times[0] = formatDate(beginOfDay(new Date(selectYear, 0, 1)))
  queryParams.times[1] = formatDate(endOfDay(new Date(selectYear, 11, 31)))

  // 执行查询
  switch (activeTab.value) {
    case 'ContractCountPerformance':
      ContractCountPerformanceRef.value?.loadData?.()
      break
    case 'ContractPricePerformance':
      ContractPricePerformanceRef.value?.loadData?.()
      break
    case 'ReceivablePricePerformance':
      ReceivablePricePerformanceRef.value?.loadData?.()
      break
    case 'TargetCompletionPerformance':
      TargetCompletionPerformanceRef.value?.loadData?.()
      break
    case 'PerformanceTargetManagement':
      PerformanceTargetManagementRef.value?.loadData?.()
      break
  }
}

// 当 activeTab 改变时，刷新当前活动的 tab
watch(activeTab, () => {
  handleQuery()
})

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  selectedYear.value = String(currentYear)
  handleQuery()
}

// 加载部门树
onMounted(async () => {
  deptList.value = handleTree(await DeptApi.getSimpleDeptList())
  userList.value = await UserApi.getSimpleUserList()
})
</script>
