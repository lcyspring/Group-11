<template>
  <Dialog v-model="dialogVisible" title="工单详情" width="750px">
    <el-descriptions v-loading="loading" :column="2" border>
      <el-descriptions-item label="工单编号">{{ detail.id }}</el-descriptions-item>
      <el-descriptions-item label="工单类型">{{ detail.typeName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工单标题" :span="2">{{ detail.title }}</el-descriptions-item>
      <el-descriptions-item label="优先级">
        <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_PRIORITY" :value="detail.priority" />
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.WO_WORK_ORDER_STATUS" :value="detail.status" />
      </el-descriptions-item>
      <el-descriptions-item label="处理人">{{ detail.handlerUserName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发起人">{{ detail.submitterUserName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="处理时间">{{ detail.handleTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="预计完成时间">{{ detail.expectedFinishTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="关联客户ID">{{ detail.customerId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="关联商机ID">{{ detail.businessId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工单内容" :span="2">
        <div style="white-space: pre-wrap;">{{ detail.content || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="处理结果" :span="2">
        <div style="white-space: pre-wrap;">{{ detail.result || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ detail.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import * as WorkOrderApi from '@/api/workorder/workOrder'

defineOptions({ name: 'WorkOrderDetail' })

const dialogVisible = ref(false)
const loading = ref(false)
const detail = ref<WorkOrderApi.WorkOrderVO>({})

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  loading.value = true
  try {
    const res = await WorkOrderApi.getWorkOrder(id)
    detail.value = res
  } finally {
    loading.value = false
  }
}
defineExpose({ open })
</script>
