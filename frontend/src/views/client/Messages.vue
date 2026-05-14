<template>
  <div class="page-container glass-panel">
    <div class="header-banner">
      <h2>消息中心</h2>
      <p>您的还款提醒与平台通知都在这里</p>
      <el-button size="small" type="primary" plain @click="markAll" style="margin-top:12px" :disabled="messages.length === 0">全部标为已读</el-button>
    </div>

    <div v-loading="loading">
      <!-- 空态 -->
      <div v-if="!loading && messages.length === 0" class="empty-state">
        <div class="empty-icon-wrap">
          <el-icon class="empty-icon-el"><Bell /></el-icon>
        </div>
        <div class="empty-title">收件箱目前无新消息</div>
        <div class="empty-desc">
          您目前没有任何通知<br/>保持良好的还款记录，系统将在有新动态时第一时间提醒您
        </div>
        <div class="empty-badge">🎉 信用良好</div>
      </div>

      <!-- 消息列表 -->
      <div v-for="msg in messages" :key="msg.id" class="msg-card" :class="{ unread: msg.isRead === 0 }" @click="readMsg(msg)">
        <div class="msg-header">
          <span class="msg-title">{{ msg.title }}</span>
          <el-tag v-if="msg.isRead === 0" type="danger" size="small" effect="dark">未读</el-tag>
          <el-tag v-else type="info" size="small">已读</el-tag>
        </div>
        <div class="msg-body">{{ msg.content }}</div>
        <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const messages = ref([])
const loading = ref(false)

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await request.get('/message/list')
    messages.value = res.data || []
  } finally {
    loading.value = false
  }
}

const readMsg = async (msg) => {
  if (msg.isRead === 1) return
  await request.put(`/message/read/${msg.id}`)
  msg.isRead = 1
  window.dispatchEvent(new CustomEvent('unread-changed'))
}

const markAll = async () => {
  await request.put('/message/read-all')
  messages.value.forEach(m => (m.isRead = 1))
  ElMessage.success('全部已读')
  window.dispatchEvent(new CustomEvent('unread-changed'))
}

onMounted(() => loadMessages())
</script>

<style scoped>
.page-container { padding: 30px; }
.header-banner { margin-bottom: 30px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 20px; }
.header-banner h2 { font-size: 24px; color: #fff; margin-bottom: 5px; }
.header-banner p { color: #cbd5e1; }

/* 空态区块 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  border: 1px dashed rgba(255,255,255,0.12);
  border-radius: 20px;
  background: rgba(255,255,255,0.02);
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.empty-icon-wrap {
  width: 110px;
  height: 110px;
  background: rgba(98,106,239,0.18);
  border: 1px solid rgba(98,106,239,0.35);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
  box-shadow: 0 0 50px rgba(98,106,239,0.35);
}

.empty-icon-el {
  font-size: 52px;
  color: #a5b4fc;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 8px rgba(0,0,0,0.5);
}

.empty-desc {
  font-size: 14px;
  color: #94a3b8;
  line-height: 1.8;
  max-width: 320px;
  margin-bottom: 24px;
}

.empty-badge {
  font-size: 13px;
  color: #67c23a;
  background: rgba(103,194,58,0.12);
  border: 1px solid rgba(103,194,58,0.25);
  border-radius: 20px;
  padding: 6px 18px;
  letter-spacing: 0.5px;
}

/* 消息卡片 */
.msg-card {
  background: rgba(0,0,0,0.35) !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(10px);
}
.msg-card:hover { background: rgba(0,0,0,0.5) !important; border-color: rgba(98,106,239,0.7) !important; box-shadow: 0 4px 15px rgba(0,0,0,0.35); }
.msg-card.unread { border-left: 4px solid #f56c6c !important; }

.msg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.msg-title { font-size: 16px; font-weight: bold; color: #ffffff !important; letter-spacing: 0.5px; }
.msg-body { font-size: 14px; color: #f8fafc !important; line-height: 1.6; margin-bottom: 8px; font-weight: 500; }
.msg-time { font-size: 12px; color: #cbd5e1 !important; text-align: right; }
</style>
