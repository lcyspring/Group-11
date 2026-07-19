<template>
  <ContentWrap v-loading="loading" :title="t('oa.workRequest.title')">
    <el-descriptions :column="2" border>
      <el-descriptions-item :label="t('oa.workRequest.titleLabel')" :span="2">
        {{ detail.title }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('oa.workRequest.urgency')">
        {{ detail.urgency }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('oa.workRequest.status')">
        <dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="detail.status" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('oa.workRequest.content')" :span="2">
        {{ detail.content }}
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import * as WorkRequestApi from '@/api/bpm/oaWorkRequest'

defineOptions({ name: 'BpmOAWorkRequestDetail' })
const props = defineProps<{ id?: number | string }>()
const { t } = useI18n('bpm')
const loading = ref(false)
const detail = ref<WorkRequestApi.WorkRequestVO>({} as WorkRequestApi.WorkRequestVO)

const load = async () => {
  const id = Number(props.id)
  if (!Number.isSafeInteger(id) || id <= 0) return
  loading.value = true
  try {
    detail.value = await WorkRequestApi.get(id)
  } finally {
    loading.value = false
  }
}

watch(() => props.id, load, { immediate: true })
</script>

