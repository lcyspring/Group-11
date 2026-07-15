<template>
  <Dialog v-model="visible" :title="t('reimbursement.categoryManagement')" width="820px">
    <div class="mb-12px text-right">
      <el-button type="primary" @click="edit()">{{ t('reimbursement.createCategory') }}</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column :label="t('reimbursement.categoryCode')" min-width="130" prop="code" />
      <el-table-column :label="t('reimbursement.categoryName')" min-width="130" prop="name" />
      <el-table-column :label="t('reimbursement.categoryStatus')" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? t('reimbursement.enabled') : t('reimbursement.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('reimbursement.categorySort')" width="90" prop="sort" />
      <el-table-column :label="t('reimbursement.categoryDescription')" min-width="160" prop="description" />
      <el-table-column fixed="right" :label="t('common.action')" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="edit(row)">{{ t('common.edit') }}</el-button>
          <el-button link type="danger" @click="remove(row.id)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </Dialog>

  <Dialog v-model="formVisible" :title="formData.id ? t('reimbursement.updateCategory') : t('reimbursement.createCategory')" width="560px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <el-form-item :label="t('reimbursement.categoryCode')" prop="code">
        <el-input v-model="formData.code" maxlength="40" @input="normalizeCode" />
      </el-form-item>
      <el-form-item :label="t('reimbursement.categoryName')" prop="name">
        <el-input v-model="formData.name" maxlength="100" />
      </el-form-item>
      <el-form-item :label="t('reimbursement.categoryStatus')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">{{ t('reimbursement.enabled') }}</el-radio>
          <el-radio :value="1">{{ t('reimbursement.disabled') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('reimbursement.categorySort')" prop="sort">
        <el-input-number v-model="formData.sort" :max="9999" :min="0" />
      </el-form-item>
      <el-form-item :label="t('reimbursement.categoryDescription')" prop="description">
        <el-input v-model="formData.description" maxlength="500" :rows="3" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :loading="saving" type="primary" @click="save">{{ t('dialog.confirm') }}</el-button>
      <el-button @click="formVisible = false">{{ t('dialog.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as ReimbursementApi from '@/api/crm/reimbursement'

const { t } = useI18n('crm')
const message = useMessage()
const visible = ref(false)
const formVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const list = ref<ReimbursementApi.ExpenseCategoryVO[]>([])
const formData = ref<ReimbursementApi.ExpenseCategoryVO>(emptyForm())
const required = (key: string) => ({ required: true, message: t(key), trigger: ['blur', 'change'] })
const rules = reactive({
  code: [
    required('reimbursement.categoryCodeRequired'),
    { pattern: /^[A-Z][A-Z0-9_-]{1,39}$/, message: t('reimbursement.categoryCodeInvalid'), trigger: 'blur' }
  ],
  name: [required('reimbursement.categoryNameRequired')],
  status: [required('reimbursement.categoryStatusRequired')],
  sort: [required('reimbursement.categorySortRequired')]
})
function emptyForm(): ReimbursementApi.ExpenseCategoryVO {
  return { code: '', name: '', status: 0, sort: 0, description: '' }
}
const normalizeCode = (value: string) => {
  formData.value.code = value.toUpperCase().replace(/[^A-Z0-9_-]/g, '')
}
const load = async () => {
  loading.value = true
  try {
    list.value = await ReimbursementApi.getExpenseCategoryList()
  } finally {
    loading.value = false
  }
}
const open = async () => {
  visible.value = true
  await load()
}
const edit = (row?: ReimbursementApi.ExpenseCategoryVO) => {
  formData.value = row ? { ...row } : emptyForm()
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
const save = async () => {
  if (!(await formRef.value.validate())) return
  saving.value = true
  try {
    if (formData.value.id) await ReimbursementApi.updateExpenseCategory(formData.value)
    else await ReimbursementApi.createExpenseCategory(formData.value)
    message.success(formData.value.id ? t('common.updateSuccess') : t('common.createSuccess'))
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
const remove = async (id: number) => {
  await message.delConfirm()
  await ReimbursementApi.deleteExpenseCategory(id)
  message.success(t('common.delSuccess'))
  await load()
}
defineExpose({ open })
</script>
