<template>
  <Dialog v-model="visible" :title="data.no || t('workOrder.title')" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item :label="t('workOrder.title')">{{ data.title }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.status')">{{ statusLabel(data.status) }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.customer')">{{ data.customerName }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.handler')">{{ data.handlerUserName }}</el-descriptions-item>
      <el-descriptions-item :label="t('workOrder.description')" :span="2">{{ data.description }}</el-descriptions-item>
      <el-descriptions-item v-if="data.returnReason" :label="t('workOrder.returnReason')" :span="2">{{ data.returnReason }}</el-descriptions-item>
      <el-descriptions-item v-if="data.solution" :label="t('workOrder.solution')" :span="2">{{ data.solution }}</el-descriptions-item>
    </el-descriptions>
    <h3 class="mt-20px mb-12px">{{ t('workOrder.records') }}</h3>
    <el-timeline>
      <el-timeline-item v-for="record in data.records || []" :key="record.id" :timestamp="formatDate(record.createTime)">
        {{ record.operatorUserName }} · {{ actionLabel(record.actionType) }}
        <span v-if="record.remark">：{{ record.remark }}</span>
      </el-timeline-item>
    </el-timeline>
  </Dialog>
</template>
<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import { formatDate } from '@/utils/formatTime'
const { t } = useI18n('crm')
const visible = ref(false)
const data = ref<WorkOrderApi.WorkOrderVO>({} as WorkOrderApi.WorkOrderVO)
const statusLabel = (value?: number) => ({ 10: t('workOrder.statusPending'), 20: t('workOrder.statusProcessing'), 30: t('workOrder.statusCompleted'), 40: t('workOrder.statusReturned') } as Record<number, string>)[value || 0] || value
const actionLabel = (value: number) => ({ 1: t('workOrder.actionCreate'), 2: t('workOrder.actionUpdate'), 3: t('workOrder.start'), 4: t('workOrder.return'), 5: t('workOrder.resubmit'), 6: t('workOrder.complete') } as Record<number, string>)[value] || value
const open = async (id: number) => { visible.value = true; data.value = await WorkOrderApi.getWorkOrder(id) }
defineExpose({ open })
</script>
