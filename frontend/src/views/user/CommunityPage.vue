<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import { communityApi, commentApi } from '@/api'
import type { CommentVO, PostVO } from '@/api/types'
import CommentItem from '@/components/CommentItem.vue'

const posts = ref<PostVO[]>([])
const loading = ref(false)

/** 分页状态 */
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const pages = ref(0)
const showPager = computed(() => !isSearching.value && posts.value.length > 0)

/** 当前是否处于搜索模式（有关键词） */
const searchKeyword = ref('')
const isSearching = computed(() => searchKeyword.value.trim().length > 0)

/* 点赞/收藏中状态：postId -> boolean，防止重复点击 */
const interacting = ref<Set<number>>(new Set())

/** 把 ISO 时间转成 "2小时前" 这种相对时间 */
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

const fetchFeed = async (targetPage = pageNum.value) => {
  const res = await communityApi.getFeed({ pageNum: targetPage, pageSize: pageSize.value })
  posts.value = res.rows
  total.value = res.total
  pageNum.value = res.pageNum
  pages.value = res.pages
}

const loadFeed = async (targetPage = pageNum.value) => {
  loading.value = true
  try {
    await fetchFeed(targetPage)
  } finally {
    loading.value = false
  }
}

const loadByTag = async (tag: string) => {
  loading.value = true
  try {
    const res = await communityApi.getByTag(tag, { pageNum: 1, pageSize: 20 })
    posts.value = res
    total.value = res.length
    pages.value = 0
  } catch {
    posts.value = []
  } finally {
    loading.value = false
  }
}

const onPageChange = (p: number) => {
  // 不要用 v-model 的 pageNum 比较（v-model 已先把 pageNum 改了，会永远相等）
  if (loading.value) return
  loadFeed(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const onSearch = () => {
  const tag = searchKeyword.value.trim().replace(/^#+/, '')
  if (!tag) {
    showToast('请输入话题标签')
    return
  }
  searchKeyword.value = tag
  loadByTag(tag)
}

const clearSearch = () => {
  searchKeyword.value = ''
  loadFeed()
}

/** 点击话题标签 → 跳到搜索 */
const onTagClick = (tag: string) => {
  searchKeyword.value = tag
  showDetail.value = false
  loadByTag(tag)
}

const handleFavorite = async (post: PostVO) => {
  if (interacting.value.has(post.id)) return
  interacting.value.add(post.id)
  try {
    if (post.isFavorited) {
      await communityApi.unfavorite(post.id)
    } else {
      await communityApi.favorite(post.id)
    }
    await fetchFeed()
  } catch {
    // ignore
  } finally {
    interacting.value.delete(post.id)
  }
}

const handleLike = async (post: PostVO) => {
  if (interacting.value.has(post.id)) return
  interacting.value.add(post.id)
  try {
    if (post.isLiked) {
      await communityApi.unlike(post.id)
    } else {
      await communityApi.like(post.id)
    }
    await fetchFeed()
  } catch {
    // ignore
  } finally {
    interacting.value.delete(post.id)
  }
}

/* ==================== 发布帖子 ==================== */

const showPublish = ref(false)
const publishing = ref(false)
const uploadingImage = ref(false)
const draftContent = ref('')
const draftTagInput = ref('')
const draftImages = ref<string[]>([])
const draftTags = ref<string[]>([])

const fileInputRef = ref<HTMLInputElement | null>(null)

const MAX_IMAGES = 9

const openPublish = () => {
  showPublish.value = true
}

const closePublish = () => {
  showPublish.value = false
  draftContent.value = ''
  draftTagInput.value = ''
  draftImages.value = []
  draftTags.value = []
}

/** 触发文件选择 */
const triggerPickImages = () => {
  if (uploadingImage.value) return
  if (draftImages.value.length >= MAX_IMAGES) {
    showToast(`最多 ${MAX_IMAGES} 张图片`)
    return
  }
  fileInputRef.value?.click()
}

/** 文件选择回调：上传并回填 URL */
const onFilesPicked = async (e: Event) => {
  const input = e.target as HTMLInputElement
  if (!input.files || !input.files.length) return

  const remain = MAX_IMAGES - draftImages.value.length
  if (input.files.length > remain) {
    showToast(`还能上传 ${remain} 张，已截取前 ${remain} 张`)
  }
  const files = Array.from(input.files).slice(0, remain)

  uploadingImage.value = true
  try {
    const res = await communityApi.uploadImages(files)
    // 防御：响应可能是 string[] / {urls:[...]} / {data:[...]} / null，统一抽成 string[]
    const candidate = (res as any)?.urls ?? (res as any)?.data ?? res
    const urls: string[] = Array.isArray(candidate)
      ? candidate.filter((x: unknown) => typeof x === 'string')
      : []
    if (urls.length) {
      draftImages.value.push(...urls)
    } else {
      showToast('上传失败：未返回 URL')
    }
  } catch {
    // 拦截器已 toast
  } finally {
    uploadingImage.value = false
    // 重置 input，否则同一文件无法再次选择
    input.value = ''
  }
}

const removeImage = (idx: number) => {
  draftImages.value.splice(idx, 1)
}

const confirmAddTag = () => {
  const t = draftTagInput.value.trim().replace(/^#+/, '')
  if (!t) return
  if (draftTags.value.includes(t)) {
    showToast('标签已存在')
    return
  }
  if (draftTags.value.length >= 6) {
    showToast('最多 6 个标签')
    return
  }
  draftTags.value.push(t)
  draftTagInput.value = ''
}

const removeTag = (t: string) => {
  draftTags.value = draftTags.value.filter((x) => x !== t)
}

const submitPost = async () => {
  const content = draftContent.value.trim()
  if (!content) {
    showToast('请输入帖子内容')
    return
  }
  publishing.value = true
  try {
    const payload = {
      content,
      ...(draftImages.value.length ? { images: [...draftImages.value] } : {}),
      ...(draftTags.value.length ? { tags: [...draftTags.value] } : {}),
    }
    const newPost = await communityApi.createPost(payload)
    // 插到列表顶部
    posts.value.unshift(newPost)
    showSuccessToast('发布成功')
    closePublish()
  } catch {
    // 拦截器已 toast
  } finally {
    publishing.value = false
  }
}

onMounted(() => loadFeed(1))

/* ==================== 帖子详情弹窗 ==================== */

const showDetail = ref(false)
const detailPost = ref<PostVO | null>(null)
const comments = ref<CommentVO[]>([])
const commentsLoading = ref(false)

/* 发表评论 / 回复 */
const commentInput = ref('')
const commentSending = ref(false)
const replyTarget = ref<{ id: number; username: string } | null>(null)
const commentBarRef = ref<HTMLDivElement | null>(null)

/** 完整时间 yyyy-MM-dd HH:mm:ss */
function formatDateTime(s: string) {
  if (!s) return ''
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const loadComments = async (postId: number) => {
  commentsLoading.value = true
  try {
    comments.value = await commentApi.getByPostId(postId)
  } catch {
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

const openDetail = (post: PostVO) => {
  detailPost.value = post
  showDetail.value = true
  loadComments(post.id)
}

const closeDetail = () => {
  showDetail.value = false
  detailPost.value = null
  comments.value = []
  commentInput.value = ''
  replyTarget.value = null
}

/** 切换到"回复 @xxx"模式 */
const startReply = (c: { id: number; username: string }) => {
  replyTarget.value = { id: c.id, username: c.username }
  // 等 popup 渲染完再聚焦
  setTimeout(() => {
    const el = commentBarRef.value?.querySelector('input, textarea') as
      | HTMLInputElement
      | HTMLTextAreaElement
      | null
    el?.focus()
  }, 50)
}

const cancelReply = () => {
  replyTarget.value = null
}

/** 找到目标评论的父列表引用（顶层或某条 children） */
function findCommentList(
  list: CommentVO[],
  parentId: number,
): CommentVO[] | null {
  for (const c of list) {
    if (c.id === parentId) return c.children ?? (c.children = [])
    if (c.children?.length) {
      const found = findCommentList(c.children, parentId)
      if (found) return found
    }
  }
  return null
}

const submitComment = async () => {
  if (!detailPost.value) return
  const content = commentInput.value.trim()
  if (!content) {
    showToast('请输入评论内容')
    return
  }
  commentSending.value = true
  try {
    const newComment = await commentApi.create({
      postId: detailPost.value.id,
      parentId: replyTarget.value?.id,
      content,
    })
    // 兜底字段
    if (!newComment.children) newComment.children = []
    if (!newComment.postId) newComment.postId = detailPost.value.id

    if (replyTarget.value) {
      // 找到父评论的 children 数组挂上去
      const target = findCommentList(comments.value, replyTarget.value.id)
      if (target) {
        target.push(newComment)
      } else {
        comments.value.push(newComment)
      }
    } else {
      // 顶层评论：插到列表顶部
      comments.value.unshift(newComment)
    }

    // 帖子评论数 +1
    if (detailPost.value) {
      detailPost.value.commentCount = (detailPost.value.commentCount || 0) + 1
    }
    commentInput.value = ''
    replyTarget.value = null
    showSuccessToast('发送成功')
  } catch {
    // 拦截器已 toast
  } finally {
    commentSending.value = false
  }
}

/* ==================== 删除评论 ==================== */

/** 递归统计评论及其子评论总数（用于扣减帖子评论数） */
function countWithChildren(c: CommentVO): number {
  return 1 + (c.children?.reduce((sum, child) => sum + countWithChildren(child), 0) || 0)
}

/** 递归从树中删除指定 id 的评论，返回是否删除成功 + 删了几个 */
function removeCommentById(
  list: CommentVO[],
  id: number,
): { removed: CommentVO | null; count: number } {
  for (let i = 0; i < list.length; i++) {
    if (list[i].id === id) {
      const removed = list.splice(i, 1)[0]
      return { removed, count: countWithChildren(removed) }
    }
    if (list[i].children?.length) {
      const res = removeCommentById(list[i].children, id)
      if (res.removed) return res
    }
  }
  return { removed: null, count: 0 }
}

const deletingId = ref<number | null>(null)

const handleDeleteComment = async (target: { id: number }) => {
  try {
    await showConfirmDialog({
      title: '删除评论',
      message: '确定要删除这条评论吗？子评论也会被删除。',
      confirmButtonText: '删除',
      confirmButtonColor: 'var(--color-error)',
    })
  } catch {
    return // 取消
  }

  if (!detailPost.value) return
  deletingId.value = target.id
  try {
    await commentApi.delete(target.id)
    const { count } = removeCommentById(comments.value, target.id)
    // 帖子评论数减去被删的（含子评论）
    if (detailPost.value && count > 0) {
      detailPost.value.commentCount = Math.max(
        0,
        (detailPost.value.commentCount || 0) - count,
      )
    }
    showSuccessToast('已删除')
  } catch {
    // 拦截器已 toast
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <div class="community-page">
    <div class="community-header">
      <h2 class="community-title">猫友社区</h2>
      <van-button
        type="primary"
        size="small"
        round
        class="publish-btn"
        @click="openPublish"
      >
        + 发布
      </van-button>
    </div>

    <!-- 搜索框 -->
    <div class="community-search">
      <van-search
        v-model="searchKeyword"
        placeholder="搜索话题标签，例如：橘猫"
        shape="round"
        background="transparent"
        clearable
        @search="onSearch"
        @clear="clearSearch"
        @keyup.enter="onSearch"
      >
        <template #left-icon>
          <span class="search-icon">🔍</span>
        </template>
      </van-search>
      <div v-if="isSearching" class="search-tip">
        正在展示标签 <span class="search-tip-tag">#{{ searchKeyword }}</span> 的帖子
        <span class="search-tip-clear" @click="clearSearch">返回推荐</span>
      </div>
    </div>

    <div v-if="loading" class="state">加载中...</div>

    <div v-else-if="!posts.length" class="state">
      {{ isSearching ? '没有找到相关帖子' : '暂无动态' }}
    </div>

    <div v-else class="post-list">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        @click="openDetail(post)"
      >
        <div class="post-header">
          <span class="post-avatar">{{ post.avatar }}</span>
          <div class="post-user">
            <span class="post-name">{{ post.username }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <div class="post-content">{{ post.content }}</div>

        <!-- 图片 -->
        <div v-if="post.images?.length" class="post-images" :class="`grid-${Math.min(post.images.length, 3)}`">
          <img
            v-for="(img, i) in post.images.slice(0, 9)"
            :key="i"
            :src="img"
            class="post-image"
            alt="post image"
            @click.stop
          />
        </div>

        <!-- 标签 -->
        <div v-if="post.tags?.length" class="post-tags" @click.stop>
          <span
            v-for="tag in post.tags"
            :key="tag"
            class="tag"
            @click="onTagClick(tag)"
          >
            #{{ tag }}
          </span>
        </div>

        <div class="post-actions" @click.stop>
          <span
            class="action-item"
            :class="{ active: post.isLiked, 'is-loading': interacting.has(post.id) }"
            @click.stop="handleLike(post)"
          >
            <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
            <span class="action-count">{{ post.likeCount || 0 }}</span>
            <span class="action-label">点赞</span>
          </span>
          <span class="action-item">
            <span class="action-icon">💬</span>
            <span class="action-count">{{ post.commentCount || 0 }}</span>
            <span class="action-label">评论</span>
          </span>
          <span
            class="action-item"
            :class="{ active: post.isFavorited, 'is-loading': interacting.has(post.id) }"
            @click.stop="handleFavorite(post)"
          >
            <span class="action-icon">{{ post.isFavorited ? '⭐' : '☆' }}</span>
            <span class="action-count">{{ post.favoriteCount || 0 }}</span>
            <span class="action-label">收藏</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="showPager" class="pagination-wrapper">
      <van-pagination
        v-model="pageNum"
        mode="simple"
        :total-items="total"
        :items-per-page="pageSize"
        @change="onPageChange"
      />
    </div>

    <!-- 帖子详情弹窗 -->
    <van-popup
      v-model:show="showDetail"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '85vh' }"
      class="detail-popup"
      @closed="closeDetail"
    >
      <div v-if="detailPost" class="post-detail">
        <div class="post-detail-header">
          <div class="post-detail-user">
            <span class="post-detail-avatar">{{ detailPost.avatar }}</span>
            <div>
              <div class="post-detail-name">{{ detailPost.username }}</div>
              <div class="post-detail-time">{{ formatDateTime(detailPost.createdAt) }}</div>
            </div>
          </div>
        </div>

        <div class="post-detail-content">{{ detailPost.content }}</div>

        <!-- 全部图片 -->
        <div v-if="detailPost.images?.length" class="post-detail-section">
          <h4 class="post-detail-title">图片（{{ detailPost.images.length }}）</h4>
          <div class="photo-wall">
            <img
              v-for="(url, idx) in detailPost.images"
              :key="idx"
              :src="url"
              class="photo-wall-item"
              :alt="`image-${idx + 1}`"
            />
          </div>
        </div>

        <!-- 话题标签 -->
        <div v-if="detailPost.tags?.length" class="post-detail-section">
          <h4 class="post-detail-title">话题标签</h4>
          <div class="post-detail-tags">
            <span
              v-for="tag in detailPost.tags"
              :key="tag"
              class="tag"
              @click="onTagClick(tag)"
            >
              #{{ tag }}
            </span>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="post-detail-section">
          <h4 class="post-detail-title">
            评论（{{ detailPost.commentCount || comments.length }}）
          </h4>

          <div v-if="commentsLoading" class="state-inline">加载评论中...</div>

          <div v-else-if="!comments.length" class="state-inline">还没有人评论，快来抢沙发~</div>

          <ul v-else class="comment-list">
            <CommentItem
              v-for="c in comments"
              :key="c.id"
              :comment="c"
              @reply="startReply"
              @delete="handleDeleteComment"
            />
          </ul>
        </div>
      </div>

      <!-- 底部评论输入栏 -->
      <div ref="commentBarRef" class="comment-bar">
        <div v-if="replyTarget" class="comment-bar-reply">
          <span class="comment-bar-reply-text">
            回复 <b>@{{ replyTarget.username }}</b>
          </span>
          <span class="comment-bar-reply-cancel" @click="cancelReply">取消</span>
        </div>
        <div class="comment-bar-input">
          <van-field
            v-model="commentInput"
            :placeholder="replyTarget ? `回复 @${replyTarget.username}` : '说点什么...'"
            :border="false"
            class="comment-bar-field"
            @keyup.enter="submitComment"
          />
          <van-button
            type="primary"
            size="small"
            :loading="commentSending"
            :disabled="!commentInput.trim()"
            @click="submitComment"
          >
            发送
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 发布弹窗 -->
    <van-popup
      v-model:show="showPublish"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '90vh' }"
      class="publish-popup"
    >
      <div class="publish">
        <div class="publish-header">
          <h3 class="publish-title">发布帖子</h3>
        </div>

        <!-- 内容 -->
        <div class="field-wrap field-wrap--content">
          <van-field
            v-model="draftContent"
            type="textarea"
            rows="5"
            autosize
            maxlength="500"
            show-word-limit
            placeholder="说点什么吧～"
          />
        </div>

        <!-- 图片列表 -->
        <div class="publish-images">
          <div
            v-for="(url, idx) in draftImages"
            :key="idx"
            class="publish-image-item"
          >
            <img :src="url" alt="" />
            <span class="publish-image-remove" @click="removeImage(idx)">×</span>
          </div>
          <div
            v-if="draftImages.length < MAX_IMAGES"
            class="publish-image-add"
            :class="{ 'is-loading': uploadingImage }"
            @click="triggerPickImages"
          >
            <template v-if="uploadingImage">
              <van-loading size="20" color="#ff8c42" />
              <span class="hint">上传中</span>
            </template>
            <template v-else>
              <span class="plus">+</span>
              <span class="hint">{{ draftImages.length }}/{{ MAX_IMAGES }}</span>
            </template>
          </div>
        </div>

        <!-- 隐藏的文件选择器 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          multiple
          hidden
          @change="onFilesPicked"
        />

        <!-- 标签输入 -->
        <div class="field-wrap field-wrap--tag">
          <van-field
            v-model="draftTagInput"
            placeholder="添加话题标签（回车）"
            maxlength="20"
            @keyup.enter="confirmAddTag"
          >
            <template #button>
              <van-button
                size="mini"
                type="primary"
                plain
                :disabled="!draftTagInput.trim()"
                @click="confirmAddTag"
              >
                添加
              </van-button>
            </template>
          </van-field>
        </div>
        <div v-if="draftTags.length" class="publish-tags-list">
          <span
            v-for="t in draftTags"
            :key="t"
            class="publish-tag"
            @click="removeTag(t)"
          >
            #{{ t }} ×
          </span>
        </div>

        <!-- 操作 -->
        <div class="publish-footer">
          <van-button
            block
            type="primary"
            :loading="publishing"
            loading-text="发布中..."
            @click="submitPost"
          >
            发布
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.community-page {
  padding: var(--space-lg);
}

.community-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
}

.community-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.publish-btn {
  padding: 0 18px;
  font-weight: 600;
}

/* 搜索 */
.community-search {
  margin-bottom: var(--space-md);
}

.community-search :deep(.van-search) {
  padding: 0;
}

.community-search :deep(.van-search__content) {
  background: var(--color-bg-container);
  border: 1px solid var(--color-border);
}

.community-search :deep(.van-search__content .van-field__left-icon) {
  color: var(--color-text-tertiary);
}

.community-search :deep(.van-field__control) {
  color: var(--color-text-primary);
  font-size: 14px;
  caret-color: var(--color-primary);
}

.community-search :deep(.van-field__control::placeholder) {
  color: var(--color-text-disabled);
}

.search-icon {
  font-size: 16px;
  line-height: 1;
}

.search-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 4px 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.search-tip-tag {
  color: var(--color-primary);
  font-weight: 600;
}

.search-tip-clear {
  margin-left: auto;
  color: var(--color-primary);
  cursor: pointer;
}

.state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.post-card {
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.post-card:active {
  transform: scale(0.99);
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

/* 图片九宫格 */
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

/* 标签 */
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

.action-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.action-item.active .action-label {
  color: var(--color-primary);
}

/* ==================== 帖子详情弹窗 ==================== */

.detail-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.detail-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.detail-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: var(--color-text-tertiary);
}

.post-detail {
  padding: 20px 16px 28px;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  max-height: 85vh;
  overflow-y: auto;
}

.post-detail-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-divider);
  margin-bottom: var(--space-md);
}

.post-detail-user {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex: 1;
}

.post-detail-avatar {
  font-size: 36px;
  line-height: 1;
}

.post-detail-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.post-detail-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.post-detail-content {
  font-size: 15px;
  color: var(--color-text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: var(--space-md);
}

.post-detail-section {
  margin-top: var(--space-lg);
}

.post-detail-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  margin: 0 0 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.photo-wall {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.photo-wall-item {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: var(--color-secondary);
}

.post-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.post-detail-tags .tag {
  cursor: pointer;
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

.state-inline {
  text-align: center;
  padding: var(--space-md);
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-secondary);
  border-radius: var(--radius-md);
}

.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.comment-list > :deep(.comment-item) {
  border-bottom: 1px solid var(--color-divider);
}

.comment-list > :deep(.comment-item:last-child) {
  border-bottom: none;
}

/* 底部评论输入栏 */
.comment-bar {
  position: sticky;
  bottom: 0;
  background: var(--color-bg-page);
  border-top: 1px solid var(--color-divider);
  padding: 8px 12px env(safe-area-inset-bottom, 8px);
}

.comment-bar-reply {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-tertiary);
  background: var(--color-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  margin-bottom: 6px;
}

.comment-bar-reply-text b {
  color: var(--color-primary);
  font-weight: 600;
}

.comment-bar-reply-cancel {
  color: var(--color-text-secondary);
  cursor: pointer;
}

.comment-bar-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-bar-field {
  flex: 1;
  background: var(--color-secondary);
  border-radius: var(--radius-full);
  padding: 6px 14px !important;
}

.comment-bar-field :deep(.van-field__control) {
  font-size: 14px;
  color: var(--color-text-primary);
  caret-color: var(--color-primary);
}

.comment-bar-field :deep(.van-field__control::placeholder) {
  color: var(--color-text-disabled);
}

/* ==================== 分页 ==================== */

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--space-sm) 0 var(--space-xl);
}

.pagination-wrapper :deep(.van-pagination) {
  --van-pagination-height: 28px;
  --van-pagination-font-size: 12px;
  --van-pagination-item-width: 28px;
  --van-pagination-background: transparent;
  --van-pagination-item-default-color: var(--color-text-secondary);
  --van-pagination-item-disabled-color: var(--color-text-disabled);
  --van-pagination-item-disabled-background: transparent;
  gap: 4px;
}

.pagination-wrapper :deep(.van-pagination__item) {
  background: var(--color-bg-container);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
  min-width: 28px;
  font-size: 12px;
  white-space: nowrap;
  padding: 0 8px;
}

.pagination-wrapper :deep(.van-pagination__item--active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.pagination-wrapper :deep(.van-pagination__page-desc) {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.pagination-wrapper :deep(.van-pagination__item .van-pagination__prev-text),
.pagination-wrapper :deep(.van-pagination__item .van-pagination__next-text) {
  font-size: 11px;
  white-space: nowrap;
}

/* ==================== 发布弹窗 ==================== */

.publish-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.publish-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.publish {
  padding: 20px 16px 24px;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  position: relative;
}

/* Vant 默认关闭按钮被圆角裁掉，强制让它在内容区右上角 */
.publish-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: var(--color-text-tertiary);
}

.publish-popup :deep(.van-popup__close-icon:active) {
  background: var(--color-secondary);
  border-radius: 50%;
}

.publish-header {
  text-align: center;
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-sm);
  padding-right: 36px;
  border-bottom: 1px solid var(--color-divider);
}

.publish-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

/* 输入框统一样式 */
.field-wrap :deep(.van-field) {
  background: var(--color-bg-container);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 4px 12px;
  margin: 0;
}

.field-wrap :deep(.van-field__left-icon),
.field-wrap :deep(.van-field__right-icon) {
  color: var(--color-text-tertiary);
}

.field-wrap :deep(.van-field__control) {
  color: var(--color-text-primary);
  font-size: 15px;
  caret-color: var(--color-primary);
}

.field-wrap :deep(.van-field__control::placeholder) {
  color: var(--color-text-disabled);
}

.field-wrap :deep(.van-field__word-limit) {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.field-wrap--content {
  margin-top: var(--space-sm);
}

.field-wrap--content :deep(.van-field) {
  padding: 10px 12px;
  align-items: flex-start;
}

.field-wrap--content :deep(.van-field__control) {
  min-height: 96px;
  line-height: 1.6;
}

.field-wrap--tag {
  margin-top: var(--space-md);
}

.publish-images {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: var(--space-md);
}

.publish-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--color-secondary);
}

.publish-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.publish-image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  line-height: 16px;
  text-align: center;
  cursor: pointer;
  user-select: none;
}

.publish-image-add {
  aspect-ratio: 1;
  background: var(--color-secondary);
  border: 1px dashed var(--color-border-dark);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  cursor: pointer;
  gap: 2px;
  transition: opacity var(--transition-fast);
}

.publish-image-add:active {
  opacity: 0.7;
}

.publish-image-add.is-loading {
  pointer-events: none;
  opacity: 0.7;
}

.publish-image-add .plus {
  font-size: 22px;
  line-height: 1;
}

.publish-image-add .hint {
  font-size: 11px;
}

.publish-tags {
  margin-top: var(--space-md);
}

.publish-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.publish-tag {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  cursor: pointer;
}

.publish-footer {
  margin-top: var(--space-lg);
}
</style>