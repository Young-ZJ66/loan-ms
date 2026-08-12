<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>贷款产品管理</h2>
          <p>配置面向客户的贷款产品线，设置利率、额度与期限规则</p>
        </div>
        <el-button type="primary" color="#626aef" style="color:#fff;" @click="openAdd">
          <el-icon><Plus /></el-icon>
          新增产品
        </el-button>
      </div>
    </div>

    <el-table :data="pagedList" style="width: 100%" class="custom-table admin-table" v-loading="loading">
      <el-table-column prop="id" label="产品ID" min-width="80" />
      <el-table-column prop="name" label="产品名称" min-width="150" />
      <el-table-column label="产品类型" min-width="100">
        <template #default="scope">
          <el-tag :type="PRODUCT_TYPE_MAP[scope.row.type]?.color" effect="plain">
            {{ PRODUCT_TYPE_MAP[scope.row.type]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="年化利率" min-width="100">
        <template #default="scope">
          <span class="rate-value">{{ (scope.row.annualRate * 100).toFixed(2) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="贷款额度（元）" min-width="180">
        <template #default="scope">
          {{ scope.row.minAmount?.toLocaleString() }} ~ {{ scope.row.maxAmount?.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="贷款期限（月）" min-width="140">
        <template #default="scope">
          {{ scope.row.minTerm }} ~ {{ scope.row.maxTerm }} 个月
        </template>
      </el-table-column>
      <el-table-column label="上架状态" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small" round>
            {{ scope.row.status === 1 ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
          <el-button
            size="small"
            :type="scope.row.status === 1 ? 'warning' : 'success'"
            plain
            @click="toggleStatus(scope.row.id, scope.row.status)"
          >
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap">
      <el-pagination v-model:current-page="currentPage" :page-size="10" :total="list.length" layout="total, prev, pager, next" background />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑贷款产品' : '新增贷款产品'" width="560px" custom-class="dark-dialog">
      <el-form :model="form" label-width="120px" label-position="right">
        <el-form-item label="产品名称">
          <el-input v-model="form.name" placeholder="如：极速周转贷、惠民消费贷" />
        </el-form-item>
        <el-form-item label="产品类型">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option label="消费贷" :value="0" />
            <el-option label="经营贷" :value="1" />
            <el-option label="房贷" :value="2" />
            <el-option label="车贷" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="年化利率 (%)">
          <el-input-number v-model="form.annualRatePercent" :min="0.01" :max="36" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低贷款金额">
          <el-input-number v-model="form.minAmount" :min="0" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最高贷款金额">
          <el-input-number v-model="form.maxAmount" :min="0" :step="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最短期限（月）">
          <el-input-number v-model="form.minTerm" :min="1" :max="360" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最长期限（月）">
          <el-input-number v-model="form.maxTerm" :min="1" :max="360" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产品说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="向客户展示的产品描述..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" color="#626aef" style="color:#fff;" @click="save" :loading="saving">
          {{ isEdit ? '保存修改' : '创建产品' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { PRODUCT_TYPE_MAP } from '../../constants'

const list = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pagedList = computed(() => {
  const start = (currentPage.value - 1) * 10
  return list.value.slice(start, start + 10)
})
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)

const form = ref({
  id: null,
  name: '',
  type: 0,
  annualRatePercent: 4.8,
  minAmount: 1000,
  maxAmount: 200000,
  minTerm: 3,
  maxTerm: 36,
  description: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/all')
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', type: 0, annualRatePercent: 4.8, minAmount: 1000, maxAmount: 200000, minTerm: 3, maxTerm: 36, description: '' }
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    name: row.name,
    type: row.type,
    annualRatePercent: parseFloat((row.annualRate * 100).toFixed(2)),
    minAmount: row.minAmount,
    maxAmount: row.maxAmount,
    minTerm: row.minTerm,
    maxTerm: row.maxTerm,
    description: row.description
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name) {
    ElMessage.error('请填写产品名称')
    return
  }
  if (form.value.minAmount >= form.value.maxAmount) {
    ElMessage.error('最低金额必须小于最高金额')
    return
  }
  if (form.value.minTerm >= form.value.maxTerm) {
    ElMessage.error('最短期限必须小于最长期限')
    return
  }
  saving.value = true
  const payload = {
    ...form.value,
    // 将百分比转换为小数存储
    annualRate: parseFloat((form.value.annualRatePercent / 100).toFixed(6))
  }
  delete payload.annualRatePercent
  try {
    if (isEdit.value) {
      await request.put('/product/update', payload)
      ElMessage.success('产品信息已更新')
    } else {
      await request.post('/product/add', payload)
      ElMessage.success('产品创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (id, currentStatus) => {
  const action = currentStatus === 1 ? '下架' : '上架'
  try {
    await ElMessageBox.confirm(`确定要${action}该产品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await request.post(`/product/toggle/${id}`)
  ElMessage.success(currentStatus === 1 ? '产品已下架' : '产品已上架')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px; }
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.admin-table) { background: transparent !important; color: #fff; }
:deep(.admin-table th.el-table__cell), :deep(.admin-table tr) { background-color: rgba(0,0,0,0.5) !important; color: #fff; font-weight: bold; }
:deep(.admin-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #f8fafc; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(98,106,239,0.25) !important; color: #fff; }

.rate-value {
  color: #f59e0b;
  font-weight: bold;
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
