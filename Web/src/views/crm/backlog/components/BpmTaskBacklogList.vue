<template>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :table-layout="'auto'">
      <el-table-column
        :label="t('process.task.processName')"
        prop="processInstance.name"
        min-width="180"
      />
      <el-table-column :label="t('process.instance.summary')" min-width="200">
        <template #default="{ row }">
          <el-space v-if="row.processInstance?.summary?.length" direction="vertical" alignment="start">
            <el-text v-for="item in row.processInstance.summary" :key="item.key" type="info">
              {{ item.key }}：{{ item.value }}
            </el-text>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('process.instance.initiator')"
        prop="processInstance.startUser.nickname"
        min-width="110"
      />
      <el-table-column :label="t('process.task.currentTask')" prop="name" min-width="150" />
      <el-table-column
        :label="t('process.task.taskTime')"
        prop="createTime"
        :formatter="dateFormatter"
        min-width="180"
      />
      <el-table-column :label="t('common.operation')" fixed="right" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleAudit(row)">
            {{ t('process.task.handle') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="loadData"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import * as TaskApi from '@/api/bpm/task'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'CrmBpmTaskBacklogList' })
const { t } = useI18n('bpm')
const { push } = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<any[]>([])
const query = reactive({ pageNo: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const data = await TaskApi.getTaskTodoPage(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleAudit = (row: any) => {
  push({
    name: 'BpmProcessInstanceDetail',
    query: { id: row.processInstance.id, taskId: row.id }
  })
}
onMounted(loadData)
defineExpose({ loadData })
</script>
