<template>
  <ContentWrap>
    <div class="mb-12px flex gap-8px">
      <el-button v-hasPermi="['crm:contract:attachment']" type="primary" @click="openAttachment">
        {{ t('crm.contract.addAttachment') }}
      </el-button>
      <el-button
        v-if="contract.auditStatus === 20 && !data.signing"
        v-hasPermi="['crm:contract:sign']"
        type="success"
        :disabled="signedCopies.length === 0 || data.supportedSignMethods.length === 0"
        @click="openSign"
      >
        {{ t('crm.contract.signContract') }}
      </el-button>
      <el-button
        v-if="data.signing?.status === 10"
        v-hasPermi="['crm:contract:sign-void']"
        type="danger"
        @click="voidSign"
      >
        {{ t('crm.contract.voidSignature') }}
      </el-button>
      <el-button
        v-if="data.signing?.status === 10 && !openAmendment"
        v-hasPermi="['crm:contract:amendment']"
        type="warning"
        @click="amendmentDialogRef?.open()"
      >
        {{ t('crm.contract.createAmendment') }}
      </el-button>
    </div>
    <el-alert
      v-if="contract.auditStatus === 20 && !data.signing && signedCopies.length === 0"
      :title="t('crm.contract.signedCopyHint')"
      type="info"
      :closable="false"
      class="mb-12px"
    />
    <el-descriptions
      v-if="data.signing"
      :title="t('crm.contract.signingFact')"
      :column="3"
      border
      class="mb-16px"
    >
      <el-descriptions-item :label="t('crm.contract.signingStatus')">
        {{
          data.signing.status === 10 ? t('crm.contract.signed') : t('crm.contract.signatureVoided')
        }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.contract.signingMethod')">
        {{ signMethodLabel(data.signing.method) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.contract.signedTime')">
        {{ formatDate(new Date(data.signing.signedTime)) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.contract.provider')">
        {{ data.signing.providerCode }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('crm.contract.requestId')">
        {{ data.signing.providerRequestId }}
      </el-descriptions-item>
      <el-descriptions-item v-if="data.signing.voidReason" :label="t('crm.contract.voidReason')">
        {{ data.signing.voidReason }}
      </el-descriptions-item>
    </el-descriptions>
    <el-table :data="data.attachments" class="mb-16px">
      <el-table-column :label="t('crm.contract.attachmentName')" prop="fileName" min-width="200">
        <template #default="scope">
          <el-link type="primary" @click="downloadAttachment(scope.row)">
            {{ scope.row.fileName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('crm.contract.attachmentCategory')" min-width="130">
        <template #default="scope">{{ categoryLabel(scope.row.category) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('crm.contract.contractVersion')"
        prop="contractVersion"
        width="100"
      />
      <el-table-column :label="t('crm.contract.immutable')" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.immutable ? 'success' : 'info'">
            {{ scope.row.immutable ? t('common.yes') : t('common.no') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="100">
        <template #default="scope">
          <el-button
            v-if="!scope.row.immutable"
            v-hasPermi="['crm:contract:attachment']"
            link
            type="danger"
            @click="removeAttachment(scope.row)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-divider>{{ t('crm.contract.amendmentTitle') }}</el-divider>
    <el-table :data="amendments" class="mb-16px">
      <el-table-column :label="t('crm.contract.amendmentNo')" prop="no" min-width="160" />
      <el-table-column :label="t('crm.contract.amendmentName')" prop="title" min-width="180" />
      <el-table-column :label="t('crm.contract.contractVersion')" width="110">
        <template #default="scope">V{{ scope.row.baseVersion }} → V{{ scope.row.targetVersion }}</template>
      </el-table-column>
      <el-table-column :label="t('crm.contract.auditStatus')" width="110">
        <template #default="scope">{{ auditStatusLabel(scope.row.auditStatus) }}</template>
      </el-table-column>
      <el-table-column :label="t('crm.contract.amendmentAmountAfter')" prop="amountAfter" width="140" />
      <el-table-column :label="t('crm.contract.amendmentAmountDelta')" prop="amountDelta" width="140" />
      <el-table-column :label="t('common.action')" fixed="right" width="220">
        <template #default="scope">
          <TableActions>
            <el-button
              v-if="[0, 30, 40].includes(scope.row.auditStatus)"
              v-hasPermi="['crm:contract:amendment']"
              link
              type="primary"
              @click="amendmentDialogRef?.open(scope.row.id)"
            >{{ t('action.edit') }}</el-button>
            <el-button
              v-if="scope.row.auditStatus === 0"
              v-hasPermi="['crm:contract:amendment']"
              link
              type="success"
              @click="submitAmendment(scope.row)"
            >{{ t('crm.contract.submitAudit') }}</el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
    <el-timeline>
      <el-timeline-item
        v-for="item in data.changeRecords"
        :key="item.id"
        :timestamp="formatDate(new Date(item.actionTime))"
      >
        {{ actionLabel(item.actionType) }} · V{{ item.contractVersion }}
        <span v-if="item.reason"> · {{ item.reason }}</span>
      </el-timeline-item>
    </el-timeline>
  </ContentWrap>

  <Dialog v-model="attachmentVisible" :title="t('crm.contract.addAttachment')" width="560px">
    <el-form ref="attachmentFormRef" :model="attachmentForm" label-width="120px">
      <el-form-item
        :label="t('crm.contract.attachmentCategory')"
        prop="category"
        :rules="[{ required: true }]"
      >
        <el-select v-model="attachmentForm.category" class="w-full">
          <el-option :label="t('crm.contract.generalAttachment')" :value="1" />
          <el-option :label="t('crm.contract.signedCopy')" :value="2" />
          <el-option :label="t('crm.contract.amendmentEvidence')" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="attachmentForm.category === 3"
        :label="t('crm.contract.amendmentName')"
        prop="amendmentId"
        :rules="[{ required: true }]"
      >
        <el-select v-model="attachmentForm.amendmentId" class="w-full">
          <el-option
            v-for="item in editableAmendments"
            :key="item.id"
            :label="`${item.no} · ${item.title}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('crm.contract.attachmentName')"
        prop="fileName"
        :rules="[{ required: true }]"
      >
        <el-input v-model="attachmentForm.fileName" maxlength="255" />
      </el-form-item>
      <el-form-item
        :label="t('crm.contract.attachmentFile')"
        prop="fileUrl"
        :rules="[{ required: true }]"
      >
        <el-upload
          v-model:file-list="attachmentFiles"
          :auto-upload="true"
          :http-request="uploadAttachment"
          :limit="1"
          :on-remove="clearUploadedAttachment"
        >
          <el-button :loading="attachmentUploading" type="primary">
            {{ t('common.selectFile') }}
          </el-button>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitAttachment">{{ t('common.ok') }}</el-button>
      <el-button @click="attachmentVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
  <Dialog v-model="signVisible" :title="t('crm.contract.signContract')" width="560px">
    <el-form ref="signFormRef" :model="signForm" label-width="120px">
      <el-form-item
        :label="t('crm.contract.signingMethod')"
        prop="method"
        :rules="[{ required: true }]"
      >
        <el-radio-group v-model="signForm.method">
          <el-radio v-for="method in data.supportedSignMethods" :key="method" :value="method">
            {{ signMethodLabel(method) }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        :label="t('crm.contract.signedTime')"
        prop="signedTime"
        :rules="[{ required: true }]"
      >
        <el-date-picker
          v-model="signForm.signedTime"
          type="datetime"
          value-format="x"
          class="w-full"
        />
      </el-form-item>
      <el-form-item
        :label="t('crm.contract.signedCopy')"
        prop="signedAttachmentId"
        :rules="[{ required: true }]"
      >
        <el-select v-model="signForm.signedAttachmentId" class="w-full">
          <el-option v-for="a in signedCopies" :key="a.id" :label="a.fileName" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('crm.contract.handler')"
        prop="handlerUserId"
        :rules="[{ required: true }]"
      >
        <el-select v-model="signForm.handlerUserId" class="w-full">
          <el-option v-for="u in users" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitSign">{{ t('crm.contract.confirmSign') }}</el-button>
      <el-button @click="signVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
  <ContractAmendmentDialog ref="amendmentDialogRef" :contract="contract" @success="load" />
</template>
<script setup lang="ts">
import * as LifecycleApi from '@/api/crm/contract/lifecycle'
import * as AmendmentApi from '@/api/crm/contract/amendment'
import * as ContractApi from '@/api/crm/contract'
import * as UserApi from '@/api/system/user'
import { formatDate } from '@/utils/formatTime'
import download from '@/utils/download'
import type {
  FormInstance,
  UploadRequestOptions,
  UploadUserFile
} from 'element-plus'
import ContractAmendmentDialog from './ContractAmendmentDialog.vue'
const props = defineProps<{ contract: ContractApi.ContractVO }>()
type UploadError = Parameters<UploadRequestOptions['onError']>[0]
const { t } = useI18n()
const message = useMessage()
const data = ref<LifecycleApi.ContractLifecycleVO>({
  attachments: [],
  changeRecords: [],
  supportedSignMethods: []
})
const users = ref<UserApi.UserVO[]>([])
const amendments = ref<AmendmentApi.ContractAmendmentVO[]>([])
const amendmentDialogRef = ref<InstanceType<typeof ContractAmendmentDialog>>()
const load = async () => {
  const [lifecycle, amendmentList] = await Promise.all([
    LifecycleApi.getContractLifecycle(props.contract.id),
    AmendmentApi.getContractAmendmentList(props.contract.id)
  ])
  data.value = lifecycle
  amendments.value = amendmentList
}
const openAmendment = computed(() => amendments.value.find((item) => item.auditStatus !== 20))
const editableAmendments = computed<Array<AmendmentApi.ContractAmendmentVO & { id: number }>>(() =>
  amendments.value.filter(
    (item): item is AmendmentApi.ContractAmendmentVO & { id: number } =>
      item.id !== undefined && [0, 30, 40].includes(item.auditStatus || 0)
  )
)
const signedCopies = computed(() =>
  data.value.attachments.filter((x) => x.category === 2 && !x.immutable)
)
const categoryLabel = (v: number) =>
  v === 1
    ? t('crm.contract.generalAttachment')
    : v === 2
      ? t('crm.contract.signedCopy')
      : t('crm.contract.amendmentEvidence')
const signMethodLabel = (v: number) =>
  v === 1 ? t('crm.contract.offlineSign') : t('crm.contract.electronicSign')
const actionLabel = (v: number) =>
  [
    t('crm.contract.created'),
    t('crm.contract.updated'),
    t('crm.contract.submitted'),
    t('crm.contract.approved'),
    t('crm.contract.rejected'),
    t('crm.contract.canceled'),
    t('crm.contract.signed'),
    t('crm.contract.signatureVoided'),
    t('crm.contract.amendmentCreated'),
    t('crm.contract.amendmentUpdated'),
    t('crm.contract.amendmentSubmitted'),
    t('crm.contract.amendmentEffective'),
    t('crm.contract.amendmentRejected'),
    t('crm.contract.amendmentCanceled')
  ][v - 1] || v
const auditStatusLabel = (v?: number) =>
  v === 0
    ? t('crm.contract.auditStatusDraft')
    : v === 10
      ? t('crm.contract.auditStatusProcess')
      : v === 20
        ? t('crm.contract.auditStatusApprove')
        : v === 30
          ? t('crm.contract.auditStatusReject')
          : t('crm.contract.canceled')
const attachmentVisible = ref(false)
const attachmentFormRef = ref<FormInstance>()
const attachmentForm = ref<LifecycleApi.ContractAttachmentCreateReqVO>({
  contractId: props.contract.id,
  category: 1,
  fileName: '',
  fileUrl: ''
})
const attachmentFiles = ref<UploadUserFile[]>([])
const attachmentUploading = ref(false)
const openAttachment = () => {
  attachmentForm.value = { contractId: props.contract.id, category: 1, fileName: '', fileUrl: '' }
  attachmentFiles.value = []
  attachmentVisible.value = true
}
const uploadAttachment = async (options: UploadRequestOptions) => {
  attachmentUploading.value = true
  try {
    const response: any = await LifecycleApi.uploadContractAttachment(
      props.contract.id,
      options.file,
      (event) => options.onProgress(event)
    )
    if (response.code !== 0) throw response
    attachmentForm.value.fileUrl = response.data
    if (!attachmentForm.value.fileName) attachmentForm.value.fileName = options.file.name
    options.onSuccess(response)
  } catch (error) {
    options.onError(error as UploadError)
    throw error
  } finally {
    attachmentUploading.value = false
  }
}
const clearUploadedAttachment = () => {
  attachmentForm.value.fileUrl = ''
}
const submitAttachment = async () => {
  if (!attachmentFormRef.value || !(await attachmentFormRef.value.validate())) return
  await LifecycleApi.createContractAttachment(attachmentForm.value)
  message.success(t('common.createSuccess'))
  attachmentVisible.value = false
  await load()
}
const submitAmendment = async (row: AmendmentApi.ContractAmendmentVO) => {
  await message.confirm(t('crm.contract.submitAmendmentConfirm'))
  await AmendmentApi.submitContractAmendment(props.contract.id, row.id!)
  message.success(t('crm.contract.submitAuditSuccess'))
  await load()
}
const removeAttachment = async (row: LifecycleApi.ContractAttachmentVO) => {
  await message.delConfirm()
  await LifecycleApi.deleteContractAttachment(props.contract.id, row.id)
  await load()
}
const downloadAttachment = async (row: LifecycleApi.ContractAttachmentVO) => {
  const content = await LifecycleApi.downloadContractAttachment(props.contract.id, row.id)
  download.file(content, row.fileName, row.contentType)
}
const signVisible = ref(false)
const signFormRef = ref<FormInstance>()
const signForm = ref<LifecycleApi.ContractSignReqVO>({
  contractId: props.contract.id,
  method: 1,
  signedTime: 0,
  signedAttachmentId: 0,
  handlerUserId: 0
})
const openSign = async () => {
  users.value = await UserApi.getSimpleUserList()
  signForm.value = {
    contractId: props.contract.id,
    method: data.value.supportedSignMethods[0],
    signedTime: Date.now(),
    signedAttachmentId: signedCopies.value[0]?.id,
    handlerUserId: props.contract.ownerUserId
  }
  signVisible.value = true
}
const submitSign = async () => {
  if (!signFormRef.value || !(await signFormRef.value.validate())) return
  await LifecycleApi.signContract({
    ...signForm.value,
    signedTime: Number(signForm.value.signedTime)
  })
  message.success(t('crm.contract.signSuccess'))
  signVisible.value = false
  await load()
}
const voidSign = async () => {
  const { value } = await ElMessageBox.prompt(t('crm.contract.voidReason'), '', {
    inputValidator: (v) => Boolean(v?.trim())
  })
  await LifecycleApi.voidContractSign(props.contract.id, value)
  message.success(t('crm.contract.voidSuccess'))
  await load()
}
watch(
  () => props.contract.id,
  () => load(),
  { immediate: true }
)
</script>
