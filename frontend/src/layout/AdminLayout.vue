<template>
  <el-container class="layout-container admin-layout">
    <el-header class="top-header">
      <div class="logo">贷款审批后台</div>
      <div class="user-profile">
        <el-dropdown trigger="click" placement="bottom-end">
          <span class="username-btn">
            <el-icon style="margin-right:4px;"><UserFilled /></el-icon>
            {{ username }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openPwdDialog">
                <el-icon><Key /></el-icon>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout" style="color:#f56c6c;">
                <el-icon><SwitchButton /></el-icon>
                安全退出
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-body">
      <el-aside width="220px" class="side-nav">
        <el-menu :default-active="activeMenu" class="el-menu-vertical" router background-color="transparent" text-color="var(--text-primary)" active-text-color="#f56c6c">
          <div class="menu-title">核心业务区</div>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataLine /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/kyc">
            <el-icon><Check /></el-icon>
            <span>客户管理</span>
            <span v-if="badges.kyc > 0" class="menu-custom-badge">{{ badges.kyc > 99 ? '99+' : badges.kyc }}</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/loan">
            <el-icon><Coordinate /></el-icon>
            <span>审批中心</span>
            <span v-if="(badges.loan + badges.credit + (badges.unfreeze || 0)) > 0" class="menu-custom-badge warning-badge">{{ (badges.loan + badges.credit + (badges.unfreeze || 0)) > 99 ? '99+' : (badges.loan + badges.credit + (badges.unfreeze || 0)) }}</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/finance">
            <el-icon><Coin /></el-icon>
            <span>财务中心</span>
            <span v-if="badges.overdue > 0" class="menu-custom-badge danger-badge">{{ badges.overdue > 99 ? '99+' : badges.overdue }}</span>
          </el-menu-item>

          <div class="menu-title" style="margin-top:12px">产品运营</div>
          <el-menu-item index="/admin/products">
            <el-icon><Goods /></el-icon>
            <span>贷款产品</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改登录密码" width="400px" custom-class="dark-dialog" append-to-body>
      <el-form label-position="top">
        <el-form-item label="原密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd" :loading="pwdLoading">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { DataLine, Check, Coordinate, Coin, Goods, UserFilled, ArrowDown, SwitchButton, Key } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)

const badges = ref({ kyc: 0, loan: 0, credit: 0, unfreeze: 0, overdue: 0 })
let timer = null

const fetchBadges = async () => {
  try {
    const res = await request.get('/admin/stat/badges')
    if (res && res.data) {
      badges.value = res.data
    }
  } catch (e) {
    // 忽略未就绪或超时的网络连接错误
  }
}

onMounted(() => {
  fetchBadges()
  timer = setInterval(fetchBadges, 10000) // 每10秒轮询一次小红点
  window.addEventListener('fetch-badges', fetchBadges)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('fetch-badges', fetchBadges)
})

// 优先读 localStorage，没有则从 JWT payload 里解析
const getUsername = () => {
  const stored = localStorage.getItem('username')
  if (stored) return stored
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      const name = payload.sub || payload.username || ''
      if (name) localStorage.setItem('username', name)
      return name
    }
  } catch {}
  return '管理员'
}
const username = ref(getUsername())

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const openPwdDialog = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
}

const submitPwd = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.error('密码不能为空')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await request.post('/auth/change-password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录！')
    pwdVisible.value = false
    logout()
  } finally {
    pwdLoading.value = false
  }
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
@import './layout.css';
.admin-layout {
    --primary-color: #f56c6c; /* 管理台变为红色警戒线体系配色 */
}

.menu-custom-badge {
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  height: 18px;
  min-width: 18px;
  margin-left: auto;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}

.warning-badge {
  background-color: #e6a23c;
}

.danger-badge {
  background-color: #f56c6c;
}
</style>
