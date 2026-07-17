<template>
  <ContentWrap :title="t('oa.event.title')">
    <el-form inline :model="form"><el-form-item :label="t('oa.event.titleLabel')"><el-input v-model="form.title" /></el-form-item><el-form-item><el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" /><el-button type="primary" @click="save">{{ form.id ? t('common.update') : t('common.create') }}</el-button><el-button @click="reset">{{ t('common.reset') }}</el-button></el-form-item></el-form>
    <el-table v-loading="loading" :data="list"><el-table-column prop="title" :label="t('oa.event.titleLabel')" /><el-table-column prop="startTime" :label="t('oa.event.startTime')" /><el-table-column prop="endTime" :label="t('oa.event.endTime')" /><el-table-column prop="location" :label="t('oa.event.location')" /><el-table-column fixed="right" :label="t('common.action')"><template #default="{ row }"><el-button link @click="edit(row)">{{ t('common.edit') }}</el-button><el-button link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button></template></el-table-column></el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import * as Api from '@/api/bpm/oaEvent'
defineOptions({ name: 'OaEvent' })
const { t } = useI18n('bpm'); const message = useMessage(); const loading = ref(false); const list = ref<Api.OaEventVO[]>([])
const range = ref<[string, string]>([dayjs().startOf('month').format('YYYY-MM-DD HH:mm:ss'), dayjs().endOf('month').format('YYYY-MM-DD HH:mm:ss')])
const form = reactive<Api.OaEventVO>({ title: '', startTime: range.value[0], endTime: range.value[1], location: '' })
const load = async () => { loading.value = true; try { list.value = await Api.getOaEventList(range.value[0], range.value[1]) } finally { loading.value = false } }
const reset = () => { Object.assign(form, { id: undefined, title: '', startTime: range.value[0], endTime: range.value[1], location: '' }); load() }
const edit = (row: Api.OaEventVO) => Object.assign(form, row)
const save = async () => { form.startTime = range.value[0]; form.endTime = range.value[1]; if (form.id) await Api.updateOaEvent(form); else await Api.createOaEvent(form); message.success(t('common.saveSuccess')); reset() }
const remove = async (row: Api.OaEventVO) => { await message.delConfirm(); await Api.deleteOaEvent(row.id!); await load() }
onMounted(load)
</script>
