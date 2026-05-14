import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 携带动态 JWT
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
    if (res.code && res.code !== 200) {
      ElMessage.error(res.msg || '操作失败，请重试')
      if (res.code === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('role')
          window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
        ElMessage.error("未登录或身份过期，请重新登录")
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        window.location.href = '/login'
    } else {
        ElMessage.error(error.message)
    }
    return Promise.reject(error)
  }
)

export default request
