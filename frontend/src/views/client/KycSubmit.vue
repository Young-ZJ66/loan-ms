<template>
  <div class="page-container flex-center">
    <el-card class="kyc-card glass-panel dark-mode-card" shadow="hover">
      <div class="card-header">
        <h2>实名与放款资料补充</h2>
        <p>为保障资金安全，请提供您的真实资料并绑定放款卡</p>
      </div>

      <!-- 结果展示区 -->
      <div v-if="profile && profile.id" class="result-box">
         <el-icon class="icon" :class="profile.status === 1 ? 'success-icon' : (profile.status === 2 ? 'danger-icon' : 'warning-icon')">
            <Warning v-if="profile.status === 0" />
            <CircleCheck v-else-if="profile.status === 1" />
            <CircleClose v-else />
         </el-icon>
         <h3>{{ profile.status === 0 ? '资料审核中' : (profile.status === 1 ? '实名已通过' : '审核被驳回') }}</h3>
         <p v-if="profile.status === 1" style="color: #67c23a; margin-top:10px;">您可以前往【申请贷款】发起融资。</p>
         <div v-if="profile.status === 2" style="margin-top:20px;">
            <el-button type="primary" color="#626aef" style="color:white; width:200px" size="large" @click="reSubmit">重新提交资料</el-button>
         </div>
      </div>

      <!-- 填表区 -->
      <el-form v-else :model="form" :rules="rules" ref="formRef" label-position="top" class="kyc-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="必须与身份证保持一致" size="large"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="输入18位有效证件号" size="large"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证人像面" prop="idCardFront">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :headers="getUploadHeaders()"
                :show-file-list="false"
                :on-success="res => handleSuccess(res, 'idCardFront')"
                :before-upload="beforeUpload">
                <img v-if="form.idCardFront" :src="form.idCardFront" class="avatar" />
                <div v-else class="upload-placeholder">
                  <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                  <p>点击上传</p>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证国徽面" prop="idCardBack">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :headers="getUploadHeaders()"
                :show-file-list="false"
                :on-success="res => handleSuccess(res, 'idCardBack')"
                :before-upload="beforeUpload">
                <img v-if="form.idCardBack" :src="form.idCardBack" class="avatar" />
                <div v-else class="upload-placeholder">
                  <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                  <p>点击上传</p>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开户银行" prop="bankName">
              <el-select v-model="form.bankName" placeholder="请选择收款银行" size="large" style="width: 100%">
                <el-option label="中国工商银行" value="中国工商银行" />
                <el-option label="中国农业银行" value="中国农业银行" />
                <el-option label="中国建设银行" value="中国建设银行" />
                <el-option label="招商银行" value="招商银行" />
                <el-option label="交通银行" value="交通银行" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行卡号" prop="bankCard">
              <el-input v-model="form.bankCard" placeholder="输入接收放款的借记卡号" size="large"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入本人手机号" size="large" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="用于接收还款提醒公告" size="large" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" size="large" @click="submit" :loading="submitting" class="submit-btn" color="#626aef" style="color:white;">
            确认提交并录入档案
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck, CircleClose, Plus } from '@element-plus/icons-vue'

const formRef = ref(null)
const profile = ref(null)
const loading = ref(false)
const submitting = ref(false)

const getUploadHeaders = () => {
  return { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }
}

const form = ref({
  realName: '',
  idCard: '',
  idCardFront: '',
  idCardBack: '',
  bankName: '',
  bankCard: '',
  phone: '',
  email: ''
})

const rules = {
  realName: [{ required: true, message: '不可为空', trigger: 'blur' }],
  idCard: [
    { required: true, message: '不可为空', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入18位合法身份证号', trigger: 'blur' }
  ],
  idCardFront: [{ required: true, message: '请上传', trigger: 'change' }],
  idCardBack: [{ required: true, message: '请上传', trigger: 'change' }],
  bankName: [{ required: true, message: '请选择', trigger: 'change' }],
  bankCard: [
    { required: true, message: '此处必填', trigger: 'blur' },
    { pattern: /^\d{16,19}$/, message: '银行卡号为16-19位数字', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '手机号不可为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的大陆手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '邮箱不可为空', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const handleSuccess = (res, field) => {
    if (res.code === 200) {
        form.value[field] = res.data
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error(res.msg || '上传失败')
    }
}

const beforeUpload = (file) => {
  const isValid = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  if (!isValid) ElMessage.error('仅仅支持 JPG/PNG/WEBP 图像！')
  return isValid
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.post('/kyc/submit', form.value)
        ElMessage.success('四要素资料与影像证件已空投至人工核查席！')
        loadData()
      } finally {
        submitting.value = false
      }
    }
  })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/kyc/my')
    profile.value = res.data
  } finally {
    loading.value = false
  }
}

const reSubmit = () => {
  if (profile.value) {
    form.value = {
      realName: profile.value.realName || '',
      idCard: profile.value.idCard || '',
      idCardFront: profile.value.idCardFront || '',
      idCardBack: profile.value.idCardBack || '',
      bankName: profile.value.bankName || '',
      bankCard: profile.value.bankCard || '',
      phone: profile.value.phone || '',
      email: profile.value.email || ''
    }
    profile.value = null
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.flex-center {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: calc(100vh - 120px);
    padding: 30px;
}

.kyc-card {
    width: 100%;
    max-width: 800px;
    background: rgba(30, 41, 59, 0.7);
    border: 1px solid rgba(255,255,255,0.1);
    box-shadow: 0 8px 32px rgba(0,0,0,0.3);
    border-radius: 16px;
    padding: 20px;
    color: #fff;
}
.card-header { text-align: center; margin-bottom: 30px; }
.card-header h2 { font-size: 26px; color: #fff; margin-bottom: 8px;}
.card-header p { color: #94a3b8; font-size: 14px;}

.result-box {
    text-align: center; padding: 40px 0; color: #fff;
}
.result-box .icon { font-size: 60px; margin-bottom: 20px; }
.warning-icon { color: #e6a23c; }
.success-icon { color: #67c23a; }
.danger-icon { color: #f56c6c; }

:deep(.el-form-item__label) { color: #cbd5e0 !important; font-weight: bold;}
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { background: rgba(0,0,0,0.3) !important; box-shadow: 0 0 0 1px rgba(255,255,255,0.1) inset !important; }
:deep(.el-input__inner) { color: #fff !important; }

.submit-btn { width: 100%; margin-top: 20px; font-weight: bold; border-radius: 8px; }

/* 图像上传组件样式重塑 */
.avatar-uploader {
  width: 100%;
  display: block;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed rgba(255,255,255,0.3);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
  background: rgba(0,0,0,0.2);
  width: 100%;
  height: 160px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}
.avatar-uploader :deep(.el-upload:hover) { border-color: #626aef; }

/* 加号 + 提示文字的复合占位区 */
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #8c939d;
}
.upload-placeholder .avatar-uploader-icon {
  font-size: 36px;
  color: #8c939d;
}
.upload-placeholder p {
  font-size: 12px;
  margin: 0;
  color: #94a3b8;
}
.avatar { width: 100%; height: 100%; object-fit: contain; display: block; }
</style>
