<template>
  <ContentWrap v-loading="loading" :title="t('oa.trip.detail')">
    <el-descriptions :column="2" border>
      <el-descriptions-item :label="t('oa.trip.id')">{{ data.id }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.status')"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="data.status" /></el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.startTime')">{{ formatDate(data.startTime) }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.endTime')">{{ formatDate(data.endTime) }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.days')">{{ data.days }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.destination')">{{ data.destination }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.estimatedExpense')">{{ data.estimatedExpense }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.companions')">{{ companionNames }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.reason')" :span="2">{{ data.reason }}</el-descriptions-item>
      <el-descriptions-item :label="t('oa.trip.attachments')" :span="2"><el-link v-for="url in data.attachmentUrls || []" :key="url" :href="url" target="_blank" type="primary" class="mr-12px">{{ url.split('/').pop() }}</el-link><span v-if="!data.attachmentUrls?.length">-</span></el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as TripApi from '@/api/bpm/trip'
import * as UserApi from '@/api/system/user'
defineOptions({ name: 'BpmOATripDetail' })
const { t } = useI18n('bpm')
const message = useMessage()
const route = useRoute()
const props = defineProps<{ id?: string | number }>()
const loading = ref(false)
const data = ref<TripApi.TripVO>({} as TripApi.TripVO)
const users = ref<any[]>([])
const companionNames = computed(() => (data.value.companionUserIds || []).map((id) => users.value.find((u) => u.id === id)?.nickname || id).join('、') || '-')
const resolveTripId = () => Number(props.id ?? route.query.id)
onMounted(async () => {
  const id = resolveTripId()
  if (!Number.isSafeInteger(id) || id <= 0) {
    message.error(t('process.instance.queryProcessError'))
    return
  }
  loading.value = true
  try {
    const [trip, userList] = await Promise.all([
      TripApi.getTrip(id),
      UserApi.getSimpleUserList()
    ])
    data.value = trip
    users.value = userList
  } finally {
    loading.value = false
  }
})
</script>
