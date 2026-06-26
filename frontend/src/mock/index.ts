import service, { type ApiResponse } from '@/utils/request'
import { mockPosts } from './data'

/**
 * 是否启用 mock
 * 由 VITE_USE_MOCK 控制（默认仅开发环境启用）
 */
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

export interface MockContext {
  params: Record<string, string>
  query: Record<string, unknown>
  body: unknown
}

export type MockHandler = (ctx: MockContext) => unknown | Promise<unknown>

interface MockRule {
  method: HttpMethod
  pattern: string
  handler: MockHandler
}

/** 模式路径转正则（支持 :param 占位） */
function patternToRegex(pattern: string): { regex: RegExp; keys: string[] } {
  const keys: string[] = []
  const regexStr = pattern.replace(/:[a-zA-Z_]\w*/g, (match) => {
    keys.push(match.slice(1))
    return '([^/]+)'
  })
  return { regex: new RegExp(`^${regexStr}$`), keys }
}

/** 构造统一成功响应（供 mock handler 使用） */
export function ok<T>(data: T): ApiResponse<T> {
  return { code: 1, message: 'ok', data }
}

/** 模拟网络延迟（供 mock handler 使用） */
export const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

/* ==================== Mock 规则定义 ==================== */

const rules: MockRule[] = [
  /* 鉴权 - 走真实后端，不 mock */

  /* 社区动态 */
  {
    method: 'GET',
    pattern: '/post/feed',
    handler: ({ query }) => {
      const pageNum = Number(query.pageNum) || 1
      const pageSize = Number(query.pageSize) || 10
      const total = mockPosts.length
      const pages = Math.max(1, Math.ceil(total / pageSize))
      const start = (pageNum - 1) * pageSize
      const rows = mockPosts.slice(start, start + pageSize)
      return ok({ total, pages, pageNum, pageSize, rows })
    },
  },
]

/* ==================== 查找并执行 Mock ==================== */

function findRule(method: string, url: string): MockRule | undefined {
  const path = url.replace(/^https?:\/\/[^/]+/, '').replace(/^\/api/, '')

  return rules.find((rule) => {
    if (rule.method !== method.toUpperCase()) return false
    const { regex } = patternToRegex(rule.pattern)
    return regex.test(path)
  })
}

/** 安装 Mock 拦截器（在 request.ts 创建 service 后调用） */
export function setupMock(): void {
  if (!USE_MOCK) return

  service.interceptors.request.use(async (config) => {
    const rule = findRule(config.method || 'GET', config.url || '')
    if (!rule) return config

    const { regex, keys } = patternToRegex(rule.pattern)
    const path = (config.url || '').replace(/^https?:\/\/[^/]+/, '').replace(/^\/api/, '')
    const match = path.match(regex)

    const params: Record<string, string> = {}
    if (match) {
      keys.forEach((k, i) => (params[k] = decodeURIComponent(match[i + 1])))
    }

    await delay()

    const result = await rule.handler({
      params,
      query: (config.params as Record<string, unknown>) || {},
      body: config.data,
    })

    config.adapter = () =>
      Promise.resolve({
        data: result,
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      } as never)

    return config
  })

  console.info('[Mock] 已启用，规则数：', rules.length)
}

export default setupMock