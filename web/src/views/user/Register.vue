<template>
  <content-field>
    <a-form
        class="form"
        :model="data"
        autocomplete="off"
        @submit.prevent="submit"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
    >
      <a-form-item
          label="用户名"
          :rules="[{ required: true, message: 'Please input your username!' }]"
      >
        <a-input
            v-model:value="data.username"
            type="text"
            class="form-control"
            id="username"
            placeholder="请输入用户名"
        />
      </a-form-item>
      <a-form-item
          label="密码"
          :rules="[{ required: true, message: 'Please input your username!' }]"
      >
        <a-input
            v-model:value="data.password"
            type="password"
            class="form-control"
            id="password"
            placeholder="请输入密码"
        />
      </a-form-item>

      <a-form-item
          label="确认密码"
          :rules="[{ required: true, message: 'Please input your username!' }]"
      >
        <a-input
            v-model:value="data.confirmPassword"
            type="password"
            class="form-control"
            id="confirmpassword"
            placeholder="请再次输入密码"
        />
      </a-form-item>
      <a-button style="width: 100%" type="primary" html-type="submit">
        注册
      </a-button>
    </a-form>
  </content-field>
</template>

<script setup>
import ContentField from "@/components/ContentField.vue";
import {reactive, ref} from "vue";
import {register} from "@/api/auth";
import router from "@/router/routes.js";

const data = reactive({
  username: "",
  password: "",
  confirmPassword: "",
});
const errorMessage = ref("");

const submit = () => {
  register(data)
      .then((res) => {
        console.log(res);
        router.push({name: "Login"});
      })
      .catch((e) => {
        console.log(e.response);
        errorMessage.value = e.response.data.message;
      });
};
</script>

<style lang="scss" scoped>
.ant-form {
  width: 400px;
  margin: 0 auto;
}
</style>
