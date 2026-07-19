<template>
  <div v-loading="loading">
    <div class="flex items-start justify-between">
      <div>
        <!-- 左上：线索基本信息 -->
        <el-col>
          <el-row>
            <span class="text-xl font-bold">{{ clue.name }}</span>
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
      <el-descriptions-item :label="t('customer.source')">
        <dict-tag :type="DICT_TYPE.CRM_CUSTOMER_SOURCE" :value="clue.source" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('customer.mobile')"> {{ clue.mobile }} </el-descriptions-item>
      <el-descriptions-item :label="t('clue.ownerUserName')">
        {{ clue.ownerUserName || t('clue.publicPool') }}
      </el-descriptions-item>
      <el-descriptions-item v-if="clue.poolStatus === 1" :label="t('clue.poolEntryTime')">
        {{ formatDate(clue.poolEntryTime) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="clue.poolStatus === 1" :label="t('clue.poolPreviousOwner')">
        {{ clue.poolPreviousOwnerUserName || t('customer.unassignedOwner') }}
      </el-descriptions-item>
      <el-descriptions-item v-if="clue.poolStatus === 1" :label="t('clue.poolReason')">
        {{ clue.poolReasonDetail || resolvePoolReason(clue.poolReason) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="clue.poolStatus === 1" :label="t('clue.poolCycleCount')">
        {{ clue.poolCycleCount }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('clue.createTime')">
        {{ formatDate(clue.createTime) }}
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import * as ClueApi from '@/api/crm/clue'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CrmClueDetailsHeader' })

const { t } = useI18n('crm') // 国际化

const resolvePoolReason = (reason?: string) => {
  const reasonMap: Record<string, string> = {
    MANUAL_PUT_POOL: t('clue.poolReasonManual'),
    AUTO_NO_FOLLOW_UP: t('clue.poolReasonNoFollowUp'),
    CREATE_UNASSIGNED: t('clue.poolReasonCreateUnassigned')
  }
  return (reason && reasonMap[reason]) || reason || '-'
}

defineProps<{
  clue: ClueApi.ClueVO // 线索信息
  loading: boolean // 加载中
}>()
</script>
