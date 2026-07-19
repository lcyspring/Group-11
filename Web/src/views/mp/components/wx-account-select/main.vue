<template>
  <div v-if="!loading && accountList.length === 0" class="flex items-center gap-8px">
    <el-text type="info">
      {{ loadFailed ? t('mp.common.accountLoadFailed') : t('mp.common.noAccountConfig') }}
    </el-text>
    <el-button v-if="loadFailed" link type="primary" @click="handleQuery">
      {{ t('mp.common.retry') }}
    </el-button>
    <el-button v-else link type="primary" @click="openAccountManagement">
      {{ t('mp.common.configureAccount') }}
    </el-button>
  </div>
  <el-select
    v-else
    v-model="account.id"
    :loading="loading"
    :placeholder="t('mp.common.selectAccount')"
    class="!w-240px"
    @change="onChanged"
  >
    <el-option v-for="item in accountList" :key="item.id" :label="item.name" :value="item.id" />
  </el-select>
</template>

<script lang="ts" setup>
import * as MpAccountApi from '@/api/mp/account'

const { t } = useI18n('mp') // 国际化
const { push } = useRouter() // 路由

defineOptions({ name: 'WxAccountSelect' })

const props = defineProps<{
  modelValue?: number
}>()

const account: MpAccountApi.AccountVO = reactive({
  id: -1,
  name: ''
})

const accountList = ref<MpAccountApi.AccountVO[]>([])
const loading = ref(false)
const loadFailed = ref(false)

const emit = defineEmits<{
  (e: 'change', id: number, name: string)
  (e: 'update:modelValue', id: number)
  (e: 'unavailable', reason: 'empty' | 'error')
}>()

const handleQuery = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    accountList.value = await MpAccountApi.getSimpleAccountList()
    if (accountList.value.length === 0) {
      emit('unavailable', 'empty')
      return
    }
    const selected = accountList.value.find((item) => item.id === props.modelValue)
    account.id = selected?.id ?? accountList.value[0].id
    if (account.id) {
      account.name = selected?.name ?? accountList.value[0].name
      emit('update:modelValue', account.id)
      emit('change', account.id, account.name)
    }
  } catch {
    accountList.value = []
    loadFailed.value = true
    emit('unavailable', 'error')
  } finally {
    loading.value = false
  }
}

const onChanged = (id?: number) => {
  const found = accountList.value.find((v) => v.id === id)
  if (found) {
    account.name = found ? found.name : ''
    emit('update:modelValue', found.id)
    emit('change', account.id, account.name)
  }
}

const openAccountManagement = () => push({ name: 'MpAccount' })

/** 初始化 */
onMounted(() => {
  void handleQuery()
})
</script>
