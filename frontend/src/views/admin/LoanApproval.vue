<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>审批中心</h2>
      <p>统一处理放款决议及客户授信额度（首批/提额）申请工作流</p>
    </div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <el-tab-pane name="loan">
        <template #label>贷款审批 <span class="tab-badge" v-if="badges.loan > 0">{{badges.loan}}</span></template>
        <el-table :data="list" style="width: 100%" class="custom-table admin-table" v-loading="loading">
          <el-table-column prop="id" label="合约编号" min-width="100" />
          <el-table-column prop="productName" label="贷款产品" min-width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.productName" type="info" size="small">{{ scope.row.productName }}</el-tag>
              <span v-else style="color:#a0aec0;font-size:12px">旧版直批单</span>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="关联账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="amount" label="融资额 (元)" min-width="150" />
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
                {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已放款' : '被驳回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作(仅待办可见)" min-width="180">
            <template #default="scope">
              <span v-if="scope.row.status === 0">
                  <el-button size="small" type="success" @click="audit(scope.row.id, true)">同意</el-button>
                  <el-button size="small" type="danger" @click="audit(scope.row.id, false)">驳回</el-button>
              </span>
              <span v-else style="color:#a0aec0;font-size:12px;">合约已生效或废止</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane name="credit">
        <template #label>额度审批 <span class="tab-badge" v-if="badges.credit > 0">{{badges.credit}}</span></template>
        <el-table :data="creditList" style="width: 100%" class="custom-table admin-table" v-loading="creditLoading">
          <el-table-column prop="id" label="提单编号" min-width="80" />
          <el-table-column prop="username" label="关联账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="idCard" label="身份证头" min-width="150">
             <template #default="scope">{{ (scope.row.idCard || '').substring(0, 14) + '****' }}</template>
          </el-table-column>
          <el-table-column prop="requestedAmount" label="期望核准额度 (元)" min-width="150" />
          <el-table-column label="填单时间" min-width="160">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag type="warning" effect="dark" v-if="scope.row.status === 0">等待风控核决</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="额度修正与下发" min-width="200">
            <template #default="scope">
              <div v-if="scope.row.status === 0" style="display:flex;gap:10px;align-items:center;">
                <el-input-number v-model="scope.row.adminApprovedAmount" :min="1000" :step="1000" size="small" style="width:120px" placeholder="批复金额" />
                <el-button size="small" type="primary" @click="approveCredit(scope.row)">发放</el-button>
                <el-button size="small" type="danger" @click="rejectCredit(scope.row.id)">打回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane name="unfreeze">
        <template #label>解冻审批 <span class="tab-badge" v-if="(badges.unfreeze || 0) > 0">{{badges.unfreeze || 0}}</span></template>
        <el-table :data="unfreezeList" style="width: 100%" class="custom-table admin-table" v-loading="unfreezeLoading">
          <el-table-column prop="id" label="工单单号" min-width="80" />
          <el-table-column prop="username" label="系统账号" min-width="100" />
          <el-table-column prop="realName" label="真实姓名" min-width="100" />
          <el-table-column prop="reason" label="解冻申诉理由" min-width="200" />
          <el-table-column label="起草时间" min-width="160">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="流转状态" min-width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
                {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已解冻' : '已打回') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="决议操作" min-width="200">
            <template #default="scope">
              <span v-if="scope.row.status === 0">
                  <el-button size="small" type="success" @click="auditUnfreeze(scope.row.id, true)" plain>解冻</el-button>
                  <el-button size="small" type="danger" @click="auditUnfreeze(scope.row.id, false)" plain>继续冻结</el-button>
              </span>
              <span v-else style="color:#606266;font-size:12px;">处置完毕归档中...</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
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

const creditList = ref([])
const creditLoading = ref(false)

const unfreezeList = ref([])
const unfreezeLoading = ref(false)

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
        let data = res.data || []
        // 初始化每个数字框为用户期望的数字
        data.forEach(item => { item.adminApprovedAmount = item.requestedAmount })
        creditList.value = data
    } finally {
        creditLoading.value = false
    }
}

const audit = async (id, isPass) => {
    try {
        if (isPass) {
            await request.post(`/loan/approve/${id}`)
            ElMessage.success('审批放款成功，账单已生成')
        } else {
            await request.post(`/loan/reject/${id}`)
            ElMessage.success('贷款申请已被驳回')
        }
        loadData()
        dispatchRefresh()
    } catch {}
}

const approveCredit = async (row) => {
    try {
        await request.post(`/credit-app/approve/${row.id}`, null, {
            params: { approveAmount: row.adminApprovedAmount }
        })
        ElMessage.success(`额度 ${row.adminApprovedAmount} 元已下发至 ${row.realName} 账户`)
        loadCreditData()
        dispatchRefresh()
    } catch {}
}

const rejectCredit = async (id) => {
    try {
        await request.post(`/credit-app/reject/${id}`)
        ElMessage.success('提额申请已驳回')
        loadCreditData()
        dispatchRefresh()
    } catch {}
}

watch(activeTab, (val) => {
    if (val === 'credit' && creditList.value.length === 0) {
        loadCreditData()
    } else if (val === 'unfreeze' && unfreezeList.value.length === 0) {
        loadUnfreezeData()
    }
})

const loadUnfreezeData = async () => {
    unfreezeLoading.value = true
    try {
        const res = await request.get('/unfreeze/all')
        unfreezeList.value = res.data || []
    } finally {
        unfreezeLoading.value = false
    }
}

const auditUnfreeze = async (id, isPass) => {
    try {
        await request.post(`/unfreeze/audit/${id}`, null, {
            params: { isPass }
        })
        ElMessage.success(isPass ? '账号解冻成功' : '继续管制决议已生效')
        loadUnfreezeData()
        dispatchRefresh()
    } catch {}
}

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
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(245,108,108,0.2) !important; color: #fff;}

:deep(.el-tabs__item) { color: #cbd5e1 !important; }
:deep(.el-tabs__item.is-active) { color: #fff !important; font-weight: bold; }
:deep(.el-tabs__active-bar) { background-color: #626aef !important; }
:deep(.el-tabs__nav-wrap::after) { background-color: rgba(255,255,255,0.1) !important; }

.tab-badge {
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  height: 18px;
  min-width: 18px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-left: 4px;
}
</style>
