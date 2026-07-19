<template>
  <ContentWrap :title="t('oa.document.title')">
    <el-button type="primary" @click="visible = true">{{ t('oa.document.create') }}</el-button>
    <el-table class="mt-12px" :data="list">
      <el-table-column prop="name" :label="t('oa.document.name')" />
      <el-table-column prop="currentVersion" :label="t('oa.document.version')" />
      <el-table-column fixed="right" :label="t('common.operation')">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="t('oa.document.create')">
      <el-form :model="form">
        <el-form-item :label="t('oa.document.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('oa.document.description')">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import * as Api from '@/api/bpm/oaDocument'

defineOptions({ name: 'OaDocument' })

const { t } = useI18n('bpm')
const message = useMessage()
const visible = ref(false)
const list = ref<Api.OaDocumentVO[]>([])
const form = reactive<Api.OaDocumentVO>({ name: '', visibility: 0 })

const load = async () => {
  list.value = await Api.list()
}

const save = async () => {
  await Api.create(form)
  visible.value = false
  message.success(t('common.saveSuccess'))
  await load()
}

const remove = async (row: Api.OaDocumentVO) => {
  await message.delConfirm()
  await Api.archive(row.id!)
  await load()
}

onMounted(load)
</script>
