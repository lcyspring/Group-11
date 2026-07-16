<template>
  <ContentWrap
    ><el-form :inline="true" class="mb-10px"
      ><el-form-item :label="t('crm.marketing.name')"
        ><el-input v-model="query.name" clearable /></el-form-item
      ><el-button type="primary" @click="reload">{{ t('common.query') }}</el-button
      ><el-button @click="openCreate">{{ t('common.create') }}</el-button></el-form
    ><el-table v-loading="loading" :data="list"
      ><el-table-column prop="name" :label="t('crm.marketing.name')" /><el-table-column
        prop="channel"
        :label="t('crm.marketing.channel')"
      /><el-table-column prop="status" :label="t('crm.marketing.status')" /><el-table-column
        prop="validCount"
        :label="t('crm.marketing.validRecipients')"
      /><el-table-column prop="sentCount" :label="t('crm.marketing.sentCount')" /><el-table-column
        prop="failedCount"
        :label="t('crm.marketing.failedCount')"
      /><el-table-column fixed="right" :label="t('common.action')" width="220"
        ><template #default="scope"
          ><TableActions
            ><el-button v-if="scope.row.status === 10" link @click="submit(scope.row.id)">{{
              t('crm.marketing.submitReview')
            }}</el-button
            ><el-button
              v-if="scope.row.status === 40 || scope.row.status === 70"
              link
              type="success"
              @click="send(scope.row.id)"
              >{{ t('crm.marketing.send') }}</el-button
            ></TableActions
          ></template
        ></el-table-column
      ></el-table
    ><Pagination
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="reload"
    /><el-dialog v-model="dialogVisible" :title="t('crm.marketing.createBroadcast')"
      ><el-form :model="form" label-width="110px"
        ><el-form-item :label="t('crm.marketing.name')"
          ><el-input v-model="form.name" /></el-form-item
        ><el-form-item :label="t('crm.marketing.channel')"
          ><el-select v-model="form.channel"
            ><el-option :label="t('crm.marketing.sms')" :value="1" /><el-option
              :label="t('crm.marketing.email')"
              :value="2" /><el-option
              :label="t('crm.marketing.both')"
              :value="3" /></el-select></el-form-item
        ><el-form-item :label="t('crm.marketing.smsTemplateCode')"
          ><el-input v-model="form.smsTemplateCode" /></el-form-item
        ><el-form-item :label="t('crm.marketing.mailTemplateCode')"
          ><el-input v-model="form.mailTemplateCode" /></el-form-item
        ><el-form-item :label="t('crm.marketing.customerIds')"
          ><el-input v-model="customerIdsText" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button
        ><el-button type="primary" @click="save">{{ t('common.ok') }}</el-button></template
      ></el-dialog
    ></ContentWrap
  >
</template>
<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import * as Api from '@/api/crm/marketing'
const { t } = useI18n()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ pageNo: 1, pageSize: 10, name: '' })
const form = reactive<any>({
  name: '',
  channel: 1,
  smsTemplateCode: '',
  mailTemplateCode: '',
  customerIds: []
})
const customerIdsText = computed({
  get: () => form.customerIds.join(','),
  set: (v: string) => {
    form.customerIds = v
      .split(',')
      .map((x) => Number(x.trim()))
      .filter(Boolean)
  }
})
const reload = async () => {
  loading.value = true
  try {
    const r = await Api.getBroadcastPage(query)
    list.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
const openCreate = () => {
  Object.assign(form, {
    name: '',
    channel: 1,
    smsTemplateCode: '',
    mailTemplateCode: '',
    customerIds: []
  })
  dialogVisible.value = true
}
const save = async () => {
  await Api.saveBroadcast(form)
  dialogVisible.value = false
  reload()
}
const submit = async (id: number) => {
  await Api.submitBroadcastReview(id)
  reload()
}
const send = async (id: number) => {
  await Api.sendBroadcast(id)
  reload()
}
reload()
</script>
