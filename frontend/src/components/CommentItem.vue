<script setup lang="ts">
import { computed } from 'vue'
import type { CommentVO } from '@/api/types'

const props = defineProps<{
  comment: CommentVO
}>()

const emit = defineEmits<{
  (e: 'reply', comment: { id: number; username: string }): void
  (e: 'delete', comment: { id: number }): void
}>()

/** 当前登录用户 ID（登录时存的 localStorage） */
const currentUserId = computed(() => {
  const v = localStorage.getItem('userId')
  return v ? Number(v) : 0
})

/** 是否是自己的评论 */
const isMine = computed(() => currentUserId.value === props.comment.userId)

/** 相对时间 */
function formatTime(iso: string): string {
  if (!iso) return ''
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

const onReply = () => {
  emit('reply', { id: props.comment.id, username: props.comment.username })
}

const onDelete = () => {
  emit('delete', { id: props.comment.id })
}
</script>

<template>
  <li class="comment-item">
    <div class="comment-main">
      <div class="comment-body">
        <div class="comment-meta">
          <span class="comment-name">
            {{ comment.username }}
            <span v-if="isMine" class="comment-mine-tag">我</span>
          </span>
          <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
        </div>
        <div class="comment-content">{{ comment.content }}</div>
      </div>
      <div class="comment-actions">
        <span class="comment-reply-btn" @click.stop="onReply">回复</span>
        <span v-if="isMine" class="comment-delete-btn" @click.stop="onDelete">删除</span>
      </div>
    </div>

    <!-- 嵌套子评论 -->
    <ul v-if="comment.children?.length" class="comment-children">
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        @reply="(c) => emit('reply', c)"
        @delete="(c) => emit('delete', c)"
      />
    </ul>
  </li>
</template>

<style scoped>
.comment-item {
  list-style: none;
}

.comment-main {
  display: flex;
  gap: 10px;
  padding: 10px 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.comment-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.comment-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-reply-btn {
  flex-shrink: 0;
  align-self: center;
  font-size: 12px;
  color: var(--color-text-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  user-select: none;
  transition: color var(--transition-fast);
}

.comment-reply-btn:active {
  color: var(--color-primary);
  background: var(--color-secondary);
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  align-self: center;
}

.comment-delete-btn {
  font-size: 12px;
  color: var(--color-text-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  user-select: none;
  transition: color var(--transition-fast);
}

.comment-delete-btn:active {
  color: var(--color-error);
  background: var(--color-bg-overlay);
}

.comment-mine-tag {
  display: inline-block;
  font-size: 10px;
  color: var(--color-primary);
  background: var(--color-secondary);
  padding: 0 4px;
  margin-left: 4px;
  border-radius: var(--radius-xs);
  font-weight: 500;
  vertical-align: middle;
}

.comment-children {
  margin: 0 0 0 18px;
  padding: 0 0 0 14px;
  border-left: 2px solid var(--color-border-light);
  list-style: none;
}
</style>
