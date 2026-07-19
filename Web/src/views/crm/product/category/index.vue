<template>
  <doc-alert title="【产品】产品管理、产品分类" url="https://doc.iocoder.cn/crm/product/" />

  <ContentWrap>
    <!-- 搜索工作区 -->
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" label-width="auto">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('common.name')" prop="name">
            <el-input
              v-model="queryParams.name"
              class="!w-240px"
              clearable
              :placeholder="t('common.namePlaceholder')"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item>
            <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> {{ t('common.search') }}</el-button>
            <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> {{ t('common.reset') }}</el-button>
            <el-button
              v-hasPermi="['crm:product-category:create']"
              plain
              type="primary"
              @click="openForm('create')"
            >
              <Icon icon="ep:plus" class="mr-5px" /> {{ t('action.add') }}
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" default-expand-all row-key="id" table-layout="auto">
      <el-table-column align="center" fixed="left" :label="t('common.id')" prop="id" />
      <el-table-column :label="t('common.name')" align="center" prop="name" />
      <el-table-column
        :label="t('common.createTime')"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        min-width="180"
      />
      <el-table-column :label="t('common.action')" align="center" width="220">
        <template #default="{ row }">
          <TableActions>
            <el-button
              v-hasPermi="['crm:product-category:update']"
              link
              type="primary"
              @click="openForm('update', row.id)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-hasPermi="['crm:product-category:delete']"
              link
              type="danger"
              @click="handleDelete(row.id)"
            >
              {{ t('common.delete') }}
            </el-button>
          </TableActions>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <ProductCategoryForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import * as ProductCategoryApi from '@/api/crm/product/category'
import ProductCategoryForm from './ProductCategoryForm.vue'
import { handleTree } from '@/utils/tree'

defineOptions({ name: 'CrmProductCategory' })

const message = useMessage()
const { t } = useI18n('crm.product')
const loading = ref(true)
const list = ref<ProductCategoryApi.ProductCategoryVO[]>([])
const queryParams = reactive<ProductCategoryApi.ProductCategoryListReqVO>({})
const queryFormRef = ref<FormInstance>()
/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ProductCategoryApi.getProductCategoryList(queryParams)
    list.value = handleTree(data, 'id', 'parentId')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getList()

/** 重置按钮操作 */
const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  await getList()
}

/** 添加/修改操作 */
const formRef = ref<InstanceType<typeof ProductCategoryForm>>()
const openForm = (type: 'create' | 'update', id?: number) => formRef.value?.open(type, id)

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
  } catch {
    return
  }
  await ProductCategoryApi.deleteProductCategory(id)
  message.success(t('common.delSuccess'))
  await getList()
}

onMounted(getList)
</script>
