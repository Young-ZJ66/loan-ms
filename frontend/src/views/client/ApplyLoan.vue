<template>
  <div class="page-container glass-panel">
    <div class="header-banner" style="display: flex; justify-content: space-between; align-items: center;">
      <div>
        <h2>申请贷款</h2>
        <p>您的全部贷款申请与审批记录</p>
      </div>
      <el-button type="primary" size="large" @click="openDialog" color="#626aef"
        style="color:#fff; display:inline-flex; align-items:center; gap:6px;">
        <el-icon><Plus /></el-icon>
        新增贷款申请
      </el-button>
    </div>

    <el-table :data="list" style="width: 100%" class="custom-table" v-loading="loading">
      <el-table-column label="申请时间" min-width="200">
        <template #default="scope">
          {{ formatTime(scope.row.applyTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="贷款金额(元)" min-width="150" />
      <el-table-column prop="termMonths" label="期限(月)" min-width="100" />
      <el-table-column prop="purpose" label="申请说明" />
    <el-table-column prop="productName" label="贷款产品">
        <template #default="scope">
          {{ scope.row.productName || '旧版无产品单' }}
        </template>
      </el-table-column>
      <el-table-column label="审批状态" min-width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
            {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已放款' : '已驳回') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 申请弹窗 -->
    <el-dialog v-model="dialogVisible" title="填写贷款申请表" width="600px" custom-class="dark-dialog" @closed="resetForm">
      
      <!-- 第一步：选择产品 -->
      <div v-if="!selectedProduct" class="product-selection">
        <div class="step-title">请选择贷款产品</div>
        <div v-if="products.length === 0" class="no-product">当前暂无上架贷款产品，请联系客服。</div>
        <div class="product-grid">
          <div 
            v-for="p in products" :key="p.id" 
            class="product-card" 
            @click="selectProduct(p)"
          >
            <div class="p-header">
              <span class="p-name">{{ p.name }}</span>
              <el-tag :type="p.type === 1 ? 'warning' : (p.type === 2 ? 'success' : (p.type === 3 ? 'danger' : ''))" size="small">{{ ['消费贷','经营贷','房贷','车贷'][p.type] }}</el-tag>
            </div>
            <div class="p-rate">年化 {{ (p.annualRate * 100).toFixed(2) }}%</div>
            <div class="p-desc">{{ p.description }}</div>
            <div class="p-limits">额度：{{ p.minAmount }} - {{ p.maxAmount }} 元</div>
            <div class="p-limits">期限：{{ p.minTerm }} - {{ p.maxTerm }} 月</div>
          </div>
        </div>
      </div>

      <!-- 第二步：填写表单 -->
      <div v-else>
        <div class="selected-product-bar">
          <div>已选产品：<strong>{{ selectedProduct.name }}</strong> (年化 {{ (selectedProduct.annualRate * 100).toFixed(2) }}%)</div>
          <el-button type="primary" link @click="selectedProduct = null">重新选择</el-button>
        </div>
        <el-form :model="form" label-position="top">
          <el-form-item label="贷款金额 (元)">
            <el-input-number v-model="form.amount" :min="selectedProduct.minAmount" :max="selectedProduct.maxAmount" :step="1000" size="large" style="width: 100%"></el-input-number>
            <div class="field-tip">限额：{{ selectedProduct.minAmount }} ~ {{ selectedProduct.maxAmount }}</div>
          </el-form-item>
          <el-form-item label="分期数 (月)">
            <el-input-number v-model="form.termMonths" :min="selectedProduct.minTerm" :max="selectedProduct.maxTerm" size="large" style="width: 100%"></el-input-number>
            <div class="field-tip">期限：{{ selectedProduct.minTerm }} ~ {{ selectedProduct.maxTerm }} 个月</div>
          </el-form-item>
          <el-form-item label="资金用途声明">
            <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="例如：日常消费、工程周转" size="large"></el-input>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="selectedProduct" type="primary" color="#626aef" @click="submit" :loading="submitting" style="color:#fff;">
            确认提交
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const list = ref([])
const products = ref([])
const selectedProduct = ref(null)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

const form = ref({
  amount: 0,
  termMonths: 0,
  purpose: ''
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
        const res = await request.get('/loan/my')
        list.value = res.data || []
    } finally {
        loading.value = false
    }
}

const loadProducts = async () => {
    const res = await request.get('/product/active')
    products.value = res.data || []
}

const openDialog = () => {
    loadProducts()
    resetForm()
    dialogVisible.value = true
}

const resetForm = () => {
    selectedProduct.value = null
    form.value = { amount: 0, termMonths: 0, purpose: '' }
}

const selectProduct = (p) => {
    selectedProduct.value = p
    form.value.amount = p.minAmount
    form.value.termMonths = p.minTerm
}

const submit = async () => {
  submitting.value = true
  try {
    await request.post('/loan/apply', {
        productId: selectedProduct.value.id,
        amount: form.value.amount,
        termMonths: form.value.termMonths,
        annualRate: selectedProduct.value.annualRate,
        purpose: form.value.purpose
    })
    ElMessage.success('贷款申请已提交并进入队列，等待审核放款！')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.el-table) { background: transparent !important; color: #fff; }
:deep(.el-table th.el-table__cell), :deep(.el-table tr) { background-color: rgba(0,0,0,0.4) !important; color: #fff; font-weight: bold;}
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #e2e8f0; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(98,106,239,0.3) !important; color: #fff; }

.rate-preview {
  margin: 15px 0 20px;
  padding: 15px;
  background: rgba(98,106,239,0.1);
  border-left: 4px solid #626aef;
  color: #333;
  border-radius: 4px;
}
.rate-preview strong { color: #000; }

.step-title { font-size: 16px; font-weight: bold; margin-bottom: 20px; color: #1e293b; }
.no-product { color: #94a3b8; text-align: center; padding: 30px; }
.product-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.product-card {
  border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px;
  cursor: pointer; transition: all 0.2s; background: #fff;
}
.product-card:hover { border-color: #626aef; box-shadow: 0 4px 12px rgba(98,106,239,0.15); transform: translateY(-2px); }
.p-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.p-name { font-weight: bold; font-size: 16px; color: #1e293b; }
.p-rate { color: #f56c6c; font-size: 18px; font-weight: bold; margin-bottom: 6px; }
.p-desc { color: #64748b; font-size: 12px; margin-bottom: 10px; min-height: 36px; line-height: 1.4; }
.p-limits { color: #475569; font-size: 12px; }

.selected-product-bar {
  display: flex; justify-content: space-between; align-items: center;
  background: rgba(98,106,239,0.1); border-left: 4px solid #626aef;
  padding: 12px 16px; border-radius: 4px; margin-bottom: 20px;
  color: #1e293b; font-size: 14px;
}
.field-tip { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
