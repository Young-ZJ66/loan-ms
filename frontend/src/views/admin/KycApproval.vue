<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>客户管理</h2>
          <p>管理系统中所有用户的实名档案和状态</p>
        </div>
        <div>
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索真实姓名或账号名..." 
            prefix-icon="Search"
            clearable
            style="width: 250px" />
        </div>
      </div>
    </div>

    <el-table :data="filteredList" style="width: 100%" class="custom-table admin-table" v-loading="loading">
      <el-table-column prop="userId" label="档案UID" min-width="100" />
      <el-table-column prop="username" label="账号名称" min-width="120" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column prop="idCard" label="身份证件号" min-width="180" />
      <el-table-column label="申请时间" min-width="160">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'warning' : (scope.row.status === 1 ? 'success' : 'danger')" effect="dark">
            {{ scope.row.status === 0 ? '待审核' : (scope.row.status === 1 ? '已通过' : '已驳回') }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="授信总额" min-width="90">
        <template #default="scope">
          <span v-if="scope.row.credit">{{ scope.row.credit.totalCredit }}</span>
          <span v-else class="text-gray">-</span>
        </template>
      </el-table-column>
      
      <el-table-column label="账户状态" min-width="90">
        <template #default="scope">
          <span v-if="!scope.row.credit">-</span>
          <el-tag v-else :type="scope.row.credit.status === 1 ? 'success' : 'danger'" effect="dark">
            {{ scope.row.credit.status === 1 ? '正常' : '已冻结' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="180">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openDetail(scope.row)" plain>审查</el-button>

          <el-dropdown style="margin-left: 10px;" trigger="click">
            <el-button size="small" type="info" plain>
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="scope.row.status === 0" @click="audit(scope.row.id, true)" style="color: #67c23a;">✓ 通过认证</el-dropdown-item>
                <el-dropdown-item v-if="scope.row.status === 0" @click="rejectKyc(scope.row.id)" style="color: #f56c6c;">✗ 驳回申请</el-dropdown-item>
                <el-dropdown-item v-if="scope.row.credit" @click="openAdjust(scope.row.credit)">调额设置</el-dropdown-item>
                <el-dropdown-item v-if="scope.row.credit && scope.row.credit.status === 1" @click="openFreeze(scope.row.credit.userId)" style="color: #f56c6c;">冻结管控</el-dropdown-item>
                <el-dropdown-item v-if="scope.row.credit && scope.row.credit.status === 0" @click="unfreeze(scope.row.credit.userId)" style="color: #67c23a;">解冻恢复</el-dropdown-item>
                <el-dropdown-item :divided="scope.row.status === 0 || scope.row.credit != null" @click="resetPwd(scope.row.userId)">重置密码</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>

    </el-table>

    <!-- 档案详情审查对话框 -->
    <el-dialog v-model="detailVisible" title="进件四要素审查工作台" width="600px" center custom-class="dark-dialog">
      <div v-if="currentRow" class="detail-container">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="开户银行">{{ currentRow.bankName || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="银行卡号">{{ currentRow.bankCard || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRow.phone || '未关联手机' }}</el-descriptions-item>
          <el-descriptions-item label="电子邮箱">{{ currentRow.email || '尚未绑定' }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="photo-zone">
            <h4>[ 证件影像采集记录 ]</h4>
            <div class="photo-grid">
              <div class="photo-item">
                <p>人像面</p>
                <el-image :src="currentRow.idCardFront" fit="contain" :preview-src-list="[currentRow.idCardFront]" v-if="currentRow.idCardFront"></el-image>
                <div v-else class="empty-pic">影像缺失</div>
              </div>
              <div class="photo-item">
                <p>国徽面</p>
                <el-image :src="currentRow.idCardBack" fit="contain" :preview-src-list="[currentRow.idCardBack]" v-if="currentRow.idCardBack"></el-image>
                <div v-else class="empty-pic">影像缺失</div>
              </div>
            </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭阅览</el-button>
          <el-button type="success" v-if="currentRow && currentRow.status === 0" @click="auditAndClose(currentRow.id, true)">准入通过</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 额度调整弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整客户授信额度" width="400px" custom-class="dark-dialog">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="目标用户ID">
          <el-input :value="adjustForm.targetUserId" disabled />
        </el-form-item>
        <el-form-item label="当前总额度">
          <el-input :value="adjustForm.currentTotal" disabled />
        </el-form-item>
        <el-form-item label="新总授信额度">
          <el-input-number v-model="adjustForm.newTotal" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="adjustVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAdjust" :loading="adjustLoading">确认调整</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 冻结弹窗 -->
    <el-dialog v-model="freezeVisible" title="管控档案冻结" width="400px" custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="冻结原因" required>
          <el-input type="textarea" v-model="freezeForm.reason" placeholder="例如：疑似欺诈/材料作假/关联逾期等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="freezeVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmFreeze" :loading="freezeLoading">立即冻结并告知</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'

const dispatchRefresh = () => window.dispatchEvent(new CustomEvent('fetch-badges'))

const list = ref([])
const searchKeyword = ref('')
const loading = ref(false)

const filteredList = computed(() => {
    if (!searchKeyword.value) return list.value
    const kw = searchKeyword.value.toLowerCase()
    return list.value.filter(item => {
        const uName = item.username || ''
        const rName = item.realName || ''
        return uName.toLowerCase().includes(kw) || rName.toLowerCase().includes(kw)
    })
})

const detailVisible = ref(false)
const currentRow = ref(null)

const openDetail = (row) => {
    currentRow.value = row
    detailVisible.value = true
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
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
        ElMessage.error('请务必输入冻结锁定原因！')
        return
    }
    freezeLoading.value = true
    try {
        await request.post(`/credit/freeze/${freezeForm.value.targetUserId}`, null, {
            params: { reason: freezeForm.value.reason }
        })
        ElMessage.success('账户已冻结，通知已发送')
        freezeVisible.value = false
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
        loadData()
        dispatchRefresh()
    } catch {}
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
        ElMessage.success('审批完成！流水线已封板。')
        loadData()
        dispatchRefresh()
    } catch {}
}

const rejectKyc = (id) => {
  ElMessageBox.prompt('请输入驳回实名认证的原因', '驳回申请', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '驳回原因不能为空',
    customClass: 'dark-dialog'
  }).then(async ({ value }) => {
    try {
      await request.post(`/kyc/audit/${id}?isPass=false&reason=${encodeURIComponent(value)}`)
      ElMessage.success('实名审核已驳回，并已通知用户')
      loadData()
      dispatchRefresh()
    } catch {}
  }).catch(() => {})
}

const auditAndClose = async (id, isPass) => {
    await audit(id, isPass)
    detailVisible.value = false
}

const resetPwd = (userId) => {
  ElMessageBox.prompt('请输入该用户的新密码', '重置密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '新密码不能为空',
    customClass: 'dark-dialog'
  }).then(async ({ value }) => {
    try {
      await request.post(`/auth/admin/reset-password/${userId}`, { newPassword: value })
      ElMessage.success('该用户密码重置成功')
    } catch {}
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
:deep(.el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell) { background-color: rgba(245,108,108,0.2) !important; color: #fff; }

.detail-container { color: #333; }
.photo-zone { margin-top: 20px; text-align: center; }
.photo-zone h4 { margin-bottom: 15px; color: #606266; }
.photo-grid { display: flex; justify-content: space-around; }
.photo-item { width: 45%; }
.photo-item p { margin-bottom: 8px; font-weight: bold; color: #909399; }
.photo-item .el-image { width: 100%; height: 160px; border-radius: 8px; border: 1px dashed #dcdfe6; background: #fafafa;}
.empty-pic { width: 100%; height: 160px; line-height: 160px; color: #c0c4cc; border: 1px dashed #dcdfe6; background: #f5f7fa; border-radius: 8px; }

/* 优化 Dialog 描述列表背景色彩对比度 */
:deep(.el-descriptions__label) { background-color: #f5f7fa !important; width: 100px; font-weight: bold;}
.text-gray { color: #909399; }
</style>
