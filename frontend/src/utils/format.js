/**
 * 时间与金额格式化工具
 */

export const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const formatDate = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export const formatMoney = (val) => {
  if (val === undefined || val === null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 将存储的文件路径转换为带 token 的可访问 URL
 * 兼容历史 /uploads/xxx 与新 /api/file/xxx 两种格式
 */
export const fileUrl = (path) => {
  if (!path) return ''
  let url = path
  if (url.startsWith('/uploads/')) {
    url = url.replace('/uploads/', '/api/file/')
  }
  const token = localStorage.getItem('token') || ''
  if (token) {
    url += (url.includes('?') ? '&' : '?') + 'token=' + encodeURIComponent(token)
  }
  return url
}
