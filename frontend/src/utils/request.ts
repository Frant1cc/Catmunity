import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { showFailToast } from 'vant'

/**
 * 跳转到登录页（按当前路径区分用户端/管理端）
 * 动态 import 避免 request <-> router 循环依赖
 */
async function redirectToLogin() {
  const { default: router } = await import('@/router')
  localStorage.removeItem('token')
  const isAdmin = router.currentRoute.value.path.startsWith('/admin')
  router.push(isAdmin ? '/admin/login' : '/login')
}

/** 后端统一返回结构 */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 业务成功的 code 值 */
const SUCCESS_CODE = 1

/** 创建 axios 实例 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
  },
})

/**
 * 请求拦截器
 * - 自动注入 token（后端约定放在 `token` 请求头）
 */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.token = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

/**
 * 响应拦截器
 * - 业务码非成功：toast + reject
 * - HTTP 错误：按状态码 toast + reject
 */
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse

    // 文件下载等场景，responseType === 'blob' 时直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }

    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code !== SUCCESS_CODE) {
        // 状态同步类错误（重复点赞/收藏 等）不弹 toast，由调用方静默处理
        const msg = res.msg || ''
        const isStateSyncError =
          msg.includes('已收藏') ||
          msg.includes('未收藏') ||
          msg.includes('已点赞') ||
          msg.includes('未点赞')

        if (!isStateSyncError) {
          showFailToast(msg || '请求失败')
        }

        // 401: 未登录 / token 失效
        if (res.code === 401) {
          redirectToLogin()
        }

        return Promise.reject(new Error(msg || 'Business Error'))
      }
      return res.data
    }

    // 非标准结构，直接返回
    return res
  },
  (error) => {
    let message = '网络请求失败'

    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          redirectToLogin()
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 408:
          message = '请求超时'
          break
        case 500:
          message = '服务器内部错误'
          break
        case 501:
          message = '服务未实现'
          break
        case 502:
          message = '网关错误'
          break
        case 503:
          message = '服务不可用'
          break
        case 504:
          message = '网关超时'
          break
        default:
          message = `请求失败（${error.response.status}）`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后再试'
    } else if (error.message?.includes('Network Error')) {
      message = '网络连接异常'
    }

    showFailToast(message)
    return Promise.reject(error)
  },
)

/**
 * 通用请求方法（泛型）
 * @example
 *   const user = await request<User>({ url: '/user/1' })
 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

/**
 * 快捷方法集合
 */
export const http = {
  get<T = unknown>(
    url: string,
    params?: object,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ ...config, url, method: 'GET', params })
  },

  post<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ ...config, url, method: 'POST', data })
  },

  put<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ ...config, url, method: 'PUT', data })
  },

  delete<T = unknown>(
    url: string,
    params?: object,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return request<T>({ ...config, url, method: 'DELETE', params })
  },
}

export default service