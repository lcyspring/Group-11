<template>
  <Dialog v-model="dialogVisible" :title="t('clue.transformTitle')" width="500px">
    <el-alert
      :closable="false"
      :description="t('clue.transformDescription', { name: clueName })"
      class="mb-18px"
      show-icon
      type="info"
    />
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
      <el-form-item :label="t('contact.name')" prop="contactName">
        <el-input v-model="formData.contactName" :placeholder="t('contact.namePlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('contact.mobile')" prop="contactMobile">
        <el-input v-model="formData.contactMobile" :placeholder="t('contact.mobilePlaceholder')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">
        {{ t('common.confirm') }}
      </el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as ClueApi from '@/api/crm/clue'

const { t } = useI18n('crm')
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const clueName = ref('')
const formRef = ref()
const formData = ref<ClueApi.ClueTransformReqVO>({
  id: 0,
  contactName: '',
  contactMobile: ''
})
const formRules = reactive({
  contactName: [{ required: true, message: t('contact.nameRequired'), trigger: 'blur' }],
  contactMobile: [{ required: true, message: t('contact.mobileRequired'), trigger: 'blur' }]
})

const open = (clue: ClueApi.ClueVO) => {
  clueName.value = clue.name
  formData.value = {
    id: clue.id,
    contactName: '',
    contactMobile: clue.mobile || ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value || !(await formRef.value.validate())) return
  formLoading.value = true
  try {
    await ClueApi.transformClue(formData.value)
    message.success(t('clue.transformSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
