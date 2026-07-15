<template>
  <ClueDetailsHeader :clue="clue" :loading="loading">
    <el-button
      v-if="permissionListRef?.validateWrite && !clue.transformStatus && clue.poolStatus === 0"
      v-hasPermi="['crm:clue:update']"
      type="primary"
      @click="openForm"
    >
      {{ t('common.edit') }}
    </el-button>
    <el-button v-if="permissionListRef?.validateOwnerUser && !clue.transformStatus && clue.poolStatus === 0" type="primary" @click="transfer">
      {{ t('customer.transfer') }}
    </el-button>
    <el-button
      v-if="permissionListRef?.validateOwnerUser && !clue.transformStatus && clue.poolStatus === 0"
      type="success"
      @click="handleTransform"
    >
      {{ t('clue.transform') }}
    </el-button>
    <el-button
      v-if="permissionListRef?.validateOwnerUser && !clue.transformStatus && clue.poolStatus === 0"
      v-hasPermi="['crm:clue-public:put']"
      type="warning"
      @click="handlePutPublic"
    >
      {{ t('clue.putPublic') }}
    </el-button>
    <el-button
      v-if="!clue.transformStatus && clue.poolStatus === 1"
      v-hasPermi="['crm:clue-public:claim']"
      type="primary"
      @click="handleClaim"
    >
      {{ t('clue.claim') }}
    </el-button>
    <el-button v-if="clue.transformStatus" disabled type="success">{{ t('clue.transformStatusYes') }}</el-button>
  </ClueDetailsHeader>
  <el-col>
    <el-tabs>
      <el-tab-pane :label="t('clue.followUpTab')">
        <FollowUpList
          :biz-id="clueId" :biz-type="BizTypeEnum.CRM_CLUE"
          :readonly="clue.transformStatus || clue.poolStatus === 1" />
      </el-tab-pane>
      <el-tab-pane :label="t('clue.basicInfoTab')">
        <ClueDetailsInfo :clue="clue" />
      </el-tab-pane>
      <el-tab-pane :label="t('clue.teamMemberTab')">
        <PermissionList
          ref="permissionListRef"
          :biz-id="clue.id!"
          :biz-type="BizTypeEnum.CRM_CLUE"
          :show-action="!clue.transformStatus && clue.poolStatus === 0"
          @quit-team="close"
        />
      </el-tab-pane>
      <el-tab-pane :label="t('clue.operateLogTab')">
        <OperateLogV2 :log-list="logList" />
      </el-tab-pane>
    </el-tabs>
  </el-col>

  <!-- 表单弹窗：添加/修改 -->
  <ClueForm ref="formRef" @success="getClue" />
  <ClueTransformForm ref="transformFormRef" @success="getClue" />
  <CrmTransferForm ref="transferFormRef" :biz-type="BizTypeEnum.CRM_CLUE" @success="close" />
</template>
<script lang="ts" setup>
import { useTagsViewStore } from '@/store/modules/tagsView'
import * as ClueApi from '@/api/crm/clue'
import ClueForm from '@/views/crm/clue/ClueForm.vue'
import ClueTransformForm from '@/views/crm/clue/ClueTransformForm.vue'
import ClueDetailsHeader from './ClueDetailsHeader.vue' // 线索明细 - 头部
import ClueDetailsInfo from './ClueDetailsInfo.vue' // 线索明细 - 详细信息
import PermissionList from '@/views/crm/permission/components/PermissionList.vue' // 团队成员列表（权限）
import CrmTransferForm from '@/views/crm/permission/components/TransferForm.vue'
import FollowUpList from '@/views/crm/followup/index.vue'
import { BizTypeEnum } from '@/api/crm/permission'
import type { OperateLogVO } from '@/api/system/operatelog'
import { getOperateLogPage } from '@/api/crm/operateLog'

defineOptions({ name: 'CrmClueDetail' })

const { t } = useI18n('crm') // 国际化

const clueId = ref(0) // 线索编号
const loading = ref(true) // 加载中
const message = useMessage() // 消息弹窗
const { delView } = useTagsViewStore() // 视图操作
const { currentRoute } = useRouter() // 路由

const permissionListRef = ref<InstanceType<typeof PermissionList>>() // 团队成员列表 Ref

/** 获取详情 */
const clue = ref<ClueApi.ClueVO>({} as ClueApi.ClueVO) // 线索详情
const getClue = async () => {
  loading.value = true
  try {
    clue.value = await ClueApi.getClue(clueId.value)
    await getOperateLog()
  } finally {
    loading.value = false
  }
}

/** 编辑线索 */
const formRef = ref<InstanceType<typeof ClueForm>>() // 线索表单 Ref
const openForm = () => {
  formRef.value?.open('update', clueId.value)
}

/** 线索转移 */
const transferFormRef = ref<InstanceType<typeof CrmTransferForm>>() // 线索转移表单 ref
const transfer = () => {
  transferFormRef.value?.open(clueId.value)
}

/** 转化为客户 */
const transformFormRef = ref<InstanceType<typeof ClueTransformForm>>()
const handleTransform = () => {
  transformFormRef.value?.open(clue.value)
}

/** 放入公共线索池 */
const handlePutPublic = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('clue.putPublicReasonPrompt', { name: clue.value.name }),
      t('clue.putPublic'),
      {
        inputValidator: (input: string) => {
          if (!input?.trim()) return t('clue.putPublicReasonRequired')
          if (input.trim().length > 500) return t('clue.putPublicReasonTooLong')
          return true
        }
      }
    )
    await ClueApi.putCluePublic({ clueId: clueId.value, reason: value.trim() })
    message.success(t('clue.putPublicSuccess'))
    await getClue()
  } catch {}
}

/** 领取当前公共线索 */
const handleClaim = async () => {
  try {
    await message.confirm(t('clue.claimConfirm', { count: 1 }))
    await ClueApi.claimPublicClues([clueId.value])
    message.success(t('clue.claimSuccess', { count: 1 }))
    await getClue()
  } catch {}
}

/** 获取操作日志 */
const logList = ref<OperateLogVO[]>([]) // 操作日志列表
const getOperateLog = async () => {
  const data = await getOperateLogPage({
    bizType: BizTypeEnum.CRM_CLUE,
    bizId: clueId.value
  })
  logList.value = data.list
}

const close = () => {
  delView(unref(currentRoute))
}

/** 初始化 */
const { params } = useRoute()
onMounted(() => {
  if (!params.id) {
    message.warning(t('clue.paramError'))
    close()
    return
  }
  clueId.value = params.id as unknown as number
  getClue()
})
</script>
