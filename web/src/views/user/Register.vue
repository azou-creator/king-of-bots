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
      <h2 class="auth-title">注册 King Of Bots</h2>
      <p class="auth-sub">创建账号，开启你的对战之旅</p>

      <a-form
        class="auth-form"
        :model="data"
        layout="vertical"
        autocomplete="off"
        @finish="submit"
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
        <a-form-item
          label="确认密码"
          name="confirmPassword"
          :rules="[
            { required: true, message: '请再次输入密码' },
            { validator: validateConfirm },
          ]"
        >
          <a-input-password
            v-model:value="data.confirmPassword"
            placeholder="请再次输入密码"
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
          注 册
        </a-button>
      </a-form>

      <div class="auth-footer">
        已有账号？<Link to="/login">直接登录</Link>
      </div>
    </div>
  </div>
</template>

<script setup>
import Link from "@/components/Link.vue";
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { register } from "@/api/auth";

const data = reactive({
  username: "",
  password: "",
  confirmPassword: "",
});
const errorMessage = ref("");
const router = useRouter();

const validateConfirm = async (_rule, value) => {
  if (value && value !== data.password) {
    throw new Error("两次输入的密码不一致");
  }
};

// @finish 在表单校验通过后才会触发
const submit = () => {
  errorMessage.value = "";
  register(data)
    .then(() => {
      message.success("注册成功，请登录");
      router.push({ name: "Login" });
    })
    .catch((e) => {
      // 失败响应经 http.js 拦截器转为 Error(message)，而非 axios error
      errorMessage.value = e?.message || "注册失败";
    });
};
</script>

<style lang="scss" scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 5vh 16px 40px;
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
