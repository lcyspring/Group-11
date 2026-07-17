<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="query" class="-mb-15px" label-width="auto">
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item :label="t('oa.trip.destination')" prop="destination"><el-input v-model="query.destination" clearable @keyup.enter="search" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item :label="t('oa.trip.status')" prop="status"><el-select v-model="query.status" clearable><el-option v-for="item in getIntDictOptions(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS)" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="8"><el-form-item><el-button @click="search"><Icon icon="ep:search" />{{ t('common.search') }}</el-button><el-button @click="reset"><Icon icon="ep:refresh" />{{ t('common.reset') }}</el-button><el-button v-hasPermi="['bpm:oa-trip:create']" type="primary" @click="create"><Icon icon="ep:plus" />{{ t('oa.trip.create') }}</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" table-layout="auto">
      <el-table-column prop="id" :label="t('oa.trip.id')" />
      <el-table-column prop="destination" :label="t('oa.trip.destination')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="startTime" :label="t('oa.trip.startTime')" :formatter="dateFormatter" min-width="180" />
      <el-table-column prop="endTime" :label="t('oa.trip.endTime')" :formatter="dateFormatter" min-width="180" />
      <el-table-column prop="days" :label="t('oa.trip.days')" />
      <el-table-column prop="estimatedExpense" :label="t('oa.trip.estimatedExpense')" />
      <el-table-column prop="status" :label="t('oa.trip.status')"><template #default="{ row }"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="row.status" /></template></el-table-column>
      <el-table-column fixed="right" :label="t('common.operation')" min-width="130"><template #default="{ row }"><TableActions mode="menu"><el-button link type="primary" @click="detail(row)">{{ t('common.detail') }}</el-button><el-button v-if="row.processInstanceId" link type="primary" @click="progress(row)">{{ t('oa.trip.progress') }}</el-button></TableActions></template></el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as TripApi from '@/api/bpm/trip'

defineOptions({ name: 'BpmOATrip' })
const { t } = useI18n('bpm')
const router = useRouter()
const loading = ref(false)
const list = ref<TripApi.TripVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const query = reactive({ pageNo: 1, pageSize: 10, destination: undefined, status: undefined })
const load = async () => { loading.value = true; try { const data = await TripApi.getTripPage(query); list.value = data.list; total.value = data.total } finally { loading.value = false } }
const search = () => { query.pageNo = 1; load() }
const reset = () => { queryFormRef.value?.resetFields(); search() }
const create = () => router.push({ name: 'OATripCreate' })
const detail = (row: TripApi.TripVO) => router.push({ name: 'OATripDetail', query: { id: row.id } })
const progress = (row: TripApi.TripVO) => router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
onMounted(load)
</script>
