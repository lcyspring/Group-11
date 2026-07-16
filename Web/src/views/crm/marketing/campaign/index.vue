<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-10px">
      <el-form-item :label="t('crm.marketing.name')"><el-input v-model="query.name" clearable @keyup.enter="reload" /></el-form-item>
      <el-form-item><el-button type="primary" @click="reload">{{ t('common.query') }}</el-button><el-button @click="openCreate">{{ t('common.create') }}</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column prop="code" :label="t('crm.marketing.code')" />
      <el-table-column prop="name" :label="t('crm.marketing.name')" />
      <el-table-column prop="status" :label="t('crm.marketing.status')" />
      <el-table-column prop="startTime" :label="t('crm.marketing.startTime')" />
      <el-table-column prop="endTime" :label="t('crm.marketing.endTime')" />
      <el-table-column fixed="right" :label="t('common.action')" width="260">
        <template #default="scope">
          <el-button v-if="scope.row.status === 10" link type="primary" @click="start(scope.row.id)">{{ t('crm.marketing.start') }}</el-button>
          <el-button v-if="scope.row.status === 20" link type="warning" @click="lock(scope.row.id)">{{ t('crm.marketing.lock') }}</el-button>
          <el-button v-if="scope.row.status === 20 || scope.row.status === 30" link @click="complete(scope.row.id)">{{ t('crm.marketing.complete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="reload" />
    <el-dialog v-model="dialogVisible" :title="t('crm.marketing.createCampaign')" width="560px">
      <el-form :model="form" label-width="110px"><el-form-item :label="t('crm.marketing.code')"><el-input v-model="form.code" /></el-form-item><el-form-item :label="t('crm.marketing.name')"><el-input v-model="form.name" /></el-form-item><el-form-item :label="t('crm.marketing.ownerUserId')"><el-input-number v-model="form.ownerUserId" :min="1" /></el-form-item><el-form-item :label="t('crm.marketing.startTime')"><el-date-picker v-model="form.startTime" type="datetime" /></el-form-item><el-form-item :label="t('crm.marketing.endTime')"><el-date-picker v-model="form.endTime" type="datetime" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button><el-button type="primary" @click="save">{{ t('common.ok') }}</el-button></template>
    </el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import * as MarketingApi from '@/api/crm/marketing'
import { ElMessage, ElMessageBox } from 'element-plus'
const { t } = useI18n(); const loading = ref(false); const list = ref<any[]>([]); const total = ref(0); const dialogVisible = ref(false)
const query = reactive({ pageNo: 1, pageSize: 10, name: '' }); const form = reactive<any>({ code: '', name: '', ownerUserId: 1, startTime: '', endTime: '' })
const reload = async () => { loading.value = true; try { const res = await MarketingApi.getCampaignPage(query); list.value = res.list || []; total.value = res.total || 0 } finally { loading.value = false } }
const openCreate = () => { Object.assign(form, { code: '', name: '', ownerUserId: 1, startTime: '', endTime: '' }); dialogVisible.value = true }
const save = async () => { await MarketingApi.saveCampaign({ ...form, relations: [] }); ElMessage.success(t('common.createSuccess')); dialogVisible.value = false; reload() }
const start = async (id: number) => { await MarketingApi.startCampaign(id); reload() }; const lock = async (id: number) => { await MarketingApi.lockCampaign(id); reload() }
const complete = async (id: number) => { await ElMessageBox.confirm(t('crm.marketing.confirmComplete')); await MarketingApi.completeCampaign({ id, summary: t('crm.marketing.completedByUser') }); reload() }
reload()
</script>
