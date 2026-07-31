<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>审批中心</h2>
      <p>贷款审批与额度申请审批</p>
    </div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <el-tab-pane name="loan">
        <template #label>贷款审批 <span class="tab-badge" v-if="badges.loan > 0">{{badges.loan}}</span></template>
        <el-table :data="pagedLoanList" style="width: 100%" class="custom-table admin-table" v-loading="loading">
          <el-table-column prop="id" label="贷款编号" min-width="100" />
          <el-table-column prop="productName" label="贷款产品" min-width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.productName" type="info" size="small">{{ scope.row.productName }}</el-tag>
              <span v-else style="color:#a0aec0;font-size:12px">无关联产品</span>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="关联账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="amount" label="贷款金额 (元)" min-width="150" />
          <el-table-column prop="termMonths" label="期限" min-width="80">
              <template #default="scope">{{ scope.row.termMonths }} 个月</template>
          </el-table-column>
          <el-table-column prop="purpose" label="用途" />
          <el-table-column label="申请时间" min-width="160">
            <template #default="scope">{{ formatTime(scope.row.applyTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
                {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已放款' : '已驳回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" size="small" type="primary" @click="openLoanDialog(scope.row)">审批</el-button>
              <span v-else style="color:#a0aec0;font-size:12px;">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="loanPage" :page-size="10" :total="list.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>

      <el-tab-pane name="credit">
        <template #label>额度审批 <span class="tab-badge" v-if="badges.credit > 0">{{badges.credit}}</span></template>
        <el-table :data="pagedCreditList" style="width: 100%" class="custom-table admin-table" v-loading="creditLoading">
          <el-table-column prop="id" label="申请编号" min-width="80" />
          <el-table-column prop="username" label="关联账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="idCard" label="身份证" min-width="150">
             <template #default="scope">{{ (scope.row.idCard || '').substring(0, 14) + '****' }}</template>
          </el-table-column>
          <el-table-column prop="requestedAmount" label="申请额度 (元)" min-width="150" />
          <el-table-column label="申请时间" min-width="160">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
                {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已通过' : '已驳回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="100">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" size="small" type="primary" @click="openCreditDialog(scope.row)">审批</el-button>
              <span v-else style="color:#a0aec0;font-size:12px;">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="creditPage" :page-size="10" :total="creditList.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>

      <el-tab-pane name="unfreeze">
        <template #label>解冻审批 <span class="tab-badge" v-if="(badges.unfreeze || 0) > 0">{{badges.unfreeze || 0}}</span></template>
        <el-table :data="pagedUnfreezeList" style="width: 100%" class="custom-table admin-table" v-loading="unfreezeLoading">
          <el-table-column prop="id" label="申请编号" min-width="80" />
          <el-table-column prop="username" label="系统账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="reason" label="解冻申诉理由" min-width="200" />
          <el-table-column label="申请时间" min-width="160">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
                {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已解冻' : '已驳回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="100">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" size="small" type="primary" @click="openUnfreezeDialog(scope.row)">审批</el-button>
              <span v-else style="color:#a0aec0;font-size:12px;">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="unfreezePage" :page-size="10" :total="unfreezeList.length" layout="total, prev, pager, next" background />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 贷款审批弹窗 -->
    <el-dialog v-model="loanDialogVisible" title="贷款审批" width="500px" custom-class="dark-dialog">
      <el-descriptions :column="1" border v-if="loanFormData.row">
        <el-descriptions-item label="贷款编号">{{ loanFormData.row.id }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ loanFormData.row.realName }}</el-descriptions-item>
        <el-descriptions-item label="贷款产品">{{ loanFormData.row.productName || '无关联产品' }}</el-descriptions-item>
        <el-descriptions-item label="贷款金额">{{ loanFormData.row.amount }} 元</el-descriptions-item>
        <el-descriptions-item label="分期期限">{{ loanFormData.row.termMonths }} 个月</el-descriptions-item>
        <el-descriptions-item label="资金用途">{{ loanFormData.row.purpose || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="loanDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="auditLoan(false)" :loading="loanSubmitting">驳回</el-button>
        <el-button type="success" @click="auditLoan(true)" :loading="loanSubmitting">同意放款</el-button>
      </template>
    </el-dialog>

    <!-- 额度审批弹窗 -->
    <el-dialog v-model="creditDialogVisible" title="额度审批" width="500px" custom-class="dark-dialog">
      <el-descriptions :column="1" border v-if="creditFormData.row">
        <el-descriptions-item label="申请编号">{{ creditFormData.row.id }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ creditFormData.row.realName }}</el-descriptions-item>
        <el-descriptions-item label="申请额度">{{ creditFormData.row.requestedAmount }} 元</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" style="margin-top: 20px;">
        <el-form-item label="批复额度">
          <el-input-number v-model="creditFormData.approveAmount" :min="1000" :step="1000" size="large" style="width: 100%" />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">默认为申请额度，可调整</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="auditCredit(false)" :loading="creditSubmitting">驳回</el-button>
        <el-button type="success" @click="auditCredit(true)" :loading="creditSubmitting">批准下发</el-button>
      </template>
    </el-dialog>

    <!-- 解冻审批弹窗 -->
    <el-dialog v-model="unfreezeDialogVisible" title="解冻审批" width="500px" custom-class="dark-dialog">
      <el-descriptions :column="1" border v-if="unfreezeFormData.row">
        <el-descriptions-item label="申请编号">{{ unfreezeFormData.row.id }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ unfreezeFormData.row.realName }}</el-descriptions-item>
        <el-descriptions-item label="申诉理由">{{ unfreezeFormData.row.reason }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="unfreezeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="auditUnfreeze(false)" :loading="unfreezeSubmitting">拒绝解冻</el-button>
        <el-button type="success" @click="auditUnfreeze(true)" :loading="unfreezeSubmitting">同意解冻</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const activeTab = ref('loan')
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

const list = ref([])
const loading = ref(false)
const loanPage = ref(1)
const pagedLoanList = computed(() => {
  const start = (loanPage.value - 1) * 10
  return list.value.slice(start, start + 10)
})

const creditList = ref([])
const creditLoading = ref(false)
const creditPage = ref(1)
const pagedCreditList = computed(() => {
  const start = (creditPage.value - 1) * 10
  return creditList.value.slice(start, start + 10)
})

const unfreezeList = ref([])
const unfreezeLoading = ref(false)
const unfreezePage = ref(1)
const pagedUnfreezeList = computed(() => {
  const start = (unfreezePage.value - 1) * 10
  return unfreezeList.value.slice(start, start + 10)
})

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const loadData = async () => {
    loading.value = true
    try {
        const res = await request.get('/loan/list')
        list.value = res.data || []
    } finally {
        loading.value = false
    }
}

const loadCreditData = async () => {
    creditLoading.value = true
    try {
        const res = await request.get('/credit-app/pending')
        creditList.value = res.data || []
    } finally {
        creditLoading.value = false
    }
}

const loadUnfreezeData = async () => {
    unfreezeLoading.value = true
    try {
        const res = await request.get('/unfreeze/all')
        unfreezeList.value = res.data || []
    } finally {
        unfreezeLoading.value = false
    }
}

// 贷款审批弹窗
const loanDialogVisible = ref(false)
const loanSubmitting = ref(false)
const loanFormData = ref({ row: null })

const openLoanDialog = (row) => {
    loanFormData.value = { row: { ...row } }
    loanDialogVisible.value = true
}

const auditLoan = async (isPass) => {
    loanSubmitting.value = true
    try {
        if (isPass) {
            await request.post(`/loan/approve/${loanFormData.value.row.id}`)
            ElMessage.success('审批放款成功，账单已生成')
        } else {
            await request.post(`/loan/reject/${loanFormData.value.row.id}`)
            ElMessage.success('贷款申请已驳回')
        }
        loanDialogVisible.value = false
        loadData()
        dispatchRefresh()
    } catch {} finally {
        loanSubmitting.value = false
    }
}

// 额度审批弹窗
const creditDialogVisible = ref(false)
const creditSubmitting = ref(false)
const creditFormData = ref({ row: null, approveAmount: 0 })

const openCreditDialog = (row) => {
    creditFormData.value = {
        row: { ...row },
        // 默认批复额度等于用户申请额度
        approveAmount: row.requestedAmount
    }
    creditDialogVisible.value = true
}

const auditCredit = async (isPass) => {
    creditSubmitting.value = true
    try {
        if (isPass) {
            await request.post(`/credit-app/approve/${creditFormData.value.row.id}`, null, {
                params: { approveAmount: creditFormData.value.approveAmount }
            })
            ElMessage.success(`额度 ${creditFormData.value.approveAmount} 元已批准`)
        } else {
            await request.post(`/credit-app/reject/${creditFormData.value.row.id}`)
            ElMessage.success('额度申请已驳回')
        }
        creditDialogVisible.value = false
        loadCreditData()
        dispatchRefresh()
    } catch {} finally {
        creditSubmitting.value = false
    }
}

// 解冻审批弹窗
const unfreezeDialogVisible = ref(false)
const unfreezeSubmitting = ref(false)
const unfreezeFormData = ref({ row: null })

const openUnfreezeDialog = (row) => {
    unfreezeFormData.value = { row: { ...row } }
    unfreezeDialogVisible.value = true
}

const auditUnfreeze = async (isPass) => {
    unfreezeSubmitting.value = true
    try {
        await request.post(`/unfreeze/audit/${unfreezeFormData.value.row.id}`, null, {
            params: { isPass }
        })
        ElMessage.success(isPass ? '账号解冻成功' : '已拒绝解冻')
        unfreezeDialogVisible.value = false
        loadUnfreezeData()
        dispatchRefresh()
    } catch {} finally {
        unfreezeSubmitting.value = false
    }
}

watch(activeTab, (val) => {
    if (val === 'credit' && creditList.value.length === 0) {
        loadCreditData()
    } else if (val === 'unfreeze' && unfreezeList.value.length === 0) {
        loadUnfreezeData()
    }
})

onMounted(() => {
    fetchBadges()
    loadData()
})
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.admin-table) { background: transparent !important; color: #fff;}
:deep(.admin-table th.el-table__cell), :deep(.admin-table tr) { background-color: rgba(0,0,0,0.5) !important; color: #fff; font-weight: bold;}
:deep(.admin-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #f8fafc;}
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(98,106,239,0.25) !important; color: #fff;}

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

:deep(.dark-dialog) {
  border-radius: 16px;
}
:deep(.dark-dialog .el-descriptions__body) {
  background: transparent;
}
:deep(.dark-dialog .el-descriptions__label) {
  width: 100px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
