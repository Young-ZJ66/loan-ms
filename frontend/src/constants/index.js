/**
 * 业务状态码常量与映射
 */

// 贷款申请状态：0-待审批, 1-已放款, 2-驳回, 3-已结清
export const LOAN_STATUS = {
  PENDING: 0,
  DISBURSED: 1,
  REJECTED: 2,
  SETTLED: 3
}

export const LOAN_STATUS_MAP = {
  0: { label: '待审批', type: 'warning' },
  1: { label: '已放款', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
  3: { label: '已结清', type: 'info' }
}

// 还款计划状态：0-待还, 1-已还清, 2-逾期中, 3-提前结清
export const BILL_STATUS = {
  PENDING: 0,
  PAID: 1,
  OVERDUE: 2,
  EARLY_SETTLED: 3
}

export const BILL_STATUS_MAP = {
  0: { label: '待偿还', type: 'warning' },
  1: { label: '已结清', type: 'success' },
  2: { label: '逾期中', type: 'danger' },
  3: { label: '已结清', type: 'success' }
}

// KYC 实名状态：0-待审核, 1-已通过, 2-已驳回
export const KYC_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2
}

export const KYC_STATUS_MAP = {
  0: { label: '待审批', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' }
}

// 额度/解冻申请状态：0-待审核, 1-通过, 2-驳回
export const AUDIT_STATUS_MAP = {
  0: { label: '待审批', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' }
}

// 贷款产品类型
export const PRODUCT_TYPE_MAP = {
  0: { label: '消费贷', color: '' },
  1: { label: '经营贷', color: 'warning' },
  2: { label: '房贷', color: 'success' },
  3: { label: '车贷', color: 'danger' }
}

// 还款类型：1-正常按期, 2-逾期清欠, 3-提前结清
export const PAY_TYPE_MAP = {
  1: { label: '正常按期', type: 'success' },
  2: { label: '逾期清欠', type: 'warning' },
  3: { label: '提前结清', type: 'primary' }
}

/**
 * 敏感信息脱敏
 */
export const maskIdCard = (val) => {
  if (!val || val.length < 8) return val || ''
  return val.replace(/^(\d{3}).*(\d{4})$/, '$1***********$2')
}

export const maskBankCard = (val) => {
  if (!val || val.length < 8) return val || ''
  return val.replace(/^(\d{4}).*(\d{4})$/, '$1********$2')
}

export const maskPhone = (val) => {
  if (!val || val.length !== 11) return val || ''
  return val.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

export const maskEmail = (val) => {
  if (!val || !val.includes('@')) return val || ''
  const [name, domain] = val.split('@')
  if (name.length <= 1) return val
  return name[0] + '***@' + domain
}
