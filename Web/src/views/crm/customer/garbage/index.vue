<template>
  <ContentWrap>
    <el-alert :title="t('garbageAdminOnly')" type="warning" :closable="false" show-icon class="mb-16px" />
    <el-form ref="queryFormRef" :model="queryParams" class="-mb-15px" label-width="auto">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('name')" prop="name">
            <el-input v-model="queryParams.name" clearable class="!w-240px" @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('mobile')" prop="mobile">
            <el-input v-model="queryParams.mobile" clearable class="!w-240px" @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('level')" prop="level">
            <el-select v-model="queryParams.level" clearable class="!w-240px">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_LEVEL)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('source')" prop="source">
            <el-select v-model="queryParams.source" clearable class="!w-240px">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_SOURCE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />{{ t('common.search') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe show-overflow-tooltip table-layout="auto">
      <el-table-column :label="t('name')" prop="name" fixed="left" min-width="160">
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="openDetail(scope.row.id)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('source')" prop="source" align="center" min-width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_SOURCE" :value="scope.row.source" />
        </template>
      </el-table-column>
      <el-table-column :label="t('mobile')" prop="mobile" align="center" min-width="120" />
      <el-table-column :label="t('level')" prop="level" align="center" min-width="120">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_LEVEL" :value="scope.row.level" />
        </template>
      </el-table-column>
      <el-table-column :label="t('poolPreviousOwner')" prop="poolPreviousOwnerUserName" align="center" min-width="130" />
      <el-table-column :label="t('poolCycleCount')" prop="poolCycleCount" align="center" min-width="120" />
      <el-table-column :label="t('garbageTime')" prop="garbageTime" :formatter="dateFormatter" align="center" min-width="180" />
      <el-table-column :label="t('garbageReason')" prop="garbageReason" align="center" min-width="220" />
      <el-table-column :label="t('common.action')" align="center" fixed="right" width="220">
        <template #default="scope">
          <TableActions>
            <div class="garbage-actions">
              <el-button
                v-hasPermi="['crm:customer-garbage:manage']"
                link
                type="primary"
                @click="handleRestore(scope.row)"
              >
                {{ t('restoreToPublic') }}
              </el-button>
              <el-button
                v-hasPermi="['crm:customer-garbage:delete']"
                link
                type="danger"
                @click="handlePermanentDelete(scope.row)"
              >
                {{ t('permanentDelete') }}
              </el-button>
            </div>
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
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as CustomerApi from '@/api/crm/customer'
import { customerGarbageRefreshRevision } from './refreshSignal'

defineOptions({ name: 'CrmCustomerGarbage' })

const { t } = useI18n('crm.customer')
const { push } = useRouter()
const message = useMessage()
const loading = ref(false)
const list = ref<CustomerApi.CustomerVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  mobile: '',
  level: undefined,
  source: undefined
})

const openDetail = (id: number) => push({ name: 'CrmCustomerDetail', params: { id } })

let listRequest: Promise<void> | undefined
const getList = () => {
  if (listRequest) return listRequest
  loading.value = true
  listRequest = CustomerApi.getCustomerGarbagePage(queryParams)
    .then((data) => {
      list.value = data.list
      total.value = data.total
    })
    .finally(() => {
      loading.value = false
      listRequest = undefined
    })
  return listRequest
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleRestore = async (row: CustomerApi.CustomerVO) => {
  await message.confirm(t('restoreGarbageConfirm', { name: row.name }))
  await CustomerApi.restoreCustomerFromGarbage(row.id)
  message.success(t('restoreGarbageSuccess'))
  await getList()
}

const handlePermanentDelete = async (row: CustomerApi.CustomerVO) => {
  await message.confirm(t('permanentDeleteConfirm', { name: row.name }))
  await CustomerApi.permanentlyDeleteGarbageCustomer(row.id)
  message.success(t('permanentDeleteSuccess'))
  await getList()
}

// The page is cached by the layout tabs. Refresh on every activation so a customer
// moved from the public pool appears without requiring a browser refresh.
onActivated(getList)
watch(customerGarbageRefreshRevision, getList)
</script>

<style scoped>
.garbage-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  white-space: nowrap;
}

.garbage-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
