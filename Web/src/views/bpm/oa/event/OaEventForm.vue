<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="680px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item :label="t('oa.event.titleLabel')" prop="title">
        <el-input
          v-model="formData.title"
          maxlength="200"
          show-word-limit
          :placeholder="t('oa.event.titlePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('oa.event.timeRange')" prop="eventRange">
        <el-date-picker
          v-model="formData.eventRange"
          class="!w-100%"
          end-placeholder=""
          range-separator="-"
          start-placeholder=""
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item :label="t('oa.event.location')" prop="location">
        <el-input
          v-model="formData.location"
          maxlength="255"
          :placeholder="t('oa.event.locationPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('oa.event.reminderMinutes')" prop="reminderMinutes">
        <el-input-number
          v-model="formData.reminderMinutes"
          :max="10080"
          :min="0"
          :step="5"
        />
        <span class="ml-8px text-[var(--el-text-color-secondary)]">
          {{ t('oa.event.reminderHint') }}
        </span>
      </el-form-item>
      <el-form-item :label="t('oa.event.allDay')" prop="allDay">
        <el-switch v-model="formData.allDay" />
      </el-form-item>
      <el-form-item :label="t('oa.event.description')" prop="description">
        <el-input
          v-model="formData.description"
          maxlength="2000"
          :rows="4"
          show-word-limit
          type="textarea"
        />
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
import dayjs from 'dayjs'
import type { FormInstance, FormRules } from 'element-plus'
import * as Api from '@/api/bpm/oaEvent'

defineOptions({ name: 'OaEventForm' })

type FormType = 'create' | 'update'
type EventFormData = {
  id?: number
  title: string
  eventRange: [string, string]
  description: string
  allDay: boolean
  location: string
  reminderMinutes: number
}

const { t } = useI18n('bpm')
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<FormType>('create')
const formRef = ref<FormInstance>()

const defaultRange = (): [string, string] => {
  const start = dayjs().add(1, 'hour').startOf('hour')
  return [
    start.format('YYYY-MM-DD HH:mm:ss'),
    start.add(1, 'hour').format('YYYY-MM-DD HH:mm:ss')
  ]
}

const createEmptyForm = (): EventFormData => ({
  title: '',
  eventRange: defaultRange(),
  description: '',
  allDay: false,
  location: '',
  reminderMinutes: 15
})

const formData = reactive<EventFormData>(createEmptyForm())
const formRules = reactive<FormRules<EventFormData>>({
  title: [{ required: true, message: t('oa.event.titleRequired'), trigger: 'blur' }],
  eventRange: [
    {
      validator: (_rule, value: [string, string] | undefined, callback) => {
        if (!value?.[0] || !value?.[1]) {
          callback(new Error(t('oa.event.timeRangeRequired')))
          return
        }
        if (!dayjs(value[1]).isAfter(dayjs(value[0]))) {
          callback(new Error(t('oa.event.timeRangeInvalid')))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
})

const resetForm = () => {
  Object.assign(formData, createEmptyForm())
  formRef.value?.resetFields()
}

const open = (type: FormType, row?: Api.OaEventVO) => {
  resetForm()
  formType.value = type
  dialogTitle.value = t(type === 'create' ? 'oa.event.create' : 'oa.event.updateTitle')
  if (type === 'update' && row) {
    Object.assign(formData, {
      id: row.id,
      title: row.title,
      eventRange: [row.startTime, row.endTime],
      description: row.description ?? '',
      allDay: row.allDay ?? false,
      location: row.location ?? '',
      reminderMinutes: row.reminderMinutes ?? 15
    })
  }
  dialogVisible.value = true
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  const payload: Api.OaEventSaveReqVO = {
    title: formData.title.trim(),
    description: formData.description.trim(),
    startTime: formData.eventRange[0],
    endTime: formData.eventRange[1],
    allDay: formData.allDay,
    location: formData.location.trim(),
    reminderMinutes: formData.reminderMinutes
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await Api.createOaEvent(payload)
      message.success(t('common.createSuccess'))
    } else {
      if (formData.id === undefined) return
      await Api.updateOaEvent({ ...payload, id: formData.id })
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
