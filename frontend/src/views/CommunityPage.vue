<script setup lang="ts">
import { ref } from 'vue'

const posts = ref([
  { id: 1, avatar: '🐱', name: '橘猫小胖', time: '2小时前', content: '今天天气真好，在阳台晒太阳 🌞', likes: 128 },
  { id: 2, avatar: '🐈', name: '布偶豆豆', time: '4小时前', content: '新买的猫窝到了，超喜欢！', likes: 256 },
  { id: 3, avatar: '😺', name: '英短球球', time: '6小时前', content: '铲屎官今天忘记喂罐头了，抗议！', likes: 89 },
])
</script>

<template>
  <div class="community-page">
    <div class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card">
        <div class="post-header">
          <span class="post-avatar">{{ post.avatar }}</span>
          <div class="post-user">
            <span class="post-name">{{ post.name }}</span>
            <span class="post-time">{{ post.time }}</span>
          </div>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-actions">
          <span class="action-item">❤️ {{ post.likes }}</span>
          <span class="action-item">💬 评论</span>
          <span class="action-item">📤 分享</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.community-page {
  padding: var(--space-lg);
}

.post-card {
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--color-border);
}

.post-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.post-avatar {
  font-size: 32px;
}

.post-user {
  display: flex;
  flex-direction: column;
}

.post-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.post-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.post-content {
  font-size: 15px;
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: var(--space-md);
}

.post-actions {
  display: flex;
  gap: var(--space-xl);
  padding-top: var(--space-md);
  border-top: 1px solid var(--color-divider);
}

.action-item {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
</style>
