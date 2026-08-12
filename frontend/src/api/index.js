import request from '../utils/request'

// 认证相关
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  changePassword: (data) => request.post('/auth/change-password', data),
  logout: () => request.post('/auth/logout'),
  resetPassword: (userId, newPassword) => request.post(`/auth/admin/reset-password/${userId}`, { newPassword })
}

// 贷款申请
export const loanApi = {
  apply: (data) => request.post('/loan/apply', data),
  my: () => request.get('/loan/my'),
  list: (params) => request.get('/loan/list', { params }),
  pending: () => request.get('/loan/pending'),
  approve: (appId) => request.post(`/loan/approve/${appId}`),
  reject: (appId) => request.post(`/loan/reject/${appId}`)
}

// 还款
export const repaymentApi = {
  myPlans: (params) => request.get('/repayment/my-plans', { params }),
  pay: (params) => request.post('/repayment/pay', null, { params }),
  payEarly: (loanId) => request.post(`/repayment/pay-early/${loanId}`)
}

// 授信额度
export const creditApi = {
  my: () => request.get('/credit/my'),
  all: () => request.get('/credit/all'),
  adjust: (params) => request.post('/credit/adjust', null, { params }),
  freeze: (targetUserId, params) => request.post(`/credit/freeze/${targetUserId}`, null, { params }),
  unfreeze: (targetUserId) => request.post(`/credit/unfreeze/${targetUserId}`)
}

// 额度申请
export const creditAppApi = {
  apply: (params) => request.post('/credit-app/apply', null, { params }),
  myPending: () => request.get('/credit-app/my_pending'),
  pending: () => request.get('/credit-app/pending'),
  approve: (id, params) => request.post(`/credit-app/approve/${id}`, null, { params }),
  reject: (id) => request.post(`/credit-app/reject/${id}`)
}

// KYC 实名
export const kycApi = {
  submit: (data) => request.post('/kyc/submit', data),
  my: () => request.get('/kyc/my'),
  pending: () => request.get('/kyc/pending'),
  all: () => request.get('/kyc/all'),
  audit: (id, params) => request.post(`/kyc/audit/${id}`, null, { params })
}

// 解冻申诉
export const unfreezeApi = {
  apply: (params) => request.post('/unfreeze/apply', null, { params }),
  myPending: () => request.get('/unfreeze/my_pending'),
  all: () => request.get('/unfreeze/all'),
  audit: (id, params) => request.post(`/unfreeze/audit/${id}`, null, { params })
}

// 催收
export const collectionApi = {
  overduePlans: () => request.get('/collection/overdue-plans'),
  action: (data) => request.post('/collection/action', data),
  records: (planId) => request.get(`/collection/records/${planId}`)
}

// 财务
export const financeApi = {
  plans: () => request.get('/finance/plans'),
  records: () => request.get('/finance/records'),
  triggerOverdue: () => request.post('/finance/trigger-overdue')
}

// 贷款产品
export const productApi = {
  all: () => request.get('/product/all'),
  active: () => request.get('/product/active'),
  add: (data) => request.post('/product/add', data),
  update: (data) => request.put('/product/update', data),
  toggle: (id) => request.post(`/product/toggle/${id}`)
}

// 站内消息
export const messageApi = {
  list: () => request.get('/message/list'),
  unreadCount: () => request.get('/message/unread-count'),
  markRead: (id) => request.put(`/message/read/${id}`),
  markAllRead: () => request.put('/message/read-all')
}

// 管理端统计
export const adminStatApi = {
  overview: () => request.get('/admin/stat/overview'),
  userCount: () => request.get('/admin/stat/users/count'),
  badges: () => request.get('/admin/stat/badges'),
  productDistribution: () => request.get('/admin/stat/product-distribution'),
  weeklyTrend: () => request.get('/admin/stat/weekly-trend')
}

// 文件上传
export const uploadApi = {
  upload: (formData, config) => request.post('/upload', formData, config)
}
