<template>
  <ContentWrap>
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <h3 class="m-0 text-18px font-600">{{ t('oa.event.title') }}</h3>
      <el-button
        v-hasPermi="['bpm:oa-event:create']"
        type="primary"
        @click="openForm('create')"
      >
        <Icon class="mr-5px" icon="ep:plus" />
        {{ t('oa.event.create') }}
      </el-button>
    </div>

    <div class="mb-16px flex flex-wrap items-center gap-8px">
      <el-select v-model="viewMode" class="!w-120px" @change="changeView">
        <el-option :label="t('oa.event.day')" value="day" />
        <el-option :label="t('oa.event.week')" value="week" />
        <el-option :label="t('oa.event.month')" value="month" />
      </el-select>
      <el-date-picker
        v-model="anchor"
        type="date"
        value-format="YYYY-MM-DD"
        @change="changeView"
      />
      <el-button @click="movePeriod(-1)">{{ t('oa.event.previous') }}</el-button>
      <el-button @click="movePeriod(1)">{{ t('oa.event.next') }}</el-button>
      <el-button @click="goToday">{{ t('oa.event.today') }}</el-button>
    </div>

    <el-empty
      v-if="!loading && list.length === 0"
      :description="t('oa.event.empty')"
    >
      <el-button
        v-hasPermi="['bpm:oa-event:create']"
        type="primary"
        @click="openForm('create')"
      >
        {{ t('oa.event.createFirst') }}
      </el-button>
    </el-empty>

    <el-table v-else v-loading="loading" :data="list">
      <el-table-column :label="t('oa.event.titleLabel')" min-width="180" prop="title" />
      <el-table-column :label="t('oa.event.startTime')" min-width="170" prop="startTime" />
      <el-table-column :label="t('oa.event.endTime')" min-width="170" prop="endTime" />
      <el-table-column :label="t('oa.event.location')" min-width="140" prop="location" />
      <el-table-column fixed="right" :label="t('common.action')" width="180">
        <template #default="{ row }">
          <TableActions>
            <el-button
              v-hasPermi="['bpm:oa-event:update']"
              link
              type="primary"
              @click="openForm('update', row)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-hasPermi="['bpm:oa-event:delete']"
              link
              type="danger"
              @click="remove(row)"
            >
              {{ t('common.delete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <OaEventForm ref="formRef" @success="load" />
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import * as Api from '@/api/bpm/oaEvent'
import { resolveDialogAction } from '@/utils/dialogAction'
import OaEventForm from './OaEventForm.vue'

defineOptions({ name: 'OaEvent' })

type ViewMode = 'day' | 'week' | 'month'

const { t } = useI18n('bpm')
const message = useMessage()
const loading = ref(false)
const list = ref<Api.OaEventVO[]>([])
const viewMode = ref<ViewMode>('month')
const anchor = ref(dayjs().format('YYYY-MM-DD'))
const range = ref<[string, string]>(['', ''])
const formRef = ref<InstanceType<typeof OaEventForm>>()

const updateRange = () => {
  const date = dayjs(anchor.value)
  range.value = [
    date.startOf(viewMode.value).format('YYYY-MM-DD HH:mm:ss'),
    date.endOf(viewMode.value).format('YYYY-MM-DD HH:mm:ss')
  ]
}

const load = async () => {
  loading.value = true
  try {
    list.value = await Api.getOaEventList(range.value[0], range.value[1])
  } finally {
    loading.value = false
  }
}

const changeView = async () => {
  updateRange()
  await load()
}

const movePeriod = async (step: number) => {
  anchor.value = dayjs(anchor.value).add(step, viewMode.value).format('YYYY-MM-DD')
  await changeView()
}

const goToday = async () => {
  anchor.value = dayjs().format('YYYY-MM-DD')
  await changeView()
}

const openForm = (type: 'create' | 'update', row?: Api.OaEventVO) =>
  formRef.value?.open(type, row)

const remove = async (row: Api.OaEventVO) => {
  if (row.id === undefined) return
  if (!(await resolveDialogAction(message.delConfirm()))) return
  await Api.deleteOaEvent(row.id)
  message.success(t('common.delSuccess'))
  await load()
}

onMounted(changeView)
</script>
