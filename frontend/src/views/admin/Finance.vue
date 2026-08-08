<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>财务中心</h2>
      <p>还款计划与入账明细</p>
    </div>

    <!-- 切换 Tab -->
    <el-tabs v-model="activeTab" class="dark-tabs">
      <el-tab-pane label="还款计划总览" name="plans">
        <el-table :data="pagedPlans" style="width: 100%" class="custom-table admin-table" v-loading="loadingPlans">
          <el-table-column prop="id" label="计划ID" min-width="80" />
          <el-table-column prop="loanId" label="贷款ID" min-width="80" />
          <el-table-column prop="remark" label="客户姓名" min-width="120" />
          <el-table-column label="期次" min-width="80">
            <template #default="scope">第 {{ scope.row.termIndex }} 期</template>
          </el-table-column>
          <el-table-column prop="principal" label="本金(元)" min-width="110" />
          <el-table-column prop="interest" label="利息(元)" min-width="110" />
          <el-table-column label="罚息(元)" min-width="110">
            <template #default="scope">
              <span :style="{ color: scope.row.penalty > 0 ? '#f56c6c' : 'inherit' }">{{ scope.row.penalty || '0.00' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="应还金额(元)" min-width="120" />
          <el-table-column prop="dueDate" label="最后还款日" min-width="140" sortable>
            <template #default="scope">{{ formatDate(scope.row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="还款状态" min-width="110">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
                {{ scope.row.status === 0 ? '待还款' : (scope.row.status === 1 ? '已结清' : `逾期 ${getOverdueDays(scope.row.dueDate)} 天`) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="plansPage" :page-size="10" :total="plans.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>

      <el-tab-pane label="历史入账明细" name="records">
        <el-table :data="pagedRecords" style="width: 100%" class="custom-table admin-table" v-loading="loadingRecords">
          <el-table-column prop="id" label="流水ID" min-width="80" />
          <el-table-column prop="userId" label="用户ID" min-width="80" />
          <el-table-column prop="loanId" label="贷款ID" min-width="80" />
          <el-table-column prop="payAmount" label="入账金额(元)" min-width="140" />
          <el-table-column label="还款类型" min-width="130">
            <template #default="scope">
              <el-tag :type="scope.row.payType === 1 ? 'success' : (scope.row.payType === 2 ? 'warning' : scope.row.payType === 3 ? 'primary' : 'info')" effect="dark">
                {{ scope.row.payType === 1 ? '正常按期' : (scope.row.payType === 2 ? '逾期清欠' : scope.row.payType === 3 ? '提前结清' : '-') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="入账时间" min-width="180">
            <template #default="scope">{{ formatTime(scope.row.payTime) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="recordsPage" :page-size="10" :total="records.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>
      <!-- 第三个 Tab：逾期明细 -->
      <el-tab-pane name="overdue">
        <template #label>逾期明细 <span class="tab-badge" v-if="badges.overdue > 0">{{badges.overdue}}</span></template>
        <el-table :data="pagedOverduePlans" style="width: 100%" class="custom-table admin-table" v-loading="loadingOverdue">
          <el-table-column prop="id" label="计划ID" min-width="80" />
          <el-table-column prop="remark" label="客户姓名" min-width="110" />
          <el-table-column prop="loanId" label="贷款ID" min-width="80" />
          <el-table-column label="期次" min-width="80">
            <template #default="scope">第 {{ scope.row.termIndex }} 期</template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="当前应还(含罚息)" min-width="160" />
          <el-table-column label="最后还款日" min-width="130">
            <template #default="scope">{{ formatDate(scope.row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="逾期天数" min-width="110">
            <template #default="scope">
              <el-tag type="danger" effect="dark">{{ getOverdueDays(scope.row.dueDate) }} 天</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120">
            <template #default="scope">
              <el-button size="small" type="danger" @click="openCollect(scope.row)">催收</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="overduePage" :page-size="10" :total="overduePlans.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 催收弹窗 -->
    <el-dialog v-model="collectVisible" title="发起催收" width="460px" custom-class="dark-dialog">
      <el-form :model="collectForm" label-width="90px">
        <el-form-item label="逾期账单">
          <el-input :value="`ID:${collectForm.planId}  第${collectForm.termIndex}期  应还${collectForm.totalAmount}元`" disabled />
        </el-form-item>
        <el-form-item label="催收方式">
          <el-select v-model="collectForm.method" style="width:100%">
            <el-option label="站内信" value="站内信" />
            <el-option label="电话催收" value="电话催收" />
            <el-option label="法务警示" value="法务警示" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注结果">
          <el-input type="textarea" v-model="collectForm.result" :rows="3" placeholder="请输入催收反馈或处理结果..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="danger" @click="submitCollect" :loading="collectLoading">确认发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const activeTab = ref('plans')
const badges = ref({ kyc: 0, loan: 0, credit: 0, unfreeze: 0, overdue: 0 })

const fetchBadges = async () => {
  try {
    const res = await request.get('/admin/stat/badges')
    if (res && res.data) badges.value = res.data
  } catch (e) {}
}

const dispatchRefresh = () => {
    fetchBadges()
    window.dispatchEvent(new CustomEvent('fetch-badges'))
}
const plans = ref([])
const records = ref([])
const loadingPlans = ref(false)
const loadingRecords = ref(false)
const plansPage = ref(1)
const recordsPage = ref(1)
const overduePage = ref(1)
const pagedPlans = computed(() => { const s = (plansPage.value - 1) * 10; return plans.value.slice(s, s + 10) })
const pagedRecords = computed(() => { const s = (recordsPage.value - 1) * 10; return records.value.slice(s, s + 10) })

const formatDate = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}`
}

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

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
  loadingPlans.value = true
  try {
    const res = await request.get('/finance/plans')
    plans.value = res.data || []
  } finally { loadingPlans.value = false }
}

const loadRecords = async () => {
  loadingRecords.value = true
  try {
    const res = await request.get('/finance/records')
    records.value = res.data || []
  } finally { loadingRecords.value = false }
}

const overduePlans = ref([])
const loadingOverdue = ref(false)
const pagedOverduePlans = computed(() => { const s = (overduePage.value - 1) * 10; return overduePlans.value.slice(s, s + 10) })
const collectVisible = ref(false)
const collectLoading = ref(false)
const collectForm = ref({ planId: null, termIndex: 0, totalAmount: 0, method: '站内信', result: '' })

const loadOverduePlans = async () => {
  loadingOverdue.value = true
  try {
    const res = await request.get('/collection/overdue-plans')
    overduePlans.value = res.data || []
  } finally { loadingOverdue.value = false }
}

const triggerOverdue = async () => {
  await request.post('/finance/trigger-overdue')
}

const loadOverdueTab = async () => {
  loadingOverdue.value = true
  try {
    await triggerOverdue()
    const res = await request.get('/collection/overdue-plans')
    overduePlans.value = res.data || []
    fetchBadges()
  } finally { loadingOverdue.value = false }
}

const openCollect = (row) => {
  collectForm.value = { planId: row.id, termIndex: row.termIndex, totalAmount: row.totalAmount, method: '站内信', result: '' }
  collectVisible.value = true
}

const submitCollect = async () => {
  collectLoading.value = true
  try {
    await request.post('/collection/action', collectForm.value)
    ElMessage.success('催收记录已保存，已通知客户。')
    collectVisible.value = false
    loadOverduePlans()
    dispatchRefresh()
  } finally { collectLoading.value = false }
}

// 切换视图时按需加载数据
watch(activeTab, (val) => {
  if (val === 'records' && records.value.length === 0) loadRecords()
  if (val === 'overdue') loadOverdueTab()
})

onMounted(() => {
  fetchBadges()
  loadPlans()
})
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.admin-table) { background: transparent !important; color: #fff;}
:deep(.admin-table th.el-table__cell), :deep(.admin-table tr) { background-color: rgba(0,0,0,0.5) !important; color: #fff; font-weight: bold;}
:deep(.admin-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #f8fafc; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(98,106,239,0.25) !important; color: #fff; }

:deep(.el-tabs__item) { color: #cbd5e1 !important; }
:deep(.el-tabs__item.is-active) { color: #fff !important; font-weight: bold; }
:deep(.el-tabs__active-bar) { background-color: #626aef !important; }
:deep(.el-tabs__nav-wrap::after) { background-color: rgba(255,255,255,0.1) !important; }

.tab-badge {
  background-color: rgba(245, 108, 108, 0.65) !important;
  color: #ffffff !important;
  border: 1px solid rgba(245, 108, 108, 0.8) !important;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 16px;
  height: 16px;
  min-width: 16px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-left: 4px;
  font-weight: bold;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
