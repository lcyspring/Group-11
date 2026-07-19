<template>
  <ContentWrap>
    <el-alert :title="t('clue.publicPoolDescription')" type="info" show-icon :closable="false" class="mb-16px" />
    <el-form ref="queryFormRef" :model="queryParams" label-width="auto" class="-mb-15px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('clue.name')" prop="name">
            <el-input
              v-model="queryParams.name" :placeholder="t('clue.namePlaceholder')" clearable
              class="!w-240px" @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('customer.mobile')" prop="mobile">
            <el-input
              v-model="queryParams.mobile" :placeholder="t('customer.mobilePlaceholder')" clearable
              class="!w-240px" @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('customer.source')" prop="source">
            <el-select
              v-model="queryParams.source" :placeholder="t('customer.sourcePlaceholder')" clearable
              class="!w-240px">
              <el-option
                v-for="item in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_SOURCE)"
                :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('customer.industryId')" prop="industryId">
            <el-select
              v-model="queryParams.industryId" :placeholder="t('customer.industryPlaceholder')" clearable
              class="!w-240px">
              <el-option
                v-for="item in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_INDUSTRY)"
                :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('customer.level')" prop="level">
            <el-select
              v-model="queryParams.level" :placeholder="t('customer.levelPlaceholder')" clearable
              class="!w-240px">
              <el-option
                v-for="item in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_LEVEL)"
                :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item>
            <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />{{ t('common.search') }}</el-button>
            <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="mb-12px flex gap-8px">
      <el-button
        type="primary" :disabled="selection.length === 0" @click="handleClaim(selection)"
        v-hasPermi="['crm:clue-public:claim']">
        <Icon icon="ep:download" class="mr-5px" />{{ t('clue.claimSelected') }}
      </el-button>
      <el-button
        type="success" :disabled="selection.length === 0" @click="openAssign(selection)"
        v-hasPermi="['crm:clue-public:assign']">
        <Icon icon="ep:user" class="mr-5px" />{{ t('clue.assignSelected') }}
      </el-button>
    </div>
    <el-table
      v-loading="loading" :data="list" row-key="id" stripe show-overflow-tooltip
      @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" reserve-selection />
      <el-table-column :label="t('clue.name')" prop="name" min-width="180" fixed="left">
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="openDetail(scope.row.id)">{{ scope.row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('customer.source')" prop="source" min-width="110">
        <template #default="scope"><dict-tag :type="DICT_TYPE.CRM_CUSTOMER_SOURCE" :value="scope.row.source" /></template>
      </el-table-column>
      <el-table-column :label="t('customer.mobile')" prop="mobile" min-width="130" />
      <el-table-column :label="t('customer.industryId')" prop="industryId" min-width="110">
        <template #default="scope"><dict-tag :type="DICT_TYPE.CRM_CUSTOMER_INDUSTRY" :value="scope.row.industryId" /></template>
      </el-table-column>
      <el-table-column :label="t('customer.level')" prop="level" min-width="110">
        <template #default="scope"><dict-tag :type="DICT_TYPE.CRM_CUSTOMER_LEVEL" :value="scope.row.level" /></template>
      </el-table-column>
      <el-table-column :label="t('clue.poolPreviousOwner')" prop="poolPreviousOwnerUserName" min-width="120">
        <template #default="scope">{{ scope.row.poolPreviousOwnerUserName || t('customer.unassignedOwner') }}</template>
      </el-table-column>
      <el-table-column :label="t('clue.poolEntryTime')" prop="poolEntryTime" :formatter="dateFormatter" min-width="180" />
      <el-table-column :label="t('clue.poolReason')" prop="poolReason" min-width="150">
        <template #default="scope">
          {{ scope.row.poolReasonDetail || resolvePoolReason(scope.row.poolReason) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('clue.poolCycleCount')" prop="poolCycleCount" min-width="110" />
      <el-table-column :label="t('clue.contactLastTime')" prop="contactLastTime" :formatter="dateFormatter" min-width="180" />
      <el-table-column :label="t('common.action')" fixed="right" width="220">
        <template #default="scope">
          <TableActions>
            <el-button link type="primary" @click="handleClaim([scope.row])" v-hasPermi="['crm:clue-public:claim']">
              {{ t('clue.claim') }}
            </el-button>
            <el-button link type="success" @click="openAssign([scope.row])" v-hasPermi="['crm:clue-public:assign']">
              {{ t('clue.assign') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="assignVisible" :title="t('clue.assignTitle')">
    <el-form :model="assignForm" label-width="auto">
      <el-form-item :label="t('clue.assignCount')">{{ assignIds.length }}</el-form-item>
      <el-form-item :label="t('clue.ownerUserId')" required>
        <el-select
          v-model="assignForm.ownerUserId" filterable class="w-1/1"
          :placeholder="t('customer.ownerUserPlaceholder')">
          <el-option v-for="user in userOptions" :key="user.id" :label="user.nickname" :value="user.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="assignLoading" :disabled="!assignForm.ownerUserId" @click="submitAssign">
        {{ t('common.confirm') }}
      </el-button>
      <el-button @click="assignVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { resolveDialogAction } from '@/utils/dialogAction'
import * as ClueApi from '@/api/crm/clue'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'CrmCluePublic' })

const { t } = useI18n('crm')
const message = useMessage()
const { push } = useRouter()
const loading = ref(false)
const list = ref<ClueApi.ClueVO[]>([])
const total = ref(0)
const selection = ref<ClueApi.ClueVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  mobile: undefined,
  source: undefined,
  industryId: undefined,
  level: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ClueApi.getPublicCluePage(queryParams)
    list.value = data.list
    total.value = data.total
    selection.value = []
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
const handleSelectionChange = (rows: ClueApi.ClueVO[]) => {
  selection.value = rows
}
const openDetail = (id: number) => push({ name: 'CrmClueDetail', params: { id } })

const handleClaim = async (rows: ClueApi.ClueVO[]) => {
  if (!rows.length) return
  const confirmation = message.confirm(t('clue.claimConfirm', { count: rows.length }))
  if (!(await resolveDialogAction(confirmation))) return
  await ClueApi.claimPublicClues(rows.map((row) => row.id))
  message.success(t('clue.claimSuccess', { count: rows.length }))
  await getList()
}

const assignVisible = ref(false)
const assignLoading = ref(false)
const assignIds = ref<number[]>([])
const assignForm = reactive<{ ownerUserId?: number }>({ ownerUserId: undefined })
const userOptions = ref<UserApi.UserVO[]>([])
const openAssign = async (rows: ClueApi.ClueVO[]) => {
  if (!rows.length) return
  assignIds.value = rows.map((row) => row.id)
  assignForm.ownerUserId = undefined
  assignVisible.value = true
  if (!userOptions.value.length) userOptions.value = await UserApi.getSimpleUserList()
}
const submitAssign = async () => {
  if (!assignForm.ownerUserId) return
  assignLoading.value = true
  try {
    await ClueApi.assignPublicClues(assignIds.value, assignForm.ownerUserId)
    message.success(t('clue.assignSuccess', { count: assignIds.value.length }))
    assignVisible.value = false
    await getList()
  } finally {
    assignLoading.value = false
  }
}

const resolvePoolReason = (reason?: string) => {
  const reasonMap: Record<string, string> = {
    MANUAL_PUT_POOL: t('clue.poolReasonManual'),
    AUTO_NO_FOLLOW_UP: t('clue.poolReasonNoFollowUp'),
    CREATE_UNASSIGNED: t('clue.poolReasonCreateUnassigned')
  }
  return (reason && reasonMap[reason]) || reason || '-'
}

// 菜单页面使用 KeepAlive 缓存；每次重新进入公共池都重新读取服务端状态。
onActivated(getList)
</script>
