<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px">
      <el-form-item :label="t('crm.marketing.name')" prop="name">
        <el-input
          v-model="queryParams.name"
          clearable
          :placeholder="t('common.inputText')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.channel')" prop="channel">
        <el-select v-model="queryParams.channel" clearable class="!w-140px">
          <el-option v-for="item in channels" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.marketing.status')" prop="status">
        <el-select v-model="queryParams.status" clearable class="!w-160px">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}</el-button>
        <el-button v-hasPermi="['crm:marketing-outreach:update']" type="primary" @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />{{ t('common.create') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column fixed="left" min-width="190" prop="name" :label="t('crm.marketing.name')" />
      <el-table-column min-width="100" prop="channel" :label="t('crm.marketing.channel')">
        <template #default="{ row }"><el-tag>{{ channelLabel(row.channel) }}</el-tag></template>
      </el-table-column>
      <el-table-column min-width="120" prop="status" :label="t('crm.marketing.status')">
        <template #default="{ row }">
          <el-tooltip v-if="row.reviewComment" :content="row.reviewComment">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </el-tooltip>
          <el-tag v-else :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="right" min-width="105" prop="validCount" :label="t('crm.marketing.validRecipients')" />
      <el-table-column align="right" min-width="90" prop="suppressedCount" :label="t('crm.marketing.suppressedCount')" />
      <el-table-column align="right" min-width="90" prop="sentCount" :label="t('crm.marketing.sentCount')" />
      <el-table-column align="right" min-width="90" prop="failedCount" :label="t('crm.marketing.failedCount')" />
      <el-table-column
        min-width="170"
        prop="scheduledAt"
        :formatter="dateFormatter"
        :label="t('crm.marketing.scheduledAt')"
      />
      <el-table-column
        min-width="170"
        prop="createTime"
        :formatter="dateFormatter"
        :label="t('common.createTime')"
      />
      <el-table-column align="center" fixed="right" width="150" :label="t('common.action')">
        <template #default="{ row }">
          <TableActions mode="menu">
            <el-button link type="primary" @click="openRecipients(row)">{{ t('crm.marketing.recipients') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).edit"
              v-hasPermi="['crm:marketing-outreach:update']"
              link
              type="primary"
              @click="openForm(row.id)"
            >{{ t('common.edit') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).submit"
              v-hasPermi="['crm:marketing-outreach:update']"
              link
              type="success"
              @click="submitReview(row)"
            >{{ t('crm.marketing.submitReview') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).review && row.creatorUserId !== currentUserId"
              v-hasPermi="['crm:marketing-outreach:review']"
              link
              type="success"
              @click="approve(row)"
            >{{ t('crm.marketing.approve') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).review && row.creatorUserId !== currentUserId"
              v-hasPermi="['crm:marketing-outreach:review']"
              link
              type="danger"
              @click="reject(row)"
            >{{ t('crm.marketing.reject') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).send && isDue(row.scheduledAt)"
              v-hasPermi="['crm:marketing-outreach:send']"
              link
              type="success"
              @click="send(row)"
            >{{ t('crm.marketing.send') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).retry"
              v-hasPermi="['crm:marketing-outreach:send']"
              link
              type="warning"
              @click="retry(row)"
            >{{ t('crm.marketing.retryFailed') }}</el-button>
            <el-button
              v-if="actionsFor(row.status).delete"
              v-hasPermi="['crm:marketing-outreach:update']"
              link
              type="danger"
              @click="deleteDraft(row)"
            >{{ t('common.delete') }}</el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <Dialog
    v-model="formVisible"
    :title="formData.id ? t('crm.marketing.updateBroadcast') : t('crm.marketing.createBroadcast')"
    width="820px"
  >
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="125px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('crm.marketing.name')" prop="name">
            <el-input v-model="formData.name" maxlength="200" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.marketing.channel')" prop="channel">
            <el-select v-model="formData.channel" class="w-full">
              <el-option v-for="item in channels" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.marketing.campaign')" prop="campaignId">
            <el-select v-model="formData.campaignId" clearable filterable class="w-full">
              <el-option v-for="item in campaigns" :key="item.id" :label="`${item.code} · ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.marketing.scheduledAt')" prop="scheduledAt">
            <el-date-picker v-model="formData.scheduledAt" class="!w-full" type="datetime" value-format="x" />
          </el-form-item>
        </el-col>
        <el-col v-if="channelNeedsSms(formData.channel)" :span="12">
          <el-form-item :label="t('crm.marketing.smsTemplateCode')" prop="smsTemplateCode">
            <el-input v-model="formData.smsTemplateCode" maxlength="100" />
          </el-form-item>
        </el-col>
        <el-col v-if="channelNeedsEmail(formData.channel)" :span="12">
          <el-form-item :label="t('crm.marketing.mailTemplateCode')" prop="mailTemplateCode">
            <el-input v-model="formData.mailTemplateCode" maxlength="100" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="t('crm.marketing.customers')" prop="customerIds">
            <el-select v-model="formData.customerIds" class="w-full" collapse-tags filterable multiple>
              <el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="t('crm.marketing.contacts')" prop="contactIds">
            <el-select v-model="formData.contactIds" class="w-full" collapse-tags filterable multiple>
              <el-option
                v-for="item in contacts"
                :key="item.id"
                :label="contactOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="t('crm.marketing.templateParams')" prop="templateParams">
            <el-input
              v-model="formData.templateParams"
              :placeholder="t('crm.marketing.templateParamsHint')"
              maxlength="4000"
              rows="4"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="save">{{ t('common.confirm') }}</el-button>
      <el-button :disabled="formLoading" @click="formVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>

  <Dialog v-model="recipientVisible" :title="`${recipientTitle} · ${t('crm.marketing.recipients')}`" width="1000px">
    <el-form :inline="true" class="mb-10px">
      <el-form-item :label="t('crm.marketing.recipientStatus')">
        <el-select v-model="recipientQuery.status" clearable class="!w-160px" @change="handleRecipientQuery">
          <el-option v-for="item in recipientStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-table v-loading="recipientLoading" :data="recipients" max-height="520" stripe>
      <el-table-column min-width="160" :label="t('crm.marketing.customers')">
        <template #default="{ row }">{{ customerName(row.customerId) }}</template>
      </el-table-column>
      <el-table-column min-width="150" :label="t('crm.marketing.contacts')">
        <template #default="{ row }">{{ contactName(row.contactId) }}</template>
      </el-table-column>
      <el-table-column min-width="90" :label="t('crm.marketing.channel')">
        <template #default="{ row }">{{ channelLabel(row.channel) }}</template>
      </el-table-column>
      <el-table-column min-width="180" :label="t('crm.marketing.address')">
        <template #default="{ row }">{{ row.channel === 1 ? row.mobile : row.email }}</template>
      </el-table-column>
      <el-table-column min-width="120" :label="t('crm.marketing.recipientStatus')">
        <template #default="{ row }"><el-tag>{{ recipientStatusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column min-width="220" show-overflow-tooltip :label="t('crm.marketing.resultReason')">
        <template #default="{ row }">{{ row.failureReason || row.suppressedReason || '-' }}</template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="recipientQuery.pageNo"
      v-model:limit="recipientQuery.pageSize"
      :total="recipientTotal"
      @pagination="getRecipients"
    />
  </Dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import * as MarketingApi from '@/api/crm/marketing'
import { useUserStore } from '@/store/modules/user'
import { dateFormatter } from '@/utils/formatTime'
import {
  BroadcastStatus,
  RecipientStatus,
  broadcastActionVisibility,
  channelNeedsEmail,
  channelNeedsSms,
  hasTargets,
  isValidTemplateParams
} from './outreachManagement.mjs'

defineOptions({ name: 'CrmMarketingOutreach' })

const { t } = useI18n()
const message = useMessage()
const currentUserId = useUserStore().getUser.id
const loading = ref(false)
const formLoading = ref(false)
const recipientLoading = ref(false)
const list = ref<MarketingApi.MarketingBroadcastVO[]>([])
const total = ref(0)
const customers = ref<MarketingApi.MarketingTargetOptionsVO['customers']>([])
const contacts = ref<MarketingApi.MarketingTargetOptionsVO['contacts']>([])
const campaigns = ref<MarketingApi.MarketingCampaignVO[]>([])
const queryFormRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const formVisible = ref(false)
const recipientVisible = ref(false)
const recipientTitle = ref('')
const recipients = ref<MarketingApi.MarketingRecipientVO[]>([])
const recipientTotal = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  channel: undefined as number | undefined,
  status: undefined as number | undefined
})
const recipientQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  broadcastId: 0,
  status: undefined as number | undefined
})
const emptyForm = (): MarketingApi.MarketingBroadcastVO => ({
  name: '',
  channel: 1,
  campaignId: undefined,
  smsTemplateCode: '',
  mailTemplateCode: '',
  templateParams: '',
  scheduledAt: undefined,
  customerIds: [],
  contactIds: []
})
const formData = ref<MarketingApi.MarketingBroadcastVO>(emptyForm())

const channels = computed(() => [
  { value: 1, label: t('crm.marketing.sms') },
  { value: 2, label: t('crm.marketing.email') },
  { value: 3, label: t('crm.marketing.both') }
])
const statuses = computed(() => [
  { value: BroadcastStatus.DRAFT, label: t('crm.marketing.broadcastDraft') },
  { value: BroadcastStatus.PENDING_REVIEW, label: t('crm.marketing.pendingReview') },
  { value: BroadcastStatus.REJECTED, label: t('crm.marketing.rejected') },
  { value: BroadcastStatus.READY, label: t('crm.marketing.readyToSend') },
  { value: BroadcastStatus.SENDING, label: t('crm.marketing.sending') },
  { value: BroadcastStatus.SENT, label: t('crm.marketing.sent') },
  { value: BroadcastStatus.PARTIAL_FAILED, label: t('crm.marketing.partialFailed') },
  { value: BroadcastStatus.CANCELLED, label: t('crm.marketing.cancelled') }
])
const recipientStatuses = computed(() => [
  { value: RecipientStatus.PENDING, label: t('crm.marketing.recipientPending') },
  { value: RecipientStatus.SUPPRESSED, label: t('crm.marketing.recipientSuppressed') },
  { value: RecipientStatus.SENDING, label: t('crm.marketing.sending') },
  { value: RecipientStatus.SENT, label: t('crm.marketing.sent') },
  { value: RecipientStatus.FAILED, label: t('crm.marketing.failedCount') },
  { value: RecipientStatus.RECORDED, label: t('crm.marketing.recorded') }
])
const validateTemplates = (_rule: unknown, _value: string, callback: (error?: Error) => void) => {
  if (channelNeedsSms(formData.value.channel) && !formData.value.smsTemplateCode?.trim()) {
    callback(new Error(t('crm.marketing.smsTemplateRequired')))
  } else if (channelNeedsEmail(formData.value.channel) && !formData.value.mailTemplateCode?.trim()) {
    callback(new Error(t('crm.marketing.mailTemplateRequired')))
  } else callback()
}
const validateTargets = (_rule: unknown, _value: number[], callback: (error?: Error) => void) => {
  callback(hasTargets(formData.value.customerIds, formData.value.contactIds)
    ? undefined
    : new Error(t('crm.marketing.targetRequired')))
}
const validateParams = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  callback(isValidTemplateParams(value) ? undefined : new Error(t('crm.marketing.templateParamsInvalid')))
}
const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  channel: [{ required: true, message: t('common.required'), trigger: 'change' }],
  smsTemplateCode: [{ validator: validateTemplates, trigger: 'blur' }],
  mailTemplateCode: [{ validator: validateTemplates, trigger: 'blur' }],
  customerIds: [{ validator: validateTargets, trigger: 'change' }],
  contactIds: [{ validator: validateTargets, trigger: 'change' }],
  templateParams: [{ validator: validateParams, trigger: 'blur' }]
}))

const actionsFor = (status?: number) => broadcastActionVisibility(status)
const channelLabel = (channel?: number) => channels.value.find((item) => item.value === channel)?.label ?? '-'
const statusLabel = (status?: number) => statuses.value.find((item) => item.value === status)?.label ?? '-'
const recipientStatusLabel = (status?: number) => recipientStatuses.value.find((item) => item.value === status)?.label ?? '-'
const statusType = (status?: number) => {
  if (status === BroadcastStatus.SENT || status === BroadcastStatus.READY) return 'success'
  if (status === BroadcastStatus.REJECTED || status === BroadcastStatus.PARTIAL_FAILED) return 'danger'
  if (status === BroadcastStatus.PENDING_REVIEW || status === BroadcastStatus.SENDING) return 'warning'
  return 'info'
}
const isDue = (scheduledAt?: Date | string) => !scheduledAt || new Date(scheduledAt).getTime() <= Date.now()
const contactOptionLabel = (contact: MarketingApi.MarketingTargetOptionsVO['contacts'][number]) =>
  `${customerName(contact.customerId)} · ${contact.name}${contact.mobile ? ` · ${contact.mobile}` : ''}`
const customerName = (id?: number) => customers.value.find((item) => item.id === id)?.name ?? `#${id ?? '-'}`
const contactName = (id?: number) => !id ? '-' : contacts.value.find((item) => item.id === id)?.name ?? `#${id}`

const getList = async () => {
  loading.value = true
  try {
    const data = await MarketingApi.getBroadcastPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  void getList()
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}
const openForm = async (id?: number) => {
  formData.value = emptyForm()
  formVisible.value = true
  formRef.value?.clearValidate()
  if (!id) return
  formLoading.value = true
  try {
    formData.value = await MarketingApi.getBroadcast(id)
  } finally {
    formLoading.value = false
  }
}
const save = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    await MarketingApi.saveBroadcast(formData.value)
    message.success(t('crm.marketing.broadcastSaved'))
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}
const submitReview = async (row: MarketingApi.MarketingBroadcastVO) => {
  await message.confirm(t('crm.marketing.confirmSubmitReview'))
  await MarketingApi.submitBroadcastReview(row.id!)
  message.success(t('crm.marketing.submittedReview'))
  await getList()
}
const approve = async (row: MarketingApi.MarketingBroadcastVO) => {
  const { value } = await ElMessageBox.prompt(t('crm.marketing.reviewCommentOptional'), t('crm.marketing.approve'), {
    confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel')
  })
  await MarketingApi.approveBroadcast({ id: row.id, comment: value })
  message.success(t('crm.marketing.approved'))
  await getList()
}
const reject = async (row: MarketingApi.MarketingBroadcastVO) => {
  const { value } = await ElMessageBox.prompt(t('crm.marketing.rejectReasonPrompt'), t('crm.marketing.reject'), {
    confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'),
    inputValidator: (value) => Boolean(value?.trim()) || t('crm.marketing.rejectReasonRequired')
  })
  await MarketingApi.rejectBroadcast({ id: row.id, comment: value })
  message.success(t('crm.marketing.rejectedSuccess'))
  await getList()
}
const send = async (row: MarketingApi.MarketingBroadcastVO) => {
  await message.confirm(t('crm.marketing.confirmSend'))
  await MarketingApi.sendBroadcast(row.id!)
  message.success(t('crm.marketing.sentSuccess'))
  await getList()
}
const retry = async (row: MarketingApi.MarketingBroadcastVO) => {
  await message.confirm(t('crm.marketing.confirmRetry'))
  await MarketingApi.retryBroadcast(row.id!)
  message.success(t('crm.marketing.retrySuccess'))
  await getList()
}
const deleteDraft = async (row: MarketingApi.MarketingBroadcastVO) => {
  await message.delConfirm()
  await MarketingApi.deleteBroadcast(row.id!)
  message.success(t('common.delSuccess'))
  await getList()
}
const openRecipients = async (row: MarketingApi.MarketingBroadcastVO) => {
  recipientTitle.value = row.name
  recipientQuery.broadcastId = row.id!
  recipientQuery.pageNo = 1
  recipientQuery.status = undefined
  recipientVisible.value = true
  await getRecipients()
}
const getRecipients = async () => {
  recipientLoading.value = true
  try {
    const data = await MarketingApi.getRecipientPage(recipientQuery)
    recipients.value = data.list || []
    recipientTotal.value = data.total || 0
  } finally {
    recipientLoading.value = false
  }
}
const handleRecipientQuery = () => {
  recipientQuery.pageNo = 1
  void getRecipients()
}

const initialize = async () => {
  const [targetResult, campaignResult] = await Promise.allSettled([
    MarketingApi.getBroadcastTargetOptions(),
    MarketingApi.getCampaignPage({ pageNo: 1, pageSize: 100 })
  ])
  if (targetResult.status === 'fulfilled') {
    customers.value = targetResult.value.customers || []
    contacts.value = targetResult.value.contacts || []
  }
  if (campaignResult.status === 'fulfilled') {
    campaigns.value = campaignResult.value.list || []
  }
  await getList()
}

onMounted(() => {
  void initialize()
})
</script>
