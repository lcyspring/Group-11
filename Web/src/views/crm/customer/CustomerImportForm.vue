<!-- 客户导入预检窗口 -->
<template>
  <Dialog v-model="dialogVisible" :title="t('importTitle')" width="980px">
    <el-alert :closable="false" class="mb-16px" show-icon :title="t('previewGuidance')" />

    <div class="mb-16px flex flex-wrap items-center gap-16px">
      <span>{{ t('ownerUserId') }}</span>
      <el-select v-model="ownerUserId" class="!w-240px" clearable :placeholder="t('ownerOptional')">
        <el-option
          v-for="item in userOptions"
          :key="item.id"
          :label="item.nickname"
          :value="item.id"
        />
      </el-select>
      <el-checkbox v-model="updateSupport">{{ t('updateExisting') }}</el-checkbox>
    </div>

    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      accept=".xlsx, .xls"
      action="none"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text" v-html="t('uploadFile')"></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>{{ t('allowFormat') }}</span>
          <el-link
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            type="primary"
            @click="importTemplate"
          >
            {{ t('downloadTemplate') }}
          </el-link>
        </div>
      </template>
    </el-upload>

    <template v-if="preview">
      <el-divider content-position="left">{{ t('fieldMapping') }}</el-divider>
      <div class="grid grid-cols-1 gap-12px md:grid-cols-2 lg:grid-cols-3">
        <div v-for="header in preview.headers" :key="header" class="flex items-center gap-8px">
          <el-text class="w-120px" truncated>{{ header }}</el-text>
          <el-select v-model="fieldMapping[header]" clearable :placeholder="t('ignoreColumn')">
            <el-option
              v-for="field in preview.fields"
              :key="field.key"
              :disabled="isFieldUsed(field.key, header)"
              :label="field.required ? `${field.label} *` : field.label"
              :value="field.key"
            />
          </el-select>
        </div>
      </div>

      <div class="my-16px flex flex-wrap gap-12px">
        <el-tag type="info">{{ t('previewTotal', { count: preview.totalCount }) }}</el-tag>
        <el-tag type="success">{{ t('previewCreate', { count: preview.createCount }) }}</el-tag>
        <el-tag type="warning">{{ t('previewUpdate', { count: preview.updateCount }) }}</el-tag>
        <el-tag type="danger">{{ t('previewFailure', { count: preview.failureCount }) }}</el-tag>
        <el-button v-if="preview.failureCount > 0" link type="danger" @click="downloadFailureDetails">
          {{ t('downloadFailureDetails') }}
        </el-button>
        <el-text type="info">{{ t('previewExpiresAt', { time: formatDate(preview.expiresAt) }) }}</el-text>
      </div>

      <el-table :data="preview.rows" max-height="360" stripe>
        <el-table-column :label="t('rowNumber')" prop="rowNumber" width="80" />
        <el-table-column :label="t('name')" min-width="160" prop="customer.name" />
        <el-table-column :label="t('mobile')" min-width="130" prop="customer.mobile" />
        <el-table-column :label="t('previewAction')" width="110">
          <template #default="{ row }">
            <el-tag :type="actionType(row.action)">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('previewErrors')" min-width="280">
          <template #default="{ row }">{{ row.errors?.join('；') || '-' }}</template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button :loading="formLoading" type="primary" @click="runPreview">
        {{ preview ? t('rerunPreview') : t('startPreview') }}
      </el-button>
      <el-button
        v-if="preview"
        :disabled="preview.createCount + preview.updateCount === 0"
        :loading="confirmLoading"
        type="success"
        @click="confirmImport"
      >
        {{ t('confirmImport') }}
      </el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as CustomerApi from '@/api/crm/customer'
import download from '@/utils/download'
import { formatDate } from '@/utils/formatTime'
import type { UploadFile, UploadUserFile } from 'element-plus'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'CrmCustomerImportForm' })

const { t } = useI18n('crm.customer')
const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const confirmLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const updateSupport = ref(false)
const ownerUserId = ref<undefined | number>()
const userOptions = ref<UserApi.UserVO[]>([])
const preview = ref<CustomerApi.CustomerImportPreviewVO>()
const fieldMapping = reactive<Record<string, string>>({})

const open = async () => {
  dialogVisible.value = true
  await resetForm()
  userOptions.value = await UserApi.getSimpleUserList()
  ownerUserId.value = useUserStore().getUser.id
}
defineExpose({ open })

const runPreview = async () => {
  if (!fileList.value[0]?.raw) {
    message.error(t('pleaseUploadFile'))
    return
  }
  formLoading.value = true
  try {
    const formData = new FormData()
    formData.append('updateSupport', String(updateSupport.value))
    formData.append('file', fileList.value[0].raw as Blob)
    if (ownerUserId.value !== undefined) formData.append('ownerUserId', String(ownerUserId.value))
    if (Object.keys(fieldMapping).length > 0) {
      formData.append('fieldMapping', JSON.stringify(fieldMapping))
    }
    const response: any = await CustomerApi.previewImport(formData)
    const data = response?.data ?? response
    preview.value = data
    Object.keys(fieldMapping).forEach((key) => delete fieldMapping[key])
    Object.assign(fieldMapping, data.fieldMapping)
  } finally {
    formLoading.value = false
  }
}

const emits = defineEmits(['success'])
const confirmImport = async () => {
  if (!preview.value) return
  confirmLoading.value = true
  try {
    const result = await CustomerApi.confirmImportPreview(preview.value.id)
    showImportResult(result)
    dialogVisible.value = false
    emits('success')
  } finally {
    confirmLoading.value = false
  }
}

const showImportResult = (data: CustomerApi.CustomerImportResultVO) => {
  const text = [
    `${t('uploadSuccessCount')}：${data.createCustomerNames.length}`,
    `${t('updateSuccessCount')}：${data.updateCustomerNames.length}`,
    `${t('updateFailCount')}：${Object.keys(data.failureCustomerNames).length}`
  ].join('；')
  message.success(text)
}

const isFieldUsed = (field: string, currentHeader: string) =>
  Object.entries(fieldMapping).some(([header, value]) => header !== currentHeader && value === field)

const actionType = (action: string) => action === 'CREATE' ? 'success'
  : action === 'UPDATE' ? 'warning' : 'danger'
const actionLabel = (action: string) => action === 'CREATE' ? t('previewActionCreate')
  : action === 'UPDATE' ? t('previewActionUpdate') : t('previewActionFailure')

const downloadFailureDetails = () => {
  const rows = preview.value?.rows.filter((row) => row.action === 'FAILURE') ?? []
  const blob = new Blob([JSON.stringify(rows, null, 2)], { type: 'application/json;charset=utf-8' })
  download.file(blob, t('failureDetailsFileName'), 'application/json;charset=utf-8')
}

const handleFileChange = (_file: UploadFile) => {
  preview.value = undefined
  Object.keys(fieldMapping).forEach((key) => delete fieldMapping[key])
}

const resetForm = async () => {
  fileList.value = []
  updateSupport.value = false
  ownerUserId.value = undefined
  preview.value = undefined
  Object.keys(fieldMapping).forEach((key) => delete fieldMapping[key])
  await nextTick()
  uploadRef.value?.clearFiles()
}

const handleExceed = () => message.error(t('maxFileLimit'))
const importTemplate = async () => {
  const res = await CustomerApi.importCustomerTemplate()
  download.excel(res, t('templateFileName'))
}
</script>
