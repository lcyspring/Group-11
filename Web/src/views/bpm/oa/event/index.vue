<template>
  <ContentWrap :title="t('oa.event.title')">
    <el-form inline :model="form"><el-form-item :label="t('oa.event.titleLabel')"><el-input v-model="form.title" /></el-form-item><el-form-item><el-date-picker v-model="eventRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" /><el-button type="primary" @click="save">{{ form.id ? t('common.update') : t('common.create') }}</el-button><el-button @click="reset">{{ t('common.reset') }}</el-button></el-form-item></el-form>
    <div class="mb-12px flex gap-8px"><el-select v-model="viewMode" class="!w-120px" @change="changeView"><el-option value="day" :label="t('oa.event.day')" /><el-option value="week" :label="t('oa.event.week')" /><el-option value="month" :label="t('oa.event.month')" /></el-select><el-date-picker v-model="anchor" type="date" value-format="YYYY-MM-DD" @change="changeView" /><el-button @click="movePeriod(-1)">{{ t('oa.event.previous') }}</el-button><el-button @click="movePeriod(1)">{{ t('oa.event.next') }}</el-button><el-button @click="goToday">{{ t('oa.event.today') }}</el-button></div>
    <el-empty v-if="!loading && list.length === 0" :description="t('oa.event.empty')"><el-button type="primary" @click="focusCreate">{{ t('oa.event.createFirst') }}</el-button></el-empty>
    <el-table v-else v-loading="loading" :data="list"><el-table-column prop="title" :label="t('oa.event.titleLabel')" /><el-table-column prop="startTime" :label="t('oa.event.startTime')" /><el-table-column prop="endTime" :label="t('oa.event.endTime')" /><el-table-column prop="location" :label="t('oa.event.location')" /><el-table-column fixed="right" :label="t('common.action')"><template #default="{ row }"><el-button link @click="edit(row)">{{ t('common.edit') }}</el-button><el-button link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button></template></el-table-column></el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import * as Api from '@/api/bpm/oaEvent'
import dayjs from 'dayjs'
defineOptions({ name: 'OaEvent' })
const { t } = useI18n('bpm'); const message = useMessage(); const loading = ref(false); const list = ref<Api.OaEventVO[]>([])
type ViewMode = 'day' | 'week' | 'month'
const viewMode = ref<ViewMode>('month')
const anchor = ref(dayjs().format('YYYY-MM-DD'))
const range = ref<[string, string]>(['', ''])
const eventRange = ref<[string, string]>([dayjs().add(1, 'hour').startOf('hour').format('YYYY-MM-DD HH:mm:ss'), dayjs().add(2, 'hour').startOf('hour').format('YYYY-MM-DD HH:mm:ss')])
const form = reactive<Api.OaEventVO>({ title: '', startTime: eventRange.value[0], endTime: eventRange.value[1], location: '' })
const updateRange = () => {
  const date = dayjs(anchor.value)
  range.value = [date.startOf(viewMode.value).format('YYYY-MM-DD HH:mm:ss'), date.endOf(viewMode.value).format('YYYY-MM-DD HH:mm:ss')]
}
const load = async () => { loading.value = true; try { list.value = await Api.getOaEventList(range.value[0], range.value[1]) } finally { loading.value = false } }
const changeView = async () => { updateRange(); await load() }
const movePeriod = async (step: number) => { anchor.value = dayjs(anchor.value).add(step, viewMode.value).format('YYYY-MM-DD'); await changeView() }
const goToday = async () => { anchor.value = dayjs().format('YYYY-MM-DD'); await changeView() }
const focusCreate = () => document.querySelector<HTMLInputElement>('.el-form input')?.focus()
const reset = () => { const start = dayjs().add(1, 'hour').startOf('hour'); eventRange.value = [start.format('YYYY-MM-DD HH:mm:ss'), start.add(1, 'hour').format('YYYY-MM-DD HH:mm:ss')]; Object.assign(form, { id: undefined, title: '', startTime: eventRange.value[0], endTime: eventRange.value[1], location: '' }); load() }
const edit = (row: Api.OaEventVO) => { Object.assign(form, row); eventRange.value = [row.startTime, row.endTime] }
const save = async () => { form.startTime = eventRange.value[0]; form.endTime = eventRange.value[1]; if (form.id) await Api.updateOaEvent(form); else await Api.createOaEvent(form); message.success(t('common.saveSuccess')); reset() }
const remove = async (row: Api.OaEventVO) => { await message.delConfirm(); await Api.deleteOaEvent(row.id!); await load() }
onMounted(async () => { updateRange(); await load() })
</script>
