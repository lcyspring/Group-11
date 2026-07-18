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
          <Icon class="mr-5px" icon="ep:plus" />{{ t('action.create') }}
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
              :rows="4"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="t('crm.marketing.trackingLinks')">
            <div class="w-full">
              <div class="mb-8px text-12px text-gray-500">{{ t('crm.marketing.trackingLinksHint') }}</div>
              <el-table :data="formData.links" border>
                <el-table-column :label="t('crm.marketing.linkCode')" min-width="150">
                  <template #default="{ row }"><el-input v-model="row.code" maxlength="32" /></template>
                </el-table-column>
                <el-table-column :label="t('crm.marketing.linkName')" min-width="160">
                  <template #default="{ row }"><el-input v-model="row.name" maxlength="100" /></template>
                </el-table-column>
                <el-table-column :label="t('crm.marketing.targetUrl')" min-width="300">
                  <template #default="{ row }"><el-input v-model="row.targetUrl" maxlength="2000" /></template>
                </el-table-column>
                <el-table-column :label="t('common.action')" width="90">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="removeTrackingLink($index)">{{ t('common.delete') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button class="mt-8px" type="primary" plain @click="addTrackingLink">
                <Icon class="mr-5px" icon="ep:plus" />{{ t('crm.marketing.addTrackingLink') }}
              </el-button>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="save">{{ t('common.confirm') }}</el-button>
      <el-button :disabled="formLoading" @click="formVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>

  <Dialog
    v-model="recipientVisible"
    :title="`${recipientTitle} · ${t('crm.marketing.recipients')}`"
    width="1000px"
    scroll
    max-height="calc(100vh - 150px)"
  >
    <el-form :inline="true" class="mb-10px">
      <el-form-item :label="t('crm.marketing.recipientStatus')">
        <el-select v-model="recipientQuery.status" clearable class="!w-160px" @change="handleRecipientQuery">
          <el-option v-for="item in recipientStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="deliverySyncing" type="primary" @click="syncDeliveryResults">
          <Icon class="mr-5px" icon="ep:refresh" />{{ t('crm.marketing.refreshDeliveryResults') }}
        </el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="deliverySummary" :column="4" border class="mb-16px">
      <el-descriptions-item :label="t('crm.marketing.smsDelivered')">
        {{ deliverySummary.smsDeliveredCount }} / {{ deliverySummary.smsSentCount }}
        ({{ formatMetricRate(deliverySummary.smsDeliveryRate) }})
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.marketing.emailAccepted')">
        {{ deliverySummary.emailAcceptedCount }} / {{ deliverySummary.emailSentCount }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.marketing.emailOpened')">
        {{ deliverySummary.emailOpenedCount }}
        ({{ formatMetricRate(deliverySummary.emailOpenRate) }})
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.marketing.providerPending')">
        {{ deliverySummary.providerPendingCount }} / {{ deliverySummary.unknownCount }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.marketing.uniqueClicks')">
        {{ deliverySummary.uniqueClickCount }} / {{ deliverySummary.trackedRecipientCount }}
        ({{ formatMetricRate(deliverySummary.uniqueClickRate) }})
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.marketing.totalClicks')">
        {{ deliverySummary.totalClickCount }}
      </el-descriptions-item>
    </el-descriptions>
    <el-table v-if="deliverySummary?.links?.length" :data="deliverySummary.links" border class="mb-16px">
      <el-table-column prop="name" :label="t('crm.marketing.linkName')" min-width="150" />
      <el-table-column prop="code" :label="t('crm.marketing.linkCode')" min-width="130" />
      <el-table-column prop="targetUrl" :label="t('crm.marketing.targetUrl')" min-width="260" show-overflow-tooltip />
      <el-table-column :label="t('crm.marketing.uniqueClicks')" min-width="150">
        <template #default="{ row }">
          {{ row.uniqueClickCount }} / {{ row.trackedRecipientCount }}
          ({{ formatMetricRate(row.uniqueClickRate) }})
        </template>
      </el-table-column>
      <el-table-column prop="totalClickCount" :label="t('crm.marketing.totalClicks')" min-width="110" />
    </el-table>
    <el-table v-loading="recipientLoading" :data="recipients" max-height="520" stripe>
      <el-table-column min-width="160" :label="t('crm.marketing.customers')">
        <template #default="{ row }">{{ row.customerName || customerName(row.customerId) }}</template>
      </el-table-column>
      <el-table-column min-width="150" :label="t('crm.marketing.contacts')">
        <template #default="{ row }">{{ row.contactName || contactName(row.contactId) }}</template>
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
      <el-table-column min-width="130" :label="t('crm.marketing.deliveryStatus')">
        <template #default="{ row }"><el-tag>{{ deliveryStatusLabel(row.deliveryStatus, row.channel) }}</el-tag></template>
      </el-table-column>
      <el-table-column
        min-width="170"
        prop="deliveredAt"
        :formatter="dateFormatter"
        :label="t('crm.marketing.deliveredAt')"
      />
      <el-table-column
        min-width="170"
        prop="openedAt"
        :formatter="dateFormatter"
        :label="t('crm.marketing.openedAt')"
      />
      <el-table-column min-width="220" show-overflow-tooltip :label="t('crm.marketing.resultReason')">
        <template #default="{ row }">{{ row.failureReason || row.suppressedReason || '-' }}</template>
      </el-table-column>
      <el-table-column fixed="right" min-width="150" :label="t('common.action')">
        <template #default="{ row }">
          <TableActions>
            <el-button
              v-if="recipientBroadcastEditable && row.status === RecipientStatus.SUPPRESSED"
              v-hasPermi="['crm:marketing-outreach:consent']"
              :disabled="!recipientAddress(row)"
              link type="success" @click="setRecipientConsent(row, 1)"
            >{{ t('crm.marketing.grantConsent') }}</el-button>
            <el-button
              v-if="recipientBroadcastEditable && recipientAddress(row)"
              v-hasPermi="['crm:marketing-outreach:consent']"
              link type="danger" @click="setRecipientConsent(row, 2)"
            >{{ t('crm.marketing.optOut') }}</el-button>
          </TableActions>
        </template>
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
  DeliveryStatus,
  RecipientStatus,
  broadcastActionVisibility,
  channelNeedsEmail,
  channelNeedsSms,
  hasTargets,
  isValidTemplateParams,
  formatMetricRate
} from './outreachManagement.mjs'

defineOptions({ name: 'CrmMarketingOutreach' })

const { t } = useI18n()
const message = useMessage()
const currentUserId = useUserStore().getUser.id
const loading = ref(false)
const formLoading = ref(false)
const recipientLoading = ref(false)
const deliverySyncing = ref(false)
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
const recipientBroadcastStatus = ref<number>()
const recipients = ref<MarketingApi.MarketingRecipientVO[]>([])
const recipientTotal = ref(0)
const deliverySummary = ref<MarketingApi.MarketingDeliverySummaryVO>()

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
  contactIds: [],
  links: []
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
const recipientBroadcastEditable = computed(() =>
  recipientBroadcastStatus.value === BroadcastStatus.DRAFT ||
  recipientBroadcastStatus.value === BroadcastStatus.REJECTED
)
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
const deliveryStatusLabel = (status?: number, channel?: number) => {
  if (status === DeliveryStatus.PROVIDER_PENDING) return t('crm.marketing.providerPending')
  if (status === DeliveryStatus.DELIVERED) return t('crm.marketing.smsDelivered')
  if (status === DeliveryStatus.FAILED) return t('crm.marketing.providerFailed')
  if (status === DeliveryStatus.ACCEPTED) return channel === 2
    ? t('crm.marketing.emailAccepted') : t('crm.marketing.smsDelivered')
  return t('crm.marketing.deliveryUnknown')
}
const statusType = (status?: number) => {
  if (status === BroadcastStatus.SENT || status === BroadcastStatus.READY) return 'success'
  if (status === BroadcastStatus.REJECTED || status === BroadcastStatus.PARTIAL_FAILED) return 'danger'
  if (status === BroadcastStatus.PENDING_REVIEW || status === BroadcastStatus.SENDING) return 'warning'
  return 'info'
}
const isDue = (scheduledAt?: Date | string) => !scheduledAt || new Date(scheduledAt).getTime() <= Date.now()
const contactOptionLabel = (contact: MarketingApi.MarketingTargetOptionsVO['contacts'][number]) =>
  `${customerName(contact.customerId)} · ${contact.name}${contact.mobile ? ` · ${contact.mobile}` : ''}`
const customerName = (id?: number) => customers.value.find((item) => item.id === id)?.name
  ?? t('crm.marketing.deletedCustomer', { id: id ?? '-' })
const contactName = (id?: number) => !id ? '-' : contacts.value.find((item) => item.id === id)?.name
  ?? t('crm.marketing.deletedContact', { id })

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
  const links = formData.value.links || []
  const codes = links.map((item) => item.code.trim().toLowerCase())
  const linksValid = links.every((item) => {
    if (!/^[A-Za-z][A-Za-z0-9_]{0,31}$/.test(item.code.trim()) || !item.name.trim()) return false
    try {
      const target = new URL(item.targetUrl)
      return target.protocol === 'http:' || target.protocol === 'https:'
    } catch {
      return false
    }
  }) && new Set(codes).size === codes.length
  if (!linksValid) {
    message.error(t('crm.marketing.trackingLinkInvalid'))
    return
  }
  formLoading.value = true
  try {
    const id = await MarketingApi.saveBroadcast(formData.value)
    const saved = await MarketingApi.getBroadcast(id)
    message.success(t('crm.marketing.broadcastSaved'))
    formVisible.value = false
    await getList()
    if ((saved.validCount ?? 0) === 0) {
      message.warning(t('crm.marketing.noSendableRecipients'))
      await openRecipients(saved)
    }
  } finally {
    formLoading.value = false
  }
}
const addTrackingLink = () => {
  const links = formData.value.links ?? (formData.value.links = [])
  links.push({ code: '', name: '', targetUrl: '' })
}
const removeTrackingLink = (index: number) => {
  formData.value.links?.splice(index, 1)
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
  recipientBroadcastStatus.value = row.status
  recipientQuery.broadcastId = row.id!
  recipientQuery.pageNo = 1
  recipientQuery.status = undefined
  recipientVisible.value = true
  await Promise.all([getRecipients(), getDeliverySummary()])
}
const recipientAddress = (row: MarketingApi.MarketingRecipientVO) =>
  row.channel === 1 ? row.mobile : row.email
const setRecipientConsent = async (row: MarketingApi.MarketingRecipientVO, status: number) => {
  await MarketingApi.saveConsent({
    customerId: row.customerId,
    contactId: row.contactId,
    channel: row.channel,
    status,
    source: 'broadcast-recipient-result'
  })
  const validCount = await MarketingApi.refreshBroadcastRecipients(recipientQuery.broadcastId)
  await Promise.all([getRecipients(), getDeliverySummary(), getList()])
  message.success(status === 1
    ? t('crm.marketing.consentGranted', { count: validCount })
    : t('crm.marketing.optOutSaved'))
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
const getDeliverySummary = async () => {
  deliverySummary.value = await MarketingApi.getBroadcastDeliverySummary(recipientQuery.broadcastId)
}
const syncDeliveryResults = async () => {
  deliverySyncing.value = true
  try {
    const changed = await MarketingApi.syncBroadcastDeliveryResults(recipientQuery.broadcastId)
    await Promise.all([getRecipients(), getDeliverySummary(), getList()])
    message.success(t('crm.marketing.deliveryRefreshSuccess', { count: changed }))
  } finally {
    deliverySyncing.value = false
  }
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
