import { http } from '@/utils/request'
import type {
  BanUserDTO,
  CatBreedDistributionVO,
  CatHealthEventDTO,
  CatCheckinCalendarVO,
  CatCheckinVO,
  CatHealthEventVO,
  CatProfileDTO,
  CatProfileListParams,
  CatProfileQueryDTO,
  CatProfileVO,
  CommentDTO,
  CommentVO,
  DashboardOverviewVO,
  DatasetFilesPageParams,
  IPageDatasetFiles,
  DatasetVO,
  LoginParams,
  LoginResult,
  PageResult,
  PageResultCatProfileVO,
  PageResultPostVO,
  PageResultUserAdminVO,
  PostDTO,
  PostFeedParams,
  PostVO,
  Question,
  QuestionListParams,
  RegisterParams,
  SessionInfo,
  SessionList,
  UploadResult,
  UserAdminVO,
  UserGrowthTrendVO,
  UserInfo,
  UserQueryDTO,
  UserStats,
  UserUpdateDTO,
} from './types'

/* ==================== 鉴权 ==================== */

export const authApi = {
  /** 登录 */
  login: (data: LoginParams) => http.post<LoginResult>('/auth/login', data),

  /** 注册 */
  register: (data: RegisterParams) => http.post<LoginResult>('/auth/register', data),

  /** 发送注册邮箱验证码 */
  sendRegisterCode: (email: string) =>
    http.post<string>('/auth/register/sendCode', null, { params: { email } }),

  /** 登出 */
  logout: () => http.post<void>('/auth/logout'),
}

/* ==================== 用户 ==================== */

export const userApi = {
  /** 获取当前用户信息 */
  getInfo: () => http.get<UserInfo>('/user/current'),

  /** 更新用户信息 */
  updateInfo: (data: UserUpdateDTO) => http.put<string>('/user/update', data),

  /** 获取用户统计（猫咪数、粉丝、获赞） */
  getStats: () => http.get<UserStats>('/user/stats'),

  /** 获取打卡日历视图 */
  getCheckinCalendar: (params?: { catProfileId?: number; year?: number; month?: number }) =>
    http.get<CatCheckinCalendarVO>('/user/checkin/calendar', params),

  /** 上传猫咪打卡照片 */
  uploadCheckinPhoto: (file: File) => {
    const form = new FormData()
    form.append('photo', file)
    return http.post<string>('/user/checkin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /** 猫咪打卡 */
  checkin: (params: { catProfileId: number; remark?: string; photoURL?: string; syncToPost?: boolean }) =>
    http.post<CatCheckinVO>('/user/checkin/checkin', null, { params }),
}

/* ==================== 用户管理（管理端） ==================== */

export const adminUserApi = {
  /**
   * 管理端：分页查询用户列表
   * @param query 查询条件（UserQueryDTO）
   * @param page 分页参数
   */
  getList: (
    query?: UserQueryDTO,
    page?: { pageNum?: number; pageSize?: number },
  ) =>
    http.get<PageResultUserAdminVO>('/admin/user/list', {
      ...(query || {}),
      ...(page || {}),
    }),

  /** 管理端：查看用户详情 */
  getDetail: (userId: number) =>
    http.get<UserAdminVO>(`/admin/user/${userId}`),

  /** 管理端：封禁用户 */
  ban: (data: BanUserDTO) => http.post<void>('/admin/user/ban', data),

  /** 管理端：解封用户 */
  unban: (userId: number) => http.post<void>(`/admin/user/unban/${userId}`),

  /**
   * 管理端：分配用户角色
   * @param userId 用户 ID
   * @param userType 角色类型：0-超管，1-管理员，2-普通用户
   */
  assignRole: (userId: number, userType: 0 | 1 | 2) =>
    http.post<void>('/admin/user/role', null, {
      params: { userId, userType },
    }),
}

/* ==================== AI 数据集 / 知识库 ==================== */

export const aiApi = {
  /**
   * 获取用户的数据集列表（知识库）
   * 返回结构后端未明确约定，data 字段为 object，
   * 可能是数组，也可能是 { rows: [...], total } 等包裹结构
   */
  getDatasetList: () =>
    http.get<DatasetVO[] | { rows?: DatasetVO[]; total?: number; list?: DatasetVO[] }>(
      '/ai/dataset/list',
    ),

  /**
   * 分页获取知识库文件列表
   * @param params current 页码（从 1 开始）、size 每页条数
   */
  getDatasetFilesPage: (params: DatasetFilesPageParams) =>
    http.get<IPageDatasetFiles>('/ai/dataset/page', params),

  /**
   * 下载知识库文件
   * @param fileId 文件 ID
   * @returns 二进制 Blob
   */
  downloadDatasetFile: (fileId: string | number) =>
    http.get<Blob>(`/ai/dataset/file/${fileId}`, undefined, {
      responseType: 'blob',
      // 文件下载耗时较长，单次放宽到 2 分钟
      timeout: 120_000,
    }),

  /**
   * 上传文件到知识库
   * @param file 待上传的文件
   */
  uploadDatasetFile: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<unknown>('/ai/dataset/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      // 大文件上传耗时较长，单次放宽到 2 分钟
      timeout: 120_000,
    })
  },

  /**
   * 获取 AI 问答会话列表
   */
  getSessions: () => http.get<SessionList[]>('/ai/chat/sessions'),

  /**
   * 获取 AI 会话详情（包含所有消息）
   * @param sessionId 会话 ID
   */
  getSessionDetail: (sessionId: string) =>
    http.get<SessionInfo[]>(`/ai/chat/sessions/${sessionId}`),

  /**
   * 停止正在进行的对话（同时调后端停止 + 前端中断 SSE 流）
   * @param sessionId
   * @param messageId
   */
  stopMessage: (sessionId: string, messageId: string) =>
    http.post<string>(`/ai/chat/messages/${sessionId}/${messageId}/stop`),

  /**
   * 清除 / 删除指定会话的历史记录
   * @param sessionId
   */
  clearSession: (sessionId: string) =>
    http.delete<string>(`/ai/chat/sessions/${sessionId}`),

  /**
   * RAG 增强对话（SSE 流式响应）
   * @param params sessionId / messageId / content / files
   * @param onChunk 每收到一段增量文本就回调
   * @param onDone  整段响应结束时回调
   * @param onError 出错时回调
   * @returns 一个 abort 函数，调用可中断流
   */
  sendMessage: async (
    params: {
      sessionId: string
      messageId: string
      content?: string
      files?: File[]
    },
    callbacks: {
      onChunk?: (text: string) => void
      onDone?: () => void
      onError?: (err: Error) => void
    },
  ): Promise<() => void> => {
    const form = new FormData()
    if (params.files?.length) {
      params.files.forEach((f) => form.append('files', f))
    }
    const query = new URLSearchParams()
    query.set('sessionId', params.sessionId)
    query.set('messageId', params.messageId)
    if (params.content) query.set('content', params.content)

    const url =
      `${import.meta.env.VITE_API_BASE_URL || '/api'}/ai/chat/messages?${query.toString()}`

    // SSE 流式不能走 axios，用 fetch
    const token = localStorage.getItem('token')
    const controller = new AbortController()
    fetch(url, {
      method: 'POST',
      body: form,
      headers: token ? { token } : {},
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok || !res.body) {
          throw new Error(`HTTP ${res.status}`)
        }
        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')

        // 后端实际返回的是 text/html;charset=utf-8 的分块传输流，
        // 每段就是原始 markdown 文本，不是 SSE（没有 data: 前缀）。
        // 仅当响应真的是 text/event-stream 时才走 SSE 解析。
        const contentType = res.headers.get('content-type') || ''
        const isSSE = contentType.includes('text/event-stream')

        if (!isSSE) {
          // 原始分块流：每读到一个 chunk 就直接回调
          // eslint-disable-next-line no-constant-condition
          while (true) {
            const { value, done } = await reader.read()
            if (done) break
            const text = decoder.decode(value, { stream: true })
            if (text) callbacks.onChunk?.(text)
          }
          callbacks.onDone?.()
          return
        }

        // 真正的 SSE 流（保留原解析逻辑）
        let buffer = ''
        let finished = false

        const flushEvent = (rawEvent: string) => {
          if (!rawEvent.trim()) return
          // 统一换行符
          const lines = rawEvent.replace(/\r\n/g, '\n').split('\n')
          const dataLines = lines
            .filter((l) => l.startsWith('data:'))
            .map((l) => l.slice(5).trim())
          if (!dataLines.length) return
          const payload = dataLines.join('\n')
          if (payload === '[DONE]') {
            finished = true
            return
          }
          callbacks.onChunk?.(payload)
        }

        // 持续读取流
        // eslint-disable-next-line no-constant-condition
        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })

          // SSE 事件以 \n\n（也兼容 \r\n\r\n）分隔
          let idx
          while ((idx = buffer.search(/\n\n|\r\n\r\n/)) !== -1) {
            const sepLen = buffer.startsWith('\r\n\r\n', idx) ? 4 : 2
            const rawEvent = buffer.slice(0, idx)
            buffer = buffer.slice(idx + sepLen)
            flushEvent(rawEvent)
            if (finished) return
          }
        }

        // 流结束时，flush buffer 里残留的最后一条事件（可能没有 \n\n 结尾）
        flushEvent(buffer)
        callbacks.onDone?.()
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          callbacks.onError?.(err)
        }
      })

    return () => controller.abort()
  },
}

/* ==================== 社区动态 ==================== */

export const communityApi = {
  /** 首页信息流 */
  getFeed: (params?: PostFeedParams) =>
    http.get<PageResultPostVO>('/post/feed', params),

  /** 根据话题标签获取帖子 */
  getByTag: (tag: string, params?: PostFeedParams) =>
    http.get<PostVO[]>(`/post/tag/${encodeURIComponent(tag)}`, params),

  /** 发布帖子 */
  createPost: (data: PostDTO) => http.post<PostVO>('/post', data),

  /**
   * 上传帖子图片
   * @param files File 数组（最多 9 张）
   * @returns 图片 URL 列表
   */
  uploadImages: (files: File[]) => {
    const form = new FormData()
    files.forEach((f) => form.append('files', f))
    // 必须显式声明 multipart/form-data，axios 会自动追加 boundary
    return http.post<string[]>('/post/images', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /**
   * 获取我的收藏列表
   * @param params 分页参数
   * @returns 分页收藏列表
   */
  getFavorites: (params?: { pageNum?: number; pageSize?: number }) =>
    http.get<PageResultPostVO>('/post/interaction/favorites', params),

  /**
   * 收藏帖子
   * @param postId 帖子 ID
   * @returns 更新后的 PostVO
   */
  favorite: (postId: number) =>
    http.post<PostVO>(`/post/interaction/${postId}/favorite`),

  /**
   * 取消收藏帖子
   * @param postId 帖子 ID
   * @returns 更新后的 PostVO
   */
  unfavorite: (postId: number) =>
    http.delete<PostVO>(`/post/interaction/${postId}/favorite`),

  /**
   * 点赞帖子
   * @param postId 帖子 ID
   * @returns 更新后的 PostVO
   */
  like: (postId: number) =>
    http.post<PostVO>(`/post/interaction/${postId}/like`),

  /**
   * 取消点赞帖子
   * @param postId 帖子 ID
   * @returns 更新后的 PostVO
   */
  unlike: (postId: number) =>
    http.delete<PostVO>(`/post/interaction/${postId}/like`),
}

/* ==================== 评论 ==================== */

export const commentApi = {
  /** 获取帖子的评论列表（含嵌套子评论） */
  getByPostId: (postId: number) =>
    http.get<CommentVO[]>(`/comment/post/${postId}`),

  /** 发表评论 / 回复评论 */
  create: (data: CommentDTO) => http.post<CommentVO>('/comment', data),

  /** 删除评论（仅本人可删） */
  delete: (id: number) => http.delete<void>(`/comment/${id}`),
}

/* ==================== 问答 ==================== */

export const qaApi = {
  /** 问题列表 */
  getQuestions: (params?: QuestionListParams) =>
    http.get<PageResult<Question>>('/qa/questions', params),

  /** 问题详情 */
  getQuestion: (id: number) => http.get<Question>(`/qa/questions/${id}`),

  /** 提问 */
  createQuestion: (data: { title: string }) =>
    http.post<Question>('/qa/questions', data),

  /** 回答 */
  answerQuestion: (id: number, data: { answer: string }) =>
    http.post<Question>(`/qa/questions/${id}/answer`, data),
}

/* ==================== 猫咪档案 ==================== */

export const catApi = {
  /** 用户端：分页查询猫咪档案列表（基础分页） */
  getList: (params?: CatProfileListParams) =>
    http.get<PageResultCatProfileVO>('/catProfile/list', params),

  /**
   * 管理端：分页查询猫咪档案列表（带筛选条件）
   * @param query 查询条件（CatProfileQueryDTO）
   * @param page 分页参数
   */
  getAdminList: (
    query?: CatProfileQueryDTO,
    page?: { pageNum?: number; pageSize?: number },
  ) =>
    http.get<PageResultCatProfileVO>('/admin/catProfile/list', {
      ...(query || {}),
      ...(page || {}),
    }),

  /** 管理端：查看猫咪档案详情 */
  getAdminDetail: (id: number) =>
    http.get<CatProfileVO>(`/admin/catProfile/${id}`),

  /** 管理端：删除猫咪档案 */
  deleteAdmin: (id: number) => http.delete<void>(`/admin/catProfile/${id}`),

  /** 管理端：更新猫咪档案 */
  updateAdmin: (data: CatProfileDTO) =>
    http.put<CatProfileVO>('/admin/catProfile', data),

  /** 管理端：创建猫咪档案 */
  createAdmin: (data: CatProfileDTO) =>
    http.post<CatProfileVO>('/admin/catProfile', data),

  /**
   * 管理端：上传猫咪图片
   * @param files File 数组
   * @returns 图片 URL 列表
   */
  uploadPhotos: (files: File[]) => {
    const form = new FormData()
    files.forEach((f) => form.append('files', f))
    return http.post<string[]>('/admin/catProfile/photos', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

/* ==================== 猫咪健康事件 ==================== */

export const healthApi = {
  /**
   * 获取某只猫咪的所有健康事件（按日期倒序）
   * @param catProfileId 猫咪档案 ID
   */
  getByCatId: (catProfileId: number) =>
    http.get<CatHealthEventVO[]>(`/catHealthEvent/cat/${catProfileId}`),

  /**
   * 获取某只猫咪的健康时间线（按时间正序）
   * @param catProfileId 猫咪档案 ID
   */
  getTimeline: (catProfileId: number) =>
    http.get<CatHealthEventVO[]>(`/catHealthEvent/cat/${catProfileId}/timeline`),

  /** 创建健康事件 */
  create: (data: CatHealthEventDTO) =>
    http.post<CatHealthEventVO>('/catHealthEvent', data),

  /** 更新健康事件 */
  update: (data: CatHealthEventDTO) =>
    http.put<CatHealthEventVO>('/catHealthEvent', data),

  /** 删除健康事件 */
  delete: (id: number) => http.delete<void>(`/catHealthEvent/${id}`),

  /** 获取未来 7 天需要提醒的健康事件 */
  getReminders: () =>
    http.get<CatHealthEventVO[]>('/catHealthEvent/reminders'),
}

/* ==================== 数据看板 ==================== */

export const dashboardApi = {
  /** 概览数据 */
  getOverview: () => http.get<DashboardOverviewVO>('/admin/dashboard/overview'),

  /** 猫咪品种分布 */
  getCatBreedDistribution: () =>
    http.get<CatBreedDistributionVO>('/admin/dashboard/cat-breed-distribution'),

  /**
   * 用户增长趋势
   * @param dimension day | week | month
   * @param days 统计天数
   */
  getUserGrowth: (params?: { dimension?: 'day' | 'week' | 'month'; days?: number }) =>
    http.get<UserGrowthTrendVO>('/admin/dashboard/user-growth', params),
}

/* ==================== 上传 ==================== */

export const uploadApi = {
  /** 上传图片 */
  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<UploadResult>('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

/** 默认导出聚合对象，方便按需引入 */
export default {
  auth: authApi,
  user: userApi,
  community: communityApi,
  comment: commentApi,
  qa: qaApi,
  upload: uploadApi,
  cat: catApi,
  dashboard: dashboardApi,
  health: healthApi,
  adminUser: adminUserApi,
  ai: aiApi,
}