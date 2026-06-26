<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { catApi } from '@/api'
import type { CatProfileVO } from '@/api/types'

const cats = ref<CatProfileVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const pages = ref(0)
const showPager = computed(() => cats.value.length > 0)

/** 性别 0/1 → "妹妹 / 弟弟" */
function genderText(g: number) {
  return g === 1 ? '弟弟' : '妹妹'
}

/** 状态 0/1/2 → 文字 */
function statusText(s: number) {
  return ['正常', '已领养', '其他'][s] || '未知'
}

/** 生日 → 年龄文本 */
function ageText(birthday: string) {
  if (!birthday) return ''
  const ageMs = Date.now() - new Date(birthday).getTime()
  const years = Math.floor(ageMs / (365 * 24 * 3600 * 1000))
  if (years >= 1) return `${years}岁`
  const months = Math.floor(ageMs / (30 * 24 * 3600 * 1000))
  return months >= 1 ? `${months}月龄` : '未满月'
}

const loadCats = async (targetPage = pageNum.value) => {
  loading.value = true
  try {
    const res = await catApi.getList({ pageNum: targetPage, pageSize: pageSize.value })
    cats.value = res.rows
    total.value = res.total
    pageNum.value = res.pageNum
    pages.value = res.pages
  } finally {
    loading.value = false
  }
}

const onPageChange = (p: number) => {
  // v-model 已先把 pageNum 改了，所以不能用 p === pageNum 来判断
  if (loading.value) return
  loadCats(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ==================== 详情弹窗 ==================== */

const showDetail = ref(false)
const detailCat = ref<CatProfileVO | null>(null)

/** 时间格式 yyyy-MM-dd HH:mm */
function formatDateTime(s: string) {
  if (!s) return ''
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const openDetail = (cat: CatProfileVO) => {
  detailCat.value = cat
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  detailCat.value = null
}

onMounted(() => loadCats(1))
</script>

<template>
  <div class="archive-page">
    <div class="archive-header">
      <h2 class="archive-title">猫咪档案</h2>
      <p class="archive-subtitle">共 {{ total }} 只猫咪</p>
    </div>

    <div v-if="loading && !cats.length" class="state">加载中...</div>

    <div v-else-if="!cats.length" class="state">暂无猫咪档案</div>

    <div v-else class="cat-grid">
      <div
        v-for="cat in cats"
        :key="cat.id"
        class="cat-card"
        @click="openDetail(cat)"
      >
        <div class="cat-image">
          <img
            v-if="cat.photoUrls && cat.photoUrls.length"
            :src="cat.photoUrls[0]"
            :alt="cat.name"
          />
          <span v-else class="cat-emoji">🐱</span>
        </div>
        <div class="cat-info">
          <div class="cat-info-row">
            <h3 class="cat-name">{{ cat.name }}</h3>
            <span class="cat-status" :class="`status-${cat.status}`">
              {{ statusText(cat.status) }}
            </span>
          </div>
          <p class="cat-breed">{{ cat.breed }} · {{ genderText(cat.gender) }}</p>
          <p class="cat-meta">
            <span v-if="cat.birthday">{{ ageText(cat.birthday) }}</span>
            <span v-if="cat.weight">· {{ cat.weight }}kg</span>
          </p>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
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
      <div v-if="detailCat" class="detail">
        <div class="detail-header">
          <h3 class="detail-title">{{ detailCat.name }}</h3>
          <p class="detail-sub">{{ detailCat.breed }} · {{ genderText(detailCat.gender) }}</p>
        </div>

        <!-- 照片墙 -->
        <div v-if="detailCat.photoUrls && detailCat.photoUrls.length" class="detail-section">
          <h4 class="detail-section-title">照片</h4>
          <div class="photo-wall">
            <img
              v-for="(url, idx) in detailCat.photoUrls"
              :key="idx"
              :src="url"
              class="photo-wall-item"
              :alt="`${detailCat.name}-${idx + 1}`"
            />
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="detail-section-title">基本信息</h4>
          <ul class="detail-list">
            <li class="detail-list-item">
              <span class="detail-list-label">生日</span>
              <span class="detail-list-value">{{ detailCat.birthday || '—' }}</span>
            </li>
            <li class="detail-list-item">
              <span class="detail-list-label">体重</span>
              <span class="detail-list-value">{{ detailCat.weight ? `${detailCat.weight} kg` : '—' }}</span>
            </li>
            <li class="detail-list-item">
              <span class="detail-list-label">状态</span>
              <span class="detail-list-value">{{ statusText(detailCat.status) }}</span>
            </li>
            <li class="detail-list-item">
              <span class="detail-list-label">档案 ID</span>
              <span class="detail-list-value">#{{ detailCat.id }}</span>
            </li>
            <li class="detail-list-item">
              <span class="detail-list-label">创建时间</span>
              <span class="detail-list-value">{{ formatDateTime(detailCat.createdAt) }}</span>
            </li>
            <li class="detail-list-item">
              <span class="detail-list-label">更新时间</span>
              <span class="detail-list-value">{{ formatDateTime(detailCat.updatedAt) }}</span>
            </li>
          </ul>
        </div>

        <!-- 健康事件时间线 -->
        <div class="detail-section">
          <h4 class="detail-section-title">健康事件</h4>
          <div v-if="!detailCat.healthEvents || !detailCat.healthEvents.length" class="empty-events">
            暂无健康事件
          </div>
          <div v-else class="timeline">
            <div
              v-for="ev in detailCat.healthEvents"
              :key="ev.id"
              class="timeline-item"
            >
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="timeline-head">
                  <span class="timeline-title">{{ ev.title || ev.eventTypeDesc }}</span>
                  <span class="timeline-date">{{ ev.eventDate }}</span>
                </div>
                <div v-if="ev.eventTypeDesc" class="timeline-type">
                  {{ ev.eventTypeDesc }}
                </div>
                <div v-if="ev.description" class="timeline-desc">
                  {{ ev.description }}
                </div>
                <div v-if="ev.nextRemindDate" class="timeline-remind">
                  下次提醒：{{ ev.nextRemindDate }}
                </div>
                <div v-if="ev.remark" class="timeline-remark">
                  备注：{{ ev.remark }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <div v-if="showPager" class="pagination-wrapper">
      <van-pagination
        v-model="pageNum"
        mode="simple"
        :total-items="total"
        :items-per-page="pageSize"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.archive-page {
  padding: var(--space-lg);
}

.archive-header {
  margin-bottom: var(--space-xl);
}

.archive-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

.archive-subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-md);
}

.cat-card {
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
  transition: transform var(--transition-normal);
  cursor: pointer;
}

.cat-card:active {
  transform: scale(0.98);
}

.cat-image {
  width: 100%;
  aspect-ratio: 1;
  background: var(--gradient-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
}

.cat-info {
  padding: var(--space-md);
}

.cat-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-xs);
}

.cat-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.cat-status {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-secondary);
  color: var(--color-text-secondary);
}

.cat-status.status-0 {
  background: rgba(125, 211, 192, 0.15);
  color: #4ea896;
}

.cat-status.status-1 {
  background: rgba(255, 175, 122, 0.18);
  color: #c97a3a;
}

.cat-status.status-2 {
  background: var(--color-secondary);
  color: var(--color-text-tertiary);
}

.cat-breed {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 4px 0 0;
}

.cat-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 2px 0 0;
}

.cat-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cat-emoji {
  font-size: 72px;
  line-height: 1;
}

.state {
  text-align: center;
  padding: var(--space-2xl) 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--space-sm) 0 var(--space-xl);
}

/* ==================== 详情弹窗 ==================== */

.detail-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.detail-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.detail {
  padding: 20px 20px 32px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.detail-header {
  text-align: center;
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-divider);
}

.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.detail-sub {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.detail-section {
  margin-top: var(--space-lg);
}

.detail-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-md);
}

.photo-wall {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.photo-wall-item {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-md);
  background: var(--gradient-warm);
}

.detail-list {
  list-style: none;
  padding: 0;
  margin: 0;
  background: var(--color-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.detail-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  font-size: 13px;
}

.detail-list-item:not(:last-child) {
  border-bottom: 1px solid var(--color-bg-container);
}

.detail-list-label {
  color: var(--color-text-tertiary);
}

.detail-list-value {
  color: var(--color-text-primary);
  font-weight: 500;
}

.empty-events {
  text-align: center;
  padding: var(--space-md);
  color: var(--color-text-tertiary);
  font-size: 13px;
  background: var(--color-secondary);
  border-radius: var(--radius-md);
}

.timeline {
  position: relative;
  padding-left: 18px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 6px;
  bottom: 6px;
  width: 1px;
  background: var(--color-border);
}

.timeline-item {
  position: relative;
  padding-bottom: var(--space-md);
}

.timeline-dot {
  position: absolute;
  left: -18px;
  top: 4px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 2px solid var(--color-bg-page);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.timeline-content {
  background: var(--color-secondary);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.timeline-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.timeline-date {
  font-size: 12px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.timeline-type {
  font-size: 12px;
  color: var(--color-primary);
  margin-top: 2px;
}

.timeline-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  line-height: 1.5;
}

.timeline-remind,
.timeline-remark {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
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

.pagination-wrapper :deep(.van-pagination__item .van-pagination__prev-text),
.pagination-wrapper :deep(.van-pagination__item .van-pagination__next-text) {
  font-size: 11px;
  white-space: nowrap;
}

.pagination-wrapper :deep(.van-pagination__item--prev .van-button__text),
.pagination-wrapper :deep(.van-pagination__item--next .van-button__text) {
  font-size: 11px;
  white-space: nowrap;
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
</style>
