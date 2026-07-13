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
          <el-form-item :label="t('name')" prop="name">
            <el-input
              v-model="formData.name"
              :placeholder="t('namePlaceholder')"
              @input="resetDuplicateCheck"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('source')" prop="source">
            <el-select v-model="formData.source" :placeholder="t('sourcePlaceholder')" class="w-1/1">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_SOURCE)"
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
          <el-form-item :label="t('parentCustomer')" prop="parentCustomerId">
            <el-select
              v-model="formData.parentCustomerId"
              class="w-1/1"
              clearable
              filterable
              :placeholder="t('parentCustomerPlaceholder')"
            >
              <el-option
                v-for="item in availableParentCustomers"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('mobile')" prop="mobile">
            <el-input
              v-model="formData.mobile"
              :placeholder="t('mobilePlaceholder')"
              @input="resetDuplicateCheck"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('ownerUserId')" prop="ownerUserId">
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
          <el-form-item :label="t('telephone')" prop="telephone">
            <el-input v-model="formData.telephone" :placeholder="t('telephonePlaceholder')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('email')" prop="email">
            <el-input v-model="formData.email" :placeholder="t('emailPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('wechat')" prop="wechat">
            <el-input v-model="formData.wechat" :placeholder="t('wechatPlaceholder')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('qq')" prop="qq">
            <el-input v-model="formData.qq" :placeholder="t('qqPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('industryId')" prop="industryId">
            <el-select v-model="formData.industryId" :placeholder="t('industryPlaceholder')" class="w-1/1">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_INDUSTRY)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('level')" prop="level">
            <el-select v-model="formData.level" :placeholder="t('levelPlaceholder')" class="w-1/1">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.CRM_CUSTOMER_LEVEL)"
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
          <el-form-item :label="t('areaId')" prop="areaId">
            <el-cascader
              v-model="formData.areaId"
              :options="areaList"
              :props="defaultProps"
              class="w-1/1"
              clearable
              filterable
              :placeholder="t('areaPlaceholder')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('detailAddress')" prop="detailAddress">
            <el-input v-model="formData.detailAddress" :placeholder="t('detailAddressPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('contactNextTime')" prop="contactNextTime">
            <el-date-picker
              v-model="formData.contactNextTime"
              :placeholder="t('contactNextTimePlaceholder')"
              type="datetime"
              value-format="x"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('remark')" prop="remark">
            <el-input type="textarea" v-model="formData.remark" :placeholder="t('remarkPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="formType === 'create'">
        <el-col :span="24">
          <el-form-item :label="t('duplicateCheck')">
            <el-button :loading="duplicateChecking" @click="checkDuplicates">
              {{ t('duplicateCheckAction') }}
            </el-button>
            <span v-if="duplicateChecked && duplicateCandidates.length === 0" class="ml-12px text-success">
              {{ t('duplicateNone') }}
            </span>
          </el-form-item>
          <el-alert
            v-if="duplicateCandidates.length > 0"
            :closable="false"
            :title="t('duplicateFound', { count: duplicateCandidates.length })"
            type="warning"
            show-icon
            class="mb-16px"
          >
            <div v-for="candidate in duplicateCandidates" :key="candidate.id">
              #{{ candidate.id }} · {{ candidate.name }} · {{ candidate.mobile || t('duplicateNoMobile') }}
            </div>
          </el-alert>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as CustomerApi from '@/api/crm/customer'
import * as AreaApi from '@/api/system/area'
import { defaultProps } from '@/utils/tree'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n('crm.customer') // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const duplicateChecking = ref(false)
const duplicateChecked = ref(false)
const duplicateCandidates = ref<CustomerApi.CustomerDuplicateVO[]>([])
const areaList = ref([]) // 地区列表
const userOptions = ref<UserApi.UserVO[]>([]) // 用户列表
const customerOptions = ref<CustomerApi.CustomerVO[]>([]) // 当前用户可见的上级客户候选
const formData = ref({
  id: undefined,
  name: undefined,
  parentCustomerId: undefined,
  contactNextTime: undefined,
  ownerUserId: 0,
  mobile: undefined,
  telephone: undefined,
  qq: undefined,
  wechat: undefined,
  email: undefined,
  areaId: undefined,
  detailAddress: undefined,
  industryId: undefined,
  level: undefined,
  source: undefined,
  remark: undefined
})
const formRules = reactive({
  name: [{ required: true, message: t('nameRequired'), trigger: 'blur' }],
  ownerUserId: [{ required: true, message: t('ownerUserRequired'), trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const availableParentCustomers = computed(() =>
  customerOptions.value.filter((customer) => customer.id !== formData.value.id)
)

const resetDuplicateCheck = () => {
  duplicateChecked.value = false
  duplicateCandidates.value = []
}

/** 查询疑似重复客户 */
const checkDuplicates = async () => {
  const name = formData.value.name?.trim()
  const mobile = formData.value.mobile?.trim()
  if (!name && !mobile) {
    message.warning(t('duplicateConditionRequired'))
    return []
  }
  duplicateChecking.value = true
  try {
    duplicateCandidates.value = await CustomerApi.getDuplicateCustomerList({ name, mobile })
    duplicateChecked.value = true
    return duplicateCandidates.value
  } finally {
    duplicateChecking.value = false
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CustomerApi.getCustomer(id)
    } finally {
      formLoading.value = false
    }
  }
  // 获得地区列表
  areaList.value = await AreaApi.getAreaTree()
  // 获得用户列表
  userOptions.value = await UserApi.getSimpleUserList()
  // 获得当前用户可见的上级客户候选；后端仍会校验同租户、禁止自关联和循环
  customerOptions.value = await CustomerApi.getCustomerSimpleList()
  // 默认新建时选中自己
  if (formType.value === 'create') {
    formData.value.ownerUserId = useUserStore().getUser.id
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
  let duplicateCheckConfirmed = false
  if (formType.value === 'create') {
    const candidates = duplicateChecked.value ? duplicateCandidates.value : await checkDuplicates()
    const normalizedName = formData.value.name?.trim().toLocaleLowerCase()
    if (candidates.some((candidate) => candidate.name.trim().toLocaleLowerCase() === normalizedName)) {
      message.error(t('duplicateNameBlocked'))
      return
    }
    if (candidates.length > 0) {
      await message.confirm(t('duplicateContinueConfirm', { count: candidates.length }))
      duplicateCheckConfirmed = true
    }
  }
  // 提交请求
  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      duplicateCheckConfirmed
    } as unknown as CustomerApi.CustomerVO
    if (formType.value === 'create') {
      await CustomerApi.createCustomer(data)
      message.success(t('common.createSuccess'))
    } else {
      await CustomerApi.updateCustomer(data)
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
  resetDuplicateCheck()
  formData.value = {
    id: undefined,
    name: undefined,
    parentCustomerId: undefined,
    contactNextTime: undefined,
    ownerUserId: 0,
    mobile: undefined,
    telephone: undefined,
    qq: undefined,
    wechat: undefined,
    email: undefined,
    areaId: undefined,
    detailAddress: undefined,
    industryId: undefined,
    level: undefined,
    source: undefined,
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
