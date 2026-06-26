<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { aiApi } from '@/api'
import type { DatasetFiles } from '@/api/types'

const loading = ref(false)
const uploading = ref(false)
const dataSource = ref<DatasetFiles[]>([])
const total = ref(0)

/** 当前查看详情的文件 */
const drawerOpen = ref(false)
const detail = ref<DatasetFiles | null>(null)

/** 分页参数 */
const query = reactive({
  current: 1,
  size: 10,
})

/** 隐藏的文件选择 input */
const fileInputRef = ref<HTMLInputElement | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await aiApi.getDatasetFilesPage({
      current: query.current,
      size: query.size,
    })
    dataSource.value = res?.records ?? []
    total.value = res?.total ?? 0
  } catch {
    dataSource.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 翻页 */
const onTableChange = (pag: { current: number; pageSize: number }) => {
  query.current = pag.current
  query.size = pag.pageSize
  fetchData()
}

const onView = (row: DatasetFiles) => {
  detail.value = row
  drawerOpen.value = true
}

/** 触发文件选择 */
const triggerUpload = () => {
  fileInputRef.value?.click()
}

/** 选择文件后上传 */
const onFileSelected = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  // 重置 input，确保下次选择同一文件也能触发 change
  target.value = ''
  if (!file) return

  uploading.value = true
  try {
    await aiApi.uploadDatasetFile(file)
    message.success(`上传成功：${file.name}`)
    query.current = 1
    fetchData()
  } catch {
    // 错误已由响应拦截器 toast
  } finally {
    uploading.value = false
  }
}

/** 下载文件 */
const onDownload = async (row: DatasetFiles) => {
  try {
    const blob = (await aiApi.downloadDatasetFile(row.id)) as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.name
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    // 错误已由响应拦截器 toast
  }
}

/** 工具 */
const formatDate = (val?: string) => {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

const formatSize = (bytes?: number) => {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const fileTypeColor = (type?: string) => {
  const t = (type || '').toLowerCase()
  if (!t) return 'default'
  if (['pdf'].includes(t)) return 'red'
  if (['doc', 'docx'].includes(t)) return 'blue'
  if (['xls', 'xlsx', 'csv'].includes(t)) return 'green'
  if (['ppt', 'pptx'].includes(t)) return 'orange'
  if (['md', 'markdown', 'txt'].includes(t)) return 'cyan'
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(t)) return 'magenta'
  return 'default'
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <a-card :bordered="false" class="header-card">
      <div class="header-bar">
        <span class="page-title">知识库文件</span>
        <a-space>
          <a-button type="primary" :loading="uploading" @click="triggerUpload">
            + 上传文件
          </a-button>
        </a-space>
      </div>
      <!-- 隐藏的文件 input -->
      <input
        ref="fileInputRef"
        type="file"
        style="display: none"
        @change="onFileSelected"
      />
    </a-card>

    <a-card :bordered="false" class="table-card">
      <a-table
        :columns="[
          { title: 'ID', dataIndex: 'id', width: 90 },
          { title: '文件名', dataIndex: 'name', width: 280, ellipsis: true },
          { title: '类型', dataIndex: 'fileType', width: 90 },
          { title: '大小', dataIndex: 'fileSize', width: 110 },
          { title: '上传用户', dataIndex: 'userId', width: 110 },
          { title: '命中次数', dataIndex: 'hitCount', width: 100 },
          { title: '状态', dataIndex: 'disabled', width: 90 },
          { title: '创建时间', dataIndex: 'createdAt', width: 170 },
          { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
        ]"
        :data-source="dataSource"
        :loading="loading"
        row-key="id"
        :pagination="{
          current: query.current,
          pageSize: query.size,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        :scroll="{ x: 1200 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            <span style="font-family: monospace; font-size: 12px; color: #8c8c8c">
              {{ record.id }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <div class="file-cell">
              <span class="file-icon">📄</span>
              <a-tooltip :title="record.name">
                <span class="file-name">{{ record.name }}</span>
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'fileType'">
            <a-tag :color="fileTypeColor(record.fileType)">
              {{ (record.fileType || '-').toUpperCase() }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'fileSize'">
            <span style="color: #595959">{{ formatSize(record.fileSize) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'userId'">
            <span style="font-family: monospace; color: #8c8c8c">#{{ record.userId }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'hitCount'">
            <a-tag color="purple">{{ record.hitCount ?? 0 }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'disabled'">
            <a-tag :color="record.disabled === 1 ? 'green' : 'red'">
              {{ record.disabled === 1 ? '启用' : '已禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createdAt'">
            <span style="color: #595959">{{ formatDate(record.createdAt) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="onView(record)">详情</a>
              <a-divider type="vertical" />
              <a @click="onDownload(record)">下载</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="drawerOpen"
      :width="560"
      :title="detail ? `文件详情 · ${detail.name}` : '文件详情'"
      :destroy-on-close="false"
    >
      <template v-if="detail">
        <div class="section-title">基础信息</div>
        <a-descriptions
          :column="1"
          size="small"
          bordered
          :label-style="{ width: '120px', color: '#8c8c8c' }"
        >
          <a-descriptions-item label="ID">
            <span style="font-family: monospace; font-size: 12px">{{ detail.id }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="文件名">
            {{ detail.name }}
          </a-descriptions-item>
          <a-descriptions-item label="文件类型">
            <a-tag :color="fileTypeColor(detail.fileType)">
              {{ (detail.fileType || '-').toUpperCase() }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="文件大小">
            {{ formatSize(detail.fileSize) }}
          </a-descriptions-item>
          <a-descriptions-item label="文件路径">
            <a-typography-paragraph
              :copyable="{ text: detail.filePath }"
              style="margin-bottom: 0; word-break: break-all"
            >
              {{ detail.filePath }}
            </a-typography-paragraph>
          </a-descriptions-item>
          <a-descriptions-item label="上传用户">
            <span style="font-family: monospace">#{{ detail.userId }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="命中次数">
            <a-tag color="purple">{{ detail.hitCount ?? 0 }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detail.disabled === 1 ? 'green' : 'red'">
              {{ detail.disabled === 1 ? '启用' : '已禁用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDate(detail.createdAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="禁用时间">
            {{ formatDate(detail.disabledAt) }}
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card,
.table-card {
  border-radius: 8px;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f1f1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 抽屉 */
:deep(.ant-drawer-body) {
  padding: 20px 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 16px 0 12px;
}
</style>
