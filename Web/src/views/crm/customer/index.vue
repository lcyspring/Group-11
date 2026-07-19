<template>
  <doc-alert title="【客户】客户管理、公海客户" url="https://doc.iocoder.cn/crm/customer/" />
  <doc-alert title="【通用】数据权限" url="https://doc.iocoder.cn/crm/permission/" />

  <el-alert
    v-if="queryParams.parentCustomerId"
    :closable="false"
    type="info"
    show-icon
    class="mb-16px"
    :title="t('childCustomerFilter', { name: parentCustomerName || `#${queryParams.parentCustomerId}` })"
  >
    <el-button link type="primary" @click="clearParentFilter">{{ t('clearHierarchyFilter') }}</el-button>
  </el-alert>

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      class="-mb-15px"
      label-width="auto"
    >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('name')" prop="name">
            <el-input
              v-model="queryParams.name"
              class="!w-240px"
              clearable
              :placeholder="t('namePlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('contactName')" prop="contactName">
            <el-input
              v-model="queryParams.contactName"
              class="!w-240px"
              clearable
              :placeholder="t('contactNamePlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('primaryContact')" prop="primaryContactName">
            <el-input
              v-model="queryParams.primaryContactName"
              class="!w-240px"
              clearable
              :placeholder="t('primaryContactPlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('mobile')" prop="mobile">
            <el-input
              v-model="queryParams.mobile"
              class="!w-240px"
              clearable
              :placeholder="t('mobilePlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('industryId')" prop="industryId">
            <el-select
              v-model="queryParams.industryId"
              class="!w-240px"
              clearable
              :placeholder="t('industryPlaceholder')"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_INDUSTRY)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('level')" prop="level">
            <el-select
              v-model="queryParams.level"
              class="!w-240px"
              clearable
              :placeholder="t('levelPlaceholder')"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_LEVEL)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('source')" prop="source">
            <el-select
              v-model="queryParams.source"
              class="!w-240px"
              clearable
              :placeholder="t('sourcePlaceholder')"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_SOURCE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('lifecycleStatus')" prop="lifecycleStatus">
            <el-select
              v-model="queryParams.lifecycleStatus"
              class="!w-240px"
              clearable
              :placeholder="t('lifecycleStatusPlaceholder')"
            >
              <el-option
                v-for="status in lifecycleStatusOptions"
                :key="status.value"
                :label="status.label"
                :value="status.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item>
            <el-button @click="handleQuery">
              <Icon class="mr-5px" icon="ep:search" />
              {{ t('common.search') }}
            </el-button>
            <el-button @click="resetQuery">
              <Icon class="mr-5px" icon="ep:refresh" />
              {{ t('common.reset') }}
            </el-button>
            <el-button v-hasPermi="['crm:customer:create']" type="primary" @click="openForm('create')">
              <Icon class="mr-5px" icon="ep:plus" />
              {{ t('common.add') }}
            </el-button>
            <el-button v-hasPermi="['crm:customer:import']" plain type="warning" @click="handleImport">
              <Icon icon="ep:upload" />
              {{ t('common.import') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:customer:export']"
              :loading="exportLoading"
              plain
              type="success"
              @click="handleExport"
            >
              <Icon class="mr-5px" icon="ep:download" />
              {{ t('common.export') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:customer:export']"
              plain
              type="success"
              @click="openExportTasks"
            >
              <Icon class="mr-5px" icon="ep:clock" />
              {{ t('exportTaskEntry') }}
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-tabs v-model="activeName" @tab-click="handleTabClick">
      <el-tab-pane :label="t('myResponsible')" name="1" />
      <el-tab-pane :label="t('myInvolved')" name="2" />
      <el-tab-pane :label="t('subordinateResponsible')" name="3" />
      <el-tab-pane :label="t('organizationScope')" name="4" />
    </el-tabs>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true" :table-layout="'auto'">
      <el-table-column align="center" fixed="left" :label="t('name')" prop="name" min-width="160">
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('parentCustomer')" prop="parentCustomerName" min-width="160" />
      <el-table-column align="center" :label="t('primaryContact')" prop="primaryContactName" min-width="120" />
      <el-table-column align="center" :label="t('primaryContactMobile')" prop="primaryContactMobile" min-width="130" />
      <el-table-column align="center" :label="t('source')" prop="source" min-width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_SOURCE" :value="scope.row.source" />
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('mobile')" prop="mobile" min-width="120" />
      <el-table-column align="center" :label="t('telephone')" prop="telephone" min-width="130" />
      <el-table-column align="center" :label="t('email')" prop="email" min-width="180" />
      <el-table-column align="center" :label="t('birthday')" prop="birthday" min-width="120" />
      <el-table-column align="center" :label="t('level')" prop="level" min-width="135">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_LEVEL" :value="scope.row.level" />
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('industryId')" prop="industryId" min-width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_INDUSTRY" :value="scope.row.industryId" />
        </template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('contactNextTime')"
        prop="contactNextTime"
        min-width="180"
      />
      <el-table-column align="center" :label="t('remark')" prop="remark" min-width="200" />
      <el-table-column align="center" :label="t('lockStatus')" prop="lockStatus">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="scope.row.lockStatus" />
        </template>
      </el-table-column>
      <el-table-column align="center" :label="t('lifecycleStatus')" prop="lifecycleStatus" min-width="120">
        <template #default="scope">
          <el-tag :type="lifecycleStatusTag(scope.row.lifecycleStatus)">
            {{ lifecycleStatusLabel(scope.row.lifecycleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('lastContactTime')"
        prop="contactLastTime"
        min-width="180"
      />
      <el-table-column align="center" :label="t('lastContactContent')" prop="contactLastContent" min-width="200" />
      <el-table-column align="center" :label="t('areaId')" prop="detailAddress" min-width="180" />
      <el-table-column align="center" :label="t('poolDay')" prop="poolDay" min-width="140">
        <template #default="scope"> {{ scope.row.poolDay }} {{ t('dayUnit') }}</template>
      </el-table-column>
      <el-table-column align="center" :label="t('ownerUserId')" prop="ownerUserName" min-width="100" />
      <el-table-column align="center" :label="t('ownerUserDeptName')" prop="ownerUserDeptName" min-width="100" />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('common.updateTime')"
        prop="updateTime"
        min-width="180"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('common.createTime')"
        prop="createTime"
        min-width="180"
      />
      <el-table-column align="center" :label="t('common.creator')" prop="creatorName" min-width="100" />
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="220">
        <template #default="scope">
          <TableActions>
            <el-button
              v-hasPermi="['crm:customer:update']"
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:customer:delete']"
              link
              type="danger"
              @click="handleDelete(scope.row.id)"
            >
              {{ t('common.delete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <CustomerForm ref="formRef" @success="getList" />
  <CustomerImportForm ref="importFormRef" @success="getList" />
  <CustomerExportTaskDialog ref="exportTaskDialogRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { resolveDialogAction } from '@/utils/dialogAction'
import * as CustomerApi from '@/api/crm/customer'
import CustomerForm from './CustomerForm.vue'
import CustomerImportForm from './CustomerImportForm.vue'
import CustomerExportTaskDialog from './CustomerExportTaskDialog.vue'
import { TabsPaneContext } from 'element-plus'

defineOptions({ name: 'CrmCustomer' })

const message = useMessage() // 消息弹窗
const { t } = useI18n('crm.customer') // 国际化
const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneType: '1', // 默认与 activeName 相等
  name: '',
  mobile: '',
  contactName: '',
  primaryContactName: '',
  industryId: undefined,
  level: undefined,
  source: undefined,
  lifecycleStatus: undefined as CustomerApi.CustomerLifecycleStatus | undefined,
  parentCustomerId: undefined as number | undefined,
  pool: undefined
})
const parentCustomerName = ref('')
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const activeName = ref('1') // 列表 tab
const lifecycleStatusOptions = computed(() => [
  { value: CustomerApi.CustomerLifecycleStatus.POTENTIAL, label: t('lifecyclePotential') },
  { value: CustomerApi.CustomerLifecycleStatus.INTENTIONAL, label: t('lifecycleIntentional') },
  { value: CustomerApi.CustomerLifecycleStatus.DEAL, label: t('lifecycleDeal') },
  { value: CustomerApi.CustomerLifecycleStatus.LOST, label: t('lifecycleLost') }
])
const lifecycleStatusLabel = (status: CustomerApi.CustomerLifecycleStatus) =>
  lifecycleStatusOptions.value.find((item) => item.value === status)?.label || t('lifecycleUnknown')
const lifecycleStatusTags: Record<number, 'info' | 'warning' | 'success' | 'danger'> = {
  10: 'info', 20: 'warning', 30: 'success', 40: 'danger'
}
const lifecycleStatusTag = (status: CustomerApi.CustomerLifecycleStatus) =>
  lifecycleStatusTags[status] || 'info'

/** tab 切换 */
const handleTabClick = (tab: TabsPaneContext) => {
  queryParams.sceneType = tab.paneName as string
  handleQuery()
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CustomerApi.getCustomerPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const syncHierarchyFilterFromRoute = () => {
  const parentId = Number(currentRoute.value.query.parentCustomerId)
  queryParams.parentCustomerId = Number.isSafeInteger(parentId) && parentId > 0 ? parentId : undefined
  parentCustomerName.value = String(currentRoute.value.query.parentCustomerName || '')
}

const clearParentFilter = () => {
  push({ name: 'CrmCustomer' })
}

/** 打开客户详情 */
const { currentRoute, push } = useRouter()
const openDetail = (id: number) => {
  push({ name: 'CrmCustomerDetail', params: { id } })
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  if (!(await resolveDialogAction(message.delConfirm()))) return
  await CustomerApi.deleteCustomer(id)
  message.success(t('common.delSuccess'))
  await getList()
}

/** 导入按钮操作 */
const importFormRef = ref<InstanceType<typeof CustomerImportForm>>()
const handleImport = () => {
  importFormRef.value?.open()
}

const exportTaskDialogRef = ref<InstanceType<typeof CustomerExportTaskDialog>>()
const openExportTasks = () => exportTaskDialogRef.value?.open()

/** 导出按钮操作 */
const handleExport = async () => {
  if (!(await resolveDialogAction(message.exportConfirm()))) return
  exportLoading.value = true
  try {
    await CustomerApi.createCustomerExportTask({
      ...queryParams,
      sceneType: queryParams.sceneType ? Number(queryParams.sceneType) : undefined
    })
    message.success(t('exportTaskSubmitted'))
    await exportTaskDialogRef.value?.open()
  } finally {
    exportLoading.value = false
  }
}

/** 监听路由变化更新列表 */
watch(
  () => currentRoute.value,
  () => {
    syncHierarchyFilterFromRoute()
    getList()
  }
)

/** 初始化 **/
onMounted(() => {
  syncHierarchyFilterFromRoute()
  getList()
})
</script>
