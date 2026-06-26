<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { adminUserApi } from '@/api'
import type { BanUserDTO, UserAdminVO, UserQueryDTO } from '@/api/types'

const loading = ref(false)
const dataSource = ref<UserAdminVO[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// 查询条件
const filters = reactive<UserQueryDTO>({
  keyword: '',
  type: undefined,
  status: undefined,
  createdAtStart: '',
  createdAtEnd: '',
})

// 详情抽屉
const drawerOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<UserAdminVO | null>(null)

// 封禁弹窗
const banOpen = ref(false)
const banLoading = ref(false)
const banForm = reactive<BanUserDTO>({})
const banFormRef = ref()

// 分配角色弹窗
const roleOpen = ref(false)
const roleLoading = ref(false)
const roleUserId = ref<number | null>(null)
const roleTarget = ref<0 | 1 | 2>(2)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户', dataIndex: 'identifier', width: 240 },
  { title: '手机', dataIndex: 'phone', width: 140 },
  { title: '邮箱', dataIndex: 'email', width: 220, ellipsis: true },
  { title: '类型', dataIndex: 'type', width: 130 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '注册时间', dataIndex: 'createdAt', width: 170 },
  { title: '最近活跃', dataIndex: 'lastActive', width: 170 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

const typeMeta: Record<number, { label: string; color: string }> = {
  0: { label: '超级管理员', color: 'magenta' },
  1: { label: '普通管理员', color: 'orange' },
  2: { label: '普通用户', color: 'blue' },
}

const typeOptions = [
  { value: 0 as const, label: '超级管理员', color: 'magenta' },
  { value: 1 as const, label: '普通管理员', color: 'orange' },
  { value: 2 as const, label: '普通用户', color: 'blue' },
]

const statusMeta: Record<number, { label: string; color: string }> = {
  0: { label: '正常', color: 'green' },
  1: { label: '已封禁', color: 'red' },
}

const statCards = [
  { key: 'post', label: '发帖数', icon: '📝', color: '#ff8c42' },
  { key: 'checkin', label: '打卡数', icon: '✅', color: '#7dd3c0' },
  { key: 'follow', label: '关注数', icon: '👀', color: '#1890ff' },
  { key: 'follower', label: '粉丝数', icon: '💖', color: '#eb2f96' },
] as const

const banRules = {
  reason: [{ required: true, message: '请输入封禁原因', trigger: 'blur' }],
}

const getStatValue = (key: typeof statCards[number]['key']): number => {
  if (!detail.value) return 0
  const map = {
    post: detail.value.postCount,
    checkin: detail.value.checkinCount,
    follow: detail.value.followCount,
    follower: detail.value.followerCount,
  } as const
  return map[key]
}

/** 把筛选条件中空值剔除 */
const buildQuery = (): UserQueryDTO | undefined => {
  const q: UserQueryDTO = {}
  if (filters.keyword?.trim()) q.keyword = filters.keyword.trim()
  if (filters.type !== undefined) q.type = filters.type
  if (filters.status !== undefined) q.status = filters.status
  if (filters.createdAtStart) q.createdAtStart = filters.createdAtStart
  if (filters.createdAtEnd) q.createdAtEnd = filters.createdAtEnd
  return Object.keys(q).length ? q : undefined
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await adminUserApi.getList(buildQuery(), {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    dataSource.value = res.rows
    pagination.total = res.total
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

const onTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const onSearch = () => {
  pagination.current = 1
  fetchData()
}

const onReset = () => {
  filters.keyword = ''
  filters.type = undefined
  filters.status = undefined
  filters.createdAtStart = ''
  filters.createdAtEnd = ''
  onSearch()
}

const onDateChange = (dates: [string, string] | null) => {
  if (dates && dates.length === 2) {
    filters.createdAtStart = dates[0]
    filters.createdAtEnd = dates[1]
  } else {
    filters.createdAtStart = ''
    filters.createdAtEnd = ''
  }
}

const onView = async (record: UserAdminVO) => {
  drawerOpen.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await adminUserApi.getDetail(record.id)
  } catch {
    drawerOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

/* ============== 封禁 ============== */
const onBan = (record: UserAdminVO) => {
  banForm.userId = record.id
  banForm.reason = ''
  banOpen.value = true
}

const onBanSubmit = async () => {
  try {
    await banFormRef.value?.validate()
  } catch {
    return
  }
  banLoading.value = true
  try {
    await adminUserApi.ban({ userId: banForm.userId, reason: banForm.reason })
    message.success('已封禁')
    banOpen.value = false
    await fetchData()
    // 如果当前抽屉就是该用户，刷新详情
    if (drawerOpen.value && detail.value?.id === banForm.userId) {
      const fresh = await adminUserApi.getDetail(banForm.userId!)
      detail.value = fresh
    }
  } catch {
    /* 错误已由拦截器处理 */
  } finally {
    banLoading.value = false
  }
}

/* ============== 解封 ============== */
const onUnban = (record: UserAdminVO) => {
  Modal.confirm({
    title: '解封确认',
    content: `确定要解封「${record.identifier}」吗？解封后该用户将恢复使用。`,
    okText: '解封',
    cancelText: '取消',
    async onOk() {
      try {
        await adminUserApi.unban(record.id)
        message.success('已解封 ' + record.identifier)
        await fetchData()
        if (drawerOpen.value && detail.value?.id === record.id) {
          const fresh = await adminUserApi.getDetail(record.id)
          detail.value = fresh
        }
      } catch {
        /* 错误已由拦截器处理 */
      }
    },
  })
}

/* ============== 分配角色 ============== */
const onAssignRole = (record?: UserAdminVO) => {
  const target = record || detail.value
  if (!target) return
  roleUserId.value = target.id
  roleTarget.value = target.type
  roleOpen.value = true
}

const onRoleSubmit = async () => {
  if (!roleUserId.value) return
  roleLoading.value = true
  try {
    await adminUserApi.assignRole(roleUserId.value, roleTarget.value)
    const newType = typeOptions.find((o) => o.value === roleTarget.value)
    message.success('已分配为：' + newType?.label)
    roleOpen.value = false
    await fetchData()
    if (drawerOpen.value && detail.value?.id === roleUserId.value) {
      const fresh = await adminUserApi.getDetail(roleUserId.value)
      detail.value = fresh
    }
  } catch {
    /* 错误已由拦截器处理 */
  } finally {
    roleLoading.value = false
  }
}

const formatDate = (val?: string) => {
  if (!val) return '-'
  return val.replace('T', ' ').slice(0, 16)
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <a-card :bordered="false" class="search-card">
      <a-form layout="vertical" :model="filters" class="search-form">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="关键词">
              <a-input
                v-model:value="filters.keyword"
                placeholder="账号 / 手机 / 邮箱"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="用户类型">
              <a-select
                v-model:value="filters.type"
                placeholder="全部"
                style="width: 100%"
                allow-clear
              >
                <a-select-option :value="0">超级管理员</a-select-option>
                <a-select-option :value="1">普通管理员</a-select-option>
                <a-select-option :value="2">普通用户</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="账号状态">
              <a-select
                v-model:value="filters.status"
                placeholder="全部"
                style="width: 100%"
                allow-clear
              >
                <a-select-option :value="0">正常</a-select-option>
                <a-select-option :value="1">已封禁</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="注册时间">
              <a-range-picker
                style="width: 100%"
                @change="onDateChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24" style="text-align: right">
            <a-space>
              <a-button @click="onReset">重置</a-button>
              <a-button type="primary" @click="onSearch">查询</a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        row-key="id"
        :scroll="{ x: 1300 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'identifier'">
            <div class="user-cell">
              <a-avatar
                v-if="record.avatar"
                :size="36"
                :src="record.avatar"
              />
              <a-avatar v-else :size="36" style="background: #fff5eb; color: #ff8c42">
                {{ record.identifier?.[0] || '?' }}
              </a-avatar>
              <div class="user-info">
                <div class="user-name">{{ record.identifier || '-' }}</div>
                <div class="user-id">ID: {{ record.id }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="typeMeta[record.type]?.color">
              {{ record.typeStr || typeMeta[record.type]?.label }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusMeta[record.status]?.color">
              {{ record.statusStr || statusMeta[record.status]?.label }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createdAt' || column.dataIndex === 'lastActive'">
            <span style="color: #595959">{{ formatDate(record[column.dataIndex]) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="onView(record)">查看</a>
              <a-divider type="vertical" />
              <a
                :style="{ color: record.status === 1 ? '#52c41a' : '#ff4d4f' }"
                @click="record.status === 1 ? onUnban(record) : onBan(record)"
              >
                {{ record.status === 1 ? '解封' : '封禁' }}
              </a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="drawerOpen"
      :width="560"
      :title="detail ? `用户详情 · ${detail.identifier}` : '用户详情'"
      :destroy-on-close="false"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <!-- 头部：头像 + 账号 + 状态 -->
          <div class="detail-header">
            <a-avatar
              v-if="detail.avatar"
              :size="64"
              :src="detail.avatar"
            />
            <a-avatar
              v-else
              :size="64"
              style="background: linear-gradient(135deg, #ff8c42 0%, #ffa666 100%); color: #fff; font-size: 24px"
            >
              {{ detail.identifier?.[0] || '?' }}
            </a-avatar>
            <div class="detail-head-info">
              <div class="detail-name">{{ detail.identifier || '-' }}</div>
              <div class="detail-tags">
                <a-tag :color="typeMeta[detail.type]?.color">
                  {{ detail.typeStr || typeMeta[detail.type]?.label }}
                </a-tag>
                <a-tag :color="statusMeta[detail.status]?.color">
                  {{ detail.statusStr || statusMeta[detail.status]?.label }}
                </a-tag>
              </div>
            </div>
          </div>

          <!-- 互动数据 4 卡 -->
          <div class="section-title">互动数据</div>
          <a-row :gutter="12">
            <a-col v-for="s in statCards" :key="s.key" :span="6">
              <div class="stat-mini-card" :style="{ borderTopColor: s.color }">
                <div class="mini-icon">{{ s.icon }}</div>
                <div class="mini-body">
                  <div class="mini-value">{{ getStatValue(s.key).toLocaleString() }}</div>
                  <div class="mini-label">{{ s.label }}</div>
                </div>
              </div>
            </a-col>
          </a-row>

          <!-- 基础信息 -->
          <div class="section-title">基础信息</div>
          <a-descriptions
            :column="1"
            size="small"
            bordered
            :label-style="{ width: '100px', color: '#8c8c8c' }"
          >
            <a-descriptions-item label="用户 ID">{{ detail.id }}</a-descriptions-item>
            <a-descriptions-item label="账号">{{ detail.identifier || '-' }}</a-descriptions-item>
            <a-descriptions-item label="手机号">{{ detail.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ detail.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="用户类型">
              <a-tag :color="typeMeta[detail.type]?.color">
                {{ detail.typeStr || typeMeta[detail.type]?.label }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="账号状态">
              <a-tag :color="statusMeta[detail.status]?.color">
                {{ detail.statusStr || statusMeta[detail.status]?.label }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>

          <!-- 时间信息 -->
          <div class="section-title">时间信息</div>
          <a-descriptions
            :column="1"
            size="small"
            bordered
            :label-style="{ width: '100px', color: '#8c8c8c' }"
          >
            <a-descriptions-item label="注册时间">
              {{ formatDate(detail.createdAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="最近活跃">
              {{ formatDate(detail.lastActive) }}
            </a-descriptions-item>
            <a-descriptions-item v-if="detail.status === 1" label="封禁时间">
              {{ formatDate(detail.bannedAt) }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 封禁原因 -->
          <template v-if="detail.status === 1 && detail.banReason">
            <div class="section-title">封禁原因</div>
            <div class="ban-reason">
              <a-alert
                type="error"
                show-icon
                :message="detail.banReason"
              />
            </div>
          </template>
        </template>
      </a-spin>

      <!-- 抽屉底部操作栏 -->
      <template v-if="detail" #footer>
        <a-space>
          <a-button
            v-if="detail.status === 0"
            danger
            @click="onBan(detail)"
          >
            封禁
          </a-button>
          <a-button
            v-else
            type="primary"
            @click="onUnban(detail)"
          >
            解封
          </a-button>
          <a-button @click="onAssignRole(detail)">分配角色</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 封禁弹窗 -->
    <a-modal
      v-model:open="banOpen"
      title="封禁用户"
      :width="480"
      :confirm-loading="banLoading"
      :mask-closable="false"
      ok-text="确认封禁"
      ok-button-props="{ danger: true }"
      cancel-text="取消"
      :destroy-on-close="true"
      @ok="onBanSubmit"
    >
      <a-form
        ref="banFormRef"
        :model="banForm"
        :rules="banRules"
        layout="vertical"
        class="ban-form"
      >
        <a-alert
          type="warning"
          show-icon
          message="封禁后该用户将无法登录和操作，请谨慎操作"
          style="margin-bottom: 16px"
        />
        <a-form-item label="用户 ID">
          <a-input :value="banForm.userId" disabled />
        </a-form-item>
        <a-form-item label="封禁原因" name="reason">
          <a-textarea
            v-model:value="banForm.reason"
            placeholder="请说明封禁原因，将记录在案"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:open="roleOpen"
      title="分配用户角色"
      :width="420"
      :confirm-loading="roleLoading"
      :mask-closable="false"
      ok-text="保存"
      cancel-text="取消"
      :destroy-on-close="true"
      @ok="onRoleSubmit"
    >
      <a-form layout="vertical" class="role-form">
        <a-form-item label="用户 ID">
          <a-input :value="roleUserId ?? ''" disabled />
        </a-form-item>
        <a-form-item label="用户类型">
          <a-radio-group v-model:value="roleTarget">
            <a-radio
              v-for="o in typeOptions"
              :key="o.value"
              :value="o.value"
            >
              <a-tag :color="o.color" style="margin-right: 0">{{ o.label }}</a-tag>
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          message="修改角色会立即生效，请确认操作"
        />
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card,
.table-card {
  border-radius: 8px;
}

.search-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

/* 用户列 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f1f1f;
}

.user-id {
  font-size: 12px;
  color: #8c8c8c;
}

/* 抽屉内部 */
:deep(.ant-drawer-body) {
  padding: 20px 24px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5ebe0;
  margin-bottom: 20px;
}

.detail-head-info {
  flex: 1;
  min-width: 0;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 6px;
  word-break: break-all;
}

.detail-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 20px 0 12px;
}

/* 互动数据小卡 */
.stat-mini-card {
  background: #ffffff;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 2px solid #ff8c42;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.mini-icon {
  font-size: 20px;
  line-height: 1;
}

.mini-body {
  flex: 1;
  min-width: 0;
}

.mini-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.2;
}

.mini-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.ban-reason {
  margin-top: 4px;
}

.ban-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.role-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}
</style>
