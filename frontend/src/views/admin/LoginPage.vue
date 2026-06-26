<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { authApi } from '@/api'
import type { LoginResult } from '@/api/types'

const router = useRouter()
const identifier = ref('')
const password = ref('')
const remember = ref(true)
const loading = ref(false)

const handleLogin = async () => {
  if (!identifier.value) {
    message.warning('请输入账号')
    return
  }
  if (!password.value) {
    message.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const result: LoginResult = await authApi.login({
      identifier: identifier.value,
      password: password.value,
    })

    // 持久化登录信息
    localStorage.setItem('token', result.token)
    localStorage.setItem('userId', String(result.userId))
    localStorage.setItem('userType', String(result.type))
    localStorage.setItem('identifier', result.identifier)

    message.success('登录成功')
    router.push('/admin/dashboard')
  } catch {
    // 业务错误已由 axios 拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-bg">
        <div class="bg-shape shape-1"></div>
        <div class="bg-shape shape-2"></div>
        <div class="bg-shape shape-3"></div>
      </div>
      <div class="brand-content">
        <div class="brand-logo">
          <span class="logo-icon">🐱</span>
        </div>
        <h1 class="brand-title">Catmunity</h1>
        <p class="brand-subtitle">管理控制台 · Admin Console</p>
        <ul class="brand-features">
          <li>
            <span class="feature-dot"></span>
            <span class="feature-text">数据看板与运营分析</span>
          </li>
          <li>
            <span class="feature-dot"></span>
            <span class="feature-text">猫咪档案统一管理</span>
          </li>
          <li>
            <span class="feature-dot"></span>
            <span class="feature-text">社区内容审核与维护</span>
          </li>
          <li>
            <span class="feature-dot"></span>
            <span class="feature-text">用户与权限配置</span>
          </li>
        </ul>
        <p class="brand-footer">© 2026 Catmunity. All rights reserved.</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="form-container">
        <div class="form-header">
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-tip">请使用管理员账号登录系统</p>
        </div>

        <a-form layout="vertical" class="login-form" @submit.prevent="handleLogin">
          <a-form-item label="账号">
            <a-input
              v-model:value="identifier"
              placeholder="请输入管理员账号"
              allow-clear
            >
              <template #prefix>
                <span class="field-icon">👤</span>
              </template>
            </a-input>
          </a-form-item>

          <a-form-item label="密码">
            <a-input-password
              v-model:value="password"
              placeholder="请输入密码"
            >
              <template #prefix>
                <span class="field-icon">🔒</span>
              </template>
            </a-input-password>
          </a-form-item>

          <div class="form-options">
            <a-checkbox v-model:checked="remember">记住账号</a-checkbox>
            <a class="forgot-link">忘记密码?</a>
          </div>

          <a-button
            type="primary"
            block
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </a-button>

          <p class="form-footer">
            登录即表示同意 <a class="link">《服务协议》</a> 与 <a class="link">《隐私政策》</a>
          </p>
        </a-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* PC 端样式：直接使用 px，不走 vw 转换（postcss.config.js 已排除 /admin/） */
.admin-login {
  display: flex;
  min-height: 100vh;
  background: #fffdf9;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

/* ============== 左侧品牌区 ============== */
.brand-panel {
  flex: 1.2;
  position: relative;
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
}

.shape-1 {
  width: 480px;
  height: 480px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.4) 0%, transparent 70%);
  top: -160px;
  right: -120px;
}

.shape-2 {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(125, 211, 192, 0.3) 0%, transparent 70%);
  bottom: -80px;
  left: -80px;
}

.shape-3 {
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.25) 0%, transparent 70%);
  bottom: 30%;
  right: 15%;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 480px;
  color: #fff;
}

.brand-logo {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
}

.logo-icon {
  font-size: 36px;
  line-height: 1;
}

.brand-title {
  font-size: 40px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 2px;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 15px;
  margin: 0 0 32px;
  opacity: 0.9;
  letter-spacing: 1px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0 0 32px;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
  opacity: 0.95;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  opacity: 0.85;
  flex-shrink: 0;
}

.feature-text {
  letter-spacing: 0.5px;
}

.brand-footer {
  font-size: 13px;
  opacity: 0.7;
  margin: 0;
}

/* ============== 右侧表单区 ============== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fafafa;
}

.form-container {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  padding: 32px 32px 28px;
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.form-header {
  margin-bottom: 20px;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 0 0 4px;
}

.form-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

/* 表单 */
.login-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

.login-form :deep(.ant-form-item-label > label) {
  color: #595959;
  font-size: 13px;
  font-weight: 500;
}

.login-form :deep(.ant-input-affix-wrapper),
.login-form :deep(.ant-input) {
  border-radius: 4px;
  font-size: 14px;
  transition: all 200ms ease;
}

.login-form :deep(.ant-input-affix-wrapper:hover),
.login-form :deep(.ant-input:hover) {
  border-color: #ffa666;
}

.login-form :deep(.ant-input-affix-wrapper-focused),
.login-form :deep(.ant-input:focus) {
  border-color: #ff8c42;
  box-shadow: 0 0 0 2px rgba(255, 140, 66, 0.2);
}

.field-icon {
  font-size: 14px;
  color: #8c8c8c;
  margin-right: 6px;
  line-height: 1;
}

/* 选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 16px;
}

.forgot-link {
  font-size: 13px;
  color: #ff8c42;
  cursor: pointer;
  text-decoration: none;
  transition: opacity 150ms ease;
}

.forgot-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

/* 按钮 */
.login-btn {
  height: 38px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 4px;
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%);
  border: none;
  letter-spacing: 4px;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.3);
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.35);
  background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%) !important;
}

.login-btn:active {
  transform: scale(0.99);
}

.form-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 0;
}

.link {
  color: #ff8c42;
  text-decoration: none;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}
</style>
