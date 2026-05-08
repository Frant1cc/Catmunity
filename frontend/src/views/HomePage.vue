<script setup lang="ts">
const tabs = [
  { name: '档案', path: '/profile' },
  { name: '社区', path: '/community' },
  { name: '问答', path: '/qa' },
  { name: '拍照', path: '/camera' },
]
</script>

<template>
  <div class="home-page">
    <div class="header">
      <h1 class="logo">Catmunity</h1>
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
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary-dark);
  margin: 0;
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
