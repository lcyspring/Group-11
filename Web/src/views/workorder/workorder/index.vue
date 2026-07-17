<template>
  <!-- 搜索 -->
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      label-width="auto"
    >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="工单标题" prop="title">
            <el-input
              v-model="queryParams.title"
              placeholder="请输入工单标题"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单类型" prop="typeId">
            <el-select
              v-model="queryParams.typeId"
              placeholder="请选择工单类型"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="item in typeList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="优先级" prop="priority">
            <el-select
              v-model="queryParams.priority"
              placeholder="请选择优先级"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.WO_WORK_ORDER_PRIORITY)"
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
          <el-form-item label="工单状态" prop="status">
            <el-select
              v-model="queryParams.status"
              placeholder="请选择状态"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.WO_WORK_ORDER_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="处理人" prop="handlerUserId">
            <el-input
              v-model="queryParams.handlerUserId"
              placeholder="请输入处理人ID"
              clearable
              class="!w-240px"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="发起人" prop="submitterUserId">
            <el-input
              v-model="queryParams.submitterUserId"
              placeholder="请输入发起人ID"
              clearable
              class="!w-240px"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item>
            <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
            <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
            <el-button
              type="primary"
              plain
              @click="openForm('create')"
              v-hasPermi="['workorder:work-order:create']"
            >
              <Icon icon="ep:plus" class="mr-5px" /> 新增
            </el-button>
            <el-button
              type="success"
              plain
              @click="handleExport"
              v-hasPermi="['workorder:work-order:export']"
            >
              <Icon icon="ep:download" class="mr-5px" /> 导出
            </el-button>
            <el-button
              type="info"
              plain
              @click="openStatistics"
              v-hasPermi="['workorder:work-order:query']"
            >
              <Icon icon="ep:data-analysis" class="mr-5px" /> 统计分析
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column label="工单编号" align="center" prop="id" min-width="90" />
      <el-table-column label="工单标题" align="center" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="工单类型" align="center" prop="typeName" min-width="120" />
      <el-table-column label="优先级" align="center" prop="priorityName" min-width="80">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_PRIORITY" :value="scope.row.priority" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="statusName" min-width="90">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="处理人" align="center" prop="handlerUserName" min-width="100" />
      <el-table-column label="发起人" align="center" prop="submitterUserName" min-width="100" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        min-width="170"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" min-width="480" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row.id)"
            v-hasPermi="['workorder:work-order:query']"
          >
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['workorder:work-order:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="warning"
            @click="openPriorityForm(scope.row.id, scope.row.priority)"
            v-hasPermi="['workorder:work-order:update']"
          >
            优先级
          </el-button>
          <el-button
            link
            type="success"
            @click="openAssignForm(scope.row.id, scope.row.handlerUserName)"
            v-hasPermi="['workorder:work-order:update']"
            v-if="scope.row.status !== 2 && scope.row.status !== 3"
          >
            分配
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleProcess(scope.row)"
            v-hasPermi="['workorder:work-order:update']"
            v-if="scope.row.status === 0 || scope.row.status === 4"
          >
            处理
          </el-button>
          <el-button
            link
            type="success"
            @click="openCompleteForm(scope.row.id, scope.row.status)"
            v-hasPermi="['workorder:work-order:update']"
            v-if="scope.row.status === 1"
          >
            完结
          </el-button>
          <el-button
            link
            type="danger"
            @click="openReturnForm(scope.row.id, scope.row.status)"
            v-hasPermi="['workorder:work-order:update']"
            v-if="scope.row.status === 1"
          >
            退回
          </el-button>
          <el-button
            link
            type="warning"
            @click="openStatusForm(scope.row.id)"
            v-hasPermi="['workorder:work-order:update']"
            v-if="scope.row.status !== 2 && scope.row.status !== 3"
          >
            流转
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['workorder:work-order:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <WorkOrderForm ref="formRef" @success="getList" />
  <!-- 状态流转弹窗 -->
  <WorkOrderStatusForm ref="statusFormRef" @success="getList" />
  <!-- 优先级修改弹窗 -->
  <WorkOrderPriorityForm ref="priorityFormRef" @success="getList" />
  <!-- 分配弹窗 -->
  <WorkOrderAssignForm ref="assignFormRef" @success="getList" />
  <!-- 完结弹窗 -->
  <WorkOrderCompleteForm ref="completeFormRef" @success="getList" />
  <!-- 退回弹窗 -->
  <WorkOrderReturnForm ref="returnFormRef" @success="getList" />
  <!-- 统计分析弹窗 -->
  <WorkOrderStatistics ref="statisticsRef" />
  <!-- 详情弹窗 -->
  <WorkOrderDetail ref="detailRef" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as WorkOrderApi from '@/api/workorder/workOrder'
import * as WorkOrderTypeApi from '@/api/workorder/workOrderType'
import WorkOrderForm from './WorkOrderForm.vue'
import WorkOrderStatusForm from './WorkOrderStatusForm.vue'
import WorkOrderPriorityForm from './WorkOrderPriorityForm.vue'
import WorkOrderAssignForm from './WorkOrderAssignForm.vue'
import WorkOrderCompleteForm from './WorkOrderCompleteForm.vue'
import WorkOrderReturnForm from './WorkOrderReturnForm.vue'
import WorkOrderStatistics from './WorkOrderStatistics.vue'
import WorkOrderDetail from './WorkOrderDetail.vue'

defineOptions({ name: 'WorkOrder' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref([])
const typeList = ref<WorkOrderTypeApi.WorkOrderTypeVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: undefined,
  typeId: undefined,
  priority: undefined,
  status: undefined,
  handlerUserId: undefined,
  submitterUserId: undefined
})
const queryFormRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await WorkOrderApi.getWorkOrderPage(queryParams)
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
  queryFormRef.value?.resetFields()
  getList()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 状态流转操作 */
const statusFormRef = ref()
const openStatusForm = (id: number) => {
  statusFormRef.value.open(id)
}

/** 详情操作 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 优先级修改操作 */
const priorityFormRef = ref()
const openPriorityForm = (id: number, currentPriority: number) => {
  priorityFormRef.value.open(id, currentPriority)
}

/** 分配操作 */
const assignFormRef = ref()
const openAssignForm = (id: number, currentHandlerName: string) => {
  assignFormRef.value.open(id, currentHandlerName)
}

/** 处理操作 */
const handleProcess = async (row: any) => {
  try {
    await message.confirm(`确认开始处理工单「${row.title}」?`)
    await WorkOrderApi.processWorkOrder({ id: row.id })
    message.success('已开始处理工单')
    await getList()
  } catch {}
}

/** 完结操作 */
const completeFormRef = ref()
const openCompleteForm = (id: number, currentStatus: number) => {
  completeFormRef.value.open(id, currentStatus)
}

/** 退回操作 */
const returnFormRef = ref()
const openReturnForm = (id: number, currentStatus: number) => {
  returnFormRef.value.open(id, currentStatus)
}

/** 统计分析操作 */
const statisticsRef = ref()
const openStatistics = () => {
  statisticsRef.value.open()
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await WorkOrderApi.deleteWorkOrder(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  await WorkOrderApi.exportWorkOrderExcel(queryParams)
}

/** 加载工单类型列表 */
const loadTypeList = async () => {
  const data = await WorkOrderTypeApi.getEnableWorkOrderTypeList()
  typeList.value = data
}

/** 初始化 **/
onMounted(() => {
  getList()
  loadTypeList()
})
</script>
