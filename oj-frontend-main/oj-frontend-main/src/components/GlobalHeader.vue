<template>
  <a-row id="globalHeader" align="center" :wrap="false">
    <a-col flex="auto">
      <a-menu
        mode="horizontal"
        :selected-keys="selectedKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item
          key="0"
          :style="{ padding: 0, marginRight: '38px' }"
          disabled
        >
          <div class="title-bar">
            <img class="logo" src="../assets/oj-logo.svg" />
            <div class="title">鱼 OJ</div>
          </div>
        </a-menu-item>
        <a-menu-item v-for="item in visibleRoutes" :key="item.path">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="auto" style="max-width: 260px">
      <template v-if="isLogin">
        <a-space>
          <span>{{ loginUser.userName }}</span>
          <a-button size="small" @click="doLogout">退出登录</a-button>
        </a-space>
      </template>
      <template v-else>
        <a-space>
          <a-button size="small" @click="toLogin">登录</a-button>
          <a-button size="small" @click="toRegister">注册</a-button>
        </a-space>
      </template>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { routes } from "../router/routes";
import { useRouter } from "vue-router";
import { computed, ref } from "vue";
import { useStore } from "vuex";
import checkAccess from "@/access/checkAccess";
import ACCESS_ENUM from "@/access/accessEnum";
import message from "@arco-design/web-vue/es/message";
import { UserControllerService } from "../../generated/user";

const router = useRouter();
const store = useStore();

// 当前登录用户
const loginUser = computed(() => store.state.user.loginUser);

// 是否已登录
const isLogin = computed(
  () =>
    loginUser.value?.userRole &&
    loginUser.value.userRole !== ACCESS_ENUM.NOT_LOGIN
);

// 展示在菜单的路由数组
const visibleRoutes = computed(() => {
  return routes.filter((item, index) => {
    if (item.meta?.hideInMenu) {
      return false;
    }
    // 根据权限过滤菜单
    if (
      !checkAccess(store.state.user.loginUser, item?.meta?.access as string)
    ) {
      return false;
    }
    return true;
  });
});

// 默认主页
const selectedKeys = ref(["/"]);

// 路由跳转后，更新选中的菜单项
router.afterEach((to, from, failure) => {
  selectedKeys.value = [to.path];
});

const doMenuClick = (key: string) => {
  router.push({
    path: key,
  });
};

const toLogin = () => {
  router.push({
    path: "/user/login",
  });
};

const toRegister = () => {
  router.push({
    path: "/user/register",
  });
};

const doLogout = async () => {
  const res = await UserControllerService.userLogoutUsingPost();
  if (res.code === 0) {
    // 重置本地登录态
    store.commit("user/updateUser", {
      userName: "未登录",
      userRole: ACCESS_ENUM.NOT_LOGIN,
    });
    message.success("已退出登录");
    router.push({
      path: "/",
    });
  } else {
    message.error("退出失败，" + res.message);
  }
};
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: #444;
  margin-left: 16px;
}

.logo {
  height: 48px;
}
</style>
