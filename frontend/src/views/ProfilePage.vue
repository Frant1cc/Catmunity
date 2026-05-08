<script setup lang="ts">
</script>

<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="avatar">🐱</div>
      <div class="profile-info">
        <h2 class="nickname">爱猫人士</h2>
        <p class="bio">养猫 3 年，喜欢分享猫咪日常</p>
      </div>
    </div>
    <div class="profile-stats">
      <div class="stat-item">
        <span class="stat-num">12</span>
        <span class="stat-label">猫咪</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">888</span>
        <span class="stat-label">粉丝</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">2.3k</span>
        <span class="stat-label">获赞</span>
      </div>
    </div>
    <van-cell-group inset class="profile-menu">
      <van-cell title="我的猫咪" is-link value="3只" />
      <van-cell title="收藏夹" is-link value="12个" />
      <van-cell title="浏览记录" is-link />
      <van-cell title="设置" is-link />
    </van-cell-group>
  </div>
</template>

<style scoped>
.profile-page {
  padding: var(--space-lg);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.avatar {
  width: 72px;
  height: 72px;
  background: var(--color-secondary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  border: 3px solid var(--color-primary);
}

.profile-info {
  flex: 1;
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

.bio {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
  padding: var(--space-lg) 0;
  margin-bottom: var(--space-lg);
  background: var(--color-bg-container);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.profile-menu {
  margin-top: var(--space-lg);
}
</style>
