<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import { userApi, catApi } from '@/api'
import type { CatCheckinCalendarVO, CatProfileVO } from '@/api/types'

const calendar = ref<CatCheckinCalendarVO | null>(null)
const loading = ref(true)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

/* ==================== 打卡弹窗 ==================== */

const showCheckin = ref(false)
const catList = ref<CatProfileVO[]>([])
const checkinForm = reactive({
  catProfileId: 0,
  remark: '',
  photoURL: '',
  syncToPost: false,
})
const showCatPicker = ref(false)
const photoFile = ref<File | null>(null)
const uploading = ref(false)
const submitting = ref(false)

async function openCheckin() {
  checkinForm.catProfileId = 0
  checkinForm.remark = ''
  checkinForm.photoURL = ''
  checkinForm.syncToPost = false
  photoFile.value = null

  if (!catList.value.length) {
    try {
      const res = await catApi.getList({ pageNum: 1, pageSize: 100 })
      catList.value = res.rows || []
    } catch {
      // ignore
    }
  }
  showCheckin.value = true
}

function onCatConfirm({ selectedOptions }: { selectedOptions: { text: string; value: number }[] }) {
  checkinForm.catProfileId = selectedOptions[0]?.value ?? 0
  showCatPicker.value = false
}

function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    photoFile.value = input.files[0]
  }
}

async function uploadPhoto() {
  if (!photoFile.value) return
  uploading.value = true
  try {
    const url = await userApi.uploadCheckinPhoto(photoFile.value)
    checkinForm.photoURL = url as unknown as string
    photoFile.value = null
    showSuccessToast('照片上传成功')
  } catch {
    // error handled by interceptor
  } finally {
    uploading.value = false
  }
}

async function submitCheckin() {
  if (!checkinForm.catProfileId) {
    showToast('请选择猫咪')
    return
  }
  submitting.value = true
  try {
    await userApi.checkin({
      catProfileId: checkinForm.catProfileId,
      remark: checkinForm.remark || undefined,
      photoURL: checkinForm.photoURL || undefined,
      syncToPost: checkinForm.syncToPost || undefined,
    })
    showSuccessToast('打卡成功')
    showCheckin.value = false
    await loadCalendar()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

/* ==================== 日历 ==================== */

const checkinSet = computed<Set<string>>(() => {
  if (!calendar.value?.checkinDates) return new Set()
  return new Set(calendar.value.checkinDates.map((d) => d.slice(0, 10)))
})

const days = computed<(number | null)[]>(() => {
  const firstDay = new Date(year.value, month.value - 1, 1).getDay()
  const daysInMonth = new Date(year.value, month.value, 0).getDate()
  const cells: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(d)
  return cells
})

function isCheckin(day: number | null): boolean {
  if (!day) return false
  const dateStr =
    `${year.value}-${String(month.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return checkinSet.value.has(dateStr)
}

function isToday(day: number | null): boolean {
  if (!day) return false
  const today = new Date()
  return (
    day === today.getDate() &&
    month.value === today.getMonth() + 1 &&
    year.value === today.getFullYear()
  )
}

async function loadCalendar() {
  loading.value = true
  try {
    calendar.value = await userApi.getCheckinCalendar({
      year: year.value,
      month: month.value,
    })
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function prevMonth() {
  if (month.value === 1) {
    year.value--
    month.value = 12
  } else {
    month.value--
  }
  loadCalendar()
}

function nextMonth() {
  if (month.value === 12) {
    year.value++
    month.value = 1
  } else {
    month.value++
  }
  loadCalendar()
}

onMounted(loadCalendar)
</script>

<template>
  <div class="checkin-page">
    <!-- 月份切换 -->
    <div class="month-header">
      <van-icon name="arrow-left" class="month-arrow" @click="prevMonth" />
      <span class="month-title">{{ year }}年 {{ month }}月</span>
      <van-icon name="arrow" class="month-arrow" @click="nextMonth" />
    </div>

    <div v-if="loading" class="state">加载中...</div>

    <template v-else>
      <!-- 统计概览 -->
      <div class="checkin-summary">
        <div class="summary-item">
          <span class="summary-num">{{ calendar?.checkinCount ?? 0 }}</span>
          <span class="summary-label">本月打卡</span>
        </div>
        <div class="summary-item">
          <span class="summary-num">{{ calendar?.consecutiveDays ?? 0 }}</span>
          <span class="summary-label">连续打卡</span>
        </div>
        <div class="summary-item">
          <span class="summary-num" :class="{ full: calendar?.fullMonth }">
            {{ calendar?.fullMonth ? '✓' : '✗' }}
          </span>
          <span class="summary-label">全勤</span>
        </div>
      </div>

      <!-- 日历网格 -->
      <div class="calendar-grid">
        <div v-for="w in weekdays" :key="w" class="weekday">{{ w }}</div>
        <div
          v-for="(day, idx) in days"
          :key="idx"
          class="day"
          :class="{
            checkin: isCheckin(day),
            today: isToday(day),
            empty: day === null,
          }"
        >
          <span v-if="day" class="day-num">{{ day }}</span>
        </div>
      </div>

      <!-- 图例 -->
      <div class="legend">
        <span class="legend-item"><span class="dot checkin-dot"></span>已打卡</span>
        <span class="legend-item"><span class="dot today-dot"></span>今天</span>
      </div>

      <!-- 打卡按钮 -->
      <div class="checkin-btn-wrapper">
        <van-button type="primary" round block @click="openCheckin">
          📸 猫咪打卡
        </van-button>
      </div>
    </template>

    <!-- 打卡弹窗 -->
    <van-popup
      v-model:show="showCheckin"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '85vh' }"
      class="checkin-popup"
    >
      <div class="checkin-panel">
        <h3 class="checkin-title">猫咪打卡</h3>

        <van-form @submit="submitCheckin">
          <van-cell-group inset>
            <!-- 选择猫咪 -->
            <van-field
              name="catProfileId"
              label="猫咪"
              placeholder="请选择猫咪"
              is-link
              readonly
              clickable
              @click="showCatPicker = true"
              :model-value="catList.find(c => c.id === checkinForm.catProfileId)?.name || ''"
            />

            <!-- 拍照/上传 -->
            <van-field
              name="photo"
              label="照片"
            >
              <template #input>
                <input
                  type="file"
                  accept="image/*"
                  capture="environment"
                  @change="onPhotoChange"
                  class="photo-input"
                />
              </template>
            </van-field>
            <div v-if="checkinForm.photoURL" class="photo-preview">
              <img :src="checkinForm.photoURL" alt="预览" />
            </div>
            <div v-if="photoFile" class="upload-hint">
              <span>{{ photoFile.name }}</span>
              <van-button size="small" type="primary" :loading="uploading" @click="uploadPhoto">
                上传
              </van-button>
            </div>

            <!-- 备注 -->
            <van-field
              v-model="checkinForm.remark"
              name="remark"
              label="备注"
              type="textarea"
              placeholder="记录今天的猫咪状态..."
              rows="2"
              autosize
            />

            <!-- 同步到社区 -->
            <van-field name="syncToPost" label="同步到社区" center>
              <template #input>
                <van-switch v-model="checkinForm.syncToPost" />
              </template>
            </van-field>
          </van-cell-group>

          <div class="checkin-actions">
            <van-button
              round
              block
              type="primary"
              native-type="submit"
              :loading="submitting"
              loading-text="打卡中..."
            >
              打卡
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 猫咪选择弹窗 -->
    <van-popup v-model:show="showCatPicker" position="bottom" round>
      <van-picker
        :columns="catList.map(c => ({ text: c.name, value: c.id }))"
        @confirm="onCatConfirm"
        @cancel="showCatPicker = false"
      />
    </van-popup>
  </div>
</template>

<style scoped>
.checkin-page {
  padding: var(--space-lg);
}

.state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

/* 月份导航 */
.month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) 0 var(--space-lg);
}

.month-arrow {
  font-size: 20px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: var(--space-sm);
}

.month-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 统计 */
.checkin-summary {
  display: flex;
  justify-content: space-around;
  padding: var(--space-lg) 0;
  margin-bottom: var(--space-lg);
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.summary-item {
  text-align: center;
}

.summary-num {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.summary-num.full {
  color: #07c160;
}

.summary-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

/* 日历网格 */
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  text-align: center;
}

.weekday {
  font-size: 13px;
  color: var(--color-text-tertiary);
  padding: var(--space-sm) 0;
  font-weight: 500;
}

.day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
}

.day.empty {
  pointer-events: none;
}

.day.today .day-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.day.checkin:not(.today) {
  position: relative;
}

.day.checkin:not(.today)::after {
  content: '';
  position: absolute;
  bottom: 4px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}

/* 图例 */
.legend {
  display: flex;
  justify-content: center;
  gap: var(--space-xl);
  margin-top: var(--space-lg);
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.checkin-dot {
  background: var(--color-primary);
}

.today-dot {
  background: var(--color-primary);
  opacity: 0.8;
}

/* 打卡按钮 */
.checkin-btn-wrapper {
  margin-top: var(--space-2xl);
}

/* 打卡弹窗 */
.checkin-popup :deep(.van-popup) {
  background: var(--color-primary);
  color: #fff;
}

.checkin-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.checkin-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: rgba(255, 255, 255, 0.8);
}

.checkin-panel {
  padding: 20px 20px 32px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--color-primary);
  color: #fff;
}

.checkin-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  text-align: center;
}

.photo-input {
  font-size: 14px;
}

.photo-preview {
  padding: var(--space-md) var(--space-md) 0;
}

.photo-preview img {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.upload-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md) 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.checkin-actions {
  padding: var(--space-xl) var(--space-md);
}

.checkin-panel :deep(.van-cell-group) {
  background: transparent;
}

.checkin-panel :deep(.van-cell) {
  background: rgba(255, 255, 255, 0.15);
}

.checkin-panel :deep(.van-cell__title),
.checkin-panel :deep(.van-cell__value),
.checkin-panel :deep(.van-field__label) {
  color: #fff;
}

.checkin-panel :deep(.van-field__control) {
  color: #fff;
}

.checkin-panel :deep(.van-field__control::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

.checkin-panel :deep(.van-switch) {
  --van-switch-background: rgba(255, 255, 255, 0.3);
}
</style>
