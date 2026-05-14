<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>数据看板</h2>
      <p>全平台核心运营数据实时概览</p>
    </div>

    <el-row :gutter="20" class="stat-cards" v-loading="loading">
      <el-col :span="8">
        <div class="stat-card primary">
          <div class="icon-area">👥</div>
          <div class="info">
            <div class="title">注册用户总量</div>
            <div class="value">{{ stats.totalUsers }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card warning">
          <div class="icon-area">⏳</div>
          <div class="info">
            <div class="title">待审实名认证</div>
            <div class="value">{{ stats.kycPending }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card orange">
          <div class="icon-area">📋</div>
          <div class="info">
            <div class="title">今日新增申请</div>
            <div class="value">{{ stats.todayApplications }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stat-cards second-row">
      <el-col :span="8">
        <div class="stat-card green">
          <div class="icon-area">💰</div>
          <div class="info">
            <div class="title">全平台总放款额</div>
            <div class="value">¥ {{ formatMoney(stats.totalDisbursed) }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card danger">
          <div class="icon-area">🚨</div>
          <div class="info">
            <div class="title">逾期金额</div>
            <div class="value">¥ {{ formatMoney(stats.totalOverdue) }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card purple">
          <div class="icon-area">⚠️</div>
          <div class="info">
            <div class="title">待审放款申请</div>
            <div class="value">{{ stats.loanPending }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'

const loading = ref(false)
const stats = ref({
    totalUsers: '-', kycPending: '-', todayApplications: '-',
    totalDisbursed: 0, totalOverdue: 0, loanPending: '-'
})

const formatMoney = (val) => {
    if (val === undefined || val === null) return '0.00'
    return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
    loading.value = true
    try {
        const [r1, r2, r3] = await Promise.all([
            request.get('/admin/stat/overview'),
            request.get('/kyc/pending'),
            request.get('/loan/pending')
        ])
        if (r1.code === 200) {
            stats.value.totalUsers = r1.data.totalUsers
            stats.value.todayApplications = r1.data.todayApplications
            stats.value.totalDisbursed = r1.data.totalDisbursed
            stats.value.totalOverdue = r1.data.totalOverdue
        }
        if (r2.data) stats.value.kycPending = r2.data.length
        if (r3.data) stats.value.loanPending = r3.data.length
    } finally {
        loading.value = false
    }
})
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

.stat-cards { margin-bottom: 0; }
.second-row { margin-top: 20px; }

.stat-card {
  padding: 24px 28px;
  border-radius: 16px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  gap: 20px;
}
.icon-area { font-size: 36px; line-height: 1; }
.stat-card .title { font-size: 13px; opacity: 0.8; margin-bottom: 8px; }
.stat-card .value { font-size: 28px; font-weight: bold; }

.stat-card.primary { background: linear-gradient(135deg, #1e3a8a, #2563eb); }
.stat-card.warning { background: linear-gradient(135deg, #92400e, #d97706); }
.stat-card.orange  { background: linear-gradient(135deg, #7c2d12, #ea580c); }
.stat-card.green   { background: linear-gradient(135deg, #14532d, #16a34a); }
.stat-card.danger  { background: linear-gradient(135deg, #7f1d1d, #dc2626); }
.stat-card.purple  { background: linear-gradient(135deg, #4c1d95, #7c3aed); }
</style>
