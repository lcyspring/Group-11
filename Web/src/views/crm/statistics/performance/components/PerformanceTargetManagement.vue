<template>
  <el-card shadow="never">
    <el-form inline label-width="auto">
      <el-form-item :label="t('performance.targetScope')">
        <el-radio-group v-model="scopeType" @change="handleScopeTypeChange">
          <el-radio-button :value="1">{{ t('performance.companyTarget') }}</el-radio-button>
          <el-radio-button :value="2">{{ t('performance.departmentTarget') }}</el-radio-button>
          <el-radio-button :value="3">{{ t('performance.userTarget') }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="scopeType === 2" :label="t('dept')">
        <el-tree-select
          v-model="scopeId"
          class="!w-240px"
          :data="deptList"
          :props="defaultProps"
          check-strictly
          node-key="id"
          @change="loadData"
        />
      </el-form-item>
      <el-form-item v-if="scopeType === 3" :label="t('user')">
        <el-select v-model="scopeId" class="!w-240px" filterable @change="loadData">
          <el-option
            v-for="user in userList"
            :key="user.id"
            :label="user.nickname"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('performance.targetType')">
        <el-select v-model="targetType" class="!w-220px" @change="applySelectedTarget">
          <el-option
            v-for="option in targetTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="!loading && !targetExists"
      class="mb-16px"
      :closable="false"
      :title="t('performance.targetNotConfigured')"
      type="info"
      show-icon
    />

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="(_, index) in monthlyTargets" :key="index" :span="6">
        <el-form-item :label="t('performance.monthTarget', { month: index + 1 })">
          <el-input v-model="monthlyTargets[index]" inputmode="decimal">
            <template #append>{{ targetUnit }}</template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-descriptions :column="5" border>
      <el-descriptions-item
        v-for="(quarter, index) in quarterlyTargets"
        :key="index"
        :label="t('performance.quarterTarget', { quarter: index + 1 })"
      >
        {{ quarter ?? '--' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('performance.annualTarget')">
        {{ annualTarget ?? '--' }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="mt-16px flex justify-end gap-8px">
      <el-button
        v-if="targetExists"
        v-hasPermi="['crm:performance-target:delete']"
        :loading="submitting"
        :disabled="scopeId == null"
        type="danger"
        @click="deleteTarget"
      >
        {{ t('common.delete') }}
      </el-button>
      <el-button
        v-hasPermi="['crm:performance-target:update']"
        :loading="submitting"
        :disabled="scopeId == null"
        type="primary"
        @click="saveTarget"
      >
        {{ t('common.save') }}
      </el-button>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { PerformanceTargetApi, PerformanceTargetRespVO } from '@/api/crm/statistics/performance'
import { defaultProps, handleTree } from '@/utils/tree'
import { buildQuarterTargets, isValidTargetValue, sumTargetValues } from '../performanceTarget'

defineOptions({ name: 'PerformanceTargetManagement' })

const { t } = useI18n('crm.statistics')
const message = useMessage()
const props = defineProps<{ queryParams: any }>()
const loading = ref(false)
const submitting = ref(false)
const scopeType = ref(2)
const scopeId = ref<number | undefined>(props.queryParams.deptId)
const targetType = ref(1)
const targetList = ref<PerformanceTargetRespVO[]>([])
const targetExists = ref(false)
const monthlyTargets = ref<string[]>(Array(12).fill('0'))
const deptList = ref<Tree[]>([])
const userList = ref<UserApi.UserVO[]>([])

const targetYear = computed(() => Number(String(props.queryParams.times?.[0] || '').slice(0, 4)))
const countTarget = computed(() => targetType.value >= 3)
const targetUnit = computed(() =>
  countTarget.value ? t('performance.countTargetUnit') : t('performance.amountTargetUnit')
)
const quarterlyTargets = computed(() =>
  buildQuarterTargets(monthlyTargets.value, countTarget.value)
)
const annualTarget = computed(() => sumTargetValues(monthlyTargets.value, countTarget.value))
const targetTypeOptions = computed(() => [
  { value: 1, label: t('performance.targetContractPrice') },
  { value: 2, label: t('performance.targetReceivablePrice') },
  { value: 3, label: t('performance.targetFollowUpCount') },
  { value: 4, label: t('performance.targetCustomerCount') },
  { value: 5, label: t('performance.targetBusinessCount') }
])

const applySelectedTarget = () => {
  const target = targetList.value.find((item) => item.targetType === targetType.value)
  targetExists.value = Boolean(target)
  monthlyTargets.value = target ? [...target.monthlyTargets] : Array(12).fill('0')
}

const loadData = async () => {
  const selectedScopeId = scopeId.value
  if (!Number.isInteger(targetYear.value) || selectedScopeId == null) return
  loading.value = true
  try {
    targetList.value = (await PerformanceTargetApi.getList({
      scopeType: scopeType.value,
      scopeId: selectedScopeId,
      targetYear: targetYear.value
    })) as PerformanceTargetRespVO[]
    applySelectedTarget()
  } finally {
    loading.value = false
  }
}

const handleScopeTypeChange = async () => {
  if (scopeType.value === 1) scopeId.value = 0
  if (scopeType.value === 2) scopeId.value = props.queryParams.deptId
  if (scopeType.value === 3) scopeId.value = props.queryParams.userId || userList.value[0]?.id
  await loadData()
}

const saveTarget = async () => {
  const selectedScopeId = scopeId.value
  if (selectedScopeId == null) return
  if (monthlyTargets.value.some((value) => !isValidTargetValue(value, countTarget.value))) {
    message.error(t('performance.targetValueInvalid'))
    return
  }
  submitting.value = true
  try {
    const updating = targetExists.value
    await PerformanceTargetApi.save({
      scopeType: scopeType.value,
      scopeId: selectedScopeId,
      targetYear: targetYear.value,
      targetType: targetType.value,
      monthlyTargets: monthlyTargets.value.map((value) => value.trim())
    })
    message.success(t(updating ? 'common.updateSuccess' : 'common.createSuccess'))
    await loadData()
  } finally {
    submitting.value = false
  }
}

const deleteTarget = async () => {
  const selectedScopeId = scopeId.value
  if (selectedScopeId == null) return
  await message.delConfirm()
  submitting.value = true
  try {
    await PerformanceTargetApi.delete({
      scopeType: scopeType.value,
      scopeId: selectedScopeId,
      targetYear: targetYear.value,
      targetType: targetType.value
    })
    message.success(t('common.delSuccess'))
    await loadData()
  } finally {
    submitting.value = false
  }
}

defineExpose({ loadData })
onMounted(async () => {
  const [depts, users] = await Promise.all([
    DeptApi.getSimpleDeptList(),
    UserApi.getSimpleUserList()
  ])
  deptList.value = handleTree(depts)
  userList.value = users
  await loadData()
})
</script>
