<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" inline class="-mb-15px">
      <el-form-item :label="t('invoice.applicationNo')"><el-input v-model="queryParams.no" clearable @keyup.enter="query" /></el-form-item>
      <el-form-item :label="t('invoice.invoiceNo')"><el-input v-model="queryParams.invoiceNo" clearable @keyup.enter="query" /></el-form-item>
      <el-form-item :label="t('invoice.status')"><el-select v-model="queryParams.status" clearable @change="query"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.direction')"><el-select v-model="queryParams.direction" clearable @change="query"><el-option :label="t('invoice.directionBlue')" :value="1" /><el-option :label="t('invoice.directionRed')" :value="-1" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.type')"><el-select v-model="queryParams.type" clearable @change="query"><el-option :label="t('invoice.typeOrdinary')" :value="1" /><el-option :label="t('invoice.typeSpecial')" :value="2" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.customer')"><el-select v-model="queryParams.customerId" clearable filterable @change="query"><el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
      <el-form-item :label="t('invoice.invoiceDate')"><el-date-picker v-model="queryParams.invoiceDate" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" range-separator="-" /></el-form-item>
      <el-form-item>
        <el-button @click="query">{{ t('common.search') }}</el-button><el-button @click="reset">{{ t('common.reset') }}</el-button>
        <el-button v-hasPermi="['crm:invoice:create']" type="primary" @click="formRef.open('create')">{{ t('invoice.createDraft') }}</el-button>
        <el-button v-hasPermi="['crm:invoice:export']" :loading="exportLoading" @click="handleExport">{{ t('common.export') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column :label="t('invoice.applicationNo')" prop="no" min-width="170"><template #default="{ row }"><el-link type="primary" :underline="false" @click="detailRef.open(row.id)">{{ row.no }}</el-link></template></el-table-column>
      <el-table-column :label="t('invoice.invoiceNo')" prop="invoiceNo" min-width="150" show-overflow-tooltip />
      <el-table-column :label="t('invoice.contract')" min-width="190" show-overflow-tooltip><template #default="{ row }">{{ row.contractNo }} · {{ row.contractName }}</template></el-table-column>
      <el-table-column :label="t('invoice.customer')" prop="customerName" min-width="130" />
      <el-table-column :label="t('invoice.direction')" min-width="80"><template #default="{ row }"><el-tag :type="row.direction === -1 ? 'danger' : 'primary'">{{ row.direction === -1 ? t('invoice.directionRed') : t('invoice.directionBlue') }}</el-tag></template></el-table-column>
      <el-table-column :label="t('invoice.status')" min-width="110"><template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column :label="t('invoice.amount')" prop="amount" align="right" min-width="120"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
      <el-table-column :label="t('invoice.redAmount')" prop="redAmount" align="right" min-width="120"><template #default="{ row }">{{ money(row.redAmount) }}</template></el-table-column>
      <el-table-column :label="t('invoice.handler')" prop="handlerUserName" min-width="100" />
      <el-table-column :label="t('invoice.invoiceDate')" prop="invoiceDate" :formatter="dateFormatter" min-width="170" />
      <el-table-column :label="t('common.action')" fixed="right" min-width="300">
        <template #default="{ row }">
          <el-button v-if="canEditInvoice(row.status, row.direction)" v-hasPermi="['crm:invoice:update']" link type="primary" @click="formRef.open('update', row.id)">{{ t('common.edit') }}</el-button>
          <el-button v-if="canIssueInvoice(row.status, row.direction)" v-hasPermi="['crm:invoice:issue']" link type="success" @click="issueRef.open(row)">{{ t('invoice.issue') }}</el-button>
          <el-button v-if="canRedFlushInvoice(row.status, row.direction)" v-hasPermi="['crm:invoice:red-flush']" link type="warning" @click="redRef.open(row)">{{ t('invoice.redFlush') }}</el-button>
          <el-button v-if="canVoidInvoice(row.status)" v-hasPermi="['crm:invoice:void']" link type="danger" @click="voidRef.open(row)">{{ t('invoice.void') }}</el-button>
          <el-button v-if="canEditInvoice(row.status, row.direction)" v-hasPermi="['crm:invoice:delete']" link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>
  <InvoiceForm ref="formRef" @success="getList" />
  <InvoiceIssueDialog ref="issueRef" @success="getList" />
  <InvoiceRedFlushDialog ref="redRef" @success="getList" />
  <InvoiceVoidDialog ref="voidRef" @success="getList" />
  <InvoiceDetail ref="detailRef" />
</template>

<script setup lang="ts">
import * as InvoiceApi from '@/api/crm/invoice'
import * as CustomerApi from '@/api/crm/customer'
import InvoiceForm from './InvoiceForm.vue'
import InvoiceIssueDialog from './InvoiceIssueDialog.vue'
import InvoiceRedFlushDialog from './InvoiceRedFlushDialog.vue'
import InvoiceVoidDialog from './InvoiceVoidDialog.vue'
import InvoiceDetail from './InvoiceDetail.vue'
import { canEditInvoice, canIssueInvoice, canRedFlushInvoice, canVoidInvoice } from './constants'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'

defineOptions({ name: 'CrmInvoice' })
const { t } = useI18n('crm'); const message = useMessage()
const loading = ref(false); const exportLoading = ref(false); const list = ref<InvoiceApi.InvoiceVO[]>([]); const total = ref(0)
const formRef = ref(); const issueRef = ref(); const redRef = ref(); const voidRef = ref(); const detailRef = ref(); const queryFormRef = ref()
const customers = ref<CustomerApi.CustomerVO[]>([])
const queryParams = reactive({ pageNo: 1, pageSize: 10, no: '', invoiceNo: '', customerId: undefined as number | undefined,
  status: undefined as number | undefined, type: undefined as number | undefined, direction: undefined as number | undefined,
  invoiceDate: undefined as string[] | undefined })
const statusOptions = computed(() => [
  { value: 0, label: t('invoice.statusDraft') }, { value: 10, label: t('invoice.statusIssued') },
  { value: 20, label: t('invoice.statusPartiallyRed') }, { value: 30, label: t('invoice.statusFullyRed') },
  { value: 40, label: t('invoice.statusVoided') }
])
const statusLabel = (value: number) => statusOptions.value.find(item => item.value === value)?.label || value
const statusTag = (value: number) => ({ 0: 'info', 10: 'success', 20: 'warning', 30: 'danger', 40: 'info' } as Record<number, any>)[value]
const money = (value: number) => Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
const getList = async () => { loading.value = true; try { const data = await InvoiceApi.getInvoicePage(queryParams); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const query = () => { queryParams.pageNo = 1; getList() }
const reset = () => { queryFormRef.value?.resetFields(); Object.assign(queryParams, { pageNo: 1, no: '', invoiceNo: '', customerId: undefined, status: undefined, type: undefined, direction: undefined, invoiceDate: undefined }); getList() }
const remove = async (row: InvoiceApi.InvoiceVO) => { await message.delConfirm(); await InvoiceApi.deleteInvoice(row.id!); message.success(t('common.delSuccess')); getList() }
const handleExport = async () => { await message.exportConfirm(); exportLoading.value = true; try { const data = await InvoiceApi.exportInvoice(queryParams); download.excel(data, `${t('invoice.exportFileName')}.xls`) } finally { exportLoading.value = false } }
onMounted(async () => { customers.value = await CustomerApi.getCustomerSimpleList(); await getList() })
onActivated(getList)
</script>
