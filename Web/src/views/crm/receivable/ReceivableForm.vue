<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('receivable.no')" prop="no">
            <el-input v-model="formData.no" disabled :placeholder="t('contract.noAutoGenerate')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('receivable.ownerUserId')" prop="ownerUserId">
            <el-select
              v-model="formData.ownerUserId"
              :disabled="formType !== 'create'"
              class="w-1/1"
            >
              <el-option
                v-for="item in userOptions"
                :key="item.id"
                :label="item.nickname"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('receivable.customerName')" prop="customerId">
            <el-select
              v-model="formData.customerId"
              :disabled="formType !== 'create'"
              class="w-1/1"
              clearable
              filterable
              :placeholder="t('receivable.customerPlaceholder')"
              @change="handleCustomerChange"
            >
              <el-option
                v-for="item in customerList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('receivable.contractName')" prop="contractId">
            <el-select
              v-model="formData.contractId"
              :disabled="formType !== 'create'"
              class="w-1/1"
              filterable
              :placeholder="t('receivable.contractPlaceholder')"
              @change="handleContractChange"
            >
              <el-option
                v-for="data in contractList"
                :key="data.id"
                :disabled="data.remainingReceivablePrice <= 0"
                :label="getContractCandidateLabel(data)"
                :value="data.id"
              >
                <div class="flex items-center justify-between gap-12px">
                  <span>{{ data.customerName }} · {{ data.no }} · {{ data.name }}</span>
                  <span class="text-12px text-gray-500">
                    {{ t('receivable.remainingReceivablePrice') }}：{{ data.remainingReceivablePrice }}
                  </span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('receivable.period')" prop="planId">
            <el-select
              v-model="formData.planId"
              :disabled="formType !== 'create' || !formData.contractId"
              class="!w-1/1"
              :placeholder="t('common.selectPlaceholder')"
              @change="handleReceivablePlanChange"
            >
              <el-option
                v-for="data in receivablePlanList"
                :key="data.id"
                :disabled="Boolean(data.receivableId)"
                :label="'第 ' + data.period + ' 期'"
                :value="data.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('receivable.returnType')" prop="returnType">
            <el-select
              v-model="formData.returnType"
              class="w-1/1"
              :placeholder="t('common.selectPlaceholder')"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_RECEIVABLE_RETURN_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('receivable.price')" prop="price">
            <el-input-number
              v-model="formData.price"
              :min="0.01"
              :precision="2"
              class="!w-100%"
              controls-position="right"
              :placeholder="t('receivable.pricePlaceholder')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('receivable.returnTime')" prop="returnTime">
            <el-date-picker
              v-model="formData.returnTime"
              :placeholder="t('receivable.returnTime')"
              type="date"
              value-format="x"
              class="!w-100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="t('receivable.remark')" prop="remark">
            <el-input
              v-model="formData.remark"
              :placeholder="t('customer.remarkPlaceholder')"
              type="textarea"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">{{
        t('common.confirm')
      }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as ReceivablePlanApi from '@/api/crm/receivable/plan'
import * as ReceivableApi from '@/api/crm/receivable'
import { ReceivableVO } from '@/api/crm/receivable'
import * as UserApi from '@/api/system/user'
import * as CustomerApi from '@/api/crm/customer'
import * as ContractApi from '@/api/crm/contract'
import { useUserStore } from '@/store/modules/user'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { filterReceivableCandidates } from './receivableCandidates.mjs'

const { t } = useI18n('crm') // 国际化
const message = useMessage() // 消息弹窗
const userOptions = ref<UserApi.UserVO[]>([]) // 用户列表
const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<ReceivableApi.ReceivableVO>({} as ReceivableApi.ReceivableVO)
const formRules = reactive({
  customerId: [{ required: true, message: t('receivable.customerIdRequired'), trigger: 'blur' }],
  contractId: [{ required: true, message: t('receivable.contractIdRequired'), trigger: 'blur' }],
  returnTime: [{ required: true, message: t('receivable.returnTimeRequired'), trigger: 'blur' }],
  price: [{ required: true, message: t('receivable.priceRequired'), trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const customerList = ref<CustomerApi.CustomerVO[]>([]) // 客户列表
const allContractCandidates = ref<ContractApi.ReceivableContractCandidateVO[]>([])
const contractList = computed(() =>
  filterReceivableCandidates(allContractCandidates.value, formData.value.customerId)
)
const receivablePlanList = ref<ReceivablePlanApi.ReceivablePlanVO[]>([]) // 回款计划列表

/** 打开弹窗 */
const open = async (
  type: string,
  id?: number,
  receivablePlan?: ReceivablePlanApi.ReceivablePlanVO
) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type, { scope: 'common' })
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    const [users, customers, candidates] = await Promise.all([
      UserApi.getSimpleUserList(),
      CustomerApi.getCustomerSimpleList(),
      type === 'create' ? ContractApi.getReceivableContractCandidates() : Promise.resolve([])
    ])
    userOptions.value = users
    customerList.value = customers
    allContractCandidates.value = candidates
    // 修改时，设置数据，并补入当前只读合同供下拉框回显
    if (id) {
      const data = (await ReceivableApi.getReceivable(id)) as ReceivableVO
      formData.value = data
      formData.value.contractId = data?.contract?.id
      if (data.contract?.id) {
        allContractCandidates.value = [
          {
            id: data.contract.id,
            no: data.contract.no,
            name: data.contract.name || '',
            customerId: data.customerId!,
            customerName: data.customerName,
            totalPrice: data.contract.totalPrice,
            totalReceivablePrice: data.contract.totalPrice,
            remainingReceivablePrice: 0
          }
        ]
      }
    }
    mergeCandidateCustomers()
    // 默认新建时选中自己
    if (formType.value === 'create') {
      formData.value.ownerUserId = useUserStore().getUser.id
    }
    // 从回款计划创建回款
    if (receivablePlan) {
      formData.value.customerId = receivablePlan.customerId
      formData.value.contractId = receivablePlan.contractId
      await handleContractChange(receivablePlan.contractId)
      if (receivablePlan.id) {
        formData.value.planId = receivablePlan.id
        formData.value.price = receivablePlan.price
        formData.value.returnType = receivablePlan.returnType
      }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as ReceivableApi.ReceivableVO
    if (formType.value === 'create') {
      await ReceivableApi.createReceivable(data)
      message.success(t('common.createSuccess'))
    } else {
      await ReceivableApi.updateReceivable(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {} as ReceivableApi.ReceivableVO
  formRef.value?.resetFields()
}

/** 处理切换客户 */
const handleCustomerChange = () => {
  // 重置合同编号
  formData.value.contractId = undefined
  formData.value.planId = undefined
  receivablePlanList.value = []
}

/** 处理切换合同 */
const handleContractChange = async (contractId: number) => {
  // 重置回款计划编号
  formData.value.planId = undefined
  if (contractId) {
    const contract = allContractCandidates.value.find((item) => item.id === contractId)
    if (!contract) return
    formData.value.customerId = contract.customerId
    formData.value.price = contract.remainingReceivablePrice
    // 获得回款计划列表
    receivablePlanList.value = []
    receivablePlanList.value = await ReceivablePlanApi.getReceivablePlanSimpleList(
      contract.customerId,
      contractId
    )
  }
}

const getContractCandidateLabel = (candidate: ContractApi.ReceivableContractCandidateVO) =>
  `${candidate.customerName || '-'} · ${candidate.no} · ${candidate.name} · ${t('receivable.remainingReceivablePrice')} ${candidate.remainingReceivablePrice}`

const mergeCandidateCustomers = () => {
  const customerIds = new Set(customerList.value.map((customer) => customer.id))
  allContractCandidates.value.forEach((candidate) => {
    if (!customerIds.has(candidate.customerId)) {
      customerList.value.push({
        id: candidate.customerId,
        name: candidate.customerName || String(candidate.customerId)
      } as CustomerApi.CustomerVO)
      customerIds.add(candidate.customerId)
    }
  })
}

/** 处理切换回款计划 */
const handleReceivablePlanChange = (planId: number) => {
  if (!planId) {
    return
  }
  const receivablePlan = receivablePlanList.value.find((item) => item.id === planId)
  if (!receivablePlan) {
    return
  }
  formData.value.price = receivablePlan.price
  formData.value.returnType = receivablePlan.returnType
}
</script>
