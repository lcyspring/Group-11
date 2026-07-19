<template>
  <div>
    <el-card shadow="never">
      <el-skeleton :loading="loading" animated>
        <el-row :gutter="16" justify="space-between">
          <el-col :xl="12" :lg="12" :md="12" :sm="24" :xs="24">
            <div class="flex items-center">
              <el-avatar :src="avatar" :size="70" class="mr-16px">
                <img src="@/assets/imgs/avatar.gif" alt="" />
              </el-avatar>
              <div>
                <div class="text-20px">
                  {{ t('workplace.welcome') }} {{ username }} {{ t('workplace.happyDay') }}
                </div>
                <div class="mt-10px text-14px text-gray-500">{{ today }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xl="12" :lg="12" :md="12" :sm="24" :xs="24">
            <div class="h-70px flex items-center justify-end lt-sm:mt-10px">
              <div
                class="home-summary-link px-8px text-right"
                :class="{ 'is-disabled': !canQueryTodo }"
                role="button"
                :tabindex="canQueryTodo ? 0 : -1"
                @click="handleTodoClick"
                @keydown.enter="handleTodoClick"
              >
                <div class="mb-16px text-14px text-gray-400">{{ t('workplace.toDo') }}</div>
                <CountTo
                  class="text-20px"
                  :start-val="0"
                  :end-val="totalSate.todo"
                  :duration="2600"
                />
              </div>
              <el-divider direction="vertical" border-style="dashed" />
              <div
                class="home-summary-link px-8px text-right"
                role="button"
                tabindex="0"
                @click="handleNoticeMoreClick"
                @keydown.enter="handleNoticeMoreClick"
              >
                <div class="mb-16px text-14px text-gray-400">
                  {{ t('workplace.noticeCount') }}
                </div>
                <CountTo
                  class="text-20px"
                  :start-val="0"
                  :end-val="totalSate.notice"
                  :duration="2600"
                />
              </div>
            </div>
          </el-col>
        </el-row>
      </el-skeleton>
    </el-card>
  </div>

  <el-row class="mt-8px" :gutter="8" justify="space-between">
    <el-col :xl="16" :lg="16" :md="24" :sm="24" :xs="24" class="mb-8px">
      <el-card shadow="never" class="mt-8px">
        <el-skeleton :loading="loading" animated>
          <el-row :gutter="20" justify="space-between">
            <el-col :xl="10" :lg="10" :md="24" :sm="24" :xs="24">
              <el-card shadow="hover" class="mb-8px">
                <el-skeleton :loading="loading" animated>
                  <Echart :options="pieOptionsData" :height="280" />
                </el-skeleton>
              </el-card>
            </el-col>
            <el-col :xl="14" :lg="14" :md="24" :sm="24" :xs="24">
              <el-card shadow="hover" class="mb-8px">
                <el-skeleton :loading="loading" animated>
                  <Echart :options="barOptionsData" :height="280" />
                </el-skeleton>
              </el-card>
            </el-col>
          </el-row>
        </el-skeleton>
      </el-card>
    </el-col>
    <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24" class="mb-8px">
      <el-card shadow="never">
        <template #header>
          <div class="h-3 flex justify-between">
            <span>{{ t('workplace.shortcutOperation') }}</span>
          </div>
        </template>
        <el-skeleton :loading="loading" animated>
          <el-row>
            <el-col v-for="item in shortcut" :key="`team-${item.name}`" :span="8" class="mb-8px">
              <div class="flex items-center">
                <Icon :icon="item.icon" class="mr-8px" :style="{ color: item.color }" />
                <el-link type="default" :underline="false" @click="handleShortcutClick(item.url)">
                  {{ t(item.name) }}
                </el-link>
              </div>
            </el-col>
          </el-row>
        </el-skeleton>
      </el-card>
      <el-card shadow="never" class="mt-8px">
        <template #header>
          <div class="h-3 flex justify-between">
            <span>{{ t('workplace.notice') }}</span>
            <el-link type="primary" :underline="false" @click="handleNoticeMoreClick">
              {{ t('action.more') }}
            </el-link>
          </div>
        </template>
        <el-skeleton :loading="loading" animated>
          <el-empty
            v-if="notice.length === 0"
            :description="t('common.noData')"
            :image-size="64"
          />
          <div v-for="(item, index) in notice" :key="item.id || `dynamics-${index}`">
            <div class="flex items-center">
              <el-avatar :src="avatar" :size="35" class="mr-16px">
                <img src="@/assets/imgs/avatar.gif" alt="" />
              </el-avatar>
              <div>
                <div class="text-14px">
                  <Highlight :keys="item.keys.map((v) => t(v))">
                    {{ t(item.type) }} : {{ item.title }}
                  </Highlight>
                </div>
                <div class="mt-16px text-12px text-gray-400">
                  {{ formatTime(item.date, 'yyyy-MM-dd') }}
                </div>
              </div>
            </div>
            <el-divider />
          </div>
        </el-skeleton>
      </el-card>
    </el-col>
  </el-row>
</template>
<script lang="ts" setup>
import { set } from 'lodash-es'
import { EChartsOption } from 'echarts'
import { formatTime } from '@/utils'

import { useUserStore } from '@/store/modules/user'
// import { useWatermark } from '@/hooks/web/useWatermark'
import type { WorkplaceTotal, Notice, Shortcut } from './types'
import { pieOptions, barOptions } from './echarts-data'
import { useRouter } from 'vue-router'
import * as TaskApi from '@/api/bpm/task'
import * as NoticeApi from '@/api/system/notice'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'Index' })

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
// const { setWatermark } = useWatermark()
const loading = ref(true)
const avatar = userStore.getUser.avatar
const username = userStore.getUser.nickname
const today = formatTime(new Date(), 'yyyy-MM-dd')
const canQueryTodo = checkPermi(['bpm:task:query'])
const pieOptionsData = reactive<EChartsOption>(pieOptions) as EChartsOption
// 获取统计数
let totalSate = reactive<WorkplaceTotal>({
  notice: 0,
  todo: 0
})

const getCount = async () => {
  if (!canQueryTodo) {
    totalSate.todo = 0
    return
  }
  const data = await TaskApi.getTaskTodoPage({ pageNo: 1, pageSize: 1 })
  totalSate.todo = data.total
}

// 获取通知公告
let notice = reactive<Notice[]>([])
const getNotice = async () => {
  const page = await NoticeApi.getNoticePage({ pageNo: 1, pageSize: 5 })
  const data: Notice[] = page.list.map((item) => ({
    id: item.id,
    title: item.title,
    type: 'workplace.noticeItem',
    keys: [],
    date: item.createTime
  }))
  notice.splice(0, notice.length, ...data)
  totalSate.notice = page.total
}

// 获取快捷入口
let shortcut = reactive<Shortcut[]>([])

const getShortcut = async () => {
  const data = [
    {
      name: 'workplace.shortcutHome',
      icon: 'ion:home-outline',
      url: '/',
      color: '#1fdaca'
    },
    {
      name: 'workplace.shortcutMall',
      icon: 'ep:shop',
      url: '/mall/home',
      color: '#ff6b6b'
    },
    {
      name: 'workplace.shortcutAi',
      icon: 'tabler:ai',
      url: '/ai/chat',
      color: '#7c3aed'
    },
    {
      name: 'workplace.shortcutErp',
      icon: 'simple-icons:erpnext',
      url: '/erp/home',
      color: '#3fb27f'
    },
    {
      name: 'workplace.shortcutCrm',
      icon: 'simple-icons:civicrm',
      url: '/crm/backlog',
      color: '#4daf1bc9'
    },
    {
      name: 'workplace.shortcutIot',
      icon: 'fa-solid:hdd',
      url: '/iot/home',
      color: '#1a73e8'
    }
  ]
  shortcut = Object.assign(shortcut, data)
}

// 用户来源
const getUserAccessSource = async () => {
  const data = [
    { value: 335, name: 'analysis.directAccess' },
    { value: 310, name: 'analysis.mailMarketing' },
    { value: 234, name: 'analysis.allianceAdvertising' },
    { value: 135, name: 'analysis.videoAdvertising' },
    { value: 1548, name: 'analysis.searchEngines' }
  ]
  set(
    pieOptionsData,
    'legend.data',
    data.map((v) => t(v.name))
  )
  pieOptionsData!.series![0].data = data.map((v) => {
    return {
      name: t(v.name),
      value: v.value
    }
  })
}
const barOptionsData = reactive<EChartsOption>(barOptions) as EChartsOption

// 周活跃量
const getWeeklyUserActivity = async () => {
  const data = [
    { value: 13253, name: 'analysis.monday' },
    { value: 34235, name: 'analysis.tuesday' },
    { value: 26321, name: 'analysis.wednesday' },
    { value: 12340, name: 'analysis.thursday' },
    { value: 24643, name: 'analysis.friday' },
    { value: 1322, name: 'analysis.saturday' },
    { value: 1324, name: 'analysis.sunday' }
  ]
  set(
    barOptionsData,
    'xAxis.data',
    data.map((v) => t(v.name))
  )
  set(barOptionsData, 'series', [
    {
      name: t('analysis.activeQuantity'),
      data: data.map((v) => v.value),
      type: 'bar'
    }
  ])
}

const getAllApi = async () => {
  try {
    await Promise.allSettled([
      getCount(),
      getNotice(),
      getShortcut(),
      getUserAccessSource(),
      getWeeklyUserActivity()
    ])
  } finally {
    loading.value = false
  }
}

const handleShortcutClick = (url: string) => {
  router.push(url)
}

const handleTodoClick = () => {
  if (canQueryTodo) router.push('/bpm/task/todo')
}

const handleNoticeMoreClick = () => {
  const canOpenNoticeManagement =
    checkPermi(['system:notice:query']) && router.hasRoute('SystemNotice')
  router.push({ name: canOpenNoticeManagement ? 'SystemNotice' : 'MyNotifyMessage' })
}

onActivated(() => {
  void Promise.allSettled([getCount(), getNotice()])
})

getAllApi()
</script>

<style scoped>
.home-summary-link {
  cursor: pointer;
  border-radius: 6px;
}

.home-summary-link:hover {
  background: var(--el-fill-color-light);
}

.home-summary-link.is-disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
