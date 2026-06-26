import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/** 不需要登录就能访问的页面 */
const WHITE_LIST = ['/login', '/register', '/admin/login']

/** 区分用户端 / 管理端 */
function isAdminPath(path: string) {
  return path.startsWith('/admin')
}

const routes: RouteRecordRaw[] = [
  // ============ 用户端 ============
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/user/LoginPage.vue'),
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/user/RegisterPage.vue'),
  },
  {
    path: '/',
    name: 'UserHome',
    component: () => import('@/views/user/HomePage.vue'),
    redirect: '/archive',
    children: [
      {
        path: '/archive',
        name: 'UserArchive',
        component: () => import('@/views/user/CatArchivePage.vue'),
      },
      {
        path: '/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/ProfilePage.vue'),
      },
      {
        path: '/community',
        name: 'UserCommunity',
        component: () => import('@/views/user/CommunityPage.vue'),
      },
      {
        path: '/qa',
        name: 'UserQA',
        component: () => import('@/views/user/QAPage.vue'),
      },
      {
        path: '/camera',
        name: 'UserCamera',
        component: () => import('@/views/user/CameraPage.vue'),
      },
    ],
  },

  // ============ PC 管理端 ============
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardPage.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'cat',
        name: 'AdminCat',
        component: () => import('@/views/admin/CatProfilePage.vue'),
        meta: { title: '猫咪档案' },
      },
      {
        path: 'health',
        name: 'AdminHealth',
        component: () => import('@/views/admin/CatHealthEventPage.vue'),
        meta: { title: '猫咪健康事件' },
      },
      {
        path: 'knowledge',
        name: 'AdminKnowledge',
        component: () => import('@/views/admin/KnowledgeBasePage.vue'),
        meta: { title: '知识库管理' },
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManagementPage.vue'),
        meta: { title: '用户管理' },
      },
    ],
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/LoginPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/** 全局前置守卫：未登录访问受保护页面 → 跳转登录 */
router.beforeEach((to) => {
  if (WHITE_LIST.includes(to.path)) {
    return true
  }
  const token = localStorage.getItem('token')
  if (!token) {
    return isAdminPath(to.path) ? '/admin/login' : '/login'
  }
  return true
})

export default router
