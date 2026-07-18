<template>
  <Dialog v-model="dialogVisible" :title="t('starAssessmentTitle')" width="500">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="客户名称">
        <el-input v-model="customerName" disabled />
      </el-form-item>
      <el-form-item label="当前星级">
        <div class="flex items-center">
          <el-rate
            v-model="currentStar"
            disabled
            :max="5"
            show-text
            text-color="#ff9900"
          />
        </div>
      </el-form-item>
      <el-form-item :label="t('star')" prop="star">
        <div class="flex items-center">
          <el-rate
            v-model="form.star"
            :max="5"
            show-text
            text-color="#ff9900"
            @change="handleStarChange"
          />
        </div>
      </el-form-item>
      <el-form-item :label="t('remark')" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          :placeholder="t('starAssessmentRemarkPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleAutoAssess">
        {{ t('autoAssess') }}
      </el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as CustomerApi from '@/api/crm/customer'

defineOptions({ name: 'CustomerStarAssessmentForm' })

const { t } = useI18n('crm.customer')
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const customerName = ref('')
const currentStar = ref(0)

const form = reactive({
  id: undefined as number | undefined,
  star: 3,
  remark: ''
})

const rules = {
  star: [{ required: true, message: t('starRequired'), trigger: 'change' }]
}

const open = (id: number, name: string, star: number) => {
  dialogVisible.value = true
  form.id = id
  customerName.value = name
  currentStar.value = star || 0
  form.star = star || 3
  form.remark = ''
}
defineExpose({ open })

const handleStarChange = (value: number) => {
  form.star = value
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  formLoading.value = true
  try {
    await CustomerApi.assessCustomerStar({
      id: form.id!,
      star: form.star,
      remark: form.remark
    })
    message.success(t('starAssessmentSuccess'))
    dialogVisible.value = false
    emits('success')
  } catch {
  } finally {
    formLoading.value = false
  }
}

const handleAutoAssess = async () => {
  formLoading.value = true
  try {
    const res = await CustomerApi.autoAssessCustomerStar(form.id!)
    form.star = res.data.star
    form.remark = '系统自动评估'
    currentStar.value = res.data.star

    let detail = `评估得分：${res.data.score}分，${res.data.starName}\n`
    if (res.data.dimension) {
      const dim = res.data.dimension
      detail += `成交金额得分：${dim.dealAmountScore || 0}\n`
      detail += `成交次数得分：${dim.dealCountScore || 0}\n`
      detail += `跟进频率得分：${dim.followScore || 0}\n`
      detail += `客户等级得分：${dim.levelScore || 0}\n`
      detail += `客户来源得分：${dim.sourceScore || 0}\n`
      detail += `客户状态得分：${dim.statusScore || 0}`
    }
    message.alert(detail)
  } catch {
  } finally {
    formLoading.value = false
  }
}

const emits = defineEmits(['success'])
</script>