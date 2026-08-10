import router from "@/router";
import store from "@/store";
import ACCESS_ENUM from "@/access/accessEnum";
import checkAccess from "@/access/checkAccess";

/**
 * 全局路由守卫：根据路由 meta.access 校验访问权限。
 * 未登录 → 跳登录页；已登录但权限不足 → 跳无权限页。
 */
router.beforeEach(async (to, from, next) => {
  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN;
  // 公共页面直接放行，不额外请求登录态
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    next();
    return;
  }
  // 需要登录的页面：未拿到用户角色时，从后端拉取登录态
  if (!store.state.user.loginUser?.userRole) {
    await store.dispatch("user/getLoginUser");
  }
  const loginUser = store.state.user.loginUser;
  if (
    (loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN) === ACCESS_ENUM.NOT_LOGIN
  ) {
    // 未登录，跳转登录页并记录来源，登录后可跳回
    next(`/user/login?redirect=${to.fullPath}`);
    return;
  }
  if (!checkAccess(loginUser, needAccess)) {
    // 已登录但权限不足
    next("/noAuth");
    return;
  }
  next();
});
