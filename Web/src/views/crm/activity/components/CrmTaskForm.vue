<template>
  <Dialog v-model="visible" :title="title" width="640px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <el-form-item :label="t('title')" prop="title">
        <el-input v-model="formData.title" maxlength="256" show-word-limit />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('type')" prop="type">
            <el-select v-model="formData.type" class="w-full">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('priority')" prop="priority">
            <el-select v-model="formData.priority" class="w-full">
              <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('assignee')" prop="assigneeUserId">
        <el-select v-model="formData.assigneeUserId" class="w-full" filterable>
          <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('dueTime')" prop="dueTime">
            <el-date-picker v-model="formData.dueTime" class="w-full" type="datetime" value-format="x" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('remindTime')" prop="remindTime">
            <el-date-picker v-model="formData.remindTime" class="w-full" clearable type="datetime" value-format="x" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('notifyChannels')">
        <el-checkbox v-model="formData.notifySystem">{{ t('notifySystem') }}</el-checkbox>
        <el-checkbox v-model="formData.notifyEmail">{{ t('notifyEmail') }}</el-checkbox>
        <el-checkbox v-model="formData.notifySms">{{ t('notifySms') }}</el-checkbox>
      </el-form-item>
      <el-form-item :label="t('description')" prop="description">
        <el-input v-model="formData.description" type="textarea" :rows="5" maxlength="10000" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitting" @click="submit">{{ t('common.ok') }}</el-button>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as ActivityApi from '@/api/crm/activity'
import * as UserApi from '@/api/system/user'

const props = defineProps<{ bizType: number; bizId: number }>()
const emit = defineEmits<{ (e: 'success'): void }>()
const { t } = useI18n('crm.activity')
const message = useMessage()
const visible = ref(false)
const submitting = ref(false)
const mode = ref<'create' | 'update'>('create')
const formRef = ref()
const users = ref<UserApi.UserVO[]>([])
const formData = ref<ActivityApi.TaskVO>({} as ActivityApi.TaskVO)
const title = computed(() => (mode.value === 'create' ? t('createTask') : t('editTask')))
const typeOptions = computed(() => [
  { value: 1, label: t('typeNormal') },
  { value: 2, label: t('typeFollowUp') }
])
const priorityOptions = computed(() => [
  { value: 1, label: t('priorityLow') },
  { value: 2, label: t('priorityMedium') },
  { value: 3, label: t('priorityHigh') }
])
const validateReminder = (_rule: any, value: any, callback: (error?: Error) => void) => {
  if (value && formData.value.dueTime && Number(value) > Number(formData.value.dueTime)) {
    callback(new Error(t('remindBeforeDue')))
  } else callback()
}
const rules = computed(() => ({
  title: [{ required: true, message: t('titleRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('typeRequired'), trigger: 'change' }],
  priority: [{ required: true, message: t('priorityRequired'), trigger: 'change' }],
  assigneeUserId: [{ required: true, message: t('assigneeRequired'), trigger: 'change' }],
  dueTime: [{ required: true, message: t('dueTimeRequired'), trigger: 'change' }],
  remindTime: [{ validator: validateReminder, trigger: 'change' }]
}))

const reset = () => {
  formData.value = {
    bizType: props.bizType,
    bizId: props.bizId,
    type: 2,
    title: '',
    description: '',
    priority: 2,
    assigneeUserId: undefined as unknown as number,
    dueTime: '',
    remindTime: undefined,
    notifySystem: true,
    notifyEmail: false,
    notifySms: false
  }
  formRef.value?.resetFields()
}
const open = async (type: 'create' | 'update', task?: ActivityApi.TaskVO) => {
  mode.value = type
  visible.value = true
  if (!users.value.length) users.value = await UserApi.getSimpleUserList()
  reset()
  if (task) formData.value = { ...task, bizType: props.bizType, bizId: props.bizId }
}
const submit = async () => {
  if (!(await formRef.value?.validate())) return
  submitting.value = true
  try {
    if (mode.value === 'create') await ActivityApi.createTask(formData.value)
    else await ActivityApi.updateTask(formData.value)
    message.success(mode.value === 'create' ? t('createSuccess') : t('updateSuccess'))
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
defineExpose({ open })
</script>
