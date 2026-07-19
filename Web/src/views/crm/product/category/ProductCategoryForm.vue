<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('category.parentId')" prop="parentId">
            <el-select
              v-model="formData.parentId"
              class="w-1/1"
              :placeholder="t('category.parentIdPlaceholder')"
            >
              <el-option :key="0" :label="t('category.topCategory')" :value="0" />
              <el-option
                v-for="item in productCategoryList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('common.name')" prop="name">
            <el-input v-model="formData.name" :placeholder="t('category.namePlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">
        {{ t('common.confirm') }}
      </el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">
        {{ t('common.cancel') }}
      </el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import * as ProductCategoryApi from '@/api/crm/product/category'

defineOptions({ name: 'CrmProductCategoryForm' })

type FormType = 'create' | 'update'
type ProductCategoryFormData = {
  id?: number
  name: string
  parentId: number
}

const { t } = useI18n('crm.product')
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<FormType>('create')
const createEmptyForm = (): ProductCategoryFormData => ({ name: '', parentId: 0 })
const formData = ref<ProductCategoryFormData>(createEmptyForm())
const formRules = reactive<FormRules<ProductCategoryFormData>>({
  name: [{ required: true, message: t('category.nameRequired'), trigger: 'blur' }],
  parentId: [{ required: true, message: t('category.parentIdRequired'), trigger: 'blur' }]
})
const formRef = ref<FormInstance>()
const productCategoryList = ref<ProductCategoryApi.ProductCategoryVO[]>([])

/** 打开弹窗 */
const open = async (type: FormType, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t(type === 'create' ? 'category.createTitle' : 'category.updateTitle')
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    const categoryListPromise = ProductCategoryApi.getProductCategoryList({ parentId: 0 })
    if (type === 'update' && id !== undefined) {
      formData.value = await ProductCategoryApi.getProductCategory(id)
    }
    productCategoryList.value = await categoryListPromise
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits<{ success: [] }>()
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (formType.value === 'update' && formData.value.id === undefined) return
  formLoading.value = true
  try {
    const { id, name, parentId } = formData.value
    if (formType.value === 'create') {
      await ProductCategoryApi.createProductCategory({ name, parentId })
      message.success(t('common.createSuccess'))
    } else {
      if (id === undefined) return
      await ProductCategoryApi.updateProductCategory({ id, name, parentId })
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = createEmptyForm()
  formRef.value?.resetFields()
}
</script>
