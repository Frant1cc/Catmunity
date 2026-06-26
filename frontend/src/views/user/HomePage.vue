<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { authApi } from '@/api'

const router = useRouter()

const tabs = [
  { name: '档案', path: '/archive' },
  { name: '社区', path: '/community' },
  { name: '问答', path: '/qa' },
  { name: '打卡', path: '/camera' },
  { name: '我的', path: '/profile' },
]

const handleLogout = async () => {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？',
    })
  } catch {
    return // 用户取消
  }

  try {
    await authApi.logout()
  } catch {
    // 即使接口失败，前端也继续清理登录态
  }

  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  showSuccessToast('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <div class="home-page">
    <div class="header">
      <h1 class="logo">Catmunity</h1>
      <button class="logout-btn" @click="handleLogout">
        <span class="logout-icon">🚪</span>
        <span class="logout-text">退出</span>
      </button>
    </div>

    <div class="content">
      <router-view />
    </div>

    <!-- 底部 TabBar -->
    <div class="tab-bar-wrapper">
      <div class="tab-bar">
        <router-link
          v-for="tab in tabs"
          :key="tab.path"
          :to="tab.path"
          class="tab-item"
          :class="{ active: $route.path === tab.path }"
        >
          <span class="tab-name">{{ tab.name }}</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  padding-bottom: 100px;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg-container);
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary-dark);
  margin: 0;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.logout-btn:active {
  background: var(--color-secondary);
  transform: scale(0.97);
}

.logout-icon {
  font-size: 14px;
}

.content {
  padding: var(--space-lg);
}

/* 底部 TabBar */
.tab-bar-wrapper {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-lg) var(--space-xl);
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
  background: var(--color-bg-page);
}

.tab-bar {
  display: flex;
  background: var(--color-bg-container);
  border-radius: var(--radius-full);
  padding: var(--space-sm);
  gap: var(--space-sm);
  border: 1px solid var(--color-border);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-md);
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: all var(--transition-normal);
}

.tab-item.active {
  background: var(--color-primary);
}

.tab-item.active .tab-name {
  color: #fff;
}

.tab-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: color var(--transition-normal);
}
</style>
