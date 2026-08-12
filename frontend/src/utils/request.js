import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 401 防重入锁，避免并发 401 触发多次跳转
let isRedirecting = false

// 请求拦截器
request.interceptors.request.use(
  config => {
    // JWT 请求头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.msg || '操作失败，请重试')
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      ElMessage.error('未登录或身份过期，请重新登录')
      handleUnauthorized()
    } else if (error.response) {
      const status = error.response.status
      const msgMap = { 400: '请求参数有误', 403: '无权执行此操作', 404: '资源不存在', 429: '请求过于频繁，请稍后再试', 500: '服务繁忙，请稍后重试', 502: '网关异常', 503: '服务暂不可用', 504: '请求超时' }
      ElMessage.error(msgMap[status] || `请求失败（${status}）`)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (isRedirecting) return
  isRedirecting = true
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('username')
  router.replace('/login').finally(() => {
    isRedirecting = false
  })
}

export default request
