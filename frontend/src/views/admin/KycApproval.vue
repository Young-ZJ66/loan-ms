<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>客户管理</h2>
          <p>管理系统中所有用户的实名认证和账户状态</p>
        </div>
        <div>
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索姓名或账号" 
            :prefix-icon="Search"
            clearable
            style="width: 250px" />
        </div>
      </div>
    </div>

    <el-table :data="pagedList" style="width: 100%" class="custom-table admin-table" v-loading="loading">
      <el-table-column prop="userId" label="用户ID" min-width="100" />
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column label="真实姓名" min-width="120">
        <template #default="scope">{{ scope.row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column label="身份证号" min-width="180">
        <template #default="scope">{{ scope.row.idCard ? maskIdCard(scope.row.idCard) : '-' }}</template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="160">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="认证状态" min-width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === null || scope.row.status === undefined" type="info" effect="dark">待提交</el-tag>
          <el-tag v-else :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
            {{ scope.row.status === 0 ? '待审批' : (scope.row.status === 1 ? '已通过' : '已驳回') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授信总额" min-width="100">
        <template #default="scope">
          <span v-if="scope.row.credit">{{ formatMoney(scope.row.credit.totalCredit) }}</span>
          <span v-else>0.00</span>
        </template>
      </el-table-column>
      <el-table-column label="账户状态" min-width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status === null || scope.row.status === undefined" type="info" effect="dark">未实名</el-tag>
          <el-tag v-else-if="scope.row.status === 1 && scope.row.credit && scope.row.credit.status === 0" type="danger" effect="dark">已冻结</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="success" effect="dark">正常</el-tag>
          <el-tag v-else type="warning" effect="dark">未实名</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="100">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openDetail(scope.row)">操作</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap">
      <el-pagination v-model:current-page="currentPage" :page-size="10" :total="filteredList.length" layout="total, prev, pager, next" background />
    </div>

    <!-- 客户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="客户实名认证详情" width="600px" center custom-class="dark-dialog">
      <div v-if="currentRow" class="detail-container">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="真实姓名">{{ currentRow.realName || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentRow.idCard || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="开户银行">{{ currentRow.bankName || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="银行卡号">{{ currentRow.bankCard || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRow.phone || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="电子邮箱">{{ currentRow.email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="photo-zone">
            <h4>证件照片</h4>
            <div class="photo-grid">
              <div class="photo-item">
                <p>人像面</p>
                <el-image :src="fileUrl(currentRow.idCardFront)" fit="contain" :preview-src-list="[fileUrl(currentRow.idCardFront)]" v-if="currentRow.idCardFront"></el-image>
                <div v-else class="empty-pic">暂无照片</div>
              </div>
              <div class="photo-item">
                <p>国徽面</p>
                <el-image :src="fileUrl(currentRow.idCardBack)" fit="contain" :preview-src-list="[fileUrl(currentRow.idCardBack)]" v-if="currentRow.idCardBack"></el-image>
                <div v-else class="empty-pic">暂无照片</div>
              </div>
            </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="danger" v-if="currentRow && currentRow.status === 0" @click="auditAndClose(currentRow.id, false)">驳回</el-button>
          <el-button type="success" v-if="currentRow && currentRow.status === 0" @click="auditAndClose(currentRow.id, true)">通过认证</el-button>
          <el-button v-if="currentRow && currentRow.credit" @click="openAdjust(currentRow.credit)">调整额度</el-button>
          <el-button type="warning" v-if="currentRow && currentRow.credit && currentRow.credit.status === 1" @click="openFreeze(currentRow.credit.userId)">冻结账户</el-button>
          <el-button type="success" v-if="currentRow && currentRow.credit && currentRow.credit.status === 0" @click="unfreeze(currentRow.credit.userId)">解冻账户</el-button>
          <el-button @click="resetPwd(currentRow.userId)">重置密码</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 额度调整弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整授信额度" width="400px" custom-class="dark-dialog">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input :value="adjustForm.targetUserId" disabled />
        </el-form-item>
        <el-form-item label="当前额度">
          <el-input :value="adjustForm.currentTotal" disabled />
        </el-form-item>
        <el-form-item label="新授信额度">
          <el-input-number v-model="adjustForm.newTotal" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust" :loading="adjustLoading">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 冻结弹窗 -->
    <el-dialog v-model="freezeVisible" title="冻结账户" width="400px" custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="冻结原因" required>
          <el-input type="textarea" v-model="freezeForm.reason" placeholder="请输入冻结原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freezeVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmFreeze" :loading="freezeLoading">确认冻结</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { maskIdCard } from '../../constants'
import { formatTime, fileUrl, formatMoney } from '../../utils/format'

const dispatchRefresh = () => window.dispatchEvent(new CustomEvent('fetch-badges'))

const list = ref([])
const searchKeyword = ref('')
const loading = ref(false)
const currentPage = ref(1)

const filteredList = computed(() => {
    if (!searchKeyword.value) return list.value
    const kw = searchKeyword.value.toLowerCase()
    return list.value.filter(item => {
        const uName = item.username || ''
        const rName = item.realName || ''
        return uName.toLowerCase().includes(kw) || rName.toLowerCase().includes(kw)
    })
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * 10
  return filteredList.value.slice(start, start + 10)
})

const detailVisible = ref(false)
const currentRow = ref(null)

const openDetail = (row) => {
    currentRow.value = row
    detailVisible.value = true
}

const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustForm = ref({ targetUserId: null, currentTotal: 0, newTotal: 0 })

const openAdjust = (credit) => {
  adjustForm.value = {
    targetUserId: credit.userId,
    currentTotal: credit.totalCredit,
    newTotal: credit.totalCredit
  }
  adjustVisible.value = true
}

const confirmAdjust = async () => {
  adjustLoading.value = true
  try {
    await request.post('/credit/adjust', null, {
      params: { targetUserId: adjustForm.value.targetUserId, newTotal: adjustForm.value.newTotal }
    })
    ElMessage.success('授信额度已调整')
    adjustVisible.value = false
    loadData()
    dispatchRefresh()
  } finally { adjustLoading.value = false }
}

const freezeVisible = ref(false)
const freezeForm = ref({ targetUserId: null, reason: '' })
const freezeLoading = ref(false)

const openFreeze = (userId) => {
    freezeForm.value = { targetUserId: userId, reason: '' }
    freezeVisible.value = true
}

const confirmFreeze = async () => {
    if (!freezeForm.value.reason) {
        ElMessage.error('请输入冻结原因')
        return
    }
    freezeLoading.value = true
    try {
        await request.post(`/credit/freeze/${freezeForm.value.targetUserId}`, null, {
            params: { reason: freezeForm.value.reason }
        })
        ElMessage.success('账户已冻结')
        freezeVisible.value = false
        detailVisible.value = false
        loadData()
        dispatchRefresh()
    } finally {
        freezeLoading.value = false
    }
}

const unfreeze = async (userId) => {
    try {
        await request.post(`/credit/unfreeze/${userId}`)
        ElMessage.success('账户已解冻')
        detailVisible.value = false
        loadData()
        dispatchRefresh()
    } catch (e) { console.error(e) }
}

const loadData = async () => {
    loading.value = true
    try {
        const [resKyc, resCredit] = await Promise.all([
            request.get('/kyc/all'),
            request.get('/credit/all')
        ])
        
        const credits = resCredit.data || []
        const creditMap = {}
        for (const c of credits) {
            creditMap[c.userId] = c
        }
        
        const dataList = resKyc.data || []
        dataList.forEach(item => {
            item.credit = creditMap[item.userId] || null
        })
        
        list.value = dataList
    } finally {
        loading.value = false
    }
}

const audit = async (id, isPass) => {
    try {
        await request.post(`/kyc/audit/${id}?isPass=${isPass}`)
        ElMessage.success('审批完成')
        loadData()
        dispatchRefresh()
    } catch (e) { console.error(e) }
}

const auditAndClose = async (id, isPass) => {
    await audit(id, isPass)
    detailVisible.value = false
}

const resetPwd = (userId) => {
  ElMessageBox.prompt('请输入该用户的新密码', '重置密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputPattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/,
    inputErrorMessage: '密码至少8位且包含字母和数字',
    customClass: 'dark-dialog'
  }).then(async ({ value }) => {
    try {
      await request.post(`/auth/admin/reset-password/${userId}`, { newPassword: value })
      ElMessage.success('密码重置成功')
    } catch (e) { console.error(e) }
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

:deep(.admin-table) { background: transparent !important; color: #fff;}
:deep(.admin-table th.el-table__cell), :deep(.admin-table tr) { background-color: rgba(0,0,0,0.5) !important; color: #fff; font-weight: bold;}
:deep(.admin-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.1); color: #f8fafc; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(98,106,239,0.25) !important; color: #fff; }

.detail-container { color: #333; }
.photo-zone { margin-top: 20px; text-align: center; }
.photo-zone h4 { margin-bottom: 15px; color: #606266; }
.photo-grid { display: flex; justify-content: space-around; }
.photo-item { width: 45%; }
.photo-item p { margin-bottom: 8px; font-weight: bold; color: #909399; }
.photo-item .el-image { width: 100%; height: 160px; border-radius: 8px; border: 1px dashed #dcdfe6; background: #fafafa;}
.empty-pic { width: 100%; height: 160px; line-height: 160px; color: #c0c4cc; border: 1px dashed #dcdfe6; background: #f5f7fa; border-radius: 8px; }

.dialog-footer { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }

:deep(.el-descriptions__label) { background-color: #f5f7fa !important; width: 100px; font-weight: bold;}
.text-gray { color: #909399; }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
