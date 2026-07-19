<template>
  <div v-loading="loading">
    <div class="flex items-start justify-between">
      <div>
        <!-- 左上：客户基本信息 -->
        <el-col>
          <el-row>
            <span class="text-xl font-bold">{{ customer.name }}</span>
          </el-row>
        </el-col>
      </div>
      <div>
        <!-- 右上：按钮 -->
        <slot></slot>
      </div>
    </div>
  </div>
  <ContentWrap class="mt-10px">
    <el-descriptions :column="5" direction="vertical">
      <el-descriptions-item :label="t('level')">
        <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_LEVEL" :value="customer.level" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('lifecycleStatus')">
        <el-tag :type="statusTag">{{ statusLabel }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="t('ownerUserId')">{{ customer.ownerUserName }}</el-descriptions-item>
      <el-descriptions-item :label="t('common.createTime')">
        {{ formatDate(customer.createTime) }}
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import * as CustomerApi from '@/api/crm/customer'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CrmCustomerDetailsHeader' })
const { t } = useI18n('crm.customer') // 国际化
const props = defineProps<{
  customer: CustomerApi.CustomerVO // 客户信息
  loading: boolean // 加载中
}>()
const statusLabels = computed<Record<number, string>>(() => ({
  10: t('lifecyclePotential'),
  20: t('lifecycleIntentional'),
  30: t('lifecycleDeal'),
  40: t('lifecycleLost')
}))
const statusLabel = computed(() =>
  statusLabels.value[props.customer.lifecycleStatus] || t('lifecycleUnknown'))
const statusTags: Record<number, 'info' | 'warning' | 'success' | 'danger'> = {
  10: 'info', 20: 'warning', 30: 'success', 40: 'danger'
}
const statusTag = computed(() => statusTags[props.customer.lifecycleStatus] || 'info')
</script>
