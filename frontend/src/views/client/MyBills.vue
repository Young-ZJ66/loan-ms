<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>账单与还款</h2>
      <p>查看历史账单及进行分期还款</p>
    </div>

    <el-table :data="pagedPlans" style="width: 100%" class="custom-table" v-loading="loading">
      <el-table-column label="最后还款日" min-width="150">
        <template #default="scope">
          {{ formatDate(scope.row.dueDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="termIndex" label="当前期" min-width="100">
        <template #default="scope">第 {{ scope.row.termIndex }} 期</template>
      </el-table-column>
      <el-table-column prop="principal" label="本金(元)" :formatter="(row, column, cellValue) => formatMoney(cellValue)" />
      <el-table-column prop="interest" label="利息(元)" :formatter="(row, column, cellValue) => formatMoney(cellValue)" />
      <el-table-column label="逾期罚息(元)">
        <template #default="scope">
          <span :style="{ color: scope.row.penalty > 0 ? '#f56c6c' : 'inherit', fontWeight: scope.row.penalty > 0 ? 'bold' : 'normal' }">
            {{ formatMoney(scope.row.penalty) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="应还总计(元)" :formatter="(row, column, cellValue) => formatMoney(cellValue)" />
      <el-table-column label="账单状态" min-width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
            {{ scope.row.status === 0 ? '待偿还' : (scope.row.status === 1 ? '已结清' : `逾期 ${getOverdueDays(scope.row.dueDate)} 天`) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交互" min-width="150">
        <template #default="scope">
          <!-- 待偿还和逾期中均可还款 -->
          <el-button 
             v-if="scope.row.status === 0 || scope.row.status === 2" 
             size="small" 
             :type="scope.row.status === 2 ? 'danger' : 'primary'" 
             @click="pay(scope.row)">
             {{ scope.row.status === 2 ? '逾期还款' : '立即还款' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap">
      <el-pagination v-model:current-page="currentPage" :page-size="10" :total="plans.length" layout="total, prev, pager, next" background />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate, formatMoney } from '../../utils/format'

const loading = ref(false)
const plans = ref([])
const currentPage = ref(1)
const pagedPlans = computed(() => {
  const start = (currentPage.value - 1) * 10
  return plans.value.slice(start, start + 10)
})

const getOverdueDays = (dueDate) => {
  if (!dueDate) return 0
  const due = new Date(dueDate)
  due.setHours(0,0,0,0)
  const today = new Date()
  today.setHours(0,0,0,0)
  const diff = today - due
  return Math.max(1, Math.floor(diff / (1000 * 60 * 60 * 24)))
}

const loadPlans = async () => {
    loading.value = true
    try {
        const res = await request.get('/repayment/my-plans')
        plans.value = res.data || []
    } finally {
        loading.value = false
    }
}

const pay = async (row) => {
    const isOverdue = row.status === 2
    const overdueLabel = isOverdue ? `（含逾期罚息）` : ''
    try {
        await ElMessageBox.confirm(
            `确认支付第 ${row.termIndex} 期账单，应还金额 ${row.totalAmount} 元${overdueLabel}？`,
            isOverdue ? '逾期清欠确认' : '还款确认',
            {
                confirmButtonText: '确认还款',
                cancelButtonText: '再想想',
                type: isOverdue ? 'warning' : 'info',
                center: true
            }
        )
    } catch {
        return // 用户点了「再想想」
    }
    try {
        await request.post('/repayment/pay', null, { params: { planId: row.id, payAmount: row.totalAmount }})
        ElMessage.success(`第 ${row.termIndex} 期还款成功！`)
        loadPlans()
    } catch (e) {
        console.error(e)
    }
}

onMounted(() => loadPlans())
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.el-table) {
  background: transparent !important;
  color: #fff;
}
:deep(.el-table th.el-table__cell), :deep(.el-table tr) {
  background-color: rgba(0,0,0,0.4) !important;
  color: #fff; font-weight: bold;
}
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #e2e8f0; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) {
  background-color: rgba(98,106,239,0.3) !important; color: #fff;
}

/* 状态标签半透明化 */
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
