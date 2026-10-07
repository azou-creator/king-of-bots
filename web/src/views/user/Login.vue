<template>
  <div class="auth-page">
    <div class="auth-card">
      <svg class="auth-mark" viewBox="0 0 32 32" fill="none" aria-hidden="true">
        <path
          d="M7 23c0-5.5 4.5-5.5 9-5.5s9 0 9-5.5-4.5-5.5-9-5.5"
          stroke="currentColor"
          stroke-width="3.2"
          stroke-linecap="round"
        />
        <circle cx="7.6" cy="23" r="2.8" fill="currentColor" />
      </svg>
      <h2 class="auth-title">登录 King Of Bots</h2>
      <p class="auth-sub">与全服玩家实时对战</p>

      <a-form
        class="auth-form"
        :model="data"
        layout="vertical"
        autocomplete="off"
        @finish="login"
      >
        <a-form-item
          label="用户名"
          name="username"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input
            v-model:value="data.username"
            placeholder="请输入用户名"
            size="large"
          />
        </a-form-item>
        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="data.password"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>

        <a-alert
          v-if="errorMessage"
          type="error"
          :message="errorMessage"
          show-icon
          class="auth-error"
        />

        <a-button
          type="primary"
          html-type="submit"
          block
          size="large"
          class="auth-submit"
        >
          登 录
        </a-button>
      </a-form>

      <div class="auth-footer">
        还没有账号？<Link to="/register">立即注册</Link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import Link from "@/components/Link.vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";

const data = reactive({
  username: "",
  password: "",
});

const errorMessage = ref("");
const store = useStore();
const router = useRouter();

// @finish 在表单校验通过后才会触发
const login = async () => {
  errorMessage.value = "";
  if (!data.username || !data.password) {
    errorMessage.value = "请输入用户名和密码";
    return;
  }
  try {
    await store.dispatch("login", data);
    router.push({ name: "Home" });
  } catch (e) {
    errorMessage.value = e?.message || "用户名或密码错误";
  }
};
</script>

<style lang="scss" scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 7vh 16px 40px;
}

.auth-card {
  width: min(420px, 100%);
  background: var(--kob-panel);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 20px;
  box-shadow: var(--kob-shadow);
  padding: 34px 34px 26px;
  text-align: center;
}

.auth-mark {
  width: 40px;
  height: 40px;
  color: var(--kob-accent);
}

.auth-title {
  margin: 12px 0 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--kob-text);
}

.auth-sub {
  margin: 6px 0 24px;
  color: var(--kob-text-secondary);
  font-size: 0.92rem;
}

.auth-form {
  text-align: left;
}

.auth-error {
  margin-bottom: 20px;
}

.auth-submit {
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 4px;
}

.auth-footer {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--kob-border);
  color: var(--kob-text-secondary);
  font-size: 0.9rem;
}
</style>
