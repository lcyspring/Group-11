<template>
  <el-row :gutter="20">
    <el-col :span="16">
      <ContentWrap :title="t('customerVisit.create')">
        <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="110px">
          <el-form-item :label="t('customerVisit.customer')" prop="customerId">
            <el-select v-model="form.customerId" filterable class="w-full" @change="loadContacts">
              <el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('customerVisit.contact')">
            <el-select v-model="form.contactId" clearable filterable class="w-full">
              <el-option v-for="item in contacts" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item :label="t('customerVisit.plannedStart')" prop="plannedStartTime"><el-date-picker v-model="form.plannedStartTime" type="datetime" value-format="x" class="w-full" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="t('customerVisit.plannedEnd')" prop="plannedEndTime"><el-date-picker v-model="form.plannedEndTime" type="datetime" value-format="x" class="w-full" /></el-form-item></el-col>
          </el-row>
          <el-form-item :label="t('customerVisit.location')" prop="location"><el-input v-model="form.location" maxlength="300" show-word-limit /></el-form-item>
          <el-form-item :label="t('customerVisit.purpose')" prop="purpose"><el-input v-model="form.purpose" type="textarea" :rows="5" maxlength="1000" show-word-limit /></el-form-item>
          <el-form-item :label="t('customerVisit.participants')"><el-select v-model="form.participantUserIds" multiple filterable class="w-full"><el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" /></el-select></el-form-item>
          <el-form-item :label="t('customerVisit.attachments')"><UploadFile v-model="form.attachmentUrls" :limit="10" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="loading" @click="submit">{{ t('common.confirm') }}</el-button><el-button @click="cancel">{{ t('common.cancel') }}</el-button></el-form-item>
        </el-form>
      </ContentWrap>
    </el-col>
    <el-col :span="8"><ContentWrap :title="t('process.instance.flowDiagram')"><ProcessInstanceTimeline :activity-nodes="activityNodes" :show-status-icon="false" @select-user-confirm="selectUserConfirm" /></ContentWrap></el-col>
  </el-row>
</template>

<script setup lang="ts">
import * as VisitApi from '@/api/crm/customerVisit'
import * as CustomerApi from '@/api/crm/customer'
import * as ContactApi from '@/api/crm/contact'
import * as UserApi from '@/api/system/user'
import * as DefinitionApi from '@/api/bpm/definition'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import ProcessInstanceTimeline from '@/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue'
import { CandidateStrategy, NodeId } from '@/components/SimpleProcessDesignerV2/src/consts'
import { useTagsViewStore } from '@/store/modules/tagsView'
defineOptions({ name: 'CrmCustomerVisitCreate' })
const { t } = useI18n('crm')
const message = useMessage()
const router = useRouter()
const { delView } = useTagsViewStore()
const loading = ref(false)
const formRef = ref()
const form = ref<VisitApi.CustomerVisitVO>({ customerId: 0, plannedStartTime: '', plannedEndTime: '', location: '', purpose: '', participantUserIds: [], attachmentUrls: [] })
const customers = ref<CustomerApi.CustomerVO[]>([])
const contacts = ref<ContactApi.ContactVO[]>([])
const users = ref<UserApi.UserVO[]>([])
const processDefinitionId = ref('')
const activityNodes = ref<any[]>([])
const selectTasks = ref<any[]>([])
const assignees = ref<Record<string, number[]>>({})
const validateEnd = (_: unknown, value: number, done: (error?: Error) => void) => done(!value || Number(value) > Number(form.value.plannedStartTime) ? undefined : new Error(t('customerVisit.endAfterStart')))
const rules = {
  customerId: [{ required: true, message: t('customerVisit.customerRequired'), trigger: 'change' }],
  plannedStartTime: [{ required: true, message: t('customerVisit.startRequired'), trigger: 'change' }],
  plannedEndTime: [{ required: true, message: t('customerVisit.endRequired'), trigger: 'change' }, { validator: validateEnd, trigger: 'change' }],
  location: [{ required: true, message: t('customerVisit.locationRequired'), trigger: 'blur' }],
  purpose: [{ required: true, message: t('customerVisit.purposeRequired'), trigger: 'blur' }, { min: 5, max: 1000, message: t('customerVisit.purposeLength'), trigger: 'blur' }]
}
const loadContacts = async () => { form.value.contactId = undefined; contacts.value = form.value.customerId ? (await ContactApi.getContactPageByCustomer({ pageNo: 1, pageSize: 100, customerId: form.value.customerId })).list || [] : [] }
const selectUserConfirm = (id: string, selected: any[]) => { assignees.value[id] = selected.map((user) => user.id) }
const loadApproval = async () => { const data = await ProcessInstanceApi.getApprovalDetail({ processDefinitionId: processDefinitionId.value, activityId: NodeId.START_USER_NODE_ID, processVariablesStr: JSON.stringify({ customerId: form.value.customerId, plannedStartTime: form.value.plannedStartTime }) }); activityNodes.value = data?.activityNodes || []; selectTasks.value = activityNodes.value.filter((node) => node.candidateStrategy === CandidateStrategy.START_USER_SELECT); selectTasks.value.forEach((task) => { assignees.value[task.id] ||= [] }) }
const submit = async () => { if (!await formRef.value?.validate()) return; for (const task of selectTasks.value) if (!assignees.value[task.id]?.length) return message.warning(t('process.instance.selectCandidate', { name: task.name })); loading.value = true; try { await VisitApi.createCustomerVisit({ ...form.value, startUserSelectAssignees: assignees.value }); message.success(t('process.instance.startSuccess')); delView(unref(router.currentRoute)); await router.push({ name: 'CrmCustomerVisit' }) } finally { loading.value = false } }
const cancel = async () => { delView(unref(router.currentRoute)); await router.push({ name: 'CrmCustomerVisit' }) }
onMounted(async () => { [customers.value, users.value] = await Promise.all([CustomerApi.getCustomerSimpleList(), UserApi.getSimpleUserList()]); const definition = await DefinitionApi.getProcessDefinition(undefined, 'crm_customer_visit_audit'); if (!definition) return message.error(t('customerVisit.processModelNotFound')); processDefinitionId.value = definition.id; await loadApproval() })
</script>
