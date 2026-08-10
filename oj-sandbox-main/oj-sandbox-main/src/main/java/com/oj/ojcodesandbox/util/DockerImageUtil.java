package com.oj.ojcodesandbox.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 镜像处理工具：名称解析、存在性检查、带超时的拉取。
 */
public final class DockerImageUtil {

    private DockerImageUtil() {
    }

    /**
     * 若配置了镜像前缀则拼上；未配置则原样返回（走 daemon 的 registry-mirrors）。
     */
    public static String resolveImage(String image, String mirror) {
        if (mirror == null || mirror.trim().isEmpty()) {
            return image;
        }
        return mirror.trim() + "/" + image;
    }

    /**
     * 检查本地是否存在该镜像。
     */
    public static boolean imageExists(DockerClient dockerClient, String image) {
        List<Image> imageList = dockerClient.listImagesCmd().withShowAll(true).exec();
        return imageList.stream()
                .map(Image::getRepoTags)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .anyMatch(tag -> tag.equals(image));
    }

    /**
     * 拉取镜像，带超时与可读日志。超时/中断时抛异常并提示检查加速器。
     */
    public static void pullWithTimeout(DockerClient dockerClient, String image, long timeoutMillis) {
        System.out.println("开始拉取镜像：" + image);
        PullImageResultCallback callback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像：" + item.getStatus());
                super.onNext(item);
            }
        };
        boolean completed;
        try {
            completed = dockerClient.pullImageCmd(image)
                    .exec(callback)
                    .awaitCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("拉取镜像中断：" + image, e);
        }
        if (!completed) {
            throw new RuntimeException("拉取镜像超时（" + timeoutMillis + "ms）：" + image
                    + "。请检查 Docker 镜像加速器（/etc/docker/daemon.json 的 registry-mirrors），"
                    + "或先手动执行 docker pull " + image);
        }
        System.out.println("镜像 " + image + " 拉取完成");
    }
}