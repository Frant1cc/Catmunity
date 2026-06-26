<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, shallowRef, nextTick, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi, healthApi, catApi } from '@/api'
import type {
  DashboardOverviewVO,
  UserGrowthTrendVO,
  CatBreedDistributionVO,
  CatHealthEventVO,
} from '@/api/types'

echarts.use([
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
])

/* ============== 概览数据 ============== */
const loadingOverview = ref(false)
const overview = ref<DashboardOverviewVO | null>(null)

const overviewCards = ref<
  Array<{ key: string; label: string; value: number; suffix: string; sub?: string; color: string; icon: string }>
>([])

const buildOverviewCards = (data: DashboardOverviewVO) => {
  return [
    {
      key: 'users',
      label: '总用户数',
      value: data.totalUsers,
      suffix: '人',
      sub: `今日 +${data.todayNewUsers}`,
      color: '#ff8c42',
      icon: '👥',
    },
    {
      key: 'active',
      label: '7 日活跃用户',
      value: data.activeUsers,
      suffix: '人',
      color: '#7dd3c0',
      icon: '🔥',
    },
    {
      key: 'posts',
      label: '帖子总数',
      value: data.totalPosts,
      suffix: '条',
      sub: `今日 +${data.todayNewPosts}`,
      color: '#ffa666',
      icon: '📝',
    },
    {
      key: 'cats',
      label: '猫咪档案',
      value: data.totalCatProfiles,
      suffix: '只',
      color: '#5bbfaa',
      icon: '🐱',
    },
    {
      key: 'checkin',
      label: '打卡总数',
      value: data.totalCheckins,
      suffix: '次',
      sub: `今日 +${data.todayNewCheckins}`,
      color: '#1890ff',
      icon: '✅',
    },
    {
      key: 'pending',
      label: '待审核内容',
      value: data.pendingModerationCount,
      suffix: '项',
      color: '#ff4d4f',
      icon: '⚠️',
    },
  ]
}

const fetchOverview = async () => {
  loadingOverview.value = true
  try {
    const data = await dashboardApi.getOverview()
    overview.value = data
    overviewCards.value = buildOverviewCards(data)
  } catch {
    /* 错误已由拦截器处理 */
  } finally {
    loadingOverview.value = false
  }
}

/* ============== 用户增长趋势 ============== */
const dimension = ref<'day' | 'week' | 'month'>('day')
const days = ref(7)
const totalGrowth = ref(0)
const lineChartEl = ref<HTMLDivElement | null>(null)
const lineChart = shallowRef<echarts.ECharts | null>(null)

const initLineChart = () => {
  if (!lineChartEl.value) return
  lineChart.value = echarts.init(lineChartEl.value)
}

const renderLineChart = (data: UserGrowthTrendVO) => {
  if (!lineChart.value) return
  const dates = data.data.map((d) => d.date)
  const counts = data.data.map((d) => d.count)
  totalGrowth.value = data.totalGrowth

  lineChart.value.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#f0e4d7',
      textStyle: { color: '#4a3728' },
    },
    grid: { top: 20, right: 24, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#f0e4d7' } },
      axisLabel: { color: '#8c8c8c', fontSize: 12 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5ebe0', type: 'dashed' } },
      axisLabel: { color: '#8c8c8c', fontSize: 12 },
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        data: counts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#ff8c42' },
        itemStyle: { color: '#ff8c42', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 140, 66, 0.35)' },
            { offset: 1, color: 'rgba(255, 140, 66, 0.02)' },
          ]),
        },
      },
    ],
  })
}

const fetchUserGrowth = async () => {
  if (!lineChart.value) await nextTick()
  initLineChart()
  try {
    const data = await dashboardApi.getUserGrowth({
      dimension: dimension.value,
      days: days.value,
    })
    renderLineChart(data)
  } catch {
    /* 错误已由拦截器处理 */
  }
}

const onDimensionChange = () => fetchUserGrowth()

/* ============== 品种分布饼图 ============== */
const pieChartEl = ref<HTMLDivElement | null>(null)
const pieChart = shallowRef<echarts.ECharts | null>(null)
const breedSummary = ref<CatBreedDistributionVO | null>(null)

const initPieChart = () => {
  if (!pieChartEl.value) return
  pieChart.value = echarts.init(pieChartEl.value)
}

const colorPalette = [
  '#ff8c42',
  '#ffa666',
  '#7dd3c0',
  '#5bbfaa',
  '#1890ff',
  '#faad14',
  '#a280ff',
  '#ff7a9c',
  '#13c2c2',
  '#f759ab',
]

const renderPieChart = (data: CatBreedDistributionVO) => {
  if (!pieChart.value) return
  const items = data.breeds.map((b, i) => ({
    name: b.breed,
    value: b.count,
    itemStyle: { color: colorPalette[i % colorPalette.length] },
  }))

  pieChart.value.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 只 ({d}%)',
      backgroundColor: '#fff',
      borderColor: '#f0e4d7',
      textStyle: { color: '#4a3728' },
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 'middle',
      textStyle: { color: '#595959', fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10,
    },
    series: [
      {
        name: '品种分布',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: items,
      },
    ],
  })
}

const fetchBreed = async () => {
  if (!pieChart.value) await nextTick()
  initPieChart()
  try {
    const data = await dashboardApi.getCatBreedDistribution()
    breedSummary.value = data
    renderPieChart(data)
  } catch {
    /* 错误已由拦截器处理 */
  }
}

/* ============== 健康事件提醒 ============== */
const reminders = ref<CatHealthEventVO[]>([])
const catNameMap = ref<Map<number, string>>(new Map())
const remindersLoading = ref(false)

const eventTypeMeta: Record<number, { label: string; color: string }> = {
  1: { label: '疫苗接种', color: 'green' },
  2: { label: '驱虫', color: 'cyan' },
  3: { label: '绝育', color: 'purple' },
  4: { label: '体检', color: 'blue' },
  5: { label: '疾病治疗', color: 'red' },
  6: { label: '美容护理', color: 'pink' },
  99: { label: '其他', color: 'default' },
}

/** 计算距离提醒日期的天数 */
const daysUntil = (dateStr?: string) => {
  if (!dateStr) return null
  const target = new Date(dateStr).getTime()
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target - today.getTime()) / (1000 * 60 * 60 * 24))
}

const fetchReminders = async () => {
  remindersLoading.value = true
  try {
    const list = await healthApi.getReminders()
    reminders.value = list || []

    // 批量取一次猫咪列表（pageSize: 200 够用），构建 id -> name 映射
    if (list.length) {
      const ids = Array.from(new Set(list.map((r) => r.catProfileId)))
      const res = await catApi.getAdminList({}, { pageNum: 1, pageSize: 200 })
      const map = new Map<number, string>()
      for (const c of res.rows || []) {
        if (ids.includes(c.id)) map.set(c.id, c.name)
      }
      // 没找到的退化为"猫咪 #id"
      for (const id of ids) {
        if (!map.has(id)) map.set(id, `猫咪 #${id}`)
      }
      catNameMap.value = map
    } else {
      catNameMap.value = new Map()
    }
  } catch {
    reminders.value = []
  } finally {
    remindersLoading.value = false
  }
}

/* ============== 生命周期 ============== */
const handleResize = () => {
  lineChart.value?.resize()
  pieChart.value?.resize()
}

onMounted(async () => {
  await fetchOverview()
  await nextTick()
  initLineChart()
  initPieChart()
  await Promise.all([fetchUserGrowth(), fetchBreed(), fetchReminders()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart.value?.dispose()
  pieChart.value?.dispose()
})

// 监听 dimension 切换
watch(dimension, () => onDimensionChange())
</script>

<template>
  <div class="dashboard">
    <!-- 概览卡片 -->
    <a-row :gutter="16">
      <a-col v-for="s in overviewCards" :key="s.key" :span="4">
        <div class="stat-card" :style="{ borderTopColor: s.color }">
          <div class="stat-icon" :style="{ background: s.color }">{{ s.icon }}</div>
          <div class="stat-body">
            <div class="stat-label">{{ s.label }}</div>
            <div class="stat-value">
              <span class="num">{{ s.value.toLocaleString() }}</span>
              <span class="suffix">{{ s.suffix }}</span>
            </div>
            <div v-if="s.sub" class="stat-sub">{{ s.sub }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 用户增长趋势 + 品种分布 -->
    <a-row :gutter="16" class="mt-16">
      <a-col :span="14">
        <a-card :bordered="false">
          <template #title>
            <div class="card-title-row">
              <span>用户增长趋势</span>
              <div class="card-tools">
                <a-radio-group v-model:value="dimension" size="small">
                  <a-radio-button value="day">日</a-radio-button>
                  <a-radio-button value="week">周</a-radio-button>
                  <a-radio-button value="month">月</a-radio-button>
                </a-radio-group>
                <a-select v-model:value="days" size="small" style="width: 100px; margin-left: 8px">
                  <a-select-option :value="7">近 7 天</a-select-option>
                  <a-select-option :value="14">近 14 天</a-select-option>
                  <a-select-option :value="30">近 30 天</a-select-option>
                </a-select>
                <span class="total-growth">总增长 +{{ totalGrowth }}</span>
              </div>
            </div>
          </template>
          <div ref="lineChartEl" class="chart-container"></div>
        </a-card>
      </a-col>

      <a-col :span="10">
        <a-card :bordered="false">
          <template #title>
            <div class="card-title-row">
              <span>猫咪品种分布</span>
              <span v-if="breedSummary" class="breed-summary">
                共 {{ breedSummary.totalCats }} 只 · {{ breedSummary.breedCount }} 个品种
              </span>
            </div>
          </template>
          <div ref="pieChartEl" class="chart-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 健康事件提醒 -->
    <a-row :gutter="16" class="mt-16">
      <a-col :span="24">
        <a-card :bordered="false">
          <template #title>
            <div class="card-title-row">
              <span>未来 7 天健康事件提醒</span>
              <a-tag v-if="reminders.length" color="orange">
                {{ reminders.length }} 项待办
              </a-tag>
            </div>
          </template>

          <a-empty
            v-if="!remindersLoading && !reminders.length"
            description="未来 7 天暂无需要提醒的健康事件"
            :image-style="{ height: '80px' }"
          />

          <a-spin v-else :spinning="remindersLoading">
            <div class="reminder-grid">
              <div
                v-for="r in reminders"
                :key="r.id"
                class="reminder-item"
                :class="{ 'is-today': daysUntil(r.nextRemindDate) === 0 }"
              >
                <div class="reminder-head">
                  <a-tag :color="eventTypeMeta[r.eventType]?.color">
                    {{ r.eventTypeDesc || eventTypeMeta[r.eventType]?.label }}
                  </a-tag>
                  <span class="reminder-cat">
                    🐱 {{ catNameMap.get(r.catProfileId) || `猫咪 #${r.catProfileId}` }}
                  </span>
                </div>
                <div class="reminder-title">{{ r.title }}</div>
                <div class="reminder-date">
                  <span class="date-label">⏰ 提醒日期</span>
                  <span class="date-value">{{ r.nextRemindDate || '-' }}</span>
                  <span
                    v-if="daysUntil(r.nextRemindDate) !== null"
                    class="date-delta"
                    :class="{
                      urgent: (daysUntil(r.nextRemindDate) ?? 99) <= 1,
                      soon: (daysUntil(r.nextRemindDate) ?? 99) <= 3,
                    }"
                  >
                    {{
                      daysUntil(r.nextRemindDate) === 0
                        ? '今天'
                        : daysUntil(r.nextRemindDate) === 1
                          ? '明天'
                          : `${daysUntil(r.nextRemindDate)} 天后`
                    }}
                  </span>
                </div>
                <div v-if="r.description" class="reminder-desc">
                  {{ r.description }}
                </div>
              </div>
            </div>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
}

.mt-16 {
  margin-top: 16px;
}

/* 概览卡 */
.stat-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-top: 3px solid #ff8c42;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value .num {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1;
}

.stat-value .suffix {
  font-size: 12px;
  color: #8c8c8c;
}

.stat-sub {
  font-size: 12px;
  color: #7dd3c0;
  margin-top: 4px;
}

/* 卡片标题区 */
.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-tools {
  display: flex;
  align-items: center;
}

.total-growth {
  font-size: 12px;
  color: #7dd3c0;
  margin-left: 12px;
  font-weight: 500;
}

.breed-summary {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: normal;
}

/* 图表容器 */
.chart-container {
  width: 100%;
  height: 320px;
}

/* 提醒卡 */
.reminder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.reminder-item {
  background: #fffaf5;
  border: 1px solid #f5ebe0;
  border-left: 3px solid #ffa666;
  border-radius: 6px;
  padding: 14px 16px;
  transition: all 200ms ease;
}

.reminder-item:hover {
  border-color: #ff8c42;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.12);
  transform: translateY(-1px);
}

.reminder-item.is-today {
  border-left-color: #ff4d4f;
  background: #fff5f5;
}

.reminder-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.reminder-cat {
  font-size: 12px;
  color: #8c8c8c;
}

.reminder-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f1f1f;
  margin-bottom: 8px;
  line-height: 1.5;
}

.reminder-date {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #595959;
  margin-bottom: 6px;
}

.date-label {
  color: #8c8c8c;
}

.date-value {
  color: #1f1f1f;
  font-weight: 500;
}

.date-delta {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 10px;
  background: #fff5eb;
  color: #ff8c42;
  font-size: 11px;
  font-weight: 500;
}

.date-delta.soon {
  background: #fff1e6;
  color: #fa8c16;
}

.date-delta.urgent {
  background: #fff1f0;
  color: #ff4d4f;
  font-weight: 600;
}

.reminder-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
