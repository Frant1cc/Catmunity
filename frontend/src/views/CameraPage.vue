<script setup lang="ts">
</script>

<template>
  <div class="camera-page">
    <div class="camera-placeholder">
      <div class="camera-icon">📷</div>
      <p class="camera-text">记录猫咪精彩瞬间</p>
      <van-button type="primary" size="large" round>开始拍照</van-button>
    </div>
  </div>
</template>

<style scoped>
.camera-page {
  padding: var(--space-lg);
}

.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  text-align: center;
}

.camera-icon {
  font-size: 80px;
  margin-bottom: var(--space-xl);
}

.camera-text {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2xl);
}
</style>
