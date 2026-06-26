<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { marked } from 'marked'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import { aiApi } from '@/api'
import type { SessionInfo, SessionList } from '@/api/types'

const sessions = ref<SessionList[]>([])
const loading = ref(false)

/* marked 配置 */
marked.setOptions({
  gfm: true,
  breaks: true, // 把单个 \n 当成 <br>，流式文本体验更好
})

/** 把消息文本渲染成 HTML（带 XSS 过滤） */
function renderMarkdown(text: string): string {
  if (!text) return ''
  const html = marked.parse(text, { async: false }) as string
  // 简易 XSS 过滤：移除危险的标签和事件
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')
    .replace(/ on\w+="[^"]*"/gi, '')
    .replace(/ on\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

/* 详情弹窗 */
const showDetail = ref(false)
const detailSession = ref<SessionList | null>(null)
const messages = ref<SessionInfo[]>([])
const detailLoading = ref(false)

/* 新建会话 */
const showChat = ref(false)
const chatInput = ref('')
const chatFiles = ref<File[]>([])
const chatSending = ref(false)
const chatMessages = ref<{ role: 'user' | 'ai'; text: string; files?: string[] }[]>([])
const chatListRef = ref<HTMLDivElement | null>(null)
const chatAbort = ref<(() => void) | null>(null)
const chatFileInputRef = ref<HTMLInputElement | null>(null)
const chatSessionId = ref<string>('')
const chatMessageId = ref<string>('')
const isContinuing = computed(() => !!chatSessionId.value)

function genId(): string {
  // 优先用 crypto.randomUUID，兜底用时间戳+随机
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function formatTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 判断消息是用户发的还是 AI 回复的 */
function isUserMessage(m: SessionInfo): boolean {
  const t = (m.messageType || '').toLowerCase()
  return t === 'user' || t === 'human' || t === 'question'
}

const loadSessions = async () => {
  loading.value = true
  try {
    sessions.value = await aiApi.getSessions()
  } catch {
    sessions.value = []
  } finally {
    loading.value = false
  }
}

const openDetail = async (s: SessionList) => {
  detailSession.value = s
  showDetail.value = true
  messages.value = []
  detailLoading.value = true
  try {
    messages.value = await aiApi.getSessionDetail(s.sessionId)
  } catch {
    messages.value = []
  } finally {
    detailLoading.value = false
  }
}

const closeDetail = () => {
  showDetail.value = false
  detailSession.value = null
  messages.value = []
}

/** 从详情弹窗跳到续接对话 */
const continueFromDetail = () => {
  if (!detailSession.value) return
  const sid = detailSession.value.sessionId
  closeDetail()
  openChat(sid)
}

/* ==================== 删除会话 ==================== */

const deletingSessionId = ref<string | null>(null)

const handleDeleteSession = async (s: SessionList) => {
  try {
    await showConfirmDialog({
      title: '删除会话',
      message: `确定删除"${s.title || '未命名会话'}"吗？此操作不可恢复。`,
      confirmButtonText: '删除',
      confirmButtonColor: 'var(--color-error)',
    })
  } catch {
    return // 取消
  }
  deletingSessionId.value = s.sessionId
  try {
    await aiApi.clearSession(s.sessionId)
    // 从列表移除
    sessions.value = sessions.value.filter((x) => x.sessionId !== s.sessionId)
    showSuccessToast('已删除')
  } catch {
    // 拦截器已 toast
  } finally {
    deletingSessionId.value = null
  }
}

/* ==================== 新建对话 ==================== */

const openChat = (existingSessionId?: string) => {
  showChat.value = true
  chatInput.value = ''
  chatFiles.value = []
  chatMessages.value = []
  chatSessionId.value = existingSessionId || ''
  chatMessageId.value = ''
  // 续接会话：把历史消息拉回来展示
  if (existingSessionId) {
    loadChatHistory(existingSessionId)
  }
}

/** 加载历史消息到当前聊天窗口 */
const loadChatHistory = async (sessionId: string) => {
  try {
    const list = await aiApi.getSessionDetail(sessionId)
    chatMessages.value = list.map((m) => ({
      role: isUserMessage(m) ? 'user' as const : 'ai' as const,
      text: m.content || '',
    }))
    nextTick(scrollToBottom)
  } catch {
    // 静默失败
  }
}

const closeChat = () => {
  // 关闭时中断流
  chatAbort.value?.()
  chatAbort.value = null
  showChat.value = false
  chatSending.value = false
  chatSessionId.value = ''
  chatMessageId.value = ''
}

const triggerPickFiles = () => {
  if (chatSending.value) return
  chatFileInputRef.value?.click()
}

const onFilesPicked = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  chatFiles.value.push(...Array.from(input.files))
  input.value = ''
}

const removeFile = (idx: number) => {
  chatFiles.value.splice(idx, 1)
}

const scrollToBottom = async () => {
  await nextTick()
  const el = chatListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const sendChat = async () => {
  const content = chatInput.value.trim()
  if (!content && !chatFiles.value.length) {
    showToast('请输入内容或上传文件')
    return
  }
  chatSending.value = true

  // 用户消息先入列
  const userMsg = {
    role: 'user' as const,
    text: content,
    files: chatFiles.value.map((f) => f.name),
  }
  chatMessages.value.push(userMsg)

  // AI 占位
  const aiMsg = { role: 'ai' as const, text: '' }
  chatMessages.value.push(aiMsg)

  // 清空输入
  const payloadContent = content
  const payloadFiles = [...chatFiles.value]
  chatInput.value = ''
  chatFiles.value = []

  // 生成 sessionId / messageId 并记录（停止时要传给后端）
  // 如果是续接会话，sessionId 用原来的；新会话才生成
  const sid = chatSessionId.value || genId()
  const mid = genId()
  chatSessionId.value = sid
  chatMessageId.value = mid

  scrollToBottom()

  chatAbort.value = await aiApi.sendMessage(
    {
      sessionId: sid,
      messageId: mid,
      content: payloadContent,
      files: payloadFiles,
    },
    {
      onChunk: (text) => {
        aiMsg.text += text
        scrollToBottom()
      },
      onDone: () => {
        chatSending.value = false
        chatAbort.value = null
        showSuccessToast('对话完成')
      },
      onError: (err) => {
        chatSending.value = false
        chatAbort.value = null
        if (!aiMsg.text) {
          // 没收到内容就报错，移除占位
          chatMessages.value.pop()
        }
        showToast(`对话失败：${err.message || '未知错误'}`)
      },
    },
  )
}

const stopChat = async () => {
  // 1) 中断前端 SSE 流
  chatAbort.value?.()
  chatAbort.value = null
  chatSending.value = false
  // 2) 通知后端停止生成
  if (chatSessionId.value && chatMessageId.value) {
    try {
      await aiApi.stopMessage(chatSessionId.value, chatMessageId.value)
    } catch {
      // 静默失败
    }
  }
}

onMounted(loadSessions)
</script>

<template>
  <div class="qa-page">
    <div class="qa-header">
      <div>
        <h2 class="qa-title">AI 问答助手</h2>
        <p class="qa-subtitle">共 {{ sessions.length }} 个历史会话</p>
      </div>
      <van-button
        type="primary"
        size="small"
        round
        class="qa-new-btn"
        @click="openChat()"
      >
        + 新建对话
      </van-button>
    </div>

    <div v-if="loading" class="state">加载中...</div>

    <div v-else-if="!sessions.length" class="state">还没有历史会话，去提个问题吧~</div>

    <ul v-else class="session-list">
      <li
        v-for="s in sessions"
        :key="s.sessionId"
        class="session-card"
        @click="openDetail(s)"
      >
        <div class="session-icon">🤖</div>
        <div class="session-body">
          <div class="session-title">{{ s.title || '未命名会话' }}</div>
          <div class="session-meta">{{ formatTime(s.createdAt) }}</div>
        </div>
        <span
          class="session-delete-btn"
          :class="{ 'is-loading': deletingSessionId === s.sessionId }"
          @click.stop="handleDeleteSession(s)"
        >
          {{ deletingSessionId === s.sessionId ? '...' : '🗑' }}
        </span>
        <span class="session-arrow">›</span>
      </li>
    </ul>

    <!-- 会话详情弹窗 -->
    <van-popup
      v-model:show="showDetail"
      position="bottom"
      round
      closeable
      close-icon-position="top-right"
      :style="{ maxHeight: '85vh' }"
      class="qa-detail-popup"
      @closed="closeDetail"
    >
      <div v-if="detailSession" class="qa-detail">
        <div class="qa-detail-header">
          <h3 class="qa-detail-title">{{ detailSession.title || '会话详情' }}</h3>
          <p class="qa-detail-time">开始于 {{ formatTime(detailSession.createdAt) }}</p>
        </div>

        <div v-if="detailLoading" class="state">加载消息中...</div>

        <div v-else-if="!messages.length" class="state">该会话暂无消息</div>

        <ul v-else class="message-list">
          <li
            v-for="m in messages"
            :key="m.messageId"
            class="message-item"
            :class="{ 'is-user': isUserMessage(m), 'is-ai': !isUserMessage(m) }"
          >
            <div class="message-avatar">
              {{ isUserMessage(m) ? '👤' : '🤖' }}
            </div>
            <div class="message-bubble-wrap">
              <div class="message-bubble">{{ m.content }}</div>
              <div class="message-time">{{ formatTime(m.createdAt) }}</div>
            </div>
          </li>
        </ul>

        <div class="qa-detail-footer">
          <van-button
            type="primary"
            block
            round
            @click="continueFromDetail"
          >
            继续对话
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 新建对话弹窗 -->
    <van-popup
      v-model:show="showChat"
      position="bottom"
      :style="{ height: '90vh' }"
      class="chat-popup"
      :close-on-click-overlay="false"
      @closed="closeChat"
    >
      <div class="chat">
        <div class="chat-header">
          <span class="chat-close" @click="closeChat">‹ 关闭</span>
          <h3 class="chat-title">{{ isContinuing ? '继续对话' : '新建对话' }}</h3>
          <span class="chat-placeholder"></span>
        </div>

        <!-- 消息列表 -->
        <div ref="chatListRef" class="chat-list">
          <div v-if="!chatMessages.length" class="chat-empty">
            <div class="chat-empty-icon">🐱</div>
            <p>你好，我是猫猫知识助手</p>
            <p class="chat-empty-sub">可以上传猫咪照片或资料，让我帮你分析</p>
          </div>

          <div
            v-for="(m, idx) in chatMessages"
            :key="idx"
            class="chat-msg"
            :class="{ 'is-user': m.role === 'user', 'is-ai': m.role === 'ai' }"
          >
            <div class="chat-avatar">{{ m.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="chat-bubble-wrap">
              <div v-if="m.files?.length" class="chat-files">
                <span v-for="(f, i) in m.files" :key="i" class="chat-file-chip">📎 {{ f }}</span>
              </div>
              <div
                v-if="m.text && m.role === 'ai'"
                class="chat-bubble chat-md"
                v-html="renderMarkdown(m.text)"
              ></div>
              <div v-else-if="m.text" class="chat-bubble">{{ m.text }}</div>
              <span v-else-if="m.role === 'ai'" class="chat-typing">思考中<span class="dot">.</span><span class="dot">.</span><span class="dot">.</span></span>
            </div>
          </div>
        </div>

        <!-- 输入栏 -->
        <div class="chat-input-bar">
          <div v-if="chatFiles.length" class="chat-files-preview">
            <span
              v-for="(f, i) in chatFiles"
              :key="i"
              class="chat-file-chip"
            >
              📎 {{ f.name }}
              <span class="chat-file-remove" @click="removeFile(i)">×</span>
            </span>
          </div>
          <div class="chat-input-row">
            <span class="chat-input-icon" @click="triggerPickFiles">📎</span>
            <van-field
              v-model="chatInput"
              type="textarea"
              :rows="1"
              autosize
              placeholder="说点什么..."
              class="chat-field"
              @keyup.enter.ctrl="sendChat"
            />
            <van-button
              v-if="!chatSending"
              type="primary"
              size="small"
              :disabled="!chatInput.trim() && !chatFiles.length"
              @click="sendChat"
            >
              发送
            </van-button>
            <van-button
              v-else
              type="danger"
              size="small"
              @click="stopChat"
            >
              停止
            </van-button>
          </div>
          <input
            ref="chatFileInputRef"
            type="file"
            multiple
            hidden
            @change="onFilesPicked"
          />
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.qa-page {
  padding: var(--space-lg);
}

.qa-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
  gap: var(--space-md);
}

.qa-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

.qa-subtitle {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.qa-new-btn {
  flex-shrink: 0;
  padding: 0 16px;
  font-weight: 600;
}

.state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.session-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.session-card:not(:last-child) {
  border-bottom: 1px solid var(--color-divider);
}

.session-card:active {
  background: var(--color-secondary);
}

.session-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: var(--gradient-warm);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.session-body {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.session-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.session-arrow {
  flex-shrink: 0;
  font-size: 22px;
  color: var(--color-text-disabled);
  line-height: 1;
}

.session-delete-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--color-text-tertiary);
  border-radius: 50%;
  cursor: pointer;
  user-select: none;
  transition: all var(--transition-fast);
}

.session-delete-btn:active {
  background: rgba(255, 77, 79, 0.12);
  color: var(--color-error);
}

.session-delete-btn.is-loading {
  pointer-events: none;
  opacity: 0.5;
}

/* ==================== 会话详情弹窗 ==================== */

.qa-detail-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.qa-detail-popup :deep(.van-overlay) {
  background: var(--color-bg-overlay);
}

.qa-detail-popup :deep(.van-popup__close-icon) {
  top: 14px;
  right: 14px;
  z-index: 2;
  color: var(--color-text-tertiary);
}

.qa-detail {
  padding: 20px 16px 24px;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  max-height: 85vh;
  overflow-y: auto;
}

.qa-detail-header {
  text-align: center;
  margin-bottom: var(--space-md);
  padding: 0 36px var(--space-md);
  border-bottom: 1px solid var(--color-divider);
}

.qa-detail-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.qa-detail-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.message-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.message-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.message-item.is-user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}

.message-item.is-ai .message-avatar {
  background: var(--gradient-warm);
}

.message-bubble-wrap {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.message-item.is-user .message-bubble-wrap {
  align-items: flex-end;
}

.message-bubble {
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--color-secondary);
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.is-user .message-bubble {
  background: var(--color-primary);
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
  padding: 0 4px;
}

.qa-detail-footer {
  position: sticky;
  bottom: 0;
  background: var(--color-bg-page);
  padding: 12px 0 8px;
  margin-top: var(--space-md);
  border-top: 1px solid var(--color-divider);
}

/* ==================== 新建对话弹窗 ==================== */

.chat-popup :deep(.van-popup) {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-bg-page);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-divider);
  background: var(--color-bg-page);
}

.chat-close {
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  width: 60px;
}

.chat-placeholder {
  width: 60px;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
  padding: 60px 0;
  gap: 6px;
}

.chat-empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.chat-empty-sub {
  font-size: 12px;
  color: var(--color-text-disabled);
  margin: 0;
}

.chat-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.chat-msg.is-user {
  flex-direction: row-reverse;
}

.chat-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}

.chat-msg.is-ai .chat-avatar {
  background: var(--gradient-warm);
}

.chat-bubble-wrap {
  max-width: 78%;
  display: flex;
  flex-direction: column;
}

.chat-msg.is-user .chat-bubble-wrap {
  align-items: flex-end;
}

.chat-files {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
  justify-content: flex-end;
}

.chat-msg.is-ai .chat-files {
  justify-content: flex-start;
}

.chat-file-chip {
  font-size: 11px;
  color: var(--color-text-secondary);
  background: var(--color-bg-container);
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.chat-file-remove {
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}

.chat-bubble {
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--color-secondary);
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-msg.is-user .chat-bubble {
  background: var(--color-primary);
  color: #fff;
}

/* Markdown 渲染样式 */
.chat-md {
  white-space: normal;
}

.chat-md :deep(p) {
  margin: 0 0 6px;
}

.chat-md :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-md :deep(h1),
.chat-md :deep(h2),
.chat-md :deep(h3),
.chat-md :deep(h4) {
  margin: 8px 0 4px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.3;
}

.chat-md :deep(h1) { font-size: 18px; }
.chat-md :deep(h2) { font-size: 16px; }
.chat-md :deep(h3) { font-size: 15px; }
.chat-md :deep(h4) { font-size: 14px; }

.chat-md :deep(ul),
.chat-md :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}

.chat-md :deep(li) {
  margin: 2px 0;
}

.chat-md :deep(strong) {
  font-weight: 700;
  color: var(--color-primary-dark);
}

.chat-md :deep(em) {
  font-style: italic;
}

.chat-md :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  background: rgba(255, 140, 66, 0.12);
  color: var(--color-primary-dark);
  padding: 1px 6px;
  border-radius: 4px;
}

.chat-md :deep(pre) {
  background: var(--color-text-primary);
  color: #f5ebe0;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  margin: 6px 0;
}

.chat-md :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  font-size: 12px;
  white-space: pre;
}

.chat-md :deep(blockquote) {
  margin: 6px 0;
  padding: 4px 10px;
  border-left: 3px solid var(--color-primary);
  background: rgba(255, 140, 66, 0.06);
  color: var(--color-text-secondary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.chat-md :deep(a) {
  color: var(--color-primary);
  text-decoration: underline;
  word-break: break-all;
}

.chat-md :deep(table) {
  border-collapse: collapse;
  margin: 6px 0;
  width: 100%;
  font-size: 13px;
}

.chat-md :deep(th),
.chat-md :deep(td) {
  border: 1px solid var(--color-border);
  padding: 4px 8px;
  text-align: left;
}

.chat-md :deep(th) {
  background: var(--color-secondary);
  font-weight: 600;
}

.chat-md :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-divider);
  margin: 8px 0;
}

.chat-typing {
  display: inline-flex;
  gap: 2px;
  color: var(--color-text-tertiary);
}

.chat-typing .dot {
  animation: chat-blink 1.2s infinite;
  font-size: 18px;
  line-height: 1;
}

.chat-typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.chat-typing .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes chat-blink {
  0%, 60%, 100% { opacity: 0.3; }
  30% { opacity: 1; }
}

.chat-input-bar {
  border-top: 1px solid var(--color-divider);
  padding: 8px 12px env(safe-area-inset-bottom, 8px);
  background: var(--color-bg-page);
}

.chat-files-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.chat-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-input-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: var(--color-secondary);
  border-radius: 50%;
  cursor: pointer;
}

.chat-field {
  flex: 1;
  background: var(--color-secondary);
  border-radius: 18px;
  padding: 6px 12px !important;
}

.chat-field :deep(.van-field__control) {
  font-size: 14px;
  color: var(--color-text-primary);
  caret-color: var(--color-primary);
  max-height: 100px;
  min-height: 20px;
}

.chat-field :deep(.van-field__control::placeholder) {
  color: var(--color-text-disabled);
}
</style>
