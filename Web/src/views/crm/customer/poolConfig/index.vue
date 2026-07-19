<template>
  <doc-alert title="【客户】客户管理、公海客户" url="https://doc.iocoder.cn/crm/customer/" />
  <doc-alert title="【通用】数据权限" url="https://doc.iocoder.cn/crm/permission/" />

  <ContentWrap>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="160px"
      v-loading="formLoading"
    >
      <el-card shadow="never">
        <!-- 操作 -->
        <template #header>
          <div class="flex items-center justify-between">
            <CardTitle :title="t('poolConfig.title')" />
            <el-button
              type="primary"
              @click="onSubmit"
              v-hasPermi="['crm:customer-pool-config:update']"
            >
              {{ t('common.save') }}
            </el-button>
          </div>
        </template>
        <!-- 表单 -->
        <el-form-item :label="t('poolConfig.enabled')" prop="enabled">
          <el-radio-group v-model="formData.enabled" class="ml-4">
            <el-radio :value="false" size="large">{{ t('poolConfig.notEnabled') }}</el-radio>
            <el-radio :value="true" size="large">{{ t('poolConfig.enabledText') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <div v-if="formData.enabled">
          <el-divider content-position="left">{{ t('poolConfig.expirySection') }}</el-divider>
          <el-form-item :label="t('poolConfig.contactExpireLabel')" prop="contactExpireDays">
            <el-input-number v-model="formData.contactExpireDays" :min="1" :max="3650" />
            <span class="ml-8px text-gray-500">{{ t('poolConfig.daysUnit') }}</span>
          </el-form-item>
          <el-form-item :label="t('poolConfig.dealExpireLabel')" prop="dealExpireDays">
            <el-input-number v-model="formData.dealExpireDays" :min="1" :max="3650" />
            <span class="ml-8px text-gray-500">{{ t('poolConfig.daysUnit') }}</span>
          </el-form-item>
          <el-form-item :label="t('poolConfig.highValueLevelThreshold')" prop="highValueLevelThreshold">
            <el-input-number v-model="formData.highValueLevelThreshold" :min="1" :max="5" />
          </el-form-item>
          <el-form-item :label="t('poolConfig.highValueExpireMultiplier')" prop="highValueExpireMultiplier">
            <el-input-number v-model="formData.highValueExpireMultiplier" :min="1" :max="10" />
          </el-form-item>
          <el-form-item :label="t('poolConfig.protectionRules')">
            <el-checkbox v-model="formData.protectActiveBusiness">
              {{ t('poolConfig.protectActiveBusiness') }}
            </el-checkbox>
            <el-checkbox v-model="formData.protectActiveContract">
              {{ t('poolConfig.protectActiveContract') }}
            </el-checkbox>
          </el-form-item>

          <el-divider content-position="left">{{ t('poolConfig.claimSection') }}</el-divider>
          <el-form-item :label="t('poolConfig.dailyClaimLimit')" prop="dailyClaimLimit">
            <el-input-number v-model="formData.dailyClaimLimit" :min="1" :max="1000" />
          </el-form-item>
          <el-form-item :label="t('poolConfig.repeatClaimCooldownDays')" prop="repeatClaimCooldownDays">
            <el-input-number v-model="formData.repeatClaimCooldownDays" :min="0" :max="3650" />
            <span class="ml-8px text-gray-500">{{ t('poolConfig.daysUnit') }}</span>
          </el-form-item>

          <el-divider content-position="left">{{ t('poolConfig.schedulerSection') }}</el-divider>
          <el-form-item :label="t('poolConfig.autoPoolBatchSize')" prop="autoPoolBatchSize">
            <el-input-number
              v-model="formData.autoPoolBatchSize"
              :min="1"
              :max="formData.autoPoolMaxBatchSize ?? 1"
            />
            <span class="ml-8px text-gray-500">
              {{ t('poolConfig.autoPoolBatchMaxHint', { max: formData.autoPoolMaxBatchSize ?? 1 }) }}
            </span>
          </el-form-item>
          <el-form-item :label="t('poolConfig.notifyEnabled')" prop="notifyEnabled">
            <el-radio-group
              v-model="formData.notifyEnabled"
              class="ml-4"
            >
              <el-radio :value="false" size="large">{{ t('poolConfig.notNotify') }}</el-radio>
              <el-radio :value="true" size="large">{{ t('poolConfig.notify') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <div v-if="formData.notifyEnabled">
            <el-form-item prop="notifyDays">
              {{ t('poolConfig.notifyDaysBefore') }} <el-input-number class="mx-2" v-model="formData.notifyDays" /> {{ t('poolConfig.notifyDaysAfter') }}
            </el-form-item>
          </div>
        </div>
      </el-card>
    </el-form>
  </ContentWrap>
</template>
<script setup lang="ts">
import * as CustomerPoolConfigApi from '@/api/crm/customer/poolConfig'
import { CardTitle } from '@/components/Card'

defineOptions({ name: 'CrmCustomerPoolConfig' })

const message = useMessage() // 消息弹窗
const { t } = useI18n('crm.customer') // 国际化

const formLoading = ref(false)
const formData = ref({
  enabled: false,
  contactExpireDays: undefined,
  dealExpireDays: undefined,
  notifyEnabled: false,
  notifyDays: undefined,
  dailyClaimLimit: undefined,
  repeatClaimCooldownDays: undefined,
  highValueLevelThreshold: undefined,
  highValueExpireMultiplier: undefined,
  protectActiveBusiness: false,
  protectActiveContract: false,
  autoPoolBatchSize: undefined,
  autoPoolMaxBatchSize: undefined
})
const formRules = reactive({
  enabled: [{ required: true, message: t('poolConfig.enabledRequired'), trigger: 'change' }],
  contactExpireDays: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  dealExpireDays: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  notifyDays: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  dailyClaimLimit: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  repeatClaimCooldownDays: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  highValueLevelThreshold: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  highValueExpireMultiplier: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }],
  autoPoolBatchSize: [{ required: true, message: t('poolConfig.required'), trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 获取配置 */
const getConfig = async () => {
  try {
    formLoading.value = true
    const data = await CustomerPoolConfigApi.getCustomerPoolConfig()
    if (data === null) {
      return
    }
    formData.value = data
  } finally {
    formLoading.value = false
  }
}

/** 提交配置 */
const onSubmit = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = { ...formData.value }
    delete data.autoPoolMaxBatchSize
    await CustomerPoolConfigApi.saveCustomerPoolConfig(data)
    message.success(t('common.updateSuccess'))
    await getConfig()
    formLoading.value = false
  } finally {
    formLoading.value = false
  }
}

onMounted(() => {
  getConfig()
})
</script>
