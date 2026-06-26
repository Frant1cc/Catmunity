/** ==================== 用户管理（管理端） ==================== */

/** 管理端用户查询条件 */
export interface UserQueryDTO {
  keyword?: string
  type?: 0 | 1 | 2
  status?: 0 | 1
  createdAtStart?: string
  createdAtEnd?: string
}

/** 管理端用户 VO */
export interface UserAdminVO {
  id: number
  avatar: string
  identifier: string
  phone: string
  email: string
  type: 0 | 1 | 2
  typeStr: string
  status: 0 | 1
  statusStr: string
  banReason: string
  bannedAt: string
  createdAt: string
  lastActive: string
  postCount: number
  checkinCount: number
  followCount: number
  followerCount: number
}

/** PageResult<UserAdminVO> 后端分页结构 */
export interface PageResultUserAdminVO {
  total: number
  pages: number
  pageNum: number
  pageSize: number
  rows: UserAdminVO[]
}

/** 封禁用户 DTO */
export interface BanUserDTO {
  userId?: number
  reason?: string
}

/* ==================== AI 数据集（知识库） ==================== */

/**
 * 知识库/AI 数据集项（字段尽量宽松，后端结构待定）
 * 实际返回字段取决于后端实现
 */
export interface DatasetVO {
  id?: string | number
  name?: string
  title?: string
  description?: string
  documentCount?: number
  docCount?: number
  chunkCount?: number
  status?: string | number
  createdAt?: string
  updatedAt?: string
  avatar?: string
  icon?: string
  [key: string]: unknown
}

/** 知识库文件记录 */
export interface DatasetFiles {
  id: number
  /** 文件名 */
  name: string
  /** 文件路径 */
  filePath: string
  /** 文件类型 */
  fileType: string
  /** 文件大小（字节） */
  fileSize: number
  /** 上传用户 ID */
  userId: number
  /** 是否禁用：1-启用，-1-禁用 */
  disabled: 1 | -1
  /** 创建时间 */
  createdAt: string
  /** 禁用时间 */
  disabledAt: string
  /** 命中次数 */
  hitCount: number
}

/** 知识库文件分页结果（后端 IPage<DatasetFiles>） */
export interface IPageDatasetFiles {
  size: number
  records: DatasetFiles[]
  pages: number
  total: number
  current: number
}

/** 知识库文件分页查询参数 */
export interface DatasetFilesPageParams {
  current: number
  size: number
}

/** ==================== 用户相关 ==================== */

export interface UserInfo {
  id: number
  avatar: string
  identifier: string
  phone: string
  email: string
  type: number
  typeStr: string
  status: number
  statusStr: string
  banReason: string
  bannedAt: string
  createdAt: string
  lastActive: string
  postCount: number
  checkinCount: number
  followCount: number
  followerCount: number
}

export interface UserUpdateDTO {
  newPassword?: string
  newIdentifier?: string
  code?: string
}

export interface UserStats {
  catCount: number
  followerCount: number
  likeCount: number
}

export interface LoginParams {
  identifier: string
  password: string
}

export interface RegisterParams {
  identifier: string
  password: string
  email: string
  code: string
}

/** 后端 UserLoginVO */
export interface LoginResult {
  token: string
  userId: number
  type: number
  identifier: string
}

/** ==================== 社区动态 ==================== */

/** 后端 PostVO */
export interface PostVO {
  id: number
  userId: number
  username: string
  avatar: string
  content: string
  images: string[]
  tags: string[]
  likeCount: number
  commentCount: number
  favoriteCount: number
  isLiked: boolean
  isFavorited: boolean
  createdAt: string
}

/** PageHelper 分页结构 */
export interface PageResultPostVO {
  total: number
  pages: number
  pageNum: number
  pageSize: number
  rows: PostVO[]
}

export interface PostFeedParams {
  pageNum?: number
  pageSize?: number
  sortBy?: string
}

/** 发布帖子请求体 */
export interface PostDTO {
  id?: number
  content: string
  images?: string[]
  tags?: string[]
}

/** 评论 VO（支持嵌套子评论） */
export interface CommentVO {
  id: number
  postId: number
  userId: number
  username: string
  avatar: string
  parentId: number
  content: string
  children: CommentVO[]
  createdAt: string
}

/** 发表评论请求体 */
export interface CommentDTO {
  id?: number
  postId?: number
  parentId?: number
  content: string
}

/** ==================== 问答 ==================== */

export interface Question {
  id: number
  title: string
  answer: string
  views: number
  answerCount: number
}

export interface QuestionListParams {
  page?: number
  pageSize?: number
}

/** ==================== 上传 ==================== */

export interface UploadResult {
  url: string
  filename: string
  size: number
}

/** ==================== 通用分页 ==================== */

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** ==================== 猫咪档案 ==================== */

/** 猫咪性别：0-母猫，1-公猫 */
export type CatGender = 0 | 1

/** 猫咪状态：0-正常，1-已领养，2-其他 */
export type CatStatus = 0 | 1 | 2

/** 健康事件类型：1-疫苗接种，2-驱虫，3-绝育，4-体检，5-疾病治疗，6-美容护理，99-其他 */
export type CatHealthEventType = 1 | 2 | 3 | 4 | 5 | 6 | 99

/** CatHealthEventVO 健康事件 */
export interface CatHealthEventVO {
  id: number
  catProfileId: number
  eventType: CatHealthEventType
  eventTypeDesc: string
  title: string
  description: string
  eventDate: string
  nextRemindDate?: string
  remark: string
  createdAt: string
  updatedAt: string
}

/** CatProfileVO 猫咪档案 */
export interface CatProfileVO {
  id: number
  name: string
  breed: string
  gender: CatGender
  birthday: string
  weight: number
  photoUrls: string[]
  status: CatStatus
  createdAt: string
  updatedAt: string
  healthEvents: CatHealthEventVO[]
}

/** 猫咪档案分页查询参数 */
export interface CatProfileListParams {
  pageNum?: number
  pageSize?: number
}

/** 管理端猫咪档案查询条件 */
export interface CatProfileQueryDTO {
  name?: string
  breed?: string
  gender?: 0 | 1
  createdAtStart?: string
  createdAtEnd?: string
  status?: 0 | 1 | 2
}

/** 猫咪档案新增/更新请求体 */
export interface CatProfileDTO {
  id?: number
  name?: string
  breed?: string
  gender?: 0 | 1
  birthday?: string
  weight?: number
  photoUrls?: string[]
  status?: 0 | 1 | 2
}

/** 健康事件新增/更新请求体 */
export interface CatHealthEventDTO {
  id?: number
  catProfileId?: number
  eventType?: CatHealthEventType
  title?: string
  description?: string
  eventDate?: string
  nextRemindDate?: string
  remark?: string
}

/** 打卡记录 */
export interface CatCheckinVO {
  id: number
  userId: number
  catProfileId: number
  catName: string
  checkinDate: string
  photoUrl: string
  remark: string
  syncedToPost: boolean
  createdAt: string
}

/** 打卡日历视图 */
export interface CatCheckinCalendarVO {
  year: number
  month: number
  checkinDates: string[]
  checkinCount: number
  consecutiveDays: number
  fullMonth: boolean
}

/** PageResult<CatProfileVO> 后端分页结构 */
export interface PageResultCatProfileVO {
  total: number
  pages: number
  pageNum: number
  pageSize: number
  rows: CatProfileVO[]
}

/* ==================== 数据看板 ==================== */

/** 仪表盘核心指标 */
export interface DashboardOverviewVO {
  totalUsers: number
  todayNewUsers: number
  activeUsers: number
  totalPosts: number
  todayNewPosts: number
  totalCatProfiles: number
  totalCheckins: number
  todayNewCheckins: number
  pendingModerationCount: number
}

/** 趋势数据点 */
export interface TrendDataVO {
  date: string
  count: number
}

/** 用户增长趋势 */
export interface UserGrowthTrendVO {
  dimension: 'day' | 'week' | 'month'
  data: TrendDataVO[]
  totalGrowth: number
}

/** 品种分布项 */
export interface BreedDataVO {
  breed: string
  count: number
  percentage: number
}

/** 猫咪品种分布 */
export interface CatBreedDistributionVO {
  breeds: BreedDataVO[]
  totalCats: number
  breedCount: number
  unknownBreedCount: number
}

/* ==================== AI 会话 ==================== */

/** AI 会话列表项 */
export interface SessionList {
  sessionId: string
  title: string
  createdAt: string
}

/** AI 会话消息项（支持多轮） */
export interface SessionInfo {
  sessionId: string
  messageId: string
  content: string
  result: Record<string, unknown>
  messageType: string
  createdAt: string
}