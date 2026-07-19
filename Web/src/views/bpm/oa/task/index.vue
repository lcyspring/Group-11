<template>
  <ContentWrap :title="t('oa.task.title')">
    <el-form inline><el-form-item><div class="flex flex-wrap gap-8px"><el-select v-model="status" class="!w-180px" clearable :placeholder="t('oa.task.status')" @change="load"><el-option :value="0" :label="t('oa.task.todo')" /><el-option :value="1" :label="t('oa.task.processing')" /><el-option :value="2" :label="t('oa.task.done')" /></el-select><el-button type="primary" @click="openCreate">{{ t('oa.task.create') }}</el-button></div></el-form-item></el-form>
    <el-empty v-if="!loading && list.length === 0" :description="t('oa.task.empty')"><el-button type="primary" @click="openCreate">{{ t('oa.task.createFirst') }}</el-button></el-empty>
    <el-table v-else v-loading="loading" :data="list"><el-table-column prop="title" :label="t('oa.task.titleLabel')" /><el-table-column prop="dueTime" :label="t('oa.task.dueTime')" /><el-table-column prop="priority" :label="t('oa.task.priority')" /><el-table-column :label="t('oa.task.status')"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column><el-table-column fixed="right" :label="t('common.operation')"><template #default="{ row }"><el-button v-if="row.status === 0" link @click="start(row)">{{ t('oa.task.start') }}</el-button><el-button v-if="row.status === 1" link type="success" @click="complete(row)">{{ t('oa.task.complete') }}</el-button><el-button v-if="row.status < 2" link @click="edit(row)">{{ t('common.edit') }}</el-button><el-button link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button></template></el-table-column></el-table>
    <el-dialog v-model="visible" :title="form.id ? t('common.edit') : t('oa.task.create')"><el-form :model="form"><el-form-item :label="t('oa.task.titleLabel')"><el-input v-model="form.title" /></el-form-item><el-form-item :label="t('oa.task.assignee')"><el-input-number v-model="form.assigneeUserId" :min="1" /></el-form-item><el-form-item :label="t('oa.task.dueTime')"><el-date-picker v-model="form.dueTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item><el-form-item :label="t('oa.task.description')"><el-input v-model="form.description" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="visible = false">{{ t('common.cancel') }}</el-button><el-button type="primary" @click="save">{{ t('common.confirm') }}</el-button></template></el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import * as Api from '@/api/bpm/oaTask'
import dayjs from 'dayjs'
defineOptions({ name: 'OaTask' })
const { t } = useI18n('bpm'); const message = useMessage(); const loading = ref(false); const visible = ref(false); const status = ref<number>(); const list = ref<Api.OaTaskVO[]>([])
const form = reactive<Api.OaTaskVO>({ title: '', assigneeUserId: 1, dueTime: dayjs().add(1, 'day').format('YYYY-MM-DD HH:mm:ss'), priority: 1 })
const load = async () => { loading.value = true; try { list.value = await Api.getOaTaskList(status.value) } finally { loading.value = false } }
const reset = () => Object.assign(form, { id: undefined, title: '', assigneeUserId: 1, dueTime: dayjs().add(1, 'day').format('YYYY-MM-DD HH:mm:ss'), description: '', priority: 1 })
const openCreate = () => { reset(); visible.value = true }; const edit = (row: Api.OaTaskVO) => { Object.assign(form, row); visible.value = true }
const save = async () => { if (form.id) await Api.updateOaTask(form); else await Api.createOaTask(form); visible.value = false; message.success(t('common.saveSuccess')); await load() }
const start = async (row: Api.OaTaskVO) => { await Api.startOaTask(row.id!); await load() }; const complete = async (row: Api.OaTaskVO) => { await Api.completeOaTask(row.id!); await load() }; const remove = async (row: Api.OaTaskVO) => { await message.delConfirm(); await Api.deleteOaTask(row.id!); await load() }
const statusText = (value?: number) => [t('oa.task.todo'), t('oa.task.processing'), t('oa.task.done')][value || 0]
onMounted(load)
</script>
