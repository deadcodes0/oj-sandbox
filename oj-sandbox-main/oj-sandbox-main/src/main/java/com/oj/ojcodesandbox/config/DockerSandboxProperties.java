package com.oj.ojcodesandbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 沙箱相关配置：镜像源与启动预拉。
 */
@Component
@ConfigurationProperties(prefix = "codesandbox")
public class DockerSandboxProperties {

    private Registry registry = new Registry();

    private Prewarm prewarm = new Prewarm();

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Prewarm getPrewarm() {
        return prewarm;
    }

    public void setPrewarm(Prewarm prewarm) {
        this.prewarm = prewarm;
    }

    public static class Registry {

        /**
         * 可选镜像前缀。留空则走 Docker daemon 的 registry-mirrors；
         * 非空则作为镜像名前缀（如 docker.m.daocloud.io），按需在代码与启动预拉中生效。
         */
        private String mirror = "";

        /**
         * 单次拉取镜像超时（毫秒）
         */
        private long pullTimeout = 60000L;

        public String getMirror() {
            return mirror;
        }

        public void setMirror(String mirror) {
            this.mirror = mirror;
        }

        public long getPullTimeout() {
            return pullTimeout;
        }

        public void setPullTimeout(long pullTimeout) {
            this.pullTimeout = pullTimeout;
        }
    }

    public static class Prewarm {

        /**
         * 启动时是否后台预拉全部语言镜像
         */
        private boolean enabled = true;

        /**
         * 单个镜像预拉超时（毫秒）
         */
        private long timeout = 300000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}