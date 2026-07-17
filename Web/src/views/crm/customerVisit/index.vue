<template>
  <ContentWrap>
    <el-form ref="queryRef" :model="query" class="-mb-15px" label-width="auto"><el-row :gutter="16">
      <el-col :span="7"><el-form-item :label="t('customerVisit.customer')" prop="customerId"><el-select v-model="query.customerId" clearable filterable><el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
      <el-col :span="7"><el-form-item :label="t('customerVisit.auditStatus')" prop="auditStatus"><el-select v-model="query.auditStatus" clearable><el-option v-for="item in getIntDictOptions(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS)" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      <el-col :span="10"><el-form-item><el-button @click="search"><Icon icon="ep:search" />{{ t('common.search') }}</el-button><el-button @click="reset"><Icon icon="ep:refresh" />{{ t('common.reset') }}</el-button><el-button v-hasPermi="['crm:customer-visit:create']" type="primary" @click="create"><Icon icon="ep:plus" />{{ t('customerVisit.create') }}</el-button></el-form-item></el-col>
    </el-row></el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column prop="id" :label="t('customerVisit.id')" />
      <el-table-column prop="customerName" :label="t('customerVisit.customer')" min-width="150" />
      <el-table-column prop="contactName" :label="t('customerVisit.contact')" min-width="120" />
      <el-table-column prop="plannedStartTime" :label="t('customerVisit.plannedStart')" :formatter="dateFormatter" min-width="170" />
      <el-table-column prop="location" :label="t('customerVisit.location')" min-width="160" show-overflow-tooltip />
      <el-table-column prop="auditStatus" :label="t('customerVisit.auditStatus')"><template #default="{ row }"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="row.auditStatus" /></template></el-table-column>
      <el-table-column prop="resultStatus" :label="t('customerVisit.resultStatus')"><template #default="{ row }"><el-tag :type="row.resultStatus === 1 ? 'success' : 'info'">{{ row.resultStatus === 1 ? t('customerVisit.completed') : t('customerVisit.pending') }}</el-tag></template></el-table-column>
      <el-table-column fixed="right" :label="t('common.operation')" min-width="150"><template #default="{ row }"><TableActions mode="menu"><el-button link type="primary" @click="detail(row)">{{ t('common.detail') }}</el-button><el-button v-if="row.processInstanceId" link type="primary" @click="progress(row)">{{ t('customerVisit.progress') }}</el-button><el-button v-if="row.auditStatus === 2 && row.resultStatus === 0" v-hasPermi="['crm:customer-visit:update']" link type="success" @click="openResult(row)">{{ t('customerVisit.recordResult') }}</el-button></TableActions></template></el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </ContentWrap>
  <Dialog v-model="resultVisible" :title="t('customerVisit.recordResult')" width="620px">
    <el-form ref="resultRef" :model="result" :rules="resultRules" label-width="110px">
      <el-row :gutter="16"><el-col :span="12"><el-form-item :label="t('customerVisit.actualStart')" prop="actualStartTime"><el-date-picker v-model="result.actualStartTime" type="datetime" value-format="x" class="w-full" /></el-form-item></el-col><el-col :span="12"><el-form-item :label="t('customerVisit.actualEnd')" prop="actualEndTime"><el-date-picker v-model="result.actualEndTime" type="datetime" value-format="x" class="w-full" /></el-form-item></el-col></el-row>
      <el-form-item :label="t('customerVisit.resultContent')" prop="resultContent"><el-input v-model="result.resultContent" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item :label="t('customerVisit.nextContact')" prop="nextContactTime"><el-date-picker v-model="result.nextContactTime" type="datetime" value-format="x" class="w-full" /></el-form-item>
      <el-form-item :label="t('customerVisit.resultAttachments')"><UploadFile v-model="result.resultAttachmentUrls" :limit="10" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="resultLoading" @click="submitResult">{{ t('common.confirm') }}</el-button><el-button @click="resultVisible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as VisitApi from '@/api/crm/customerVisit'
import * as CustomerApi from '@/api/crm/customer'
defineOptions({ name: 'CrmCustomerVisit' })
const { t } = useI18n('crm')
const message = useMessage()
const router = useRouter()
const loading = ref(false)
const list = ref<VisitApi.CustomerVisitVO[]>([])
const total = ref(0)
const queryRef = ref()
const query = reactive({ pageNo: 1, pageSize: 10, customerId: undefined, auditStatus: undefined })
const customers = ref<CustomerApi.CustomerVO[]>([])
const load = async () => { loading.value = true; try { const data = await VisitApi.getCustomerVisitPage(query); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const search = () => { query.pageNo = 1; load() }
const reset = () => { queryRef.value?.resetFields(); search() }
const create = () => router.push({ name: 'CrmCustomerVisitCreate' })
const detail = (row: VisitApi.CustomerVisitVO) => router.push({ name: 'CrmCustomerVisitDetail', query: { id: row.id } })
const progress = (row: VisitApi.CustomerVisitVO) => router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
const resultVisible = ref(false)
const resultLoading = ref(false)
const resultRef = ref()
const result = ref<VisitApi.CustomerVisitResultVO>({ id: 0, actualStartTime: '', actualEndTime: '', resultContent: '', nextContactTime: '', resultAttachmentUrls: [] })
const resultRules = { actualStartTime: [{ required: true, message: t('customerVisit.actualStartRequired'), trigger: 'change' }], actualEndTime: [{ required: true, message: t('customerVisit.actualEndRequired'), trigger: 'change' }], resultContent: [{ required: true, message: t('customerVisit.resultRequired'), trigger: 'blur' }, { min: 5, max: 2000, message: t('customerVisit.resultLength'), trigger: 'blur' }], nextContactTime: [{ required: true, message: t('customerVisit.nextContactRequired'), trigger: 'change' }] }
const openResult = (row: VisitApi.CustomerVisitVO) => { result.value = { id: row.id!, actualStartTime: row.plannedStartTime, actualEndTime: Date.now(), resultContent: '', nextContactTime: '', resultAttachmentUrls: [] }; resultVisible.value = true }
const submitResult = async () => { if (!await resultRef.value?.validate()) return; resultLoading.value = true; try { await VisitApi.recordCustomerVisitResult(result.value); message.success(t('customerVisit.resultSuccess')); resultVisible.value = false; await load() } finally { resultLoading.value = false } }
onMounted(async () => { customers.value = await CustomerApi.getCustomerSimpleList(); await load() })
</script>
