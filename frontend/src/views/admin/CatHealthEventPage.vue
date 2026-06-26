<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { catApi, healthApi } from '@/api'
import type { CatHealthEventDTO, CatHealthEventVO, CatProfileVO } from '@/api/types'

const loading = ref(false)
const dataSource = ref<CatHealthEventVO[]>([])

interface CatOption {
  id: number
  name: string
  breed?: string
  photoUrl?: string
}

// 当前选中的猫咪
const selectedCatId = ref<number | null>(null)
const selectedCat = computed<CatOption | null>(() => {
  if (!selectedCatId.value) return null
  return (
    catOptions.value.find((c) => c.id === selectedCatId.value) || {
      id: selectedCatId.value,
      name: '已选猫咪',
    }
  )
})

// 远程搜索猫咪
const catOptions = ref<CatOption[]>([])
const catSearchLoading = ref(false)

const eventTypeMeta: Record<number, { label: string; color: string }> = {
  1: { label: '疫苗接种', color: 'green' },
  2: { label: '驱虫', color: 'cyan' },
  3: { label: '绝育', color: 'purple' },
  4: { label: '体检', color: 'blue' },
  5: { label: '疾病治疗', color: 'red' },
  6: { label: '美容护理', color: 'pink' },
  99: { label: '其他', color: 'default' },
}

const eventTypeOptions = [
  { value: 1, label: '疫苗接种' },
  { value: 2, label: '驱虫' },
  { value: 3, label: '绝育' },
  { value: 4, label: '体检' },
  { value: 5, label: '疾病治疗' },
  { value: 6, label: '美容护理' },
  { value: 99, label: '其他' },
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '事件类型', dataIndex: 'eventType', width: 120 },
  { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
  { title: '描述', dataIndex: 'description', width: 240, ellipsis: true },
  { title: '发生日期', dataIndex: 'eventDate', width: 140 },
  { title: '下次提醒', dataIndex: 'nextRemindDate', width: 140 },
  { title: '备注', dataIndex: 'remark', width: 180, ellipsis: true },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

/* ============== 表单弹窗 ============== */
const formOpen = ref(false)
const formLoading = ref(false)
const isEditMode = ref(false)
const editForm = ref<CatHealthEventDTO>({})
const editFormRef = ref()

const formRules = {
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择事件日期', trigger: 'change' }],
}

/** 远程搜索猫咪（按名称模糊匹配） */
const searchCats = async (keyword: string) => {
  catSearchLoading.value = true
  try {
    const res = await catApi.getAdminList(
      keyword ? { name: keyword } : {},
      { pageNum: 1, pageSize: 20 },
    )
    catOptions.value = (res.rows || []).map((c: CatProfileVO) => ({
      id: c.id,
      name: c.name,
      breed: c.breed,
      photoUrl: c.photoUrls?.[0],
    }))
  } catch {
    catOptions.value = []
  } finally {
    catSearchLoading.value = false
  }
}

const onCatSearch = (val: string) => searchCats(val)

/** 选中猫咪后加载该猫的健康事件 */
const onCatChange = (val: number) => {
  selectedCatId.value = val
  fetchEvents()
}

/** 拉取事件列表（按日期倒序） */
const fetchEvents = async () => {
  if (!selectedCatId.value) {
    dataSource.value = []
    return
  }
  loading.value = true
  try {
    const events = await healthApi.getByCatId(selectedCatId.value)
    dataSource.value = events || []
  } catch {
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

const onAdd = () => {
  if (!selectedCatId.value) {
    message.warning('请先选择猫咪')
    return
  }
  isEditMode.value = false
  editForm.value = {
    catProfileId: selectedCatId.value,
    eventType: 1,
    title: '',
    description: '',
    eventDate: undefined,
    nextRemindDate: undefined,
    remark: '',
  }
  formOpen.value = true
}

const onEdit = (row: CatHealthEventVO) => {
  isEditMode.value = true
  editForm.value = {
    id: row.id,
    catProfileId: row.catProfileId,
    eventType: row.eventType,
    title: row.title,
    description: row.description,
    eventDate: row.eventDate,
    nextRemindDate: row.nextRemindDate,
    remark: row.remark,
  }
  formOpen.value = true
}

const onFormSubmit = async () => {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }

  formLoading.value = true
  try {
    if (isEditMode.value) {
      await healthApi.update(editForm.value)
      message.success('已保存')
    } else {
      await healthApi.create(editForm.value)
      message.success('已新增')
    }
    formOpen.value = false
    await fetchEvents()
  } catch {
    /* 错误已由拦截器处理 */
  } finally {
    formLoading.value = false
  }
}

const onDelete = (row: CatHealthEventVO) => {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除「${row.title}」吗？该操作不可恢复。`,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      try {
        await healthApi.delete(row.id)
        message.success('已删除')
        await fetchEvents()
      } catch {
        /* 错误已由拦截器处理 */
      }
    },
  })
}

// 选中变化时自动刷新
watch(selectedCatId, () => fetchEvents())

onMounted(() => {
  // 默认不拉取列表，等用户选猫
})
</script>

<template>
  <div class="page-container">
    <!-- 猫咪选择 + 简介 -->
    <a-card :bordered="false" class="cat-bar">
      <a-row :gutter="16" align="middle">
        <a-col :span="8">
          <div class="cat-select-label">选择猫咪</div>
          <a-select
            :value="selectedCatId ?? undefined"
            show-search
            :filter-option="false"
            placeholder="输入名字搜索猫咪档案"
            style="width: 100%"
            :loading="catSearchLoading"
            :not-found-content="catSearchLoading ? '搜索中…' : '暂无数据'"
            allow-clear
            @search="onCatSearch"
            @change="(v: number) => onCatChange(v)"
            @focus="() => searchCats('')"
          >
            <a-select-option
              v-for="c in catOptions"
              :key="c.id"
              :value="c.id"
            >
              <div class="cat-option">
                <span class="cat-name">{{ c.name }}</span>
                <span v-if="c.breed" class="cat-breed">· {{ c.breed }}</span>
              </div>
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="16">
          <div v-if="selectedCat" class="cat-summary">
            <a-avatar
              v-if="selectedCat.photoUrl"
              :size="48"
              :src="selectedCat.photoUrl"
              shape="square"
            />
            <a-avatar v-else :size="48" shape="square" style="background: #fff5eb">
              🐱
            </a-avatar>
            <div class="cat-info">
              <div class="cat-info-name">{{ selectedCat.name }}</div>
              <div v-if="selectedCat.breed" class="cat-info-breed">
                {{ selectedCat.breed }}
              </div>
            </div>
            <div class="cat-stats">
              共 <strong>{{ dataSource.length }}</strong> 条健康事件
            </div>
          </div>
          <div v-else class="cat-empty">请先选择一只猫咪以查看其健康事件</div>
        </a-col>
      </a-row>
    </a-card>

    <!-- 事件列表 -->
    <a-card :bordered="false" class="table-card">
      <template #title>
        <div class="card-title-row">
          <span>健康事件列表（按日期倒序）</span>
          <a-button type="primary" :disabled="!selectedCatId" @click="onAdd">
            + 新增事件
          </a-button>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1200 }"
        :pagination="{ showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        :locale="{ emptyText: selectedCatId ? '该猫咪暂无健康事件' : '请先选择猫咪' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'eventType'">
            <a-tag :color="eventTypeMeta[record.eventType]?.color">
              {{ record.eventTypeDesc || eventTypeMeta[record.eventType]?.label }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="onEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a style="color: #ff4d4f" @click="onDelete(record)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formOpen"
      :title="isEditMode ? '编辑健康事件' : '新增健康事件'"
      :width="600"
      :confirm-loading="formLoading"
      :mask-closable="false"
      ok-text="保存"
      cancel-text="取消"
      :destroy-on-close="true"
      @ok="onFormSubmit"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="formRules"
        layout="vertical"
        class="edit-form"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="事件类型" name="eventType">
              <a-select
                v-model:value="editForm.eventType"
                placeholder="请选择"
                style="width: 100%"
              >
                <a-select-option
                  v-for="o in eventTypeOptions"
                  :key="o.value"
                  :value="o.value"
                >
                  <a-tag :color="eventTypeMeta[o.value]?.color" style="margin-right: 0">
                    {{ o.label }}
                  </a-tag>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标题" name="title">
              <a-input
                v-model:value="editForm.title"
                placeholder="如：三联疫苗第一针"
                maxlength="50"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发生日期" name="eventDate">
              <a-date-picker
                v-model:value="editForm.eventDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="下次提醒日期（可选）">
              <a-date-picker
                v-model:value="editForm.nextRemindDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="事件描述">
              <a-textarea
                v-model:value="editForm.description"
                placeholder="详细说明（可选）"
                :auto-size="{ minRows: 2, maxRows: 4 }"
                :maxlength="200"
                show-count
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-input
                v-model:value="editForm.remark"
                placeholder="如：已康复、无不良反应等"
                maxlength="100"
              />
            </a-form-item>
          </a-col>
        </a-row>
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

.cat-bar,
.table-card {
  border-radius: 8px;
}

.cat-select-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
  font-weight: 500;
}

.cat-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cat-name {
  font-weight: 500;
  color: #1f1f1f;
}

.cat-breed {
  color: #8c8c8c;
  font-size: 12px;
}

.cat-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.cat-info {
  flex: 1;
}

.cat-info-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}

.cat-info-breed {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.cat-stats {
  font-size: 13px;
  color: #595959;
}

.cat-stats strong {
  color: #ff8c42;
  font-size: 16px;
  font-weight: 600;
  margin: 0 2px;
}

.cat-empty {
  color: #8c8c8c;
  font-size: 13px;
  padding: 14px 0;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.edit-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}
</style>
