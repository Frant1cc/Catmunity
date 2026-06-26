<script setup lang="ts">
import { ref } from 'vue'
import { communityApi } from '@/api'
import type { PostVO } from '@/api/types'

const props = withDefaults(defineProps<{
  post: PostVO
  compact?: boolean
}>(), {
  compact: false,
})

const emit = defineEmits<{
  update: [post: PostVO]
  remove: [postId: number]
}>()

const interacting = ref(false)

function formatTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}小时前`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d}天前`
  const date = new Date(iso)
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${mm}-${dd}`
}

const handleLike = async () => {
  if (interacting.value) return
  interacting.value = true
  try {
    const updated = props.post.isLiked
      ? await communityApi.unlike(props.post.id)
      : await communityApi.like(props.post.id)
    emit('update', updated)
  } catch {
    // ignore
  } finally {
    interacting.value = false
  }
}

const handleFavorite = async () => {
  if (interacting.value) return
  interacting.value = true
  try {
    if (props.post.isFavorited) {
      await communityApi.unfavorite(props.post.id)
      emit('remove', props.post.id)
    } else {
      const updated = await communityApi.favorite(props.post.id)
      emit('update', updated)
    }
  } catch {
    // ignore
  } finally {
    interacting.value = false
  }
}
</script>

<template>
  <div class="post-card" :class="{ compact }">
    <div class="post-header">
      <span class="post-avatar">{{ post.avatar }}</span>
      <div class="post-user">
        <span class="post-name">{{ post.username }}</span>
        <span class="post-time">{{ formatTime(post.createdAt) }}</span>
      </div>
    </div>
    <div class="post-content">{{ post.content }}</div>

    <div v-if="post.images?.length" class="post-images" :class="`grid-${Math.min(post.images.length, 3)}`">
      <img
        v-for="(img, i) in post.images.slice(0, 9)"
        :key="i"
        :src="img"
        class="post-image"
        alt="post image"
      />
    </div>

    <div v-if="post.tags?.length" class="post-tags">
      <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
    </div>

    <div class="post-actions">
      <span
        class="action-item"
        :class="{ active: post.isLiked, 'is-loading': interacting }"
        @click.stop="handleLike"
      >
        <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
        <span class="action-count">{{ post.likeCount || 0 }}</span>
      </span>
      <span class="action-item">
        <span class="action-icon">💬</span>
        <span class="action-count">{{ post.commentCount || 0 }}</span>
      </span>
      <span
        class="action-item"
        :class="{ active: post.isFavorited, 'is-loading': interacting }"
        @click.stop="handleFavorite"
      >
        <span class="action-icon">{{ post.isFavorited ? '⭐' : '☆' }}</span>
        <span class="action-count">{{ post.favoriteCount || 0 }}</span>
      </span>
    </div>
  </div>
</template>

<style scoped>
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

.post-images {
  display: grid;
  gap: 4px;
  margin-bottom: var(--space-md);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.post-images.grid-1 {
  grid-template-columns: 1fr;
  max-width: 220px;
}

.post-images.grid-2 {
  grid-template-columns: repeat(2, 1fr);
  max-width: 320px;
}

.post-images.grid-3 {
  grid-template-columns: repeat(3, 1fr);
  max-width: 360px;
}

.post-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
  background: var(--color-secondary);
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: var(--space-md);
}

.tag {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-secondary);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

.post-actions {
  display: flex;
  gap: var(--space-xl);
  padding-top: var(--space-md);
  border-top: 1px solid var(--color-divider);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: color var(--transition-fast);
  user-select: none;
}

.action-item:active {
  opacity: 0.7;
}

.action-item.active {
  color: var(--color-primary);
}

.action-item.is-loading {
  pointer-events: none;
  opacity: 0.6;
}

.action-icon {
  font-size: 16px;
  line-height: 1;
}

.action-count {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  min-width: 14px;
  text-align: left;
}

.compact {
  background: var(--color-secondary);
  border: none;
  border-radius: var(--radius-md);
  padding: var(--space-md) 14px;
  margin-bottom: var(--space-md);
}
</style>
