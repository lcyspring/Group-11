<template>
  <doc-alert title="【回款】回款管理、回款计划" url="https://doc.iocoder.cn/crm/receivable/" />
  <doc-alert title="【通用】数据权限" url="https://doc.iocoder.cn/crm/permission/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form ref="queryFormRef" :model="queryParams" class="-mb-15px" label-width="auto">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('receivable.no')" prop="no">
            <el-input
              v-model="queryParams.no"
              class="!w-240px"
              clearable
              :placeholder="t('receivable.noPlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('receivable.referenceIntegrity')" prop="referenceStatus">
            <el-select
              v-model="queryParams.referenceStatus"
              class="!w-240px"
              clearable
              :placeholder="t('receivable.referenceIntegrityPlaceholder')"
            >
              <el-option
                v-for="option in referenceStatusOptions"
                :key="option.value"
                :label="t(option.label)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('receivable.customerName')" prop="customerId">
            <el-select
              v-model="queryParams.customerId"
              class="!w-240px"
              :placeholder="t('customer.ownerUserPlaceholder')"
              @keyup.enter="handleQuery"
            >
              <el-option
                v-for="item in customerList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
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
            <el-button
              v-hasPermi="['crm:receivable:create']"
              plain
              type="primary"
              @click="openForm('create')"
            >
              <Icon class="mr-5px" icon="ep:plus" />
              {{ t('action.add') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:receivable:export']"
              :loading="exportLoading"
              plain
              type="success"
              @click="handleExport"
            >
              <Icon class="mr-5px" icon="ep:download" />
              {{ t('common.export') }}
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-tabs v-model="activeName" @tab-click="handleTabClick">
      <el-tab-pane :label="t('customer.myResponsible')" name="1" />
      <el-tab-pane :label="t('customer.myInvolved')" name="2" />
      <el-tab-pane :label="t('customer.subordinateResponsible')" name="3" />
      <el-tab-pane :label="t('customer.organizationScope')" name="4" />
    </el-tabs>
    <el-table
      v-loading="loading"
      :data="list"
      :show-overflow-tooltip="true"
      :stripe="true"
      :table-layout="'auto'"
    >
      <el-table-column
        align="center"
        fixed="left"
        :label="t('receivable.no')"
        prop="no"
        min-width="180"
      >
        <template #default="scope">
          <el-link :underline="false" type="primary" @click="openDetail(scope.row.id)">
            {{ scope.row.no }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('receivable.customerName')"
        prop="customerName"
        min-width="120"
      >
        <template #default="scope">
          <el-link
            v-if="!isCustomerReferenceMissing(scope.row.referenceStatus)"
            :underline="false"
            type="primary"
            @click="openCustomerDetail(scope.row.customerId)"
          >
            {{ scope.row.customerName }}
          </el-link>
          <el-tag v-else type="danger">
            {{ t('receivable.missingCustomerReference', { id: scope.row.customerId }) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('receivable.contractNo')"
        prop="contractNo"
        min-width="180"
      >
        <template #default="scope">
          <el-link
            v-if="!isContractReferenceInvalid(scope.row.referenceStatus)"
            :underline="false"
            type="primary"
            @click="openContractDetail(scope.row.contractId)"
          >
            {{ scope.row.contract?.no }}
          </el-link>
          <el-tag v-else type="danger">
            {{ t('receivable.missingContractReference', { id: scope.row.contractId }) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('receivable.referenceIntegrity')"
        prop="referenceStatus"
        min-width="180"
      >
        <template #default="scope">
          <el-tag v-if="hasReferenceIssue(scope.row.referenceStatus)" type="warning">
            {{ t(referenceStatusLocaleKey(scope.row.referenceStatus)) }}
          </el-tag>
          <el-tag v-else type="success">{{ t('receivable.referenceValid') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter2"
        align="center"
        :label="t('receivable.returnTime')"
        prop="returnTime"
        min-width="150"
      />
      <el-table-column
        align="center"
        :label="t('receivable.amountInCny', { label: t('receivable.price') })"
        prop="price"
        min-width="140"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        align="center"
        :label="t('receivable.amountInCny', { label: t('receivable.writtenOffAmount') })"
        prop="writtenOffAmount"
        min-width="140"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        align="center"
        :label="t('receivable.amountInCny', { label: t('receivable.writeOffRemaining') })"
        prop="remainingWriteOffAmount"
        min-width="160"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        align="center"
        :label="t('receivable.returnType')"
        prop="returnType"
        min-width="130"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_RECEIVABLE_RETURN_TYPE" :value="scope.row.returnType" />
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        :label="t('receivable.remark')"
        prop="remark"
        min-width="200"
      />
      <el-table-column
        align="center"
        :label="t('receivable.amountInCny', { label: t('receivable.contractPrice') })"
        prop="contract.totalPrice"
        min-width="140"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        align="center"
        :label="t('receivable.ownerUserName')"
        prop="ownerUserName"
        min-width="120"
      />
      <el-table-column
        align="center"
        :label="t('receivable.ownerUserDeptName')"
        prop="ownerUserDeptName"
        min-width="100"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('receivable.updateTime')"
        prop="updateTime"
        min-width="180"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('receivable.createTime')"
        prop="createTime"
        min-width="180"
      />
      <el-table-column
        align="center"
        :label="t('receivable.creatorName')"
        prop="creatorName"
        min-width="120"
      />
      <el-table-column
        align="center"
        fixed="right"
        :label="t('receivable.auditStatus')"
        prop="auditStatus"
        min-width="120"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_AUDIT_STATUS" :value="scope.row.auditStatus" />
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="340">
        <template #default="scope">
          <TableActions>
            <el-button
              v-if="
                scope.row.referenceStatus === ReceivableReferenceStatus.VALID &&
                [0, 30, 40].includes(scope.row.auditStatus)
              "
              v-hasPermi="['crm:receivable:update']"
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
            >
              {{ scope.row.auditStatus === 0 ? t('common.edit') : t('receivable.revise') }}
            </el-button>
            <el-button
              v-if="scope.row.auditStatus === 0"
              v-hasPermi="['crm:receivable:update']"
              link
              type="primary"
              @click="handleSubmit(scope.row)"
            >
              {{ t('contract.submitAudit') }}
            </el-button>
            <el-button
              v-if="scope.row.processInstanceId"
              v-hasPermi="['crm:receivable:update']"
              link
              type="primary"
              @click="handleProcessDetail(scope.row)"
            >
              {{ t('contract.viewApproval') }}
            </el-button>
            <el-button
              v-if="scope.row.auditStatus === 20 && Number(scope.row.writtenOffAmount || 0) > 0"
              v-hasPermi="['crm:receivable:query']"
              link type="primary" @click="openWriteOff(scope.row)"
            >{{ t('receivable.writeOffLedger') }}</el-button>
            <el-button
              v-else-if="scope.row.auditStatus === 20 && Number(scope.row.remainingWriteOffAmount ?? scope.row.price) > 0"
              v-hasPermi="['crm:receivable:write-off']"
              link type="success" @click="openWriteOff(scope.row)"
            >{{ t('receivable.writeOff') }}</el-button>
            <el-button
              v-if="scope.row.auditStatus === 0 && !scope.row.processInstanceId"
              v-hasPermi="['crm:receivable:delete']"
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
  <ReceivableForm ref="formRef" @success="getList" />
  <ReceivableWriteOffDialog ref="writeOffDialogRef" @success="getList" />
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter, dateFormatter2 } from '@/utils/formatTime'
import download from '@/utils/download'
import { resolveDialogAction } from '@/utils/dialogAction'
import * as ReceivableApi from '@/api/crm/receivable'
import ReceivableForm from './ReceivableForm.vue'
import ReceivableWriteOffDialog from './ReceivableWriteOffDialog.vue'
import TableActions from '@/components/TableActions/index.vue'
import * as CustomerApi from '@/api/crm/customer'
import { TabsPaneContext } from 'element-plus'
import { erpPriceTableColumnFormatter } from '@/utils'
import {
  ReceivableReferenceStatus,
  hasReferenceIssue,
  isContractReferenceInvalid,
  isCustomerReferenceMissing,
  referenceStatusLocaleKey
} from './referenceIntegrity'

defineOptions({ name: 'Receivable' })

const message = useMessage() // 消息弹窗
const { t } = useI18n('crm') // 国际化
const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<ReceivableApi.ReceivableVO[]>([]) // 列表的数据
const referenceStatusOptions = [
  { value: ReceivableReferenceStatus.VALID, label: 'receivable.referenceValid' },
  {
    value: ReceivableReferenceStatus.CUSTOMER_MISSING,
    label: 'receivable.referenceCustomerMissing'
  },
  {
    value: ReceivableReferenceStatus.CONTRACT_INVALID,
    label: 'receivable.referenceContractInvalid'
  },
  { value: ReceivableReferenceStatus.BOTH_INVALID, label: 'receivable.referenceBothInvalid' }
]
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneType: '1', // 默认与 activeName 相等
  no: undefined,
  customerId: undefined,
  referenceStatus: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const activeName = ref('1') // 列表 tab
const customerList = ref<CustomerApi.CustomerVO[]>([]) // 客户列表
const writeOffDialogRef = ref<InstanceType<typeof ReceivableWriteOffDialog>>()

/** tab 切换 */
const handleTabClick = (tab: TabsPaneContext) => {
  queryParams.sceneType = String(tab.paneName)
  handleQuery()
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ReceivableApi.getReceivablePage(queryParams)
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

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  if (!(await resolveDialogAction(message.delConfirm()))) return
  await ReceivableApi.deleteReceivable(id)
  message.success(t('common.delSuccess'))
  await getList()
}

/** 提交审核 **/
const handleSubmit = async (row: ReceivableApi.ReceivableVO) => {
  await message.confirm(t('receivable.submitAuditConfirm', { no: row.no }))
  await ReceivableApi.submitReceivable(row.id)
  message.success(t('receivable.submitAuditSuccess'))
  await getList()
}

/** 查看审批 */
const handleProcessDetail = (row: ReceivableApi.ReceivableVO) => {
  push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
}
const openWriteOff = async (row: ReceivableApi.ReceivableVO) => {
  await writeOffDialogRef.value?.open(row)
}

/** 打开回款详情 */
const { push } = useRouter()
const openDetail = (id: number) => {
  push({ name: 'CrmReceivableDetail', params: { id } })
}

/** 打开客户详情 */
const openCustomerDetail = (id: number) => {
  push({ name: 'CrmCustomerDetail', params: { id } })
}

/** 打开合同详情 */
const openContractDetail = (id: number) => {
  push({ name: 'CrmContractDetail', params: { id } })
}

/** 导出按钮操作 */
const handleExport = async () => {
  if (!(await resolveDialogAction(message.exportConfirm()))) return
  exportLoading.value = true
  try {
    const data = await ReceivableApi.exportReceivable(queryParams)
    download.excel(data, t('receivable.exportFileName') + '.xls')
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
  // 获得客户列表
  customerList.value = await CustomerApi.getCustomerSimpleList()
})
</script>
