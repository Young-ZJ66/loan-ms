<template>
  <div class="page-container" v-loading="loading">
    <div class="header-banner">
      <div class="header-content">
        <h2>个人信用大盘</h2>
        <p>实时监控您的信用额度、款项占用与分期账单结构</p>
      </div>
      <div class="profile-badge" v-if="profile">
        <span>实名状态：</span>
        <el-tag :type="profile.status === 1 ? 'success' : profile.status === 0 ? 'warning' : 'danger'" size="small" round>
          {{ profile.status === 1 ? '已认证' : profile.status === 0 ? '待审批' : '已驳回' }}
        </el-tag>
      </div>
    </div>

    <!-- 三大核心指标卡 (扁平半透明玻璃卡) -->
    <el-row :gutter="24" class="stat-cards">
      <el-col :xs="24" :sm="8">
        <div class="stat-card primary">
          <div class="card-inner">
            <div class="info">
              <span class="title">总授信额度 (元)</span>
              <span class="value">{{ credit?.totalCredit ? formatMoney(credit.totalCredit) : '0.00' }}</span>
            </div>
            <div class="icon-wrapper">
              <el-icon :size="28"><CreditCard /></el-icon>
            </div>
          </div>
          <div class="card-bg-light"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card success" :class="{ 'frozen-card': credit?.status === 0 }">
          <div class="card-inner">
            <div class="info">
              <span class="title">当前可用额度 (元)</span>
              <span class="value">
                <span v-if="credit?.status === 0" class="frozen-text">已风控冻结</span>
                <span v-else>{{ credit?.availableCredit ? formatMoney(credit.availableCredit) : '0.00' }}</span>
              </span>
            </div>
            <div class="icon-wrapper">
              <el-icon :size="28">
                <Lock v-if="credit?.status === 0" />
                <CircleCheck v-else />
              </el-icon>
            </div>
          </div>
          <div class="card-bg-light"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card warning">
          <div class="card-inner">
            <div class="info">
              <span class="title">已用或冻结本金 (元)</span>
              <span class="value">{{ credit?.usedCredit ? formatMoney(credit.usedCredit) : '0.00' }}</span>
            </div>
            <div class="icon-wrapper">
              <el-icon :size="28"><PriceTag /></el-icon>
            </div>
          </div>
          <div class="card-bg-light"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 申请提额操作区 -->
    <div class="action-section" style="margin-top: 24px;" v-if="credit?.status !== 0">
      <div class="glass-subpanel">
        <div class="action-inner">
          <div class="action-desc">
            <h3>提升您的授信上限？</h3>
            <p>完善更多个人征信材料或保持良好的还款行为，有助于获得更高的信贷授权。</p>
          </div>
          <el-button type="primary" size="large" @click="handleApplyLimit" :loading="appLoading" :disabled="hasPending" round>
            <el-icon style="margin-right: 6px;"><Promotion /></el-icon>
            {{ hasPending ? '提额审核中...' : (credit ? '申请提额' : '首次开通额度') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 被冻结时的解冻申请区 -->
    <div class="action-section" style="margin-top: 24px;" v-if="credit?.status === 0">
      <div class="glass-subpanel danger-border">
        <div class="action-inner">
          <div class="action-desc">
            <h3 class="danger-text">信用账户受限中</h3>
            <p>检测到您的授信账户处于风控临时锁闭状态，额度已被冻结。若需解除，请提交申诉材料。</p>
          </div>
          <el-button type="danger" size="large" @click="handleApplyUnfreeze" :loading="unfreezeAppLoading" :disabled="hasUnfreezePending" round>
            <el-icon style="margin-right: 6px;"><Warning /></el-icon>
            {{ hasUnfreezePending ? '解冻申诉审核中...' : '提交解冻申诉' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据图表中心 -->
    <el-row :gutter="20" class="charts-row" style="margin-top: 24px;">
      <el-col :xs="24" :lg="10">
        <div class="chart-wrapper glass-subpanel-pure">
          <div class="chart-header">
            <h4>授信额度占比利用率</h4>
          </div>
          <div id="credit-gauge" class="chart-dom"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="chart-wrapper glass-subpanel-pure">
          <div class="chart-header">
            <h4>还款计划与分期供额分布</h4>
          </div>
          <div id="bill-chart" class="chart-dom"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 提额申请对话框 -->
    <el-dialog v-model="applyDialogVisible" :title="credit ? '申请提升额度' : '首次信用授信申请'" width="400px" center align-center custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="期望申请总额度 (元)" required>
          <el-input-number v-model="applyAmount" :min="1000" :max="500000" :step="5000" style="width: 100%" size="large" />
        </el-form-item>
        <p style="font-size: 12px; color: var(--text-secondary); margin-top: 10px; line-height: 1.5;">
          * 温馨提示：信用管理专员将结合您的当前实名流水与综合征信，对您期望的金额进行最终的审批下发。
        </p>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="applyDialogVisible = false" round>暂不申请</el-button>
          <el-button type="primary" @click="submitApplyLimit" :loading="submitting" round>立即提交申请</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 申诉解冻对话框 -->
    <el-dialog v-model="unfreezeDialogVisible" title="提交解冻申诉材料" width="400px" center align-center custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="详细申诉理由与资产情况说明" required>
          <el-input type="textarea" v-model="unfreezeReason" placeholder="请认真填写申诉理由，我们将有风控专员介入协同审核。" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="unfreezeDialogVisible = false" round>取消</el-button>
          <el-button type="primary" @click="submitApplyUnfreeze" :loading="unfreezeSubmitting" round>提交申诉</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { CreditCard, CircleCheck, PriceTag, Warning, Promotion, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const credit = ref(null)
const profile = ref(null)
const loading = ref(false)
const appLoading = ref(false)
const hasPending = ref(false)

const applyDialogVisible = ref(false)
const applyAmount = ref(20000)
const submitting = ref(false)

const hasUnfreezePending = ref(false)
const unfreezeAppLoading = ref(false)
const unfreezeDialogVisible = ref(false)
const unfreezeReason = ref('')
const unfreezeSubmitting = ref(false)

const billPlans = ref([])

const formatMoney = (val) => {
    if (val === undefined || val === null) return '0.00'
    return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/credit/my')
    credit.value = res.data
    
    const profileRes = await request.get('/kyc/my')
    profile.value = profileRes.data

    const billRes = await request.get('/repayment/my-plans')
    if (billRes.code === 200) {
      billPlans.value = billRes.data || []
    }

    nextTick(() => {
      initCharts()
    })
  } finally {
    loading.value = false
  }
}

let myCharts = []
const initCharts = () => {
  if (!window.echarts) return
  myCharts.forEach(c => c.dispose())
  myCharts = []

  const bodyStyle = getComputedStyle(document.documentElement)
  const textPrimary = bodyStyle.getPropertyValue('--text-primary').trim() || '#0f172a'
  const textSecondary = bodyStyle.getPropertyValue('--text-secondary').trim() || '#475569'
  const borderSubtle = bodyStyle.getPropertyValue('--border-subtle').trim() || 'rgba(0,0,0,0.1)'

  const creditDom = document.getElementById('credit-gauge')
  if (creditDom && credit.value) {
    const creditChart = window.echarts.init(creditDom)
    const total = credit.value.totalCredit || 0
    const used = credit.value.usedCredit || 0
    const available = credit.value.status === 0 ? 0 : (credit.value.availableCredit || 0)

    creditChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: '{b}: ¥{c} ({d}%)'
      },
      legend: {
        bottom: '0',
        textStyle: { color: textSecondary }
      },
      series: [
        {
          name: '额度占用',
          type: 'pie',
          radius: ['50%', '75%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: borderSubtle,
            borderWidth: 2
          },
          label: { show: false },
          data: [
            { value: available, name: '可用信用额度', itemStyle: { color: '#10b981' } },
            { value: used, name: '已占用或冻结额度', itemStyle: { color: '#f59e0b' } }
          ]
        }
      ]
    })
    myCharts.push(creditChart)
  }

  const billDom = document.getElementById('bill-chart')
  if (billDom) {
    const billChart = window.echarts.init(billDom)
    
    const activeBills = billPlans.value.filter(b => b.status === 0 || b.status === 2)
    const terms = activeBills.map(b => `第 ${b.termIndex} 期`)
    const amounts = activeBills.map(b => b.totalAmount)

    const finalTerms = terms.length > 0 ? terms : ['无待还账单']
    const finalAmounts = amounts.length > 0 ? amounts : [0]

    billChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: finalTerms,
        axisLabel: { color: textSecondary },
        axisLine: { lineStyle: { color: borderSubtle } }
      },
      yAxis: {
        type: 'value',
        name: '应还总计 (元)',
        axisLabel: { color: textSecondary },
        splitLine: { lineStyle: { color: borderSubtle, type: 'dashed' } },
        axisLine: { lineStyle: { color: borderSubtle } }
      },
      series: [
        {
          name: '账单应还金额',
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new window.echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#6366f1' },
              { offset: 1, color: '#4f46e5' }
            ])
          },
          data: finalAmounts
        }
      ]
    })
    myCharts.push(billChart)
  }

  window.addEventListener('resize', resizeHandler)
}

const resizeHandler = () => {
  myCharts.forEach(c => c.resize())
}

const checkPendingStatus = async () => {
  appLoading.value = true
  try {
    const res = await request.get('/credit-app/my_pending')
    hasPending.value = !!res.data
  } catch (e) {
  } finally {
    appLoading.value = false
  }
}

const handleApplyLimit = () => {
  if (!profile.value || profile.value.status !== 1) {
    ElMessageBox.confirm(
      '首次开通信用额度前，请先前往完成实名认证且等待管理员审批通过。',
      '前置条件未满足',
      {
        confirmButtonText: '去实名认证',
        cancelButtonText: '暂不',
        type: 'warning'
      }
    ).then(() => {
      router.push('/client/kyc')
    }).catch(() => {})
    return
  }

  if (credit.value && credit.value.totalCredit) {
    applyAmount.value = credit.value.totalCredit + 10000
  } else {
    applyAmount.value = 20000
  }
  applyDialogVisible.value = true
}

const submitApplyLimit = async () => {
  submitting.value = true
  try {
    await request.post('/credit-app/apply', null, {
      params: { amount: applyAmount.value }
    })
    ElMessage.success('授信额度申请已成功提交')
    applyDialogVisible.value = false
    checkPendingStatus()
  } finally {
    submitting.value = false
  }
}

const submitApplyUnfreeze = async () => {
    if (!unfreezeReason.value) {
        ElMessage.error('请填写申诉理由')
        return
    }
    unfreezeSubmitting.value = true
    try {
        await request.post('/unfreeze/apply', null, {
            params: { reason: unfreezeReason.value }
        })
        ElMessage.success('解冻申诉已成功提交，请等待处理。')
        unfreezeDialogVisible.value = false
        checkUnfreezePendingStatus()
    } finally {
        unfreezeSubmitting.value = false
    }
}

const checkUnfreezePendingStatus = async () => {
  unfreezeAppLoading.value = true
  try {
    const res = await request.get('/unfreeze/my_pending')
    hasUnfreezePending.value = !!res.data
  } catch (e) {
  } finally {
    unfreezeAppLoading.value = false
  }
}

const handleApplyUnfreeze = () => {
  unfreezeReason.value = ''
  unfreezeDialogVisible.value = true
}

onMounted(() => {
  loadData()
  checkPendingStatus()
  checkUnfreezePendingStatus()
})
</script>

<style scoped>
/* 动态半透明玻璃态背景 */
@media (prefers-color-scheme: dark) {
  .page-container {
    --glass-bg: rgba(30, 41, 59, 0.45);
    --glass-bg-sub: rgba(30, 41, 59, 0.25);
  }
}
@media (prefers-color-scheme: light) {
  .page-container {
    --glass-bg: rgba(255, 255, 255, 0.45);
    --glass-bg-sub: rgba(255, 255, 255, 0.25);
  }
}
.page-container {
  padding: 24px;
  --glass-bg: rgba(255, 255, 255, 0.45);
  --glass-bg-sub: rgba(255, 255, 255, 0.25);
}

.header-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  padding: 18px 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.header-banner h2 {
  font-size: 20px;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  font-weight: 600;
}

.header-banner p {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}

.stat-cards {
  margin-bottom: 24px;
}

/* 经典小彩条半透明玻璃卡片 */
.stat-card {
  height: 96px;
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 18px 24px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.stat-card .info {
  display: flex;
  flex-direction: column;
}

.stat-card .title {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.stat-card .value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.icon-wrapper {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-bg-light {
  position: absolute;
  right: -10px;
  bottom: -10px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(128, 128, 128, 0.03);
  z-index: 1;
}

.stat-card.primary { border-left: 4px solid var(--primary-color); }
.stat-card.primary .icon-wrapper { background: rgba(99, 102, 241, 0.1); color: var(--primary-color); }

.stat-card.success { border-left: 4px solid var(--success-color); }
.stat-card.success .icon-wrapper { background: rgba(16, 185, 129, 0.1); color: var(--success-color); }

.stat-card.warning { border-left: 4px solid var(--warning-color); }
.stat-card.warning .icon-wrapper { background: rgba(245, 158, 11, 0.1); color: var(--warning-color); }

.frozen-card { border-left: 4px solid var(--danger-color) !important; }
.frozen-card .icon-wrapper { background: rgba(239, 68, 68, 0.1) !important; color: var(--danger-color) !important; }

.frozen-text {
  color: var(--danger-color);
  font-weight: bold;
}

.danger-text {
  color: var(--danger-color);
}

/* 半透明玻璃面板 */
.glass-subpanel {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 16px 24px;
  box-shadow: var(--shadow-sm);
}

.glass-subpanel.danger-border {
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.03);
}

.glass-subpanel-pure {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.action-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.action-desc h3 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: var(--text-primary);
}

.action-desc p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 图表样式 */
.chart-wrapper {
  padding: 20px;
}

.chart-header {
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.chart-header h4 {
  margin: 0;
  font-size: 14px;
  color: var(--text-primary);
}

.chart-dom {
  height: 280px;
  width: 100%;
}

:deep(.dark-dialog) { background: #1e293b !important; border-radius: 16px; }
:deep(.el-dialog__title) { color: #fff !important; }
:deep(.el-form-item__label) { color: #cbd5e1 !important; }

/* 页面内红/橙状态或消息提示标签的半透明化覆盖 */
:deep(.el-tag--danger) {
  background-color: rgba(245, 108, 108, 0.65) !important;
  color: #ffffff !important;
  border: 1px solid rgba(245, 108, 108, 0.8) !important;
  font-weight: bold;
}
:deep(.el-tag--warning) {
  background-color: rgba(245, 158, 11, 0.65) !important;
  color: #ffffff !important;
  border: 1px solid rgba(245, 158, 11, 0.8) !important;
  font-weight: bold;
}
</style>
