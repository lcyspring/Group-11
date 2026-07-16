<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="loadActive">
      <el-tab-pane :label="t('crm.erpFulfillment.customerMappings')" name="customer">
        <div class="mb-12px text-right">
          <el-button v-hasPermi="['crm:erp-mapping:update']" type="primary" @click="openCustomer()">
            {{ t('crm.erpFulfillment.addCustomerMapping') }}
          </el-button>
        </div>
        <el-table v-loading="customerLoading" :data="customerList" border>
          <el-table-column :label="t('crm.erpFulfillment.crmCustomer')" min-width="180" prop="crmCustomerName" />
          <el-table-column :label="t('crm.erpFulfillment.erpCustomer')" min-width="180" prop="erpCustomerName" />
          <el-table-column :label="t('crm.erpFulfillment.remark')" min-width="180" prop="remark" />
          <el-table-column :label="t('common.action')" fixed="right" width="150">
            <template #default="{ row }">
              <el-button v-hasPermi="['crm:erp-mapping:update']" link type="primary" @click="openCustomer(row)">{{ t('common.edit') }}</el-button>
              <el-button v-hasPermi="['crm:erp-mapping:delete']" link type="danger" @click="removeCustomer(row.id)">{{ t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination v-model:page="customerQuery.pageNo" v-model:limit="customerQuery.pageSize" :total="customerTotal" @pagination="loadCustomers" />
      </el-tab-pane>
      <el-tab-pane :label="t('crm.erpFulfillment.productMappings')" name="product">
        <div class="mb-12px text-right">
          <el-button v-hasPermi="['crm:erp-mapping:update']" type="primary" @click="openProduct()">
            {{ t('crm.erpFulfillment.addProductMapping') }}
          </el-button>
        </div>
        <el-table v-loading="productLoading" :data="productList" border>
          <el-table-column :label="t('crm.erpFulfillment.crmProduct')" min-width="200">
            <template #default="{ row }">{{ row.crmProductNo }} · {{ row.crmProductName }}</template>
          </el-table-column>
          <el-table-column :label="t('crm.erpFulfillment.erpProduct')" min-width="200">
            <template #default="{ row }">{{ row.erpProductBarCode || '-' }} · {{ row.erpProductName }}</template>
          </el-table-column>
          <el-table-column :label="t('crm.erpFulfillment.remark')" min-width="180" prop="remark" />
          <el-table-column :label="t('common.action')" fixed="right" width="150">
            <template #default="{ row }">
              <el-button v-hasPermi="['crm:erp-mapping:update']" link type="primary" @click="openProduct(row)">{{ t('common.edit') }}</el-button>
              <el-button v-hasPermi="['crm:erp-mapping:delete']" link type="danger" @click="removeProduct(row.id)">{{ t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination v-model:page="productQuery.pageNo" v-model:limit="productQuery.pageSize" :total="productTotal" @pagination="loadProducts" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <Dialog v-model="customerVisible" :title="t('crm.erpFulfillment.customerMapping')" width="560px">
    <el-form ref="customerFormRef" :model="customerForm" :rules="mappingRules" label-width="120px">
      <el-form-item :label="t('crm.erpFulfillment.crmCustomer')" prop="crmId">
        <el-select v-model="customerForm.crmId" filterable class="w-full">
          <el-option v-for="item in crmCustomers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.erpFulfillment.erpCustomer')" prop="erpId">
        <el-select v-model="customerForm.erpId" filterable class="w-full">
          <el-option v-for="item in erpCustomers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.erpFulfillment.remark')" prop="remark"><el-input v-model="customerForm.remark" maxlength="500" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="saving" @click="saveCustomer">{{ t('common.ok') }}</el-button><el-button @click="customerVisible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>

  <Dialog v-model="productVisible" :title="t('crm.erpFulfillment.productMapping')" width="560px">
    <el-form ref="productFormRef" :model="productForm" :rules="mappingRules" label-width="120px">
      <el-form-item :label="t('crm.erpFulfillment.crmProduct')" prop="crmId">
        <el-select v-model="productForm.crmId" filterable class="w-full">
          <el-option v-for="item in crmProducts" :key="item.id" :label="`${item.no} · ${item.name}`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.erpFulfillment.erpProduct')" prop="erpId">
        <el-select v-model="productForm.erpId" filterable class="w-full">
          <el-option v-for="item in erpProducts" :key="item.id" :label="`${item.barCode || '-'} · ${item.name}`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('crm.erpFulfillment.remark')" prop="remark"><el-input v-model="productForm.remark" maxlength="500" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" :loading="saving" @click="saveProduct">{{ t('common.ok') }}</el-button><el-button @click="productVisible = false">{{ t('common.cancel') }}</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import * as FulfillmentApi from '@/api/crm/fulfillment'
import * as CustomerApi from '@/api/crm/customer'
import * as CrmProductApi from '@/api/crm/product'
import { CustomerApi as ErpCustomerApi } from '@/api/erp/sale/customer'
import { ProductApi as ErpProductApi } from '@/api/erp/product/product'

defineOptions({ name: 'CrmErpFulfillmentMapping' })
const { t } = useI18n()
const message = useMessage()
const activeTab = ref('customer')
const customerLoading = ref(false)
const productLoading = ref(false)
const saving = ref(false)
const customerList = ref<FulfillmentApi.CustomerMappingVO[]>([])
const productList = ref<FulfillmentApi.ProductMappingVO[]>([])
const customerTotal = ref(0)
const productTotal = ref(0)
const customerQuery = reactive({ pageNo: 1, pageSize: 10 })
const productQuery = reactive({ pageNo: 1, pageSize: 10 })
const loadCustomers = async () => {
  customerLoading.value = true
  try {
    const result = await FulfillmentApi.getCustomerMappingPage(customerQuery)
    customerList.value = result.list
    customerTotal.value = result.total
  } finally { customerLoading.value = false }
}
const loadProducts = async () => {
  productLoading.value = true
  try {
    const result = await FulfillmentApi.getProductMappingPage(productQuery)
    productList.value = result.list
    productTotal.value = result.total
  } finally { productLoading.value = false }
}
const loadActive = () => activeTab.value === 'customer' ? loadCustomers() : loadProducts()

const crmCustomers = ref<any[]>([])
const erpCustomers = ref<any[]>([])
const crmProducts = ref<any[]>([])
const erpProducts = ref<any[]>([])
const loadReferences = async () => {
  const [customers, erpCustomerData, products, erpProductData] = await Promise.all([
    CustomerApi.getCustomerSimpleList(), ErpCustomerApi.getCustomerSimpleList(),
    CrmProductApi.getProductSimpleList(), ErpProductApi.getProductSimpleList()
  ])
  crmCustomers.value = customers
  erpCustomers.value = erpCustomerData
  crmProducts.value = products
  erpProducts.value = erpProductData
}
const customerVisible = ref(false)
const productVisible = ref(false)
const customerFormRef = ref()
const productFormRef = ref()
const customerForm = ref<FulfillmentApi.ErpMappingSaveReqVO>({ crmId: 0, erpId: 0, remark: '' })
const productForm = ref<FulfillmentApi.ErpMappingSaveReqVO>({ crmId: 0, erpId: 0, remark: '' })
const mappingRules = reactive({
  crmId: [{ required: true, message: t('crm.erpFulfillment.crmRequired'), trigger: 'change' }],
  erpId: [{ required: true, message: t('crm.erpFulfillment.erpRequired'), trigger: 'change' }]
})
const openCustomer = async (row?: FulfillmentApi.CustomerMappingVO) => {
  await loadReferences()
  customerForm.value = { crmId: row?.crmCustomerId || 0, erpId: row?.erpCustomerId || 0, remark: row?.remark || '' }
  customerVisible.value = true
  nextTick(() => customerFormRef.value?.clearValidate())
}
const openProduct = async (row?: FulfillmentApi.ProductMappingVO) => {
  await loadReferences()
  productForm.value = { crmId: row?.crmProductId || 0, erpId: row?.erpProductId || 0, remark: row?.remark || '' }
  productVisible.value = true
  nextTick(() => productFormRef.value?.clearValidate())
}
const saveCustomer = async () => {
  if (!(await customerFormRef.value?.validate())) return
  saving.value = true
  try { await FulfillmentApi.saveCustomerMapping(customerForm.value); customerVisible.value = false; message.success(t('common.updateSuccess')); await loadCustomers() }
  finally { saving.value = false }
}
const saveProduct = async () => {
  if (!(await productFormRef.value?.validate())) return
  saving.value = true
  try { await FulfillmentApi.saveProductMapping(productForm.value); productVisible.value = false; message.success(t('common.updateSuccess')); await loadProducts() }
  finally { saving.value = false }
}
const removeCustomer = async (id: number) => { await message.delConfirm(); await FulfillmentApi.deleteCustomerMapping(id); await loadCustomers() }
const removeProduct = async (id: number) => { await message.delConfirm(); await FulfillmentApi.deleteProductMapping(id); await loadProducts() }
onMounted(loadCustomers)
</script>
