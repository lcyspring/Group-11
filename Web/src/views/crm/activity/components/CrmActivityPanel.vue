<template>
  <div class="crm-activity-panel">
    <el-alert v-if="conversionRecord" type="success" :closable="false" class="mb-16px">
      <template #title>{{ t('migrationCompleted') }}</template>
      <div>
        {{
          t('migrationSummary', {
            followUps: conversionRecord.followUpCount,
            tasks: conversionRecord.taskCount,
            calls: conversionRecord.callCount,
            sms: conversionRecord.smsCount
          })
        }}
        <span v-if="conversionRecord.operatorUserName">
          · {{ conversionRecord.operatorUserName }}</span
        >
        <span> · {{ formatDate(conversionRecord.convertedAt) }}</span>
      </div>
    </el-alert>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="t('tasks')" name="tasks">
        <div class="mb-12px flex items-center justify-between gap-12px">
          <el-select
            v-model="taskQuery.status"
            clearable
            :placeholder="t('statusFilter')"
            class="w-180px"
            @change="queryTasks"
          >
            <el-option
              v-for="item in taskStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button
            v-if="!readonly"
            v-hasPermi="['crm:activity:create']"
            type="primary"
            @click="taskFormRef?.open('create')"
            >{{ t('createTask') }}</el-button
          >
        </div>
        <el-table v-loading="taskLoading" :data="tasks" stripe>
          <el-table-column :label="t('title')" prop="title" min-width="180">
            <template #default="{ row }"
              ><span>{{ row.title }}</span
              ><el-tag v-if="row.sourceClueId" class="ml-8px" size="small" type="info">{{
                t('fromClue', { id: row.sourceClueId })
              }}</el-tag></template
            >
          </el-table-column>
          <el-table-column :label="t('type')" prop="type" min-width="95"
            ><template #default="{ row }">{{ taskTypeLabel(row.type) }}</template></el-table-column
          >
          <el-table-column :label="t('priority')" prop="priority" min-width="90"
            ><template #default="{ row }">{{
              priorityLabel(row.priority)
            }}</template></el-table-column
          >
          <el-table-column :label="t('status')" prop="status" min-width="100"
            ><template #default="{ row }"
              ><el-tag :type="taskStatusTag(row.status)">{{
                taskStatusLabel(row.status)
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column :label="t('assignee')" prop="assigneeUserName" min-width="110" />
          <el-table-column
            :label="t('dueTime')"
            prop="dueTime"
            :formatter="dateFormatter"
            min-width="170"
          />
          <el-table-column
            :label="t('result')"
            prop="result"
            min-width="150"
            show-overflow-tooltip
          />
          <el-table-column :label="t('common.action')" fixed="right" width="140">
            <template #default="{ row }">
              <TableActions mode="menu">
                <el-button link type="primary" @click="historyRef?.open(row.id)">{{
                  t('history')
                }}</el-button>
                <template v-if="!readonly">
                  <el-button
                    v-if="row.status === 0 && row.creator === String(userId)"
                    v-hasPermi="['crm:activity:update']"
                    link
                    type="primary"
                    @click="taskFormRef?.open('update', row)"
                    >{{ t('common.edit') }}</el-button
                  >
                  <el-button
                    v-if="[0, 50].includes(row.status) && row.assigneeUserId === userId"
                    v-hasPermi="['crm:activity:update']"
                    link
                    type="primary"
                    @click="startTask(row)"
                    >{{ t('start') }}</el-button
                  >
                  <el-button
                    v-if="[0, 10, 50].includes(row.status) && row.assigneeUserId === userId"
                    v-hasPermi="['crm:activity:update']"
                    link
                    type="success"
                    @click="completeTask(row)"
                    >{{ t('complete') }}</el-button
                  >
                  <el-button
                    v-if="[0, 10, 50].includes(row.status) && row.assigneeUserId === userId"
                    v-hasPermi="['crm:activity:update']"
                    link
                    type="warning"
                    @click="unableTask(row)"
                    >{{ t('unable') }}</el-button
                  >
                  <el-button
                    v-if="
                      [0, 10, 50].includes(row.status) &&
                      (row.assigneeUserId === userId || row.creator === String(userId))
                    "
                    v-hasPermi="['crm:activity:update']"
                    link
                    type="danger"
                    @click="cancelTask(row)"
                    >{{ t('cancelTask') }}</el-button
                  >
                </template>
              </TableActions>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          v-model:page="taskQuery.pageNo"
          v-model:limit="taskQuery.pageSize"
          :total="taskTotal"
          @pagination="loadTasks"
        />
      </el-tab-pane>

      <el-tab-pane :label="t('calls')" name="calls" lazy>
        <div class="mb-12px flex justify-end"
          ><el-button
            v-if="!readonly"
            v-hasPermi="['crm:activity:create']"
            type="primary"
            @click="callFormRef?.open()"
            >{{ t('createCall') }}</el-button
          ></div
        >
        <el-table v-loading="callLoading" :data="calls" stripe>
          <el-table-column :label="t('direction')" prop="direction" min-width="90"
            ><template #default="{ row }">{{
              row.direction === 1 ? t('callOutbound') : t('callInbound')
            }}</template></el-table-column
          >
          <el-table-column :label="t('phone')" prop="phone" min-width="130" />
          <el-table-column :label="t('status')" prop="status" min-width="100"
            ><template #default="{ row }">{{
              callStatusLabel(row.status)
            }}</template></el-table-column
          >
          <el-table-column
            :label="t('startTime')"
            prop="startTime"
            :formatter="dateFormatter"
            min-width="170"
          />
          <el-table-column :label="t('duration')" prop="durationSeconds" min-width="100"
            ><template #default="{ row }">{{
              durationLabel(row.durationSeconds)
            }}</template></el-table-column
          >
          <el-table-column :label="t('operator')" prop="operatorUserName" min-width="110" />
          <el-table-column
            :label="t('summary')"
            prop="summary"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }"
              ><el-tag v-if="row.sourceClueId" class="mr-8px" size="small" type="info">{{
                t('fromClue', { id: row.sourceClueId })
              }}</el-tag
              >{{ row.summary }}</template
            >
          </el-table-column>
        </el-table>
        <Pagination
          v-model:page="callQuery.pageNo"
          v-model:limit="callQuery.pageSize"
          :total="callTotal"
          @pagination="loadCalls"
        />
      </el-tab-pane>

      <el-tab-pane :label="t('sms')" name="sms" lazy>
        <div class="mb-12px flex justify-end"
          ><el-button
            v-if="!readonly"
            v-hasPermi="['crm:activity:create']"
            type="primary"
            @click="smsFormRef?.open()"
            >{{ t('createSms') }}</el-button
          ></div
        >
        <el-table v-loading="smsLoading" :data="smsRecords" stripe>
          <el-table-column :label="t('direction')" prop="direction" min-width="90"
            ><template #default="{ row }">{{
              row.direction === 1 ? t('smsOutbound') : t('smsInbound')
            }}</template></el-table-column
          >
          <el-table-column :label="t('mobile')" prop="mobile" min-width="130" />
          <el-table-column :label="t('status')" prop="status" min-width="100"
            ><template #default="{ row }">{{
              smsStatusLabel(row.status)
            }}</template></el-table-column
          >
          <el-table-column
            :label="t('occurredTime')"
            prop="occurredTime"
            :formatter="dateFormatter"
            min-width="170"
          />
          <el-table-column :label="t('operator')" prop="operatorUserName" min-width="110" />
          <el-table-column
            :label="t('content')"
            prop="content"
            min-width="260"
            show-overflow-tooltip
          >
            <template #default="{ row }"
              ><el-tag v-if="row.sourceClueId" class="mr-8px" size="small" type="info">{{
                t('fromClue', { id: row.sourceClueId })
              }}</el-tag
              >{{ row.content }}</template
            >
          </el-table-column>
          <el-table-column
            :label="t('failureReason')"
            prop="failureReason"
            min-width="150"
            show-overflow-tooltip
          />
        </el-table>
        <Pagination
          v-model:page="smsQuery.pageNo"
          v-model:limit="smsQuery.pageSize"
          :total="smsTotal"
          @pagination="loadSms"
        />
      </el-tab-pane>
    </el-tabs>

    <CrmTaskForm ref="taskFormRef" :biz-type="bizType" :biz-id="bizId" @success="loadTasks" />
    <CrmCallRecordForm ref="callFormRef" :biz-type="bizType" :biz-id="bizId" @success="loadCalls" />
    <CrmSmsRecordForm ref="smsFormRef" :biz-type="bizType" :biz-id="bizId" @success="loadSms" />
    <CrmTaskHistory ref="historyRef" />
  </div>
</template>

<script setup lang="ts">
import * as ActivityApi from '@/api/crm/activity'
import { BizTypeEnum } from '@/api/crm/permission'
import { useUserStore } from '@/store/modules/user'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import CrmTaskForm from './CrmTaskForm.vue'
import CrmCallRecordForm from './CrmCallRecordForm.vue'
import CrmSmsRecordForm from './CrmSmsRecordForm.vue'
import CrmTaskHistory from './CrmTaskHistory.vue'

const props = withDefaults(defineProps<{ bizType: number; bizId: number; readonly?: boolean }>(), {
  readonly: false
})
const { t } = useI18n('crm.activity')
const message = useMessage()
const userId = useUserStore().getUser.id
const activeTab = ref('tasks')
const taskFormRef = ref<InstanceType<typeof CrmTaskForm>>()
const callFormRef = ref<InstanceType<typeof CrmCallRecordForm>>()
const smsFormRef = ref<InstanceType<typeof CrmSmsRecordForm>>()
const historyRef = ref<InstanceType<typeof CrmTaskHistory>>()

const tasks = ref<ActivityApi.TaskVO[]>([])
const taskTotal = ref(0)
const taskLoading = ref(false)
const taskQuery = reactive({ pageNo: 1, pageSize: 10, status: undefined as number | undefined })
const calls = ref<ActivityApi.CallRecordVO[]>([])
const callTotal = ref(0)
const callLoading = ref(false)
const callQuery = reactive({ pageNo: 1, pageSize: 10 })
const smsRecords = ref<ActivityApi.SmsRecordVO[]>([])
const smsTotal = ref(0)
const smsLoading = ref(false)
const smsQuery = reactive({ pageNo: 1, pageSize: 10 })
const conversionRecord = ref<ActivityApi.ClueConversionRecordVO | null>(null)
const loadedTabs = reactive({ tasks: false, calls: false, sms: false })

const taskStatusOptions = computed(() =>
  [0, 10, 20, 30, 40, 50].map((value) => ({ value, label: t(`taskStatus${value}`) }))
)
const taskStatusLabel = (value: number) => t(`taskStatus${value}`)
const taskStatusTag = (value: number) =>
  ({ 0: 'info', 10: 'warning', 20: 'success', 30: 'danger', 40: 'info', 50: 'danger' })[
    value
  ] as any
const taskTypeLabel = (value: number) => (value === 1 ? t('typeNormal') : t('typeFollowUp'))
const priorityLabel = (value: number) =>
  ({ 1: t('priorityLow'), 2: t('priorityMedium'), 3: t('priorityHigh') })[value] || value
const callStatusLabel = (value: number) =>
  ({ 10: t('callConnected'), 20: t('callMissed'), 30: t('callFailed') })[value] || value
const smsStatusLabel = (value: number) =>
  ({
    0: t('smsPending'),
    10: t('smsSent'),
    20: t('smsDelivered'),
    30: t('smsFailed'),
    40: t('smsReceived')
  })[value] || value
const durationLabel = (seconds?: number) => (seconds ? t('durationSeconds', { seconds }) : '0')

const loadTasks = async () => {
  taskLoading.value = true
  try {
    const data = await ActivityApi.getTaskPage({
      ...taskQuery,
      bizType: props.bizType,
      bizId: props.bizId
    })
    tasks.value = data.list
    taskTotal.value = data.total
    loadedTabs.tasks = true
  } finally {
    taskLoading.value = false
  }
}
const queryTasks = () => {
  taskQuery.pageNo = 1
  loadTasks()
}
const loadCalls = async () => {
  callLoading.value = true
  try {
    const data = await ActivityApi.getCallRecordPage({
      ...callQuery,
      bizType: props.bizType,
      bizId: props.bizId
    })
    calls.value = data.list
    callTotal.value = data.total
    loadedTabs.calls = true
  } finally {
    callLoading.value = false
  }
}
const loadSms = async () => {
  smsLoading.value = true
  try {
    const data = await ActivityApi.getSmsRecordPage({
      ...smsQuery,
      bizType: props.bizType,
      bizId: props.bizId
    })
    smsRecords.value = data.list
    smsTotal.value = data.total
    loadedTabs.sms = true
  } finally {
    smsLoading.value = false
  }
}
const loadConversion = async () => {
  conversionRecord.value =
    props.bizType === BizTypeEnum.CRM_CLUE
      ? await ActivityApi.getConversionRecord(props.bizId)
      : null
}
const handleTabChange = (name: string | number) => {
  if (name === 'calls' && !loadedTabs.calls) loadCalls()
  if (name === 'sms' && !loadedTabs.sms) loadSms()
}
const startTask = async (row: ActivityApi.TaskVO) => {
  await ActivityApi.startTask(row.id!)
  message.success(t('startSuccess'))
  await loadTasks()
}
const completeTask = async (row: ActivityApi.TaskVO) => {
  const { value } = await ElMessageBox.prompt(t('resultPrompt'), t('complete'), {
    inputType: 'textarea'
  })
  await ActivityApi.completeTask(row.id!, value?.trim() || undefined)
  message.success(t('completeSuccess'))
  await loadTasks()
}
const unableTask = async (row: ActivityApi.TaskVO) => {
  const { value } = await ElMessageBox.prompt(t('unableReasonPrompt'), t('unable'), {
    inputType: 'textarea',
    inputValidator: (input) => !!input?.trim() || t('reasonRequired')
  })
  await ActivityApi.markTaskUnable(row.id!, value.trim())
  await loadTasks()
}
const cancelTask = async (row: ActivityApi.TaskVO) => {
  const { value } = await ElMessageBox.prompt(t('cancelReasonPrompt'), t('cancelTask'), {
    inputType: 'textarea',
    inputValidator: (input) => !!input?.trim() || t('reasonRequired')
  })
  await ActivityApi.cancelTask(row.id!, value.trim())
  await loadTasks()
}

const reload = async () => {
  if (!props.bizId) return
  loadedTabs.tasks = false
  loadedTabs.calls = false
  loadedTabs.sms = false
  await Promise.all([loadTasks(), loadConversion()])
  if (activeTab.value === 'calls') await loadCalls()
  if (activeTab.value === 'sms') await loadSms()
}
watch(() => [props.bizType, props.bizId], reload, { immediate: true })
onActivated(() => {
  if (activeTab.value === 'tasks') loadTasks()
  if (activeTab.value === 'calls') loadCalls()
  if (activeTab.value === 'sms') loadSms()
})
defineExpose({ reload })
</script>
