<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute, RouterView } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ProfileOutlined,
  HeartOutlined,
  DashboardOutlined,
  BookOutlined,
  TeamOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

interface MenuItem {
  key: string
  label: string
  icon: unknown
  path: string
}

const menus: MenuItem[] = [
  { key: 'dashboard', label: '数据看板', icon: DashboardOutlined, path: '/admin/dashboard' },
  { key: 'cat', label: '猫咪档案', icon: ProfileOutlined, path: '/admin/cat' },
  { key: 'health', label: '猫咪健康事件', icon: HeartOutlined, path: '/admin/health' },
  { key: 'knowledge', label: '知识库管理', icon: BookOutlined, path: '/admin/knowledge' },
  { key: 'user', label: '用户管理', icon: TeamOutlined, path: '/admin/user' },
]

const activeKey = computed(() => {
  const item = menus.find((m) => route.path.startsWith(m.path))
  return item?.key ?? 'dashboard'
})

const handleSelect = ({ key }: { key: string }) => {
  const item = menus.find((m) => m.key === key)
  if (item && route.path !== item.path) {
    router.push(item.path)
  }
}

const identifier = computed(() => localStorage.getItem('identifier') || '管理员')

const handleLogout = () => {
  Modal.confirm({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    okText: '退出',
    cancelText: '取消',
    onOk: () => {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userType')
      localStorage.removeItem('identifier')
      message.success('已退出登录')
      router.push('/admin/login')
    },
  })
}
</script>

<template>
  <a-layout class="admin-layout">
    <!-- 左侧栏 -->
    <a-layout-sider width="220" class="admin-sider">
      <div class="brand">
        <span class="brand-icon">🐱</span>
        <div class="brand-text">
          <div class="brand-title">Catmunity</div>
          <div class="brand-sub">管理控制台</div>
        </div>
      </div>

      <a-menu
        mode="inline"
        theme="light"
        :selected-keys="[activeKey]"
        class="admin-menu"
        @select="handleSelect"
      >
        <a-menu-item v-for="m in menus" :key="m.key">
          <component :is="m.icon" />
          <span>{{ m.label }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶栏 -->
      <a-layout-header class="admin-header">
        <div class="header-left">
          <span class="page-title">{{ menus.find((m) => m.key === activeKey)?.label }}</span>
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <a-avatar :size="28" class="user-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span class="user-name">{{ identifier }}</span>
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="admin-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

/* ============== 左侧栏 ============== */
.admin-sider {
  background: #ffffff !important;
  border-right: 1px solid #f0e4d7;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 18px;
  border-bottom: 1px solid #f5ebe0;
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.3);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.admin-menu {
  border-inline-end: none !important;
  padding: 12px 8px;
}

.admin-menu :deep(.ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 4px 0;
  border-radius: 6px;
  font-size: 14px;
  color: #595959;
}

.admin-menu :deep(.ant-menu-item:hover) {
  background-color: #fff5eb;
  color: #ff8c42;
}

.admin-menu :deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%) !important;
  color: #fff !important;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.25);
}

.admin-menu :deep(.ant-menu-item-selected .anticon) {
  color: #fff !important;
}

/* ============== 顶栏 ============== */
.admin-header {
  background: #ffffff !important;
  padding: 0 24px;
  border-bottom: 1px solid #f0e4d7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  line-height: 56px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 8px;
  height: 100%;
  transition: opacity 150ms ease;
}

.user-info:hover {
  opacity: 0.8;
}

.user-avatar {
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%);
}

.user-name {
  font-size: 14px;
  color: #595959;
  line-height: 56px;
}

/* ============== 内容区 ============== */
.admin-content {
  padding: 24px;
  background: #fafafa;
  min-height: calc(100vh - 56px);
  overflow: auto;
}
</style>
