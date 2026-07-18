<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="t('crm.marketing.carePlans')" name="plans" />
      <el-tab-pane :label="t('crm.marketing.careRecords')" name="records" />
      <el-tab-pane :label="t('crm.marketing.upcomingBirthdays')" name="birthdays" />
    </el-tabs>
  </ContentWrap>

  <template v-if="activeTab === 'plans'">
    <ContentWrap>
      <el-form ref="planQueryRef" :inline="true" :model="planQuery" class="-mb-15px">
        <el-form-item :label="t('crm.marketing.code')" prop="code">
          <el-input v-model="planQuery.code" clearable :placeholder="t('common.inputText')" @keyup.enter="handlePlanQuery" />
        </el-form-item>
        <el-form-item :label="t('crm.marketing.name')" prop="name">
          <el-input v-model="planQuery.name" clearable :placeholder="t('common.inputText')" @keyup.enter="handlePlanQuery" />
        </el-form-item>
        <el-form-item :label="t('crm.marketing.ruleType')" prop="ruleType">
          <el-select v-model="planQuery.ruleType" class="!w-170px" clearable :placeholder="t('common.selectText')">
            <el-option v-for="item in ruleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('crm.marketing.enabled')" prop="enabled">
          <el-select v-model="planQuery.enabled" class="!w-120px" clearable :placeholder="t('common.selectText')">
            <el-option :label="t('crm.marketing.enabledYes')" :value="true" />
            <el-option :label="t('crm.marketing.enabledNo')" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handlePlanQuery"><Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}</el-button>
          <el-button @click="resetPlanQuery"><Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}</el-button>
          <el-button v-hasPermi="['crm:customer-care:update']" type="primary" @click="openForm()">
            <Icon class="mr-5px" icon="ep:plus" />{{ t('action.create') }}
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-table v-loading="planLoading" :data="plans" stripe>
        <el-table-column fixed="left" min-width="140" prop="code" :label="t('crm.marketing.code')" />
        <el-table-column min-width="180" prop="name" :label="t('crm.marketing.name')" />
        <el-table-column min-width="130" prop="ruleType" :label="t('crm.marketing.ruleType')">
          <template #default="{ row }"><el-tag>{{ ruleLabel(row.ruleType) }}</el-tag></template>
        </el-table-column>
        <el-table-column min-width="150" :label="t('crm.marketing.triggerCondition')">
          <template #default="{ row }">{{ triggerLabel(row) }}</template>
        </el-table-column>
        <el-table-column min-width="100" prop="channel" :label="t('crm.marketing.channel')">
          <template #default="{ row }">{{ channelLabel(row.channel) }}</template>
        </el-table-column>
        <el-table-column min-width="150" show-overflow-tooltip prop="smsTemplateCode" :label="t('crm.marketing.smsTemplateCode')" />
        <el-table-column min-width="150" show-overflow-tooltip prop="mailTemplateCode" :label="t('crm.marketing.mailTemplateCode')" />
        <el-table-column min-width="100" align="center" prop="enabled" :label="t('crm.marketing.enabled')">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :disabled="!checkPermi(['crm:customer-care:update'])"
              :loading="statusLoadingId === row.id"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column fixed="right" width="110" align="center" :label="t('common.action')">
          <template #default="{ row }">
            <TableActions
              v-if="checkPermi(['crm:customer-care:update', 'crm:customer-care:delete'])"
              mode="menu"
            >
              <el-button v-hasPermi="['crm:customer-care:update']" link type="primary" @click="openForm(row.id)">
                {{ t('common.edit') }}
              </el-button>
              <el-button v-hasPermi="['crm:customer-care:delete']" :disabled="row.enabled" link type="danger" @click="handleDelete(row)">
                {{ t('common.delete') }}
              </el-button>
            </TableActions>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="planQuery.pageNo" v-model:limit="planQuery.pageSize" :total="planTotal" @pagination="getPlans" />
    </ContentWrap>
  </template>

  <template v-else-if="activeTab === 'records'">
    <ContentWrap>
      <el-form ref="recordQueryRef" :inline="true" :model="recordQuery" class="-mb-15px">
        <el-form-item :label="t('crm.marketing.carePlan')" prop="planId">
          <el-select v-model="recordQuery.planId" class="!w-220px" clearable filterable :placeholder="t('common.selectText')">
            <el-option v-for="plan in planOptions" :key="plan.id" :label="plan.name" :value="plan.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('crm.marketing.status')" prop="status">
          <el-select v-model="recordQuery.status" class="!w-150px" clearable :placeholder="t('common.selectText')">
            <el-option v-for="item in recordStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('crm.marketing.eventDate')" prop="eventDate">
          <el-date-picker v-model="recordQuery.eventDate" type="date" value-format="YYYY-MM-DD" :placeholder="t('common.selectText')" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleRecordQuery"><Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}</el-button>
          <el-button @click="resetRecordQuery"><Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap>
      <el-table v-loading="recordLoading" :data="records" stripe>
        <el-table-column min-width="180" prop="planName" :label="t('crm.marketing.carePlan')" />
        <el-table-column min-width="160" prop="customerName" :label="t('crm.marketing.customer')" />
        <el-table-column min-width="140" prop="contactName" :label="t('crm.marketing.contact')" />
        <el-table-column min-width="110" prop="eventDate" :label="t('crm.marketing.eventDate')" />
        <el-table-column min-width="90" prop="channel" :label="t('crm.marketing.channel')">
          <template #default="{ row }">{{ channelLabel(row.channel) }}</template>
        </el-table-column>
        <el-table-column min-width="110" prop="status" :label="t('crm.marketing.status')">
          <template #default="{ row }"><el-tag :type="recordStatusType(row.status)">{{ recordStatusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column min-width="220" show-overflow-tooltip prop="failureReason" :label="t('crm.marketing.resultReason')" />
        <el-table-column min-width="170" prop="sentAt" :formatter="dateFormatter" :label="t('crm.marketing.sentAt')" />
      </el-table>
      <Pagination v-model:page="recordQuery.pageNo" v-model:limit="recordQuery.pageSize" :total="recordTotal" @pagination="getRecords" />
    </ContentWrap>
  </template>

  <template v-else>
    <ContentWrap>
      <el-form ref="birthdayQueryRef" :inline="true" :model="birthdayQuery" class="-mb-15px">
        <el-form-item :label="t('crm.marketing.customerOrContact')" prop="keyword">
          <el-input v-model="birthdayQuery.keyword" clearable :placeholder="t('common.inputText')" @keyup.enter="handleBirthdayQuery" />
        </el-form-item>
        <el-form-item :label="t('crm.marketing.upcomingDays')" prop="upcomingDays">
          <el-input-number v-model="birthdayQuery.upcomingDays" :min="1" :max="366" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleBirthdayQuery"><Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}</el-button>
          <el-button @click="resetBirthdayQuery"><Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap>
      <el-table v-loading="birthdayLoading" :data="birthdays" stripe>
        <el-table-column min-width="180" prop="customerName" :label="t('crm.marketing.customer')" />
        <el-table-column min-width="150" prop="contactName" :label="t('crm.marketing.contact')" />
        <el-table-column min-width="110" prop="birthday" :label="t('crm.marketing.birthday')" />
        <el-table-column min-width="120" prop="nextBirthday" :label="t('crm.marketing.nextBirthday')" />
        <el-table-column min-width="110" prop="daysUntil" :label="t('crm.marketing.daysUntil')" />
        <el-table-column min-width="140" prop="mobile" :label="t('crm.marketing.mobile')" />
        <el-table-column min-width="190" prop="email" :label="t('crm.marketing.emailAddress')" />
      </el-table>
      <Pagination v-model:page="birthdayQuery.pageNo" v-model:limit="birthdayQuery.pageSize" :total="birthdayTotal" @pagination="getBirthdays" />
    </ContentWrap>
  </template>

  <Dialog v-model="dialogVisible" :title="formData.id ? t('crm.marketing.updateCarePlan') : t('crm.marketing.createCarePlan')" width="720px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="130px">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item :label="t('crm.marketing.code')" prop="code"><el-input v-model="formData.code" maxlength="64" show-word-limit /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('crm.marketing.name')" prop="name"><el-input v-model="formData.name" maxlength="200" show-word-limit /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('crm.marketing.ruleType')" prop="ruleType">
          <el-select v-model="formData.ruleType" class="w-full" @change="handleRuleChange">
            <el-option v-for="item in ruleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item></el-col>
        <el-col v-if="formData.ruleType === CareRuleType.HOLIDAY" :span="12"><el-form-item :label="t('crm.marketing.eventMonthDay')" prop="eventMonthDay">
          <el-date-picker v-model="formData.eventMonthDay" class="!w-full" type="date" format="MM-DD" value-format="MM-DD" />
        </el-form-item></el-col>
        <el-col v-if="formData.ruleType === CareRuleType.POST_DEAL_FOLLOW_UP" :span="12"><el-form-item :label="t('crm.marketing.followUpDays')" prop="followUpDays">
          <el-input-number v-model="formData.followUpDays" class="!w-full" :min="1" :max="3650" />
        </el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('crm.marketing.channel')" prop="channel">
          <el-select v-model="formData.channel" class="w-full">
            <el-option :label="t('crm.marketing.sms')" :value="1" />
            <el-option :label="t('crm.marketing.email')" :value="2" />
            <el-option :label="t('crm.marketing.both')" :value="3" />
          </el-select>
        </el-form-item></el-col>
        <el-col v-if="formData.channel !== 2" :span="12"><el-form-item :label="t('crm.marketing.smsTemplateCode')" prop="smsTemplateCode"><el-input v-model="formData.smsTemplateCode" maxlength="100" /></el-form-item></el-col>
        <el-col v-if="formData.channel !== 1" :span="12"><el-form-item :label="t('crm.marketing.mailTemplateCode')" prop="mailTemplateCode"><el-input v-model="formData.mailTemplateCode" maxlength="100" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('crm.marketing.enabled')" prop="enabled"><el-switch v-model="formData.enabled" /></el-form-item></el-col>
        <el-col :span="24"><el-alert :closable="false" show-icon type="info" :title="t('crm.marketing.careConsentHint')" /></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules, TabsPaneContext } from 'element-plus'
import * as MarketingApi from '@/api/crm/marketing'
import { dateFormatter } from '@/utils/formatTime'
import { checkPermi } from '@/utils/permission'
import { CareRecipientStatus, CareRuleType, normalizeCarePlan, validateCarePlan } from './careManagement.mjs'

defineOptions({ name: 'CrmCustomerCare' })

const { t } = useI18n()
const message = useMessage()
const activeTab = ref('plans')
const loadedTabs = reactive(new Set(['plans']))
const planLoading = ref(false)
const recordLoading = ref(false)
const birthdayLoading = ref(false)
const formLoading = ref(false)
const statusLoadingId = ref<number>()
const plans = ref<MarketingApi.CustomerCarePlanVO[]>([])
const planOptions = ref<MarketingApi.CustomerCarePlanVO[]>([])
const records = ref<MarketingApi.CustomerCareRecordVO[]>([])
const birthdays = ref<MarketingApi.CustomerBirthdayVO[]>([])
const planTotal = ref(0)
const recordTotal = ref(0)
const birthdayTotal = ref(0)
const planQueryRef = ref<FormInstance>()
const recordQueryRef = ref<FormInstance>()
const birthdayQueryRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)

const planQuery = reactive({ pageNo: 1, pageSize: 10, code: undefined as string | undefined, name: undefined as string | undefined, ruleType: undefined as number | undefined, enabled: undefined as boolean | undefined })
const recordQuery = reactive({ pageNo: 1, pageSize: 10, planId: undefined as number | undefined, status: undefined as number | undefined, eventDate: undefined as string | undefined })
const birthdayQuery = reactive({ pageNo: 1, pageSize: 10, keyword: undefined as string | undefined, upcomingDays: 30 })

const emptyForm = (): MarketingApi.CustomerCarePlanVO => ({ code: '', name: '', ruleType: CareRuleType.BIRTHDAY, eventMonthDay: undefined, followUpDays: 7, channel: 1, smsTemplateCode: '', mailTemplateCode: '', enabled: false })
const formData = reactive<MarketingApi.CustomerCarePlanVO>(emptyForm())

const ruleOptions = computed(() => [
  { value: CareRuleType.BIRTHDAY, label: t('crm.marketing.birthday') },
  { value: CareRuleType.HOLIDAY, label: t('crm.marketing.holiday') },
  { value: CareRuleType.POST_DEAL_FOLLOW_UP, label: t('crm.marketing.postDealFollowUp') }
])
const recordStatusOptions = computed(() => [
  { value: CareRecipientStatus.PENDING, label: t('crm.marketing.recipientPending') },
  { value: CareRecipientStatus.SENDING, label: t('crm.marketing.sending') },
  { value: CareRecipientStatus.SENT, label: t('crm.marketing.sent') },
  { value: CareRecipientStatus.FAILED, label: t('crm.marketing.failedCount') },
  { value: CareRecipientStatus.SUPPRESSED, label: t('crm.marketing.recipientSuppressed') },
  { value: CareRecipientStatus.RECORDED, label: t('crm.marketing.recorded') }
])
const formRules: FormRules = {
  code: [{ required: true, message: () => t('crm.marketing.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: () => t('crm.marketing.nameRequired'), trigger: 'blur' }],
  ruleType: [{ required: true, message: () => t('crm.marketing.ruleRequired'), trigger: 'change' }],
  channel: [{ required: true, message: () => t('crm.marketing.channelRequired'), trigger: 'change' }]
}

const getPlans = async () => {
  planLoading.value = true
  try {
    const result = await MarketingApi.getCarePlanPage(planQuery)
    plans.value = result.list || []
    planTotal.value = result.total || 0
  } finally { planLoading.value = false }
}
const loadPlanOptions = async () => {
  const result = await MarketingApi.getCarePlanPage({ pageNo: 1, pageSize: 100 })
  planOptions.value = result.list || []
}
const getRecords = async () => {
  recordLoading.value = true
  try {
    const result = await MarketingApi.getCareRecordPage(recordQuery)
    records.value = result.list || []
    recordTotal.value = result.total || 0
  } finally { recordLoading.value = false }
}
const getBirthdays = async () => {
  birthdayLoading.value = true
  try {
    const result = await MarketingApi.getCustomerBirthdayPage(birthdayQuery)
    birthdays.value = result.list || []
    birthdayTotal.value = result.total || 0
  } finally { birthdayLoading.value = false }
}
const handleTabChange = async (tab: TabsPaneContext['paneName']) => {
  const name = String(tab)
  if (loadedTabs.has(name)) return
  loadedTabs.add(name)
  if (name === 'records') await Promise.all([loadPlanOptions(), getRecords()])
  if (name === 'birthdays') await getBirthdays()
}
const handlePlanQuery = () => { planQuery.pageNo = 1; void getPlans() }
const resetPlanQuery = () => { planQueryRef.value?.resetFields(); handlePlanQuery() }
const handleRecordQuery = () => { recordQuery.pageNo = 1; void getRecords() }
const resetRecordQuery = () => { recordQueryRef.value?.resetFields(); handleRecordQuery() }
const handleBirthdayQuery = () => { birthdayQuery.pageNo = 1; void getBirthdays() }
const resetBirthdayQuery = () => { birthdayQueryRef.value?.resetFields(); birthdayQuery.upcomingDays = 30; handleBirthdayQuery() }

const openForm = async (id?: number) => {
  Object.assign(formData, emptyForm())
  formRef.value?.clearValidate()
  dialogVisible.value = true
  if (!id) return
  formLoading.value = true
  try { Object.assign(formData, await MarketingApi.getCarePlan(id)) }
  finally { formLoading.value = false }
}
const handleRuleChange = () => {
  formData.eventMonthDay = undefined
  formData.followUpDays = formData.ruleType === CareRuleType.POST_DEAL_FOLLOW_UP ? 7 : undefined
  formRef.value?.clearValidate(['eventMonthDay', 'followUpDays'])
}
const submitForm = async () => {
  if (!(await formRef.value?.validate())) return
  const validation = validateCarePlan(formData)
  if (validation) { message.error(t(`crm.marketing.careValidation.${validation}`)); return }
  formLoading.value = true
  try {
    await MarketingApi.saveCarePlan(normalizeCarePlan(formData))
    message.success(t(formData.id ? 'common.updateSuccess' : 'common.createSuccess'))
    dialogVisible.value = false
    await getPlans()
  } finally { formLoading.value = false }
}
const handleStatusChange = async (row: MarketingApi.CustomerCarePlanVO) => {
  statusLoadingId.value = row.id
  try {
    await MarketingApi.updateCarePlanStatus(row.id!, row.enabled)
    message.success(t('common.updateSuccess'))
  } catch (error) {
    row.enabled = !row.enabled
    throw error
  } finally { statusLoadingId.value = undefined }
}
const handleDelete = async (row: MarketingApi.CustomerCarePlanVO) => {
  await message.delConfirm()
  await MarketingApi.deleteCarePlan(row.id!)
  message.success(t('common.delSuccess'))
  await getPlans()
}

const ruleLabel = (value: number) => ruleOptions.value.find((item) => item.value === value)?.label || String(value)
const channelLabel = (value: number) => value === 1 ? t('crm.marketing.sms') : value === 2 ? t('crm.marketing.email') : t('crm.marketing.both')
const triggerLabel = (row: MarketingApi.CustomerCarePlanVO) => row.ruleType === CareRuleType.BIRTHDAY
  ? t('crm.marketing.birthdayToday')
  : row.ruleType === CareRuleType.HOLIDAY ? row.eventMonthDay || '-'
    : t('crm.marketing.afterDealDays', { days: row.followUpDays })
const recordStatusLabel = (status: number) => recordStatusOptions.value.find((item) => item.value === status)?.label || String(status)
const recordStatusType = (status: number) => status === CareRecipientStatus.FAILED ? 'danger'
  : status === CareRecipientStatus.SUPPRESSED ? 'warning'
    : status === CareRecipientStatus.SENT || status === CareRecipientStatus.RECORDED ? 'success' : 'info'

onMounted(() => { void getPlans() })
</script>
