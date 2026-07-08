<template>
  <div class="page-container">
    <!-- 横幅 -->
    <div class="header-banner">
      <div class="header-content">
        <h2>运营数据中心</h2>
        <p>全平台核心授信、款项放款及逾期数据实时监控</p>
      </div>
      <div class="time-badge">
        <span>数据刷新时间：{{ refreshTime }}</span>
      </div>
    </div>

    <!-- 顶层核心指标卡片组 (精简为 4 个，采用半透明侧彩条玻璃风格) -->
    <el-row :gutter="20" class="stat-cards" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="6" v-for="(item, idx) in cardConfigs" :key="idx">
        <div class="stat-card" :class="item.type">
          <div class="card-inner">
            <div class="info">
              <span class="title">{{ item.title }}</span>
              <span class="value">{{ item.prefix || '' }} {{ item.value }}</span>
            </div>
            <div class="icon-wrapper">
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
            </div>
          </div>
          <div class="card-bg-light"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办追踪工作台 -->
    <div class="todo-section glass-panel" v-loading="badgeLoading">
      <div class="section-title">
        <span class="indicator"></span>
        <h3>审核与风控待办追踪</h3>
      </div>
      <el-row :gutter="20" class="todo-grid">
        <el-col :xs="12" :sm="8" :md="4" v-for="(badge, bIdx) in todoBadges" :key="bIdx">
          <div class="todo-item" @click="handleTodoClick(badge.route)">
            <div class="todo-item-header">
              <el-icon :size="20" class="todo-icon"><component :is="badge.icon" /></el-icon>
              <div class="todo-badge" :class="{ 'warning-badge': badge.value > 0 }">
                {{ badge.value }}
              </div>
            </div>
            <div class="todo-label">{{ badge.name }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 图表展示区 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <div class="chart-wrapper glass-panel">
          <div class="chart-header">
            <h4>平台资金健康状况 (占比)</h4>
          </div>
          <div id="pie-chart" class="chart-dom"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="chart-wrapper glass-panel">
          <div class="chart-header">
            <h4>贷款类型与产品分布</h4>
          </div>
          <div id="rose-chart" class="chart-dom"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row" style="margin-top: 20px;">
      <el-col :span="24">
        <div class="chart-wrapper glass-panel">
          <div class="chart-header">
            <h4>平台近7日授信与款项流动趋势</h4>
          </div>
          <div id="line-chart" class="chart-dom large-chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import request from '../../utils/request'
import { useRouter } from 'vue-router'
import { User, Calendar, Money, Warning, Stamp, Wallet, RefreshRight, Bell, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const badgeLoading = ref(false)
const refreshTime = ref('')

const stats = ref({
    totalUsers: 0, kycPending: 0, todayApplications: 0,
    totalDisbursed: 0, totalOverdue: 0, loanPending: 0
})

const badges = ref({
    kyc: 0, loan: 0, credit: 0, unfreeze: 0, overdue: 0
})

const getFormatTime = () => {
  const d = new Date()
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
}

const cardConfigs = computed(() => [
  { title: '注册用户总量', value: stats.value.totalUsers, icon: User, type: 'info' },
  { title: '今日新增申请', value: stats.value.todayApplications, icon: Calendar, type: 'orange' },
  { title: '平台总放款额', value: formatMoney(stats.value.totalDisbursed), icon: Money, type: 'green', prefix: '¥' },
  { title: '逾期坏账金额', value: formatMoney(stats.value.totalOverdue), icon: Warning, type: 'danger', prefix: '¥' }
])

const todoBadges = computed(() => [
  { name: '待审实名', value: badges.value.kyc, route: '/admin/kyc', icon: Stamp },
  { name: '待审放款', value: badges.value.loan, route: '/admin/loan', icon: Wallet },
  { name: '待审提额', value: badges.value.credit, route: '/admin/finance', icon: TrendCharts },
  { name: '待审解冻', value: badges.value.unfreeze, route: '/admin/finance', icon: RefreshRight },
  { name: '逾期催收', value: badges.value.overdue, route: '/admin/finance', icon: Bell }
])

const formatMoney = (val) => {
    if (val === undefined || val === null) return '0.00'
    return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleTodoClick = (route) => {
  router.push(route)
}

let charts = []

const initCharts = () => {
  if (!window.echarts) return

  charts.forEach(c => c.dispose())
  charts = []

  const bodyStyle = getComputedStyle(document.documentElement)
  const textPrimary = bodyStyle.getPropertyValue('--text-primary').trim() || '#0f172a'
  const textSecondary = bodyStyle.getPropertyValue('--text-secondary').trim() || '#475569'
  const borderSubtle = bodyStyle.getPropertyValue('--border-subtle').trim() || 'rgba(0,0,0,0.1)'

  const pieDom = document.getElementById('pie-chart')
  if (pieDom) {
    const pieChart = window.echarts.init(pieDom)
    const activeLoan = Math.max(0, stats.value.totalDisbursed - stats.value.totalOverdue)
    pieChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: '{b} : ¥{c} ({d}%)'
      },
      legend: {
        orient: 'horizontal',
        bottom: '0',
        textStyle: { color: textSecondary }
      },
      series: [
        {
          name: '资金分配',
          type: 'pie',
          radius: ['45%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: borderSubtle,
            borderWidth: 2
          },
          label: {
            show: false
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '14',
              fontWeight: 'bold',
              color: textPrimary
            }
          },
          data: [
            { value: activeLoan, name: '正常履约中金额', itemStyle: { color: '#10b981' } },
            { value: stats.value.totalOverdue, name: '逾期坏账金额', itemStyle: { color: '#ef4444' } }
          ]
        }
      ]
    })
    charts.push(pieChart)
  }

  const roseDom = document.getElementById('rose-chart')
  if (roseDom) {
    const roseChart = window.echarts.init(roseDom)
    roseChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: '{b} : {c}笔 ({d}%)'
      },
      legend: {
        show: false
      },
      series: [
        {
          name: '产品分布',
          type: 'pie',
          radius: [20, 80],
          center: ['50%', '50%'],
          roseType: 'area',
          itemStyle: {
            borderRadius: 8
          },
          label: {
            show: true,
            color: textPrimary,
            formatter: '{b}\n{d}%'
          },
          data: [
            { value: 45, name: '惠民消费贷', itemStyle: { color: '#6366f1' } },
            { value: 30, name: '尊享经营贷', itemStyle: { color: '#3b82f6' } },
            { value: 15, name: '安居按揭贷', itemStyle: { color: '#10b981' } },
            { value: 25, name: '车主专享贷', itemStyle: { color: '#f59e0b' } }
          ]
        }
      ]
    })
    charts.push(roseChart)
  }

  const lineDom = document.getElementById('line-chart')
  if (lineDom) {
    const lineChart = window.echarts.init(lineDom)
    const dateLabels = []
    for(let i=6; i>=0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      dateLabels.push(`${d.getMonth() + 1}/${d.getDate()}`)
    }

    lineChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        textStyle: { color: textSecondary },
        top: '0'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          boundaryGap: false,
          data: dateLabels,
          axisLabel: { color: textSecondary },
          axisLine: { lineStyle: { color: borderSubtle } }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '资金规模 (元)',
          axisLabel: { color: textSecondary },
          splitLine: { lineStyle: { color: borderSubtle, type: 'dashed' } },
          axisLine: { lineStyle: { color: borderSubtle } }
        }
      ],
      series: [
        {
          name: '每日放款量',
          type: 'line',
          smooth: true,
          lineStyle: { width: 3, color: '#10b981' },
          showSymbol: false,
          areaStyle: {
            opacity: 0.1,
            color: new window.echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#10b981' },
              { offset: 1, color: 'transparent' }
            ])
          },
          data: [12000, 34000, 21000, 56000, 48000, 31000, stats.value.totalDisbursed > 0 ? stats.value.totalDisbursed % 50000 : 25000]
        },
        {
          name: '每日申请笔数',
          type: 'line',
          smooth: true,
          lineStyle: { width: 3, color: '#6366f1' },
          showSymbol: false,
          areaStyle: {
            opacity: 0.1,
            color: new window.echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#6366f1' },
              { offset: 1, color: 'transparent' }
            ])
          },
          data: [5, 12, 8, 15, 11, 9, stats.value.todayApplications > 0 ? stats.value.todayApplications : 6]
        }
      ]
    })
    charts.push(lineChart)
  }

  window.addEventListener('resize', resizeCharts)
}

const resizeCharts = () => {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
    loading.value = true
    badgeLoading.value = true
    refreshTime.value = getFormatTime()
    try {
        const [r1, r2, r3, r4] = await Promise.all([
            request.get('/admin/stat/overview'),
            request.get('/kyc/pending'),
            request.get('/loan/pending'),
            request.get('/admin/stat/badges')
        ])
        
        if (r1.code === 200) {
            stats.value.totalUsers = r1.data.totalUsers
            stats.value.todayApplications = r1.data.todayApplications
            stats.value.totalDisbursed = r1.data.totalDisbursed
            stats.value.totalOverdue = r1.data.totalOverdue
        }
        
        if (r2.data) stats.value.kycPending = r2.data.length
        if (r3.data) stats.value.loanPending = r3.data.length

        if (r4.code === 200) {
          badges.value.kyc = r4.data.kyc
          badges.value.loan = r4.data.loan
          badges.value.credit = r4.data.credit
          badges.value.unfreeze = r4.data.unfreeze
          badges.value.overdue = r4.data.overdue
        }

        nextTick(() => {
          initCharts()
        })

    } finally {
        loading.value = false
        badgeLoading.value = false
    }
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

/* 半透明玻璃头部 */
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

.time-badge {
  background: var(--glass-bg-sub);
  border: 1px solid var(--border-subtle);
  padding: 6px 14px;
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.stat-cards {
  margin-bottom: 24px;
}

/* 扁平半透明玻璃卡片 */
.stat-card {
  height: 90px;
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 16px 20px;
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
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(128, 128, 128, 0.03);
  z-index: 1;
}

/* 微色彩边框指引与图标色调 */
.stat-card.info { border-left: 4px solid var(--primary-color); }
.stat-card.info .icon-wrapper { background: rgba(79, 70, 229, 0.1); color: var(--primary-color); }

.stat-card.orange { border-left: 4px solid var(--warning-color); }
.stat-card.orange .icon-wrapper { background: rgba(245, 158, 11, 0.1); color: var(--warning-color); }

.stat-card.green { border-left: 4px solid var(--success-color); }
.stat-card.green .icon-wrapper { background: rgba(16, 185, 129, 0.1); color: var(--success-color); }

.stat-card.danger { border-left: 4px solid var(--danger-color); }
.stat-card.danger .icon-wrapper { background: rgba(239, 68, 68, 0.1); color: var(--danger-color); }

/* 半透明玻璃面板 */
.glass-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.todo-section {
  padding: 20px 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.indicator {
  width: 4px;
  height: 16px;
  background: var(--primary-color);
  border-radius: 2px;
}

.section-title h3 {
  margin: 0;
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 600;
}

.todo-grid {
  display: flex;
  flex-wrap: wrap;
}

.todo-item {
  background: var(--glass-bg-sub);
  backdrop-filter: blur(8px);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 12px;
}

.todo-item:hover {
  background: var(--glass-bg);
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.todo-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.todo-icon {
  color: var(--text-secondary);
}

.todo-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  background-color: var(--glass-bg-sub);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 2px 8px;
  line-height: 1.2;
}

.todo-badge.warning-badge {
  background-color: rgba(245, 108, 108, 0.65) !important; /* 提升半透明不透明度至 0.65 */
  color: #ffffff !important;
  border: 1px solid rgba(245, 108, 108, 0.8) !important;
  box-shadow: 0 0 6px rgba(245, 108, 108, 0.3);
}

.todo-label {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: left;
}

/* 图表布局 */
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
  font-weight: 600;
}

.chart-dom {
  height: 280px;
  width: 100%;
}

.large-chart {
  height: 320px;
}
</style>
