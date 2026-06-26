<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { UploadProps, UploadFile } from 'ant-design-vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { catApi, healthApi } from '@/api'
import type {
  CatProfileDTO,
  CatProfileQueryDTO,
  CatProfileVO,
  CatHealthEventVO,
} from '@/api/types'

const loading = ref(false)
const dataSource = ref<CatProfileVO[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// 查询条件
const filters = reactive<CatProfileQueryDTO>({
  name: '',
  breed: '',
  gender: undefined,
  status: undefined,
  createdAtStart: '',
  createdAtEnd: '',
})

// 详情抽屉
const drawerOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<CatProfileVO | null>(null)
// 健康事件（用 timeline 接口单独拉，按时间正序）
const timeline = ref<CatHealthEventVO[]>([])

// 新增/编辑弹窗（共用）
const formOpen = ref(false)
const formLoading = ref(false)
const isEditMode = ref(false)
const editForm = reactive<CatProfileDTO>({})
const photoList = ref<UploadFile[]>([])
const editFormRef = ref()

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '照片', dataIndex: 'photoUrls', width: 80 },
  { title: '名称', dataIndex: 'name', width: 120 },
  { title: '品种', dataIndex: 'breed', width: 140 },
  { title: '性别', dataIndex: 'gender', width: 80 },
  { title: '生日', dataIndex: 'birthday', width: 120 },
  { title: '体重(kg)', dataIndex: 'weight', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '健康事件', dataIndex: 'healthEvents', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const editRules = {
  name: [{ required: true, message: '请输入猫咪名字', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const eventTypeMeta: Record<number, { label: string; color: string }> = {
  1: { label: '疫苗接种', color: '#52c41a' },
  2: { label: '驱虫', color: '#13c2c2' },
  3: { label: '绝育', color: '#722ed1' },
  4: { label: '体检', color: '#1890ff' },
  5: { label: '疾病治疗', color: '#ff4d4f' },
  6: { label: '美容护理', color: '#eb2f96' },
  99: { label: '其他', color: '#8c8c8c' },
}

/** 把筛选条件中空值剔除，避免后端收到空字符串 */
const buildQuery = (): CatProfileQueryDTO | undefined => {
  const q: CatProfileQueryDTO = {}
  if (filters.name?.trim()) q.name = filters.name.trim()
  if (filters.breed?.trim()) q.breed = filters.breed.trim()
  if (filters.gender !== undefined) q.gender = filters.gender
  if (filters.status !== undefined) q.status = filters.status
  if (filters.createdAtStart) q.createdAtStart = filters.createdAtStart
  if (filters.createdAtEnd) q.createdAtEnd = filters.createdAtEnd
  return Object.keys(q).length ? q : undefined
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await catApi.getAdminList(buildQuery(), {
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
  filters.name = ''
  filters.breed = ''
  filters.gender = undefined
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

/** 把 photoList 同步成后端需要的字符串数组（只取已 done 的 URL） */
const collectPhotoUrls = (): string[] => {
  return photoList.value
    .filter((f) => f.status === 'done')
    .map((f) => {
      // 优先用 file.url（自定义上传已回填 / 编辑模式预填都是服务端地址）
      if (f.url && !f.url.startsWith('blob:')) return f.url
      // 兜底：从 response 中取（onSuccess 写入的）
      if (Array.isArray(f.response) && f.response[0]) return f.response[0]
      return ''
    })
    .filter(Boolean)
}

/** 打开新增弹窗 */
const onAdd = () => {
  isEditMode.value = false
  Object.assign(editForm, {
    id: undefined,
    name: '',
    breed: '',
    gender: 0,
    status: 0,
    birthday: undefined,
    weight: undefined,
  })
  photoList.value = []
  formOpen.value = true
}

/** 打开编辑弹窗（用详情接口保证拿到最新完整数据） */
const onEdit = async (record: CatProfileVO) => {
  isEditMode.value = true
  formOpen.value = true
  formLoading.value = true
  try {
    const detail = await catApi.getAdminDetail(record.id)
    Object.assign(editForm, {
      id: detail.id,
      name: detail.name,
      breed: detail.breed,
      gender: detail.gender,
      birthday: detail.birthday,
      weight: detail.weight,
      status: detail.status,
    })
    photoList.value = (detail.photoUrls || []).map((url, idx) => ({
      uid: -1 - idx,
      name: `photo-${idx + 1}.jpg`,
      status: 'done',
      url,
    }))
  } catch {
    formOpen.value = false
  } finally {
    formLoading.value = false
  }
}

const onFormSubmit = async () => {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }

  // 若仍有上传中的图片，提示稍候
  const uploading = photoList.value.some((f) => f.status === 'uploading')
  if (uploading) {
    message.warning('图片正在上传中，请稍候')
    return
  }

  const payload: CatProfileDTO = {
    name: editForm.name,
    breed: editForm.breed,
    gender: editForm.gender,
    birthday: editForm.birthday,
    weight: editForm.weight,
    status: editForm.status,
    photoUrls: collectPhotoUrls(),
  }
  if (isEditMode.value) payload.id = editForm.id

  formLoading.value = true
  try {
    if (isEditMode.value) {
      await catApi.updateAdmin(payload)
      message.success('已保存 ' + payload.name)
    } else {
      const created = await catApi.createAdmin(payload)
      message.success('已创建 ' + (created.name || payload.name))
    }
    formOpen.value = false
    // 新增后回到第一页，编辑则留在当前页
    if (!isEditMode.value) pagination.current = 1
    await fetchData()
  } catch {
    /* 错误已由拦截器处理 */
  } finally {
    formLoading.value = false
  }
}

/** 上传前的本地校验：类型 + 大小 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('单张图片不能超过 5MB')
    return false
  }
  return true
}

/** 自定义上传逻辑：调 uploadPhotos，把服务端返回的 URL 写回 file.url */
const customUpload: UploadProps['customRequest'] = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    const urls = await catApi.uploadPhotos([file as File])
    // 在 photoList 中找到对应的 file item，把 url 替换成服务端地址
    const item = photoList.value.find((f) => f.uid === (file as UploadFile).uid)
    if (item) {
      item.url = urls[0]
    }
    onSuccess(urls, new XMLHttpRequest())
  } catch (err) {
    onError(err as Error)
  }
}

/** 预览：a-image 已经在 picture-card 模式下自带 */
const onPreview = async (file: UploadFile) => {
  // 可在 Modal 中放大预览，简单起见用 window.open
  if (file.url) window.open(file.url, '_blank')
}

const onView = async (record: CatProfileVO) => {
  drawerOpen.value = true
  detail.value = null
  timeline.value = []
  detailLoading.value = true
  try {
    const [profile, events] = await Promise.all([
      catApi.getAdminDetail(record.id),
      healthApi.getTimeline(record.id).catch(() => [] as CatHealthEventVO[]),
    ])
    detail.value = profile
    timeline.value = events
  } catch {
    drawerOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

const onDelete = (record: CatProfileVO) => {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除「${record.name}」的档案吗？该操作不可恢复。`,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      try {
        await catApi.deleteAdmin(record.id)
        message.success('已删除 ' + record.name)
        if (dataSource.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchData()
      } catch {
        /* 错误已由拦截器处理 */
      }
    },
  })
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
          <a-col :span="5">
            <a-form-item label="猫咪名称">
              <a-input
                v-model:value="filters.name"
                placeholder="模糊搜索"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="品种">
              <a-input
                v-model:value="filters.breed"
                placeholder="模糊搜索"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="性别">
              <a-select
                v-model:value="filters.gender"
                placeholder="全部"
                style="width: 100%"
                allow-clear
              >
                <a-select-option :value="0">母猫</a-select-option>
                <a-select-option :value="1">公猫</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="状态">
              <a-select
                v-model:value="filters.status"
                placeholder="全部"
                style="width: 100%"
                allow-clear
              >
                <a-select-option :value="0">正常</a-select-option>
                <a-select-option :value="1">已领养</a-select-option>
                <a-select-option :value="2">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="创建时间">
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
              <a-button type="primary" @click="onAdd">+ 新增档案</a-button>
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
        :scroll="{ x: 1500 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'photoUrls'">
            <a-avatar
              v-if="record.photoUrls && record.photoUrls.length"
              :size="36"
              :src="record.photoUrls[0]"
              shape="square"
            />
            <a-avatar v-else :size="36" shape="square" style="background: #fff5eb">
              🐱
            </a-avatar>
          </template>
          <template v-else-if="column.dataIndex === 'gender'">
            <a-tag :color="record.gender === 0 ? 'pink' : 'blue'">
              {{ record.gender === 0 ? '♀ 母' : '♂ 公' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="['green', 'orange', 'default'][record.status]">
              {{ ['正常', '已领养', '其他'][record.status] }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'healthEvents'">
            <a-badge
              :count="record.healthEvents.length"
              :number-style="{
                backgroundColor: record.healthEvents.length ? '#ff8c42' : '#d9d9d9',
              }"
              :show-zero="true"
            />
          </template>
          <template v-else-if="column.dataIndex === 'createdAt' || column.dataIndex === 'updatedAt'">
            <span style="color: #595959">{{ formatDate(record[column.dataIndex]) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="onView(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="onEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a style="color: #ff4d4f" @click="onDelete(record)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 弹窗（共用） -->
    <a-modal
      v-model:open="formOpen"
      :title="isEditMode ? '编辑猫咪档案' : '新增猫咪档案'"
      :width="680"
      :confirm-loading="formLoading"
      :mask-closable="false"
      ok-text="保存"
      cancel-text="取消"
      :destroy-on-close="true"
      @ok="onFormSubmit"
    >
      <a-spin :spinning="formLoading && isEditMode">
        <a-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          layout="vertical"
          class="edit-form"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="名字" name="name">
                <a-input v-model:value="editForm.name" placeholder="请输入" maxlength="20" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="品种">
                <a-input v-model:value="editForm.breed" placeholder="如：英短、橘猫" maxlength="40" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="性别" name="gender">
                <a-radio-group v-model:value="editForm.gender">
                  <a-radio :value="0">♀ 母猫</a-radio>
                  <a-radio :value="1">♂ 公猫</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="状态" name="status">
                <a-select v-model:value="editForm.status" placeholder="请选择" style="width: 100%">
                  <a-select-option :value="0">正常</a-select-option>
                  <a-select-option :value="1">已领养</a-select-option>
                  <a-select-option :value="2">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="生日">
                <a-date-picker
                  v-model:value="editForm.birthday"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  placeholder="选择生日"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="体重 (kg)">
                <a-input-number
                  v-model:value="editForm.weight"
                  :min="0"
                  :max="50"
                  :step="0.1"
                  :precision="2"
                  style="width: 100%"
                  placeholder="0.00"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="猫咪照片（点击或拖拽上传，单张 ≤ 5MB）">
                <a-upload
                  v-model:file-list="photoList"
                  list-type="picture-card"
                  :multiple="true"
                  :max-count="9"
                  :before-upload="beforeUpload"
                  :custom-request="customUpload"
                  :on-preview="onPreview"
                  accept="image/*"
                >
                  <div v-if="photoList.length < 9">
                    <PlusOutlined />
                    <div style="margin-top: 4px; font-size: 12px">上传</div>
                  </div>
                </a-upload>
                <div class="upload-hint">
                  上传后会得到后端返回的 URL，最多 9 张
                </div>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-spin>
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="drawerOpen"
      :width="640"
      :title="detail ? `猫咪档案详情 · ${detail.name}` : '猫咪档案详情'"
      :destroy-on-close="false"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div v-if="detail.photoUrls && detail.photoUrls.length" class="photo-wall">
            <a-image
              v-for="(url, idx) in detail.photoUrls"
              :key="idx"
              :src="url"
              :width="96"
              :height="96"
              :preview="{ src: url }"
              class="photo-item"
            />
          </div>
          <div v-else class="photo-empty">🐱 暂无照片</div>

          <a-descriptions
            :column="2"
            size="small"
            bordered
            class="info-desc"
            :label-style="{ width: '100px', color: '#8c8c8c' }"
          >
            <a-descriptions-item label="名字">{{ detail.name }}</a-descriptions-item>
            <a-descriptions-item label="品种">{{ detail.breed || '-' }}</a-descriptions-item>
            <a-descriptions-item label="性别">
              <a-tag :color="detail.gender === 0 ? 'pink' : 'blue'">
                {{ detail.gender === 0 ? '♀ 母猫' : '♂ 公猫' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="['green', 'orange', 'default'][detail.status]">
                {{ ['正常', '已领养', '其他'][detail.status] }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="生日">{{ detail.birthday || '-' }}</a-descriptions-item>
            <a-descriptions-item label="体重">{{ detail.weight }} kg</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">
              {{ formatDate(detail.createdAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间" :span="2">
              {{ formatDate(detail.updatedAt) }}
            </a-descriptions-item>
          </a-descriptions>

          <div class="section-title">
            健康事件时间线
            <span class="section-meta">共 {{ timeline.length }} 条</span>
          </div>

          <a-empty
            v-if="!timeline.length"
            description="暂无健康事件"
            :image-style="{ height: '80px' }"
          />
          <a-timeline v-else class="health-timeline">
            <a-timeline-item
              v-for="ev in timeline"
              :key="ev.id"
              :color="eventTypeMeta[ev.eventType]?.color || '#8c8c8c'"
            >
              <div class="event-head">
                <a-tag :color="eventTypeMeta[ev.eventType]?.color">
                  {{ ev.eventTypeDesc || eventTypeMeta[ev.eventType]?.label }}
                </a-tag>
                <span class="event-title">{{ ev.title }}</span>
              </div>
              <div class="event-date">📅 {{ ev.eventDate }}</div>
              <div v-if="ev.description" class="event-desc">{{ ev.description }}</div>
              <div v-if="ev.nextRemindDate" class="event-remind">
                ⏰ 下次提醒：{{ ev.nextRemindDate }}
              </div>
              <div v-if="ev.remark" class="event-remark">备注：{{ ev.remark }}</div>
            </a-timeline-item>
          </a-timeline>
        </template>
      </a-spin>
    </a-drawer>
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

.edit-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.upload-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 8px;
}

/* 抽屉内部 */
:deep(.ant-drawer-body) {
  padding: 20px 24px;
}

.photo-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.photo-item {
  border-radius: 8px;
  overflow: hidden;
}

.photo-empty {
  width: 100%;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: #fff5eb;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #a89080;
}

.info-desc {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.section-meta {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: normal;
}

.health-timeline {
  padding-top: 4px;
}

.event-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.event-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f1f1f;
}

.event-date {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.event-desc {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}

.event-remind {
  font-size: 12px;
  color: #ff8c42;
  margin-top: 4px;
}

.event-remark {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
