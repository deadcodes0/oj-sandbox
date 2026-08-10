<template>
  <div id="userRegisterView">
    <h2 style="margin-bottom: 16px">用户注册</h2>
    <a-form
      style="max-width: 480px; margin: 0 auto"
      label-align="left"
      auto-label-width
      :model="form"
      :rules="rules"
      @submit="handleSubmit"
    >
      <a-form-item field="userAccount" label="账号">
        <a-input v-model="form.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item field="userPassword" label="密码">
        <a-input-password
          v-model="form.userPassword"
          placeholder="请输入密码（不少于 8 位）"
        />
      </a-form-item>
      <a-form-item field="checkPassword" label="确认密码">
        <a-input-password
          v-model="form.checkPassword"
          placeholder="请再次输入密码"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 120px">
          注册
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import {
  UserControllerService,
  UserRegisterRequest,
} from "../../../generated/user";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";

const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
} as UserRegisterRequest);

const rules = {
  userAccount: [{ required: true, message: "请输入账号" }],
  userPassword: [
    { required: true, message: "请输入密码" },
    { minLength: 8, message: "密码不能少于 8 位" },
  ],
  checkPassword: [
    { required: true, message: "请再次输入密码" },
    {
      validator: (value: string, callback: (error?: string) => void) => {
        if (value !== form.userPassword) {
          callback("两次输入的密码不一致");
        } else {
          callback();
        }
      },
    },
  ],
};

const router = useRouter();

const handleSubmit = async () => {
  const res = await UserControllerService.userRegisterUsingPost({
    ...form,
  });
  if (res.code === 0) {
    message.success("注册成功，请登录");
    router.push({
      path: "/user/login",
    });
  } else {
    message.error("注册失败，" + res.message);
  }
};
</script>
