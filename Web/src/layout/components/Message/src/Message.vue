<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import { useUserStoreWithOut } from '@/store/modules/user'
import { propTypes } from '@/utils/propTypes'
import { createNotificationLoader } from './notificationLoader'

defineOptions({ name: 'Message' })

defineProps({
  color: propTypes.string.def('')
})

const { push } = useRouter()
const { t } = useI18n()
const userStore = useUserStoreWithOut()
const activeName = ref('notice')
const unreadCount = ref(0) // 未读消息数量
const list = ref<NotifyMessageApi.NotifyMessageVO[]>([]) // 消息列表
const loading = ref(false)
const loadFailed = ref(false)
const popoverVisible = ref(false)

const listLoader = createNotificationLoader(NotifyMessageApi.getUnreadNotifyMessageList)
const countLoader = createNotificationLoader(NotifyMessageApi.getUnreadNotifyMessageCount)

// 获得消息列表：先展示最近一次成功结果，再在后台刷新；快速重复点击只发一个请求。
const getList = async (refresh = false) => {
  const cached = listLoader.peek()
  if (cached) {
    list.value = cached
  }
  if (!cached) {
    loading.value = true
  }
  loadFailed.value = false
  try {
    list.value = await listLoader.load(refresh)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

// 获得未读消息数
const getUnreadCount = async () => {
  try {
    unreadCount.value = await countLoader.load(true)
  } catch {
    // 计数刷新失败时保留上一次成功值，避免网络抖动造成红点闪烁。
  }
}

// 跳转我的站内信
const goMyList = () => {
  popoverVisible.value = false
  push({
    name: 'MyNotifyMessage'
  })
}

// ========== 初始化 =========
let unreadCountTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  // 同时预取列表和小红点，首次打开立即有缓存或明确的加载状态。
  void getUnreadCount()
  void getList()
  // 轮询刷新小红点
  unreadCountTimer = setInterval(
    () => {
      if (userStore.getIsSetUser) {
        void getUnreadCount()
      } else {
        unreadCount.value = 0
        list.value = []
        listLoader.clear()
        countLoader.clear()
      }
    },
    1000 * 60 * 2
  )
})

onBeforeUnmount(() => {
  if (unreadCountTimer) {
    clearInterval(unreadCountTimer)
  }
})
</script>
<template>
  <div class="message">
    <ElPopover
      v-model:visible="popoverVisible"
      :width="400"
      placement="bottom"
      trigger="click"
      @show="getList(true)"
    >
      <template #reference>
        <button
          :aria-label="t('system.notify.my')"
          class="message-trigger"
          type="button"
          @focus="getList()"
          @mouseenter="getList()"
        >
          <ElBadge :is-dot="unreadCount > 0" class="item">
            <Icon :size="18" icon="ep:bell" :color="color" />
          </ElBadge>
        </button>
      </template>
      <ElTabs v-model="activeName">
        <ElTabPane :label="t('system.notify.my')" name="notice">
          <el-scrollbar class="message-list">
            <ElSkeleton v-if="loading && list.length === 0" :rows="4" animated class="p-10px" />
            <div v-else-if="loadFailed && list.length === 0" class="message-empty">
              <span>{{ t('common.error') }}</span>
              <XButton :title="t('common.reload')" type="primary" @click="getList(true)" />
            </div>
            <ElEmpty v-else-if="list.length === 0" :description="t('common.noData')" />
            <template v-else>
              <template v-for="item in list" :key="item.id">
                <div class="message-item">
                  <img alt="" class="message-icon" src="@/assets/imgs/avatar.gif" />
                  <div class="message-content">
                    <span class="message-title">
                      {{ item.templateNickname }}：{{ item.templateContent }}
                    </span>
                    <span class="message-date">
                      {{ formatDate(item.createTime) }}
                    </span>
                  </div>
                </div>
              </template>
            </template>
          </el-scrollbar>
        </ElTabPane>
      </ElTabs>
      <!-- 更多 -->
      <div style="margin-top: 10px; text-align: right">
        <XButton preIcon="ep:view" :title="t('common.more')" type="primary" @click="goMyList" />
      </div>
    </ElPopover>
  </div>
</template>
<style lang="scss" scoped>
.message-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 260px;
  line-height: 45px;
}

.message-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 10px;
  margin: 0 -10px;
  color: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.message {
  display: flex;
  height: 100%;
  align-items: center;
}

.message-list {
  display: flex;
  height: 400px;
  flex-direction: column;

  .message-item {
    display: flex;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid var(--el-border-color-light);

    &:last-child {
      border: none;
    }

    .message-icon {
      width: 40px;
      height: 40px;
      margin: 0 20px 0 5px;
    }

    .message-content {
      display: flex;
      flex-direction: column;

      .message-title {
        margin-bottom: 5px;
      }

      .message-date {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
