package com.oj.ojcodesandbox.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.oj.ojcodesandbox.enums.LanguageEnum;
import com.oj.ojcodesandbox.util.DockerImageUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动时后台预拉全部语言镜像，避免首次提交时现场拉取导致的超时。
 */
@Component
@Order(1)
public class DockerImagePrewarmRunner implements ApplicationRunner {

    private final DockerSandboxProperties properties;

    public DockerImagePrewarmRunner(DockerSandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getPrewarm().isEnabled()) {
            System.out.println("启动预拉镜像已关闭");
            return;
        }
        LanguageEnum[] languages = LanguageEnum.values();
        ExecutorService executor = Executors.newFixedThreadPool(languages.length);
        for (LanguageEnum language : languages) {
            String image = DockerImageUtil.resolveImage(language.getDockerImage(),
                    properties.getRegistry().getMirror());
            executor.submit(() -> prewarmOne(image));
        }
        executor.shutdown();
    }

    private void prewarmOne(String image) {
        try {
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            if (DockerImageUtil.imageExists(dockerClient, image)) {
                System.out.println("预拉镜像 " + image + " 已存在，跳过");
                return;
            }
            System.out.println("预拉镜像 " + image + " 开始");
            DockerImageUtil.pullWithTimeout(dockerClient, image, properties.getPrewarm().getTimeout());
            System.out.println("预拉镜像 " + image + " 完成");
        } catch (Exception e) {
            System.out.println("预拉镜像 " + image + " 失败：" + e.getMessage());
        }
    }
}