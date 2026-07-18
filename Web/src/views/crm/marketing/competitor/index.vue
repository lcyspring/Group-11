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
        <el-select v-model="queryParams.status" class="!w-140px" clearable>
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />{{ t('common.query') }}
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />{{ t('common.reset') }}
        </el-button>
        <el-button
          v-hasPermi="['crm:competitor:update']"
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
      <el-table-column fixed="left" :label="t('crm.marketing.name')" min-width="160" prop="name" />
      <el-table-column :label="t('crm.marketing.website')" min-width="180" prop="website">
        <template #default="{ row }">
          <el-link v-if="row.website" :href="row.website" target="_blank" type="primary">
            {{ row.website }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('crm.marketing.owner')" min-width="120" prop="ownerUserName" />
      <el-table-column :label="t('crm.marketing.status')" min-width="90" prop="status">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column show-overflow-tooltip :label="t('crm.marketing.strengths')" min-width="180" prop="strengths" />
      <el-table-column show-overflow-tooltip :label="t('crm.marketing.weaknesses')" min-width="180" prop="weaknesses" />
      <el-table-column show-overflow-tooltip :label="t('crm.marketing.strategy')" min-width="180" prop="strategy" />
      <el-table-column :label="t('common.updateTime')" min-width="170" prop="updateTime" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" :label="t('common.action')" width="220">
        <template #default="{ row }">
          <TableActions>
            <el-button
              v-hasPermi="['crm:competitor:update']"
              link
              type="primary"
              @click="openForm('update', row)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:competitor:delete']"
              link
              type="danger"
              @click="handleDelete(row.id)"
            >
              {{ t('common.delete') }}
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
    :title="
      formData.id
        ? t('crm.marketing.updateCompetitor')
        : t('crm.marketing.createCompetitor')
    "
    width="760px"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item :label="t('crm.marketing.name')" prop="name">
        <el-input v-model="formData.name" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.website')" prop="website">
        <el-input v-model="formData.website" maxlength="500" placeholder="https://" />
      </el-form-item>
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
      <el-form-item :label="t('crm.marketing.status')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('crm.marketing.strengths')" prop="strengths">
        <el-input v-model="formData.strengths" maxlength="2000" :rows="3" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.weaknesses')" prop="weaknesses">
        <el-input v-model="formData.weaknesses" maxlength="2000" :rows="3" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.strategy')" prop="strategy">
        <el-input v-model="formData.strategy" maxlength="2000" :rows="3" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item :label="t('crm.marketing.remark')" prop="remark">
        <el-input v-model="formData.remark" maxlength="1000" :rows="2" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">
        {{ t('common.confirm') }}
      </el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">
        {{ t('common.cancel') }}
      </el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import * as MarketingApi from '@/api/crm/marketing'
import * as UserApi from '@/api/system/user'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'CrmCompetitor' })

const { t } = useI18n()
const message = useMessage()
const loading = ref(false)
const formLoading = ref(false)
const list = ref<MarketingApi.MarketingCompetitorVO[]>([])
const total = ref(0)
const userOptions = ref<UserApi.UserVO[]>([])
const queryFormRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  ownerUserId: undefined as number | undefined,
  status: undefined as number | undefined
})
const createEmptyForm = (): MarketingApi.MarketingCompetitorVO => ({
  name: '',
  website: '',
  ownerUserId: undefined,
  status: 0,
  strengths: '',
  weaknesses: '',
  strategy: '',
  remark: ''
})
const formData = ref<MarketingApi.MarketingCompetitorVO>(createEmptyForm())
const validateWebsite = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
  if (!value || /^https?:\/\/[^\s]+$/i.test(value)) {
    callback()
    return
  }
  callback(new Error(t('crm.marketing.websiteInvalid')))
}
const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  website: [{ validator: validateWebsite, trigger: 'blur' }],
  ownerUserId: [{ required: true, message: t('common.required'), trigger: 'change' }],
  status: [{ required: true, message: t('common.required'), trigger: 'change' }]
}))

const getList = async () => {
  loading.value = true
  try {
    const data = await MarketingApi.getCompetitorPage(queryParams)
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

const openForm = (
  mode: 'create' | 'update',
  row?: MarketingApi.MarketingCompetitorVO
) => {
  formData.value = mode === 'update' && row ? { ...row } : createEmptyForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    const updating = Boolean(formData.value.id)
    await MarketingApi.saveCompetitor(formData.value)
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
  await MarketingApi.deleteCompetitor(id)
  message.success(t('common.delSuccess'))
  await getList()
}

onMounted(async () => {
  const [, users] = await Promise.all([getList(), UserApi.getSimpleUserList()])
  userOptions.value = users
})
</script>
