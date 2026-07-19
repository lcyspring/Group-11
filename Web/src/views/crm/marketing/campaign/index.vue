<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px">
      <el-form-item :label="t('crm.marketing.code')" prop="code">
        <el-input
          v-model="queryParams.code"
          clearable
          :placeholder="t('common.inputText')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.name')" prop="name">
        <el-input
          v-model="queryParams.name"
          clearable
          :placeholder="t('common.inputText')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.owner')" prop="ownerUserId">
        <el-select
          v-model="queryParams.ownerUserId"
          class="!w-200px"
          clearable
          filterable
          :placeholder="t('common.selectText')"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="user.nickname"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.marketing.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          class="!w-140px"
          clearable
          :placeholder="t('common.selectText')"
        >
          <el-option
            v-for="status in campaignStatuses"
            :key="status.value"
            :label="status.label"
            :value="status.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"
          ><Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}</el-button
        >
        <el-button @click="resetQuery"
          ><Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}</el-button
        >
        <el-button
          v-hasPermi="['crm:marketing-campaign:update']"
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />{{ t('action.create') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column fixed="left" :label="t('crm.marketing.code')" min-width="140" prop="code" />
      <el-table-column :label="t('crm.marketing.name')" min-width="180" prop="name" />
      <el-table-column :label="t('crm.marketing.owner')" min-width="120" prop="ownerUserName" />
      <el-table-column :label="t('crm.marketing.status')" min-width="100" prop="status">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter"
        :label="t('crm.marketing.startTime')"
        min-width="170"
        prop="startTime"
      />
      <el-table-column
        :formatter="dateFormatter"
        :label="t('crm.marketing.endTime')"
        min-width="170"
        prop="endTime"
      />
      <el-table-column
        align="right"
        :label="t('crm.marketing.budgetAmount')"
        min-width="120"
        prop="budgetAmount"
      />
      <el-table-column
        align="right"
        :label="t('crm.marketing.actualCostAmount')"
        min-width="120"
        prop="actualCostAmount"
      />
      <el-table-column
        :label="t('crm.marketing.targetLeadCount')"
        min-width="110"
        prop="targetLeadCount"
      />
      <el-table-column
        :label="t('crm.marketing.targetCustomerCount')"
        min-width="110"
        prop="targetCustomerCount"
      />
      <el-table-column
        show-overflow-tooltip
        :label="t('crm.marketing.summary')"
        min-width="180"
        prop="summary"
      />
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="140">
        <template #default="{ row }">
          <TableActions mode="menu">
            <el-button
              v-if="actionsFor(row.status).edit"
              v-hasPermi="['crm:marketing-campaign:update']"
              link
              type="primary"
              @click="openForm('update', row.id)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="actionsFor(row.status).start"
              v-hasPermi="['crm:marketing-campaign:update']"
              link
              type="success"
              @click="handleStart(row.id)"
            >
              {{ t('crm.marketing.start') }}
            </el-button>
            <el-button
              v-if="actionsFor(row.status).delete"
              v-hasPermi="['crm:marketing-campaign:delete']"
              link
              type="danger"
              @click="handleDelete(row.id)"
            >
              {{ t('common.delete') }}
            </el-button>
            <el-button
              v-if="actionsFor(row.status).lock"
              v-hasPermi="['crm:marketing-campaign:update']"
              link
              type="warning"
              @click="handleLock(row.id)"
            >
              {{ t('crm.marketing.lock') }}
            </el-button>
            <el-button
              v-if="actionsFor(row.status).terminate"
              v-hasPermi="['crm:marketing-campaign:update']"
              link
              type="danger"
              @click="handleTerminate(row.id)"
            >
              {{ t('crm.marketing.terminate') }}
            </el-button>
            <el-button
              v-if="actionsFor(row.status).complete"
              v-hasPermi="['crm:marketing-campaign:update']"
              link
              type="success"
              @click="handleComplete(row.id)"
            >
              {{ t('crm.marketing.complete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <Dialog
    v-model="dialogVisible"
    :title="formData.id ? t('crm.marketing.updateCampaign') : t('crm.marketing.createCampaign')"
    width="760px"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.code')" prop="code"
            ><el-input v-model="formData.code" maxlength="64" show-word-limit /></el-form-item
        ></el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.name')" prop="name"
            ><el-input v-model="formData.name" maxlength="200" show-word-limit /></el-form-item
        ></el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.marketing.owner')" prop="ownerUserId">
            <el-select
              v-model="formData.ownerUserId"
              class="w-full"
              filterable
              :placeholder="t('common.selectText')"
            >
              <el-option
                v-for="user in userOptions"
                :key="user.id"
                :label="user.nickname"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.budgetAmount')" prop="budgetAmount"
            ><el-input-number
              v-model="formData.budgetAmount"
              class="!w-full"
              :min="0"
              :precision="2" /></el-form-item
        ></el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.startTime')" prop="startTime"
            ><el-date-picker
              v-model="formData.startTime"
              class="!w-full"
              type="datetime"
              value-format="x" /></el-form-item
        ></el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.endTime')" prop="endTime"
            ><el-date-picker
              v-model="formData.endTime"
              class="!w-full"
              type="datetime"
              value-format="x" /></el-form-item
        ></el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.targetLeadCount')" prop="targetLeadCount"
            ><el-input-number
              v-model="formData.targetLeadCount"
              class="!w-full"
              :min="1" /></el-form-item
        ></el-col>
        <el-col :span="12"
          ><el-form-item :label="t('crm.marketing.targetCustomerCount')" prop="targetCustomerCount"
            ><el-input-number
              v-model="formData.targetCustomerCount"
              class="!w-full"
              :min="1" /></el-form-item
        ></el-col>
        <el-col :span="24"
          ><el-form-item :label="t('crm.marketing.description')" prop="description"
            ><el-input
              v-model="formData.description"
              maxlength="2000"
              :rows="4"
              show-word-limit
              type="textarea" /></el-form-item
        ></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">{{
        t('common.confirm')
      }}</el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">{{
        t('common.cancel')
      }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import * as MarketingApi from '@/api/crm/marketing'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { dateFormatter } from '@/utils/formatTime'
import { CampaignStatus, campaignActionVisibility, isEndAfterStart } from './campaignManagement.mjs'

defineOptions({ name: 'CrmMarketingCampaign' })

const { t } = useI18n()
const message = useMessage()
const loading = ref(false)
const formLoading = ref(false)
const list = ref<MarketingApi.MarketingCampaignVO[]>([])
const total = ref(0)
const userOptions = ref<UserApi.UserVO[]>([])
const queryFormRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  ownerUserId: undefined as number | undefined,
  status: undefined as number | undefined
})
const campaignStatuses = computed(() => [
  { value: CampaignStatus.DRAFT, label: t('crm.marketing.statusDraft') },
  { value: CampaignStatus.ACTIVE, label: t('crm.marketing.statusActive') },
  { value: CampaignStatus.LOCKED, label: t('crm.marketing.statusLocked') },
  { value: CampaignStatus.TERMINATED, label: t('crm.marketing.statusTerminated') },
  { value: CampaignStatus.COMPLETED, label: t('crm.marketing.statusCompleted') }
])
const createEmptyForm = (): MarketingApi.MarketingCampaignVO => ({
  code: '',
  name: '',
  ownerUserId: useUserStore().getUser.id,
  startTime: '',
  endTime: '',
  budgetAmount: 0,
  targetLeadCount: undefined,
  targetCustomerCount: undefined,
  description: '',
  relations: []
})
const formData = ref<MarketingApi.MarketingCampaignVO>(createEmptyForm())
const validateEndTime = (
  _rule: unknown,
  value: string | Date,
  callback: (error?: Error) => void
) => {
  if (!isEndAfterStart(formData.value.startTime, value)) {
    callback(new Error(t('crm.marketing.endTimeInvalid')))
    return
  }
  callback()
}
const formRules = computed<FormRules>(() => ({
  code: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  name: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  ownerUserId: [{ required: true, message: t('common.required'), trigger: 'change' }],
  startTime: [{ required: true, message: t('common.required'), trigger: 'change' }],
  endTime: [
    { required: true, message: t('common.required'), trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' }
  ]
}))

const getStatusLabel = (status?: number) =>
  campaignStatuses.value.find((item) => item.value === status)?.label ?? '-'
const actionsFor = (status?: number) => campaignActionVisibility(status)
const getStatusType = (status?: number) =>
  ({
    [CampaignStatus.DRAFT]: 'info',
    [CampaignStatus.ACTIVE]: 'success',
    [CampaignStatus.LOCKED]: 'warning',
    [CampaignStatus.TERMINATED]: 'danger',
    [CampaignStatus.COMPLETED]: 'success'
  })[status ?? 0] as 'info' | 'success' | 'warning' | 'danger' | undefined

const getList = async () => {
  loading.value = true
  try {
    const data = await MarketingApi.getCampaignPage(queryParams)
    list.value = data.list ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openForm = async (mode: 'create' | 'update', id?: number) => {
  formData.value = createEmptyForm()
  dialogVisible.value = true
  if (mode === 'update' && id) {
    formLoading.value = true
    try {
      formData.value = await MarketingApi.getCampaign(id)
    } finally {
      formLoading.value = false
    }
  }
  nextTick(() => formRef.value?.clearValidate())
}
const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    const updating = Boolean(formData.value.id)
    await MarketingApi.saveCampaign(formData.value)
    message.success(t(updating ? 'common.updateSuccess' : 'common.createSuccess'))
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}
const handleDelete = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await MarketingApi.deleteCampaign(id)
  message.success(t('common.delSuccess'))
  await getList()
}
const handleStart = async (id?: number) => {
  if (id) {
    await MarketingApi.startCampaign(id)
    await getList()
  }
}
const handleLock = async (id?: number) => {
  if (id) {
    await MarketingApi.lockCampaign(id)
    await getList()
  }
}
const promptSummary = async (promptKey: string, titleKey: string) => {
  const { value } = await ElMessageBox.prompt(t(promptKey), t(titleKey), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    inputValidator: (input) => Boolean(input?.trim()) || t('common.required')
  })
  return value.trim()
}
const handleTerminate = async (id?: number) => {
  if (!id) return
  const summary = await promptSummary(
    'crm.marketing.terminateSummaryPrompt',
    'crm.marketing.terminate'
  )
  await MarketingApi.terminateCampaign({ id, summary })
  await getList()
}
const handleComplete = async (id?: number) => {
  if (!id) return
  const summary = await promptSummary(
    'crm.marketing.completeSummaryPrompt',
    'crm.marketing.complete'
  )
  await MarketingApi.completeCampaign({ id, summary })
  await getList()
}

onMounted(async () => {
  const [, users] = await Promise.all([getList(), UserApi.getSimpleUserList()])
  userOptions.value = users
})
</script>
