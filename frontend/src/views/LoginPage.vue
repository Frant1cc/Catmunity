<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)

const handleLogin = () => {
  if (!username.value) {
    showToast('请输入账号')
    return
  }
  if (!password.value) {
    showToast('请输入密码')
    return
  }
  showToast('登录成功')
  router.push('/profile')
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">
    <!-- 装饰性背景 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <!-- Logo 区域 -->
      <div class="header">
        <div class="logo-wrapper">
          <div class="logo">
            <span class="logo-icon">🐱</span>
          </div>
          <div class="logo-glow"></div>
        </div>
        <h1 class="title">Catmunity</h1>
        <p class="subtitle">欢迎回来，爱猫人士</p>
      </div>

      <!-- 表单 -->
      <div class="form-wrapper">
        <van-cell-group inset>
          <van-field
            v-model="username"
            placeholder="账号"
            :border="false"
            class="input-field"
          />
          <van-field
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="密码"
            :border="false"
            class="input-field"
          >
            <template #right-icon>
              <span class="eye" @click="showPassword = !showPassword">
                {{ showPassword ? '🙈' : '👁' }}
              </span>
            </template>
          </van-field>
        </van-cell-group>

        <van-button
          block
          type="primary"
          size="large"
          class="login-btn"
          @click="handleLogin"
        >
          登 录
        </van-button>

        <van-button
          block
          type="default"
          size="large"
          class="register-btn"
          @click="goToRegister"
        >
          创建新账号
        </van-button>
      </div>

      <!-- 底部 -->
      <div class="footer">
        <div class="paws">
          <span>🐾</span>
          <span class="paw-center">🐱</span>
          <span>🐾</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  position: relative;
  overflow: hidden;
}

/* 装饰背景 */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
}

.circle-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, var(--color-glow-orange) 0%, transparent 70%);
  top: -100px;
  right: -80px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, var(--color-glow-mint) 0%, transparent 70%);
  bottom: 20%;
  left: -60px;
  animation: float 10s ease-in-out infinite reverse;
}

.circle-3 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, var(--color-glow-orange-light) 0%, transparent 70%);
  bottom: -50px;
  right: 20%;
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

/* 内容区 */
.content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0 var(--space-2xl);
}

/* 头部 */
.header {
  padding-top: 100px;
  text-align: center;
  animation: slideUp 0.6s ease-out;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
}

.logo {
  width: 88px;
  height: 88px;
  background: var(--color-logo);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.logo-icon {
  font-size: 44px;
  line-height: 1;
}

.logo-glow {
  position: absolute;
  inset: -8px;
  background: radial-gradient(circle, var(--color-primary-glow) 0%, transparent 70%);
  border-radius: 32px;
  z-index: 1;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary-dark);
  margin-top: var(--space-xl);
  letter-spacing: 1px;
}

.subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-top: var(--space-sm);
}

/* 表单 */
.form-wrapper {
  flex: 1;
  margin-top: 48px;
  animation: slideUp 0.6s ease-out 0.1s both;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.form-wrapper :deep(.van-cell-group) {
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.form-wrapper :deep(.van-cell) {
  padding: var(--space-lg);
}

.form-wrapper :deep(.van-cell:not(:last-child)::after) {
  left: var(--space-lg);
  right: var(--space-lg);
  border-color: var(--color-divider);
}

.form-wrapper :deep(.van-field) {
  --van-field-input-text-color: var(--color-text-disabled);
  --van-field-placeholder-text-color: var(--color-text-disabled);
}

.form-wrapper :deep(.van-field__control) {
  font-size: 15px;
}

.eye {
  font-size: 16px;
}

.login-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  border: none;
  letter-spacing: 2px;
  margin-top: var(--space-xl);
}

.login-btn:active {
  transform: scale(0.98);
}

.register-btn {
  height: 48px;
  font-size: 15px;
  margin-top: var(--space-md);
  border-radius: var(--radius-full);
  background: transparent;
  border: 1.5px solid var(--color-border);
  color: var(--color-text-secondary);
}

.register-btn:active {
  background: var(--color-secondary);
}

/* 底部 */
.footer {
  padding: var(--space-2xl) 0;
  text-align: center;
  animation: slideUp 0.6s ease-out 0.2s both;
}

.paws {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  color: var(--color-paw);
  font-size: 18px;
}

.paw-center {
  font-size: 24px;
  color: var(--color-paw-center);
}
</style>
