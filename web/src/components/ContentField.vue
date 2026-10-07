<template>
  <div class="content-field">
    <div class="content-card">
      <slot />
    </div>
  </div>
</template>

<script></script>

<style lang="scss" scoped>
.content-field {
  width: min(1080px, 92vw);
  margin: 26px auto 32px;
}

.content-card {
  background: var(--kob-panel);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--kob-radius-lg);
  box-shadow: var(--kob-shadow);
  padding: 24px 28px;
}

@media (max-width: 640px) {
  .content-card {
    padding: 18px 16px;
  }
}
</style>
