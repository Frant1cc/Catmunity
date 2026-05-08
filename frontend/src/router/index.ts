import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterPage.vue'),
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
    redirect: '/profile',
    children: [
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/ProfilePage.vue'),
      },
      {
        path: '/community',
        name: 'Community',
        component: () => import('@/views/CommunityPage.vue'),
      },
      {
        path: '/qa',
        name: 'QA',
        component: () => import('@/views/QAPage.vue'),
      },
      {
        path: '/camera',
        name: 'Camera',
        component: () => import('@/views/CameraPage.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
