<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import { userApi, communityApi } from '@/api'
import type { UserInfo, PostVO } from '@/api/types'
import PostCard from '@/components/PostCard.vue'

const userInfo = ref<UserInfo | null>(null)
const profileLoading = ref(true)

/* ==================== 编辑资料 ==================== */

const showEdit = ref(false)
const editForm = reactive({
  newIdentifier: '',
  newPassword: '',
})
const editLoading = ref(false)

const openEdit = () => {
  editForm.newIdentifier = userInfo.value?.identifier || ''
  editForm.newPassword = ''
  showEdit.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    await userApi.updateInfo({
      newIdentifier: editForm.newIdentifier || undefined,
      newPassword: editForm.newPassword || undefined,
    })
    showSuccessToast('修改成功')
    showEdit.value = false
    await loadProfile()
  } catch {
    // error handled by interceptor
  } finally {
    editLoading.value = false
  }
}

/* ==================== 收藏夹 ==================== */

const showFavorites = ref(false)
const favorites = ref<PostVO[]>([])
const favoritesLoading = ref(false)
const favTotal = ref(0)
const favPage = ref(1)
const favPages = ref(0)
const favFinished = ref(false)

const loadProfile = async () => {
  profileLoading.value = true
  try {
    userInfo.value = await userApi.getInfo()
  } catch {
    // ignore
  } finally {
    profileLoading.value = false
  }
}

const openFavorites = async () => {
  showFavorites.value = true
  favoritesLoading.value = true
  try {
    const res = await communityApi.getFavorites({ pageNum: 1, pageSize: 10 })
    favorites.value = res.rows
    favTotal.value = res.total
    favPage.value = res.pageNum
    favPages.value = res.pages
    favFinished.value = res.pageNum >= res.pages
  } catch {
    showToast('加载收藏列表失败')
  } finally {
    favoritesLoading.value = false
  }
}

const closeFavorites = () => {
  showFavorites.value = false
  favorites.value = []
  favPage.value = 1
  favFinished.value = false
}

const onFavLoadMore = async () => {
  if (favFinished.value || favoritesLoading.value) return
  const nextPage = favPage.value + 1
  favoritesLoading.value = true
  try {
    const res = await communityApi.getFavorites({ pageNum: nextPage, pageSize: 10 })
    favorites.value.push(...res.rows)
    favTotal.value = res.total
    favPage.value = res.pageNum
    favPages.value = res.pages
    favFinished.value = res.pageNum >= res.pages
  } catch {
    showToast('加载失败')
  } finally {
    favoritesLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <div v-if="profileLoading" class="state">加载中...</div>

    <template v-else>
      <!-- 用户信息 -->
      <div class="profile-header">
        <div class="avatar">{{ userInfo?.avatar || '🐱' }}</div>
        <div class="profile-info">
          <h2 class="nickname">{{ userInfo?.identifier || '未设置昵称' }}</h2>
          <p class="bio">{{ userInfo?.email || '这个人很懒，什么都没写' }}</p>
        </div>
        <van-icon name="edit" class="edit-btn" @click="openEdit" />
      </div>

      <!-- 统计数据 -->
      <div class="profile-stats">
        <div class="stat-item">
          <span class="stat-num">{{ userInfo?.postCount ?? 0 }}</span>
          <span class="stat-label">帖子</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ userInfo?.followerCount ?? 0 }}</span>
          <span class="stat-label">粉丝</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ userInfo?.followCount ?? 0 }}</span>
          <span class="stat-label">关注</span>
        </div>
      </div>

      <!-- 菜单 -->
      <van-cell-group inset class="profile-menu">
        <van-cell
          title="收藏夹"
          is-link
          :value="`${favTotal || 0}个`"
          @click="openFavorites"
        />
      </van-cell-group>
    </template>

    <!-- 收藏夹弹窗 -->
    <van-popup
      v-model:show="showFavorites"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '85vh' }"
      class="favorites-popup"
      @closed="closeFavorites"
    >
      <div class="favorites-panel">
        <h3 class="favorites-title">我的收藏</h3>

        <div v-if="favoritesLoading && !favorites.length" class="state">加载中...</div>

        <div v-else-if="!favorites.length" class="state">暂无收藏</div>

        <template v-else>
          <PostCard
            v-for="post in favorites"
            :key="post.id"
            :post="post"
            compact
            @update="(p) => Object.assign(post, p)"
            @remove="favorites = favorites.filter((f) => f.id !== $event)"
          />

          <div
            v-if="!favFinished"
            class="load-more"
          >
            <van-button
              size="small"
              :loading="favoritesLoading"
              loading-text="加载中..."
              @click="onFavLoadMore"
            >
              加载更多
            </van-button>
          </div>
          <div v-else class="state">— 没有更多了 —</div>
        </template>
      </div>
    </van-popup>

    <!-- 编辑资料弹窗 -->
    <van-popup
      v-model:show="showEdit"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '85vh' }"
      class="edit-popup"
    >
      <div class="edit-panel">
        <h3 class="edit-title">编辑资料</h3>

        <van-form @submit="submitEdit">
          <van-cell-group inset>
            <van-field
              v-model="editForm.newIdentifier"
              name="newIdentifier"
              label="账号"
              placeholder="请输入新账号"
              clearable
            />
            <van-field
              v-model="editForm.newPassword"
              name="newPassword"
              label="新密码"
              type="password"
              placeholder="8-16位，包含字母和数字"
              clearable
            />
          </van-cell-group>

          <div class="edit-actions">
            <van-button
              round
              block
              type="primary"
              native-type="submit"
              :loading="editLoading"
              loading-text="保存中..."
            >
              保存
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-page {
  padding: var(--space-lg);
}

.state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

/* 用户信息 */
.profile-header {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.avatar {
  width: 72px;
  height: 72px;
  background: var(--color-secondary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  border: 3px solid var(--color-primary);
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

.bio {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin: 0;
}

/* 统计 */
.profile-stats {
  display: flex;
  justify-content: space-around;
  padding: var(--space-lg) 0;
  margin-bottom: var(--space-lg);
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.profile-menu {
  margin-top: var(--space-lg);
}

.profile-menu :deep(.van-cell-group) {
  background: var(--color-primary);
}

.profile-menu :deep(.van-cell) {
  background: var(--color-primary);
  color: #fff;
}

.profile-menu :deep(.van-cell__title) {
  color: #fff;
}

.profile-menu :deep(.van-cell__value) {
  color: rgba(255, 255, 255, 0.8);
}

.profile-menu :deep(.van-cell:active) {
  background: var(--color-primary-dark);
}

.profile-menu :deep(.van-icon) {
  color: rgba(255, 255, 255, 0.8);
}

/* 收藏夹弹窗 */
.favorites-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.favorites-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.favorites-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: var(--color-text-tertiary);
}

.favorites-panel {
  padding: 20px 20px 32px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.favorites-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-divider);
  text-align: center;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--space-md) 0;
}

/* 编辑按钮 */
.edit-btn {
  font-size: 22px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  flex-shrink: 0;
}

/* 编辑弹窗 */
.edit-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.edit-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.edit-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: var(--color-text-tertiary);
}

.edit-panel {
  padding: 20px 20px 32px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.edit-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-divider);
  text-align: center;
}

.edit-actions {
  padding: var(--space-xl) var(--space-md);
}

</style>
