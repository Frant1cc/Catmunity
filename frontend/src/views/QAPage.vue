<script setup lang="ts">
</script>

<template>
  <div class="qa-page">
    <div class="qa-list">
      <div class="qa-card">
        <div class="qa-q">Q: 猫咪最近掉毛严重是什么原因？</div>
        <div class="qa-a">A: 可能是因为换季、营养不良或者皮肤病。建议补充维生素，带去检查一下。</div>
        <div class="qa-meta">
          <span>👀 234 浏览</span>
          <span>💬 12 回答</span>
        </div>
      </div>
      <div class="qa-card">
        <div class="qa-q">Q: 猫咪多大可以绝育？</div>
        <div class="qa-a">A: 一般建议6-8个月大的时候进行绝育手术，具体要咨询兽医。</div>
        <div class="qa-meta">
          <span>👀 567 浏览</span>
          <span>💬 23 回答</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.qa-page {
  padding: var(--space-lg);
}

.qa-card {
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--color-border);
}

.qa-q {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-md);
}

.qa-a {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  padding: var(--space-md);
  background: var(--color-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-md);
}

.qa-meta {
  display: flex;
  gap: var(--space-lg);
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
