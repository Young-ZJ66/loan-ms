<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>我的额度</h2>
      <p>查看您的可用与已加载信贷凭证</p>
    </div>

    <el-row :gutter="24" class="stat-cards" v-loading="loading">
      <el-col :span="8">
        <div class="stat-card primary">
          <div class="title">总授信额度 (元)</div>
          <div class="value">{{ credit?.totalCredit || '0.00' }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card success" :class="{ 'frozen-card': credit?.status === 0 }">
          <div class="title">当前可用额度 (元)</div>
          <div class="value">
            <span v-if="credit?.status === 0" style="color: #f56c6c; font-size: 24px;">🚫 风控冻结 (0.00)</span>
            <span v-else>{{ credit?.availableCredit || '0.00' }}</span>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card warning">
          <div class="title">已用或冻结金额 (元)</div>
          <div class="value">{{ credit?.usedCredit || '0.00' }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 申请提额操作区 -->
    <div class="action-section" style="margin-top: 30px;" v-if="credit?.status !== 0">
      <el-card class="glass-panel" shadow="hover">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h3 style="margin-bottom: 5px;">需要更高额度？</h3>
            <p style="color: #94a3b8; font-size: 14px;">您的资料越丰富，信用记录越好，可获批的额度越高。</p>
          </div>
          <el-button type="primary" size="large" @click="handleApplyLimit" :loading="appLoading" :disabled="hasPending">
            {{ hasPending ? '提额审核中...' : (credit ? '申请提额' : '首次开通额度') }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 被冻结时的解冻申请区 -->
    <div class="action-section" style="margin-top: 30px;" v-if="credit?.status === 0">
      <el-card class="glass-panel" shadow="hover" style="border: 1px solid #f56c6c;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h3 style="margin-bottom: 5px; color: #f56c6c;">账户状态异常</h3>
            <p style="color: #fb923c; font-size: 14px;">您的授信账户严密管控中，额度已被锁定无法使用。</p>
          </div>
          <el-button type="danger" size="large" @click="handleApplyUnfreeze" :loading="unfreezeAppLoading" :disabled="hasUnfreezePending">
            {{ hasUnfreezePending ? '解冻申诉审核中...' : '提交解冻申诉' }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 提额申请对话框 -->
    <el-dialog v-model="applyDialogVisible" :title="credit ? '申请提升额度' : '首次信用授信申请'" width="400px" center custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="期望申请总额度 (元)" required>
          <el-input-number v-model="applyAmount" :min="1000" :max="500000" :step="5000" style="width: 100%" size="large" />
        </el-form-item>
        <p style="font-size: 12px; color: #94a3b8; margin-top: 10px;">
          * 温馨提示：人工专员将结合您的当前实名流水与征信模型，对您期望的金额进行综合决议与修正下发。
        </p>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="applyDialogVisible = false">残忍放弃</el-button>
          <el-button type="primary" @click="submitApplyLimit" :loading="submitting">立即上送申请</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 申诉解冻对话框 -->
    <el-dialog v-model="unfreezeDialogVisible" title="提交解冻申诉流水" width="400px" center custom-class="dark-dialog">
      <el-form label-position="top">
        <el-form-item label="详细申诉理由与情况说明" required>
          <el-input type="textarea" v-model="unfreezeReason" placeholder="请认真填写申诉事由，我们将有专员介入处理" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="unfreezeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApplyUnfreeze" :loading="unfreezeSubmitting">提交申诉</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const credit = ref(null)
const profile = ref(null)
const loading = ref(false)
const appLoading = ref(false)
const hasPending = ref(false)

const applyDialogVisible = ref(false)
const applyAmount = ref(10000)
const submitting = ref(false)

const hasUnfreezePending = ref(false)
const unfreezeAppLoading = ref(false)
const unfreezeDialogVisible = ref(false)
const unfreezeReason = ref('')
const unfreezeSubmitting = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/credit/my')
    credit.value = res.data
    
    // 同时获取实名状态
    const profileRes = await request.get('/kyc/my')
    profile.value = profileRes.data
  } finally {
    loading.value = false
  }
}

const checkPendingStatus = async () => {
  appLoading.value = true
  try {
    const res = await request.get('/credit-app/my_pending')
    if (res.data) {
      hasPending.value = true
    } else {
      hasPending.value = false
    }
  } catch (e) {
    // 忽略异常，默认没在途中
  } finally {
    appLoading.value = false
  }
}

const handleApplyLimit = () => {
  if (!profile.value || profile.value.status !== 1) {
    ElMessageBox.confirm(
      '首次开通信用额度前，请先前往完成实名认证且等待管理员审批通过。',
      '前置条件未满足',
      {
        confirmButtonText: '去实名认证',
        cancelButtonText: '暂不',
        type: 'warning'
      }
    ).then(() => {
      router.push('/client/kyc')
    }).catch(() => {})
    return
  }

  // 根据现有额度填充默认期望值
  if (credit.value && credit.value.totalCredit) {
    applyAmount.value = credit.value.totalCredit + 10000
  } else {
    applyAmount.value = 20000
  }
  applyDialogVisible.value = true
}

const submitApplyLimit = async () => {
  submitting.value = true
  try {
    const res = await request.post('/credit-app/apply', null, {
      params: { amount: applyAmount.value }
    })
    ElMessage.success('提额申请已成功提交')
    applyDialogVisible.value = false
    checkPendingStatus()
  } finally {
    submitting.value = false
  }
}

const submitApplyUnfreeze = async () => {
    if (!unfreezeReason.value) {
        ElMessage.error('请填写申诉理由')
        return
    }
    unfreezeSubmitting.value = true
    try {
        const res = await request.post('/unfreeze/apply', null, {
            params: { reason: unfreezeReason.value }
        })
        ElMessage.success('解冻申诉已成功提交')
        unfreezeDialogVisible.value = false
        checkUnfreezePendingStatus()
    } finally {
        unfreezeSubmitting.value = false
    }
}

const checkUnfreezePendingStatus = async () => {
  unfreezeAppLoading.value = true
  try {
    const res = await request.get('/unfreeze/my_pending')
    if (res.data) {
      hasUnfreezePending.value = true
    } else {
      hasUnfreezePending.value = false
    }
  } catch (e) {
  } finally {
    unfreezeAppLoading.value = false
  }
}

const handleApplyUnfreeze = () => {
  unfreezeReason.value = ''
  unfreezeDialogVisible.value = true
}

onMounted(() => {
  loadData()
  checkPendingStatus()
  checkUnfreezePendingStatus()
})
</script>

<style scoped>
/* 冻结状态强调 */
.frozen-card {
  background: linear-gradient(135deg, #4b5563, #374151) !important;
  opacity: 0.8;
  border: 1px solid #f56c6c;
}
:deep(.dark-dialog) { background: #1e293b; color: #fff; }
:deep(.el-dialog__title) { color: #fff; }
:deep(.el-form-item__label) { color: #cbd5e1; }

.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px;}
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

.stat-card {
  padding: 30px;
  border-radius: 16px;
  color: #fff;
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: ''; position: absolute; right: -20px; bottom: -20px;
  width: 100px; height: 100px; border-radius: 50%;
  background: rgba(255,255,255,0.1);
}
.stat-card.primary { background: linear-gradient(135deg, #626aef, #4f46e5); }
.stat-card.success { background: linear-gradient(135deg, #10b981, #059669); }
.stat-card.warning { background: linear-gradient(135deg, #f59e0b, #d97706); }

.stat-card .title { font-size: 14px; opacity: 0.8; margin-bottom: 10px; }
.stat-card .value { font-size: 32px; font-weight: bold; }
</style>
