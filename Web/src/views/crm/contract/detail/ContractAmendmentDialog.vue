<template>
  <Dialog v-model="visible" :title="t('crm.contract.amendmentTitle')" width="1180px">
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="130px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.amendmentName')" prop="title">
            <el-input v-model="form.title" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.name')" prop="contractName">
            <el-input v-model="form.contractName" maxlength="255" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.startTime')">
            <el-date-picker v-model="form.startTime" type="date" value-format="x" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.endTime')">
            <el-date-picker v-model="form.endTime" type="date" value-format="x" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.signContactId')">
            <el-select v-model="form.signContactId" clearable class="w-full">
              <el-option v-for="item in contacts" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.signUserId')">
            <el-select v-model="form.signUserId" clearable class="w-full">
              <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('crm.contract.amendmentReason')" prop="reason">
        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      </el-form-item>
      <ContractProductForm ref="productFormRef" :products="form.products" :disabled="false" />
      <el-row :gutter="16" class="mt-16px">
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.discountPercent')" prop="discountPercent">
            <el-input-number v-model="form.discountPercent" :min="0" :max="100" :precision="2" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('crm.contract.amendmentAmountAfter')">
            <el-input :model-value="totalPrice" disabled :formatter="erpPriceInputFormatter" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('crm.contract.remark')">
        <el-input v-model="form.remark" type="textarea" maxlength="2000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="submit">{{ t('common.save') }}</el-button>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as AmendmentApi from '@/api/crm/contract/amendment'
import * as ContractApi from '@/api/crm/contract'
import * as ContactApi from '@/api/crm/contact'
import * as UserApi from '@/api/system/user'
import ContractProductForm from '@/views/crm/contract/components/ContractProductForm.vue'
import { erpPriceInputFormatter, erpPriceMultiply, generateUUID } from '@/utils'
import type { FormInstance } from 'element-plus'

const props = defineProps<{ contract: ContractApi.ContractVO }>()
const emit = defineEmits<{ success: [] }>()
const { t } = useI18n()
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const productFormRef = ref()
const users = ref<UserApi.UserVO[]>([])
const contacts = ref<ContactApi.ContactVO[]>([])
const form = ref<AmendmentApi.ContractAmendmentVO>(emptyForm())
const rules = {
  title: [{ required: true, message: t('crm.contract.amendmentNameRequired'), trigger: 'blur' }],
  contractName: [{ required: true, message: t('crm.contract.nameRequired'), trigger: 'blur' }],
  reason: [{ required: true, message: t('crm.contract.amendmentReasonRequired'), trigger: 'blur' }],
  discountPercent: [{ required: true, trigger: 'change' }]
}

function emptyForm(): AmendmentApi.ContractAmendmentVO {
  return {
    contractId: props.contract.id,
    clientRequestId: generateUUID(),
    title: '',
    reason: '',
    contractName: props.contract.name,
    startTime: props.contract.startTime ? Number(props.contract.startTime) : undefined,
    endTime: props.contract.endTime ? Number(props.contract.endTime) : undefined,
    discountPercent: props.contract.discountPercent || 0,
    signContactId: props.contract.signContactId,
    signUserId: props.contract.signUserId,
    remark: props.contract.remark,
    products: (props.contract.products || []).map((item: any) => ({ ...item }))
  }
}

const totalPrice = computed(() => {
  const subtotal = form.value.products.reduce(
    (sum, item) => sum + (erpPriceMultiply(item.contractPrice || 0, item.count || 0) ?? 0),
    0
  )
  return subtotal - (erpPriceMultiply(subtotal, (form.value.discountPercent || 0) / 100) ?? 0)
})

const open = async (id?: number) => {
  visible.value = true
  loading.value = true
  try {
    const [userList, contactList] = await Promise.all([
      UserApi.getSimpleUserList(),
      ContactApi.getSimpleContactList()
    ])
    users.value = userList
    contacts.value = contactList.filter((item) => item.customerId === props.contract.customerId)
    form.value = id ? await AmendmentApi.getContractAmendment(props.contract.id, id) : emptyForm()
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!formRef.value || !(await formRef.value.validate())) return
  if (productFormRef.value && !(await productFormRef.value.validate())) return
  loading.value = true
  try {
    const payload = {
      ...form.value,
      startTime: form.value.startTime ? Number(form.value.startTime) : undefined,
      endTime: form.value.endTime ? Number(form.value.endTime) : undefined,
      products: form.value.products.map((item) => ({
        id: item.id,
        productId: item.productId,
        contractPrice: item.contractPrice,
        count: item.count
      }))
    }
    const updating = Boolean(form.value.id)
    if (updating) await AmendmentApi.updateContractAmendment(payload)
    else await AmendmentApi.createContractAmendment(payload)
    message.success(t(updating ? 'common.updateSuccess' : 'common.createSuccess'))
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
