<template>
  <ContentWrap :title="t('oa.workRequest.title')">
    <el-button v-hasPermi="['bpm:oa-work-request:create']" type="primary" @click="openCreate">
      <Icon icon="ep:plus" />{{ t('oa.workRequest.create') }}
    </el-button>
    <el-table v-loading="loading" class="mt-12px" :data="list" table-layout="auto">
      <el-table-column prop="id" :label="t('oa.workRequest.id')" min-width="80" />
      <el-table-column prop="title" :label="t('oa.workRequest.titleLabel')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" :label="t('oa.workRequest.content')" min-width="260" show-overflow-tooltip />
      <el-table-column prop="urgency" :label="t('oa.workRequest.urgency')" min-width="100">
        <template #default="{ row }"><el-tag :type="urgencyType(row.urgency)">{{ urgencyText(row.urgency) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" :label="t('oa.workRequest.status')" min-width="110">
        <template #default="{ row }"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="row.status" /></template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('oa.workRequest.createTime')" :formatter="dateFormatter" min-width="170" />
      <el-table-column fixed="right" :label="t('common.operation')" min-width="150">
        <template #default="{ row }">
          <TableActions mode="menu">
            <el-button link type="primary" @click="showDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button v-if="row.processInstanceId" link type="primary" @click="showProgress(row)">{{ t('oa.workRequest.progress') }}</el-button>
            <el-button v-if="row.status === 1 && row.processInstanceId" link type="danger" @click="cancel(row)">{{ t('oa.workRequest.cancel') }}</el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" :description="t('oa.workRequest.empty')" />
    <Dialog v-model="visible" :title="t('oa.workRequest.create')" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('oa.workRequest.titleLabel')" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('oa.workRequest.content')" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="7" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('oa.workRequest.urgency')" prop="urgency">
          <el-radio-group v-model="form.urgency">
            <el-radio-button :value="1">{{ t('oa.workRequest.urgencyNormal') }}</el-radio-button>
            <el-radio-button :value="2">{{ t('oa.workRequest.urgencyUrgent') }}</el-radio-button>
            <el-radio-button :value="3">{{ t('oa.workRequest.urgencyCritical') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('common.confirm') }}</el-button>
      </template>
    </Dialog>
    <Dialog v-model="detailVisible" :title="t('oa.workRequest.detailTitle')" width="720px">
      <el-descriptions v-if="selected" :column="2" border>
        <el-descriptions-item :label="t('oa.workRequest.id')">{{ selected.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.status')"><dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="selected.status" /></el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.titleLabel')" :span="2">{{ selected.title }}</el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.urgency')">{{ urgencyText(selected.urgency) }}</el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.createTime')">{{ formatDate(selected.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.approvedTime')" :span="2">{{ formatDate(selected.approvedTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('oa.workRequest.content')" :span="2"><div class="whitespace-pre-wrap">{{ selected.content }}</div></el-descriptions-item>
      </el-descriptions>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as Api from '@/api/bpm/oaWorkRequest'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'

defineOptions({ name: 'OaWorkRequest' })

const { t } = useI18n('bpm')
const message = useMessage()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const detailVisible = ref(false)
const selected = ref<Api.WorkRequestVO>()
const list = ref<Api.WorkRequestVO[]>([])
const form = reactive<Api.WorkRequestVO>({ title: '', content: '', urgency: 1 })
const formRef = ref()
const rules = {
  title: [{ required: true, message: t('oa.workRequest.titleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('oa.workRequest.contentRequired'), trigger: 'blur' }, { min: 5, max: 10000, message: t('oa.workRequest.contentLength'), trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try { list.value = await Api.getList() } finally { loading.value = false }
}

const openCreate = () => { Object.assign(form, { title: '', content: '', urgency: 1 }); visible.value = true }
const save = async () => {
  if (!await formRef.value?.validate()) return
  saving.value = true
  try {
    await Api.create(form)
    visible.value = false
    message.success(t('common.saveSuccess'))
    await load()
  } finally { saving.value = false }
}
const urgencyText = (value: number) => [t('oa.workRequest.urgencyNormal'), t('oa.workRequest.urgencyUrgent'), t('oa.workRequest.urgencyCritical')][value - 1] || '-'
const urgencyType = (value: number) => value === 3 ? 'danger' : value === 2 ? 'warning' : 'info'
const showDetail = (row: Api.WorkRequestVO) => { selected.value = row; detailVisible.value = true }
const showProgress = (row: Api.WorkRequestVO) => router.push({ name: 'BpmProcessInstanceDetail', query: { id: row.processInstanceId } })
const cancel = async (row: Api.WorkRequestVO) => {
  const { value } = await ElMessageBox.prompt(t('process.instance.cancelReason'), t('process.instance.cancelTitle'), {
    confirmButtonText: t('common.ok'), cancelButtonText: t('common.cancel'),
    inputPattern: /^[\s\S]*\S[\s\S]*$/, inputErrorMessage: t('process.instance.cancelReasonRequired')
  })
  await ProcessInstanceApi.cancelProcessInstanceByStartUser(row.processInstanceId!, value)
  message.success(t('process.instance.cancelSuccess'))
  await load()
}

let initialized = false
onMounted(async () => { await load(); initialized = true })
onActivated(() => { if (initialized) load() })
</script>
