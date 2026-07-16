<template>
  <ContentWrap>
    <div class="mb-12px text-right">
      <el-button v-hasPermi="['crm:work-order-group:update']" type="primary" @click="open()">
        {{ t('workOrder.addGroup') }}
      </el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column :label="t('workOrder.groupCode')" prop="code" min-width="130" />
      <el-table-column :label="t('workOrder.group')" prop="name" min-width="150" />
      <el-table-column :label="t('workOrder.groupManager')" prop="managerUserName" min-width="130" />
      <el-table-column :label="t('workOrder.supportedTypes')" min-width="220">
        <template #default="{ row }">
          <el-tag v-for="type in row.supportedTypes" :key="type" class="mr-4px">
            {{ typeLabel(type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('workOrder.groupMembers')" min-width="220">
        <template #default="{ row }">{{ row.memberUserNames?.join('、') }}</template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? t('common.enable') : t('common.disable') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" fixed="right" width="220">
        <template #default="{ row }">
          <TableActions>
            <el-button v-hasPermi="['crm:work-order-group:update']" link type="primary" @click="open(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button v-hasPermi="['crm:work-order-group:delete']" link type="danger" @click="remove(row.id)">
              {{ t('common.delete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <Dialog v-model="visible" :title="t('workOrder.groupConfig')" width="680px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('workOrder.groupCode')" prop="code">
            <el-input v-model="form.code" :disabled="Boolean(form.id)" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('workOrder.group')" prop="name"><el-input v-model="form.name" /></el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('workOrder.groupManager')" prop="managerUserId">
        <el-select v-model="form.managerUserId" class="w-full" filterable>
          <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('workOrder.groupMembers')" prop="memberUserIds">
        <el-select v-model="form.memberUserIds" class="w-full" multiple filterable>
          <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('workOrder.supportedTypes')" prop="supportedTypes">
        <el-checkbox-group v-model="form.supportedTypes">
          <el-checkbox v-for="item in types" :key="item.value" :value="item.value">{{ item.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('common.status')" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="0">{{ t('common.enable') }}</el-radio>
              <el-radio :value="1">{{ t('common.disable') }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('common.sort')" prop="sort"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('common.remark')"><el-input v-model="form.remark" type="textarea" maxlength="500" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="saving" @click="save">{{ t('dialog.confirm') }}</el-button>
      <el-button @click="visible = false">{{ t('dialog.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as WorkOrderApi from '@/api/crm/workorder'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'CrmWorkOrderGroup' })
const { t } = useI18n('crm')
const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const formRef = ref()
const list = ref<WorkOrderApi.WorkOrderGroupVO[]>([])
const users = ref<UserApi.UserVO[]>([])
const types = [
  { value: 1, label: t('workOrder.typeIssue') },
  { value: 2, label: t('workOrder.typeDemand') },
  { value: 3, label: t('workOrder.typeComplaint') },
  { value: 4, label: t('workOrder.typeConsultation') }
]
const emptyForm = (): WorkOrderApi.WorkOrderGroupVO => ({
  code: '',
  name: '',
  managerUserId: 0,
  supportedTypes: [],
  memberUserIds: [],
  status: 0,
  sort: 0,
  remark: ''
})
const form = ref<WorkOrderApi.WorkOrderGroupVO>(emptyForm())
const rules = reactive({
  code: [{ required: true, pattern: /^[a-z][a-z0-9_-]{1,31}$/, message: t('workOrder.groupCodeInvalid'), trigger: 'blur' }],
  name: [{ required: true, message: t('workOrder.groupNameRequired'), trigger: 'blur' }],
  managerUserId: [{ required: true, message: t('workOrder.groupManagerRequired'), trigger: 'change' }],
  memberUserIds: [{ required: true, type: 'array', min: 1, message: t('workOrder.groupMembersRequired'), trigger: 'change' }],
  supportedTypes: [{ required: true, type: 'array', min: 1, message: t('workOrder.supportedTypesRequired'), trigger: 'change' }]
})
const typeLabel = (value: number) => types.find((item) => item.value === value)?.label || value
const load = async () => {
  loading.value = true
  try {
    list.value = await WorkOrderApi.getWorkOrderGroupList()
  } finally {
    loading.value = false
  }
}
const open = (row?: WorkOrderApi.WorkOrderGroupVO) => {
  form.value = row ? JSON.parse(JSON.stringify(row)) : emptyForm()
  visible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
const save = async () => {
  if (!(await formRef.value?.validate())) return
  if (!form.value.memberUserIds.includes(form.value.managerUserId)) {
    message.warning(t('workOrder.groupManagerMustBeMember'))
    return
  }
  saving.value = true
  try {
    await WorkOrderApi.saveWorkOrderGroup(form.value)
    message.success(t('common.updateSuccess'))
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
const remove = async (id: number) => {
  await message.delConfirm()
  await WorkOrderApi.deleteWorkOrderGroup(id)
  await load()
}
onMounted(async () => {
  users.value = await UserApi.getSimpleUserList()
  await load()
})
</script>
