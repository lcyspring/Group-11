<template>
  <ContentWrap>
    <el-tabs v-model="scope" @tab-change="search"><el-tab-pane :label="t('workReport.mine')" name="mine" /><el-tab-pane :label="t('workReport.received')" name="received" /></el-tabs>
    <el-form ref="queryRef" :model="query" class="-mb-15px" label-width="auto"><el-row :gutter="16">
      <el-col :span="6"><el-form-item :label="t('workReport.type')"><el-select v-model="query.reportType" clearable><el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      <el-col :span="6"><el-form-item :label="t('workReport.status')"><el-select v-model="query.status" clearable><el-option :label="t('workReport.draft')" :value="0" /><el-option :label="t('workReport.submitted')" :value="1" /></el-select></el-form-item></el-col>
      <el-col :span="12"><el-form-item><el-button @click="search"><Icon icon="ep:search" />{{ t('common.search') }}</el-button><el-button @click="reset"><Icon icon="ep:refresh" />{{ t('common.reset') }}</el-button><el-button v-if="scope === 'mine'" v-hasPermi="['crm:work-report:create']" type="primary" @click="openForm()"><Icon icon="ep:plus" />{{ t('workReport.create') }}</el-button></el-form-item></el-col>
    </el-row></el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column prop="reportDate" :label="t('workReport.date')" min-width="110" />
      <el-table-column :label="t('workReport.type')"><template #default="{ row }">{{ typeLabel(row.reportType) }}</template></el-table-column>
      <el-table-column prop="title" :label="t('workReport.title')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="authorUserName" :label="t('workReport.author')" min-width="110" />
      <el-table-column :label="t('workReport.receivers')" min-width="160"><template #default="{ row }">{{ row.receiverUserNames?.join('、') }}</template></el-table-column>
      <el-table-column :label="t('workReport.status')"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? t('workReport.submitted') : t('workReport.draft') }}</el-tag></template></el-table-column>
      <el-table-column fixed="right" :label="t('common.operation')" min-width="120"><template #default="{ row }"><TableActions mode="menu"><el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button><el-button v-if="scope === 'mine' && row.status === 0" v-hasPermi="['crm:work-report:update']" link type="primary" @click="openForm(row)">{{ t('common.edit') }}</el-button><el-button v-if="scope === 'mine' && row.status === 0" v-hasPermi="['crm:work-report:update']" link type="success" @click="submit(row)">{{ t('workReport.submit') }}</el-button><el-button v-if="scope === 'mine' && row.status === 0" v-hasPermi="['crm:work-report:delete']" link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button></TableActions></template></el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </ContentWrap>
  <Dialog v-model="formVisible" :title="form.id ? t('workReport.edit') : t('workReport.create')" width="760px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="16"><el-col :span="12"><el-form-item :label="t('workReport.type')" prop="reportType"><el-select v-model="form.reportType" class="w-full"><el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item :label="t('workReport.date')" prop="reportDate"><el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item></el-col></el-row>
      <el-form-item :label="t('workReport.title')" prop="title"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
      <el-form-item :label="t('workReport.completed')" prop="completedContent"><el-input v-model="form.completedContent" type="textarea" :rows="4" maxlength="5000" show-word-limit /></el-form-item>
      <el-form-item :label="t('workReport.pending')"><el-input v-model="form.pendingContent" type="textarea" :rows="3" maxlength="5000" show-word-limit /></el-form-item>
      <el-form-item :label="t('workReport.nextPlan')" prop="nextPlan"><el-input v-model="form.nextPlan" type="textarea" :rows="4" maxlength="5000" show-word-limit /></el-form-item>
      <el-form-item :label="t('workReport.issues')"><el-input v-model="form.issues" type="textarea" :rows="3" maxlength="3000" show-word-limit /></el-form-item>
      <el-form-item :label="t('workReport.receivers')" prop="receiverUserIds"><el-select v-model="form.receiverUserIds" multiple filterable class="w-full"><el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" /></el-select></el-form-item>
      <el-form-item :label="t('workReport.attachments')"><UploadFile v-model="form.attachmentUrls" :limit="10" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="saving" @click="save">{{ t('common.confirm') }}</el-button><el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>
  <Dialog v-model="detailVisible" :title="detail.title || t('workReport.detail')" width="760px"><el-descriptions :column="2" border><el-descriptions-item :label="t('workReport.author')">{{ detail.authorUserName }}</el-descriptions-item><el-descriptions-item :label="t('workReport.period')">{{ detail.periodStart }} ~ {{ detail.periodEnd }}</el-descriptions-item><el-descriptions-item :label="t('workReport.receivers')" :span="2">{{ detail.receiverUserNames?.join('、') }}</el-descriptions-item><el-descriptions-item :label="t('workReport.completed')" :span="2"><div class="whitespace-pre-wrap">{{ detail.completedContent }}</div></el-descriptions-item><el-descriptions-item :label="t('workReport.pending')" :span="2"><div class="whitespace-pre-wrap">{{ detail.pendingContent || '-' }}</div></el-descriptions-item><el-descriptions-item :label="t('workReport.nextPlan')" :span="2"><div class="whitespace-pre-wrap">{{ detail.nextPlan }}</div></el-descriptions-item><el-descriptions-item :label="t('workReport.issues')" :span="2"><div class="whitespace-pre-wrap">{{ detail.issues || '-' }}</div></el-descriptions-item></el-descriptions></Dialog>
</template>
<script setup lang="ts">
import type { FormRules } from 'element-plus'
import * as Api from '@/api/crm/workReport'
import * as UserApi from '@/api/system/user'
import dayjs from 'dayjs'
defineOptions({ name: 'CrmWorkReport' })
const { t } = useI18n('crm'); const message = useMessage(); const loading = ref(false); const saving = ref(false)
const scope = ref('mine'); const list = ref<Api.WorkReportVO[]>([]); const total = ref(0); const users = ref<UserApi.UserVO[]>([])
const queryRef = ref(); const query = reactive({ pageNo: 1, pageSize: 10, reportType: undefined as number | undefined, status: undefined as number | undefined })
const types = computed(() => [{ value: 1, label: t('workReport.daily') }, { value: 2, label: t('workReport.weekly') }, { value: 3, label: t('workReport.monthly') }]); const typeLabel = (value: number) => types.value.find(item => item.value === value)?.label
const load = async () => { loading.value = true; try { const data = await Api.getWorkReportPage({ ...query, received: scope.value === 'received' }); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const search = () => { query.pageNo = 1; load() }; const reset = () => { queryRef.value?.resetFields(); search() }
const empty = (): Api.WorkReportVO => ({ reportType: 1, reportDate: dayjs().format('YYYY-MM-DD'), title: '', completedContent: '', pendingContent: '', nextPlan: '', issues: '', receiverUserIds: [], attachmentUrls: [] })
const formVisible = ref(false); const formRef = ref(); const form = ref<Api.WorkReportVO>(empty())
const rules: FormRules = { reportType: [{ required: true, message: t('workReport.typeRequired'), trigger: 'change' }], reportDate: [{ required: true, message: t('workReport.dateRequired'), trigger: 'change' }], title: [{ required: true, message: t('workReport.titleRequired'), trigger: 'blur' }], completedContent: [{ required: true, message: t('workReport.completedRequired'), trigger: 'blur' }], nextPlan: [{ required: true, message: t('workReport.planRequired'), trigger: 'blur' }], receiverUserIds: [{ required: true, type: 'array', min: 1, message: t('workReport.receiverRequired'), trigger: 'change' }] }
const openForm = async (row?: Api.WorkReportVO) => { form.value = row?.id ? await Api.getWorkReport(row.id) : empty(); formVisible.value = true; nextTick(() => formRef.value?.clearValidate()) }
const save = async () => { if (!await formRef.value?.validate()) return; saving.value = true; try { form.value.id ? await Api.updateWorkReport(form.value) : await Api.createWorkReport(form.value); message.success(t('common.saveSuccess')); formVisible.value = false; await load() } finally { saving.value = false } }
const submit = async (row: Api.WorkReportVO) => { await message.confirm(t('workReport.submitConfirm')); await Api.submitWorkReport(row.id!); message.success(t('workReport.submitSuccess')); await load() }
const remove = async (row: Api.WorkReportVO) => { await message.delConfirm(); await Api.deleteWorkReport(row.id!); message.success(t('common.delSuccess')); await load() }
const detailVisible = ref(false); const detail = ref<Api.WorkReportVO>(empty()); const openDetail = async (row: Api.WorkReportVO) => { detail.value = await Api.getWorkReport(row.id!); detailVisible.value = true }
onMounted(async () => { users.value = await UserApi.getSimpleUserList(); await load() })
</script>
