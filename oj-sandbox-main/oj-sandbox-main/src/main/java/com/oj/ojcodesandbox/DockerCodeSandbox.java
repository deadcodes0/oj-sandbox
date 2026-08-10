package com.oj.ojcodesandbox;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.oj.ojcodesandbox.config.DockerSandboxProperties;
import com.oj.ojcodesandbox.enums.LanguageEnum;
import com.oj.ojcodesandbox.util.DockerImageUtil;
import com.oj.ojcodesandbox.model.CodeFile;
import com.oj.ojcodesandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.model.ExecuteCodeResponse;
import com.oj.ojcodesandbox.model.ExecuteMessage;
import com.oj.ojcodesandbox.model.JudgeInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 语言感知的 Docker 代码沙箱。
 * 编译放在一个容器，每个测试用例单独用一个容器运行；输入通过 stdin 重定向注入。
 * 根文件系统只读，/app、/in 只读挂载，唯一可写目录是有容量上限的 /tmp tmpfs。
 */
@Component
public class DockerCodeSandbox implements CodeSandbox {

    private final DockerSandboxProperties properties;

    public DockerCodeSandbox(DockerSandboxProperties properties) {
        this.properties = properties;
    }

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final long TIME_OUT = 5000L;

    /**
     * 编译超时：与运行超时拆开，放宽以容纳重型头文件（如 bits/stdc++.h）在受限容器里的首次全量编译
     */
    private static final long COMPILE_TIME_OUT = 15000L;

    private static final long MEMORY_LIMIT = 100 * 1000 * 1000L;

    private static final String TMPFS_SIZE = "64M";

    private static final String KEEP_ALIVE_CMD = "sleep";

    private static final String KEEP_ALIVE_SECONDS = "600";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String language = executeCodeRequest.getLanguage();
        LanguageEnum languageEnum = LanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            return getErrorResponse(new RuntimeException("不支持的编程语言：" + language));
        }

        DockerClient dockerClient = null;
        File baseDir = null;
        List<String> createdContainerIds = new ArrayList<>();
        try {
            dockerClient = DockerClientBuilder.getInstance().build();

            // 1. 保存源码（支持多文件），并建立 src / build / in 三个隔离目录
            baseDir = saveCodeToFiles(executeCodeRequest, languageEnum.getSourceFileName());
            String srcDir = baseDir.getAbsolutePath() + File.separator + "src";
            String buildDir = baseDir.getAbsolutePath() + File.separator + "build";
            String inputDir = baseDir.getAbsolutePath() + File.separator + "in";

            // 2. 确保镜像已存在（含镜像前缀解析）
            String dockerImage = DockerImageUtil.resolveImage(languageEnum.getDockerImage(),
                    properties.getRegistry().getMirror());
            ensureImage(dockerClient, dockerImage);

            // 3. 编译容器：镜像源码只读挂载到 /code，编译产物写 /app（宿主 build 目录）
            String compileContainerId = createAndStartContainer(dockerClient, dockerImage,
                    new Bind(srcDir, new Volume("/code"), AccessMode.ro),
                    new Bind(buildDir, new Volume("/app")));
            createdContainerIds.add(compileContainerId);
            ExecuteMessage compileMessage = compileFile(dockerClient, compileContainerId, languageEnum.getCompileCmd());
            removeContainer(dockerClient, compileContainerId);
            createdContainerIds.remove(compileContainerId);
            if (compileMessage.getExitValue() != 0) {
                return buildCompileErrorResponse(compileMessage);
            }

            // 4. 每个测试用例单独一个运行容器
            List<String> inputList = executeCodeRequest.getInputList() == null
                    ? Collections.emptyList() : executeCodeRequest.getInputList();
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (int i = 0; i < inputList.size(); i++) {
                // 写输入文件（供容器内 sh -c 重定向到程序 stdin）
                String inputFilePath = inputDir + File.separator + i + ".in";
                FileUtil.writeString(inputList.get(i), inputFilePath, StandardCharsets.UTF_8);

                // 运行容器：build 与 in 目录均只读挂载
                String runContainerId = createAndStartContainer(dockerClient, dockerImage,
                        new Bind(buildDir, new Volume("/app"), AccessMode.ro),
                        new Bind(inputDir, new Volume("/in"), AccessMode.ro));
                createdContainerIds.add(runContainerId);
                ExecuteMessage executeMessage = runFile(dockerClient, runContainerId, languageEnum.getRunCmd(), i);
                removeContainer(dockerClient, runContainerId);
                createdContainerIds.remove(runContainerId);
                executeMessageList.add(executeMessage);
                // 超时或运行错误时中止后续用例
                if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                    break;
                }
            }

            // 5. 汇总输出
            return getOutputResponse(executeMessageList);
        } catch (Exception e) {
            return getErrorResponse(e);
        } finally {
            // 6. 清理容器与代码目录
            if (dockerClient != null) {
                for (String containerId : createdContainerIds) {
                    removeContainer(dockerClient, containerId);
                }
            }
            if (baseDir != null) {
                deleteFile(baseDir);
            }
        }
    }

    /**
     * 1. 保存源码并建立 src / build / in 三个隔离目录。支持一次提交多个文件（codeList），
     * 否则回退为单个 code 文件。返回代码根目录（便于统一清理）。
     */
    private File saveCodeToFiles(ExecuteCodeRequest executeCodeRequest, String fallbackFileName) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        File baseDir = new File(globalCodePathName + File.separator + UUID.randomUUID());
        File srcDir = new File(baseDir, "src");
        File buildDir = new File(baseDir, "build");
        File inputDir = new File(baseDir, "in");
        FileUtil.mkdir(srcDir.getAbsolutePath());
        FileUtil.mkdir(buildDir.getAbsolutePath());
        FileUtil.mkdir(inputDir.getAbsolutePath());

        List<CodeFile> codeList = executeCodeRequest.getCodeList();
        if (CollectionUtil.isNotEmpty(codeList)) {
            for (CodeFile codeFile : codeList) {
                // 只取文件名，防止路径穿越
                String safeName = codeFile.getName() == null ? "" : new File(codeFile.getName()).getName();
                if (StrUtil.isBlank(safeName)) {
                    throw new RuntimeException("源码文件名不能为空");
                }
                FileUtil.writeString(codeFile.getContent(),
                        new File(srcDir, safeName).getAbsolutePath(), StandardCharsets.UTF_8);
            }
        } else {
            FileUtil.writeString(executeCodeRequest.getCode(),
                    new File(srcDir, fallbackFileName).getAbsolutePath(), StandardCharsets.UTF_8);
        }
        return baseDir;
    }

    /**
     * 2. 检查本地镜像，不存在则拉取
     */
    private void ensureImage(DockerClient dockerClient, String image) {
        if (DockerImageUtil.imageExists(dockerClient, image)) {
            System.out.println("镜像 " + image + " 已存在，跳过拉取");
            return;
        }
        DockerImageUtil.pullWithTimeout(dockerClient, image, properties.getRegistry().getPullTimeout());
    }

    /**
     * 3. 创建并启动容器。根文件系统只读，/tmp 为有容量上限的 tmpfs，容器用 sleep 保活以供 exec。
     */
    private String createAndStartContainer(DockerClient dockerClient, String image, Bind... binds) {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(MEMORY_LIMIT);        // 内存限制
        hostConfig.withMemorySwap(0L);              // 关闭交换内存
        hostConfig.withCpuCount(1L);                // CPU 限制
        hostConfig.withTmpFs(Collections.singletonMap("/tmp", "rw,size=" + TMPFS_SIZE)); // 磁盘有上限
        hostConfig.setBinds(binds);//
        CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)          // 关闭网络
                .withReadonlyRootfs(true)           // 根文件系统只读
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withCmd(KEEP_ALIVE_CMD, KEEP_ALIVE_SECONDS)
                .exec();
        String containerId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    /**
     * 4. 在容器内编译代码
     */
    private ExecuteMessage compileFile(DockerClient dockerClient, String containerId, String compileCmd) {
        // 用 sh -c 包裹，支持通配符（如 /code/*.java）展开
        String[] cmdArray = new String[]{"sh", "-c", compileCmd};
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withEnv(Collections.singletonList("HOME=/tmp"))// 设置 HOME 环境变量
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();
        String execId = execCreateCmdResponse.getId();

        StringBuilder stdout = new StringBuilder();// 保存编译结果
        StringBuilder stderr = new StringBuilder();// 保存编译错误
        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                StreamType streamType = frame.getStreamType();
                if (StreamType.STDERR.equals(streamType)) {
                    stderr.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                } else {
                    stdout.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                }
                super.onNext(frame);
            }
        };

        boolean completed;
        try {
            completed = dockerClient.execStartCmd(execId)
                    .exec(callback)
                    .awaitCompletion(COMPILE_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("编译执行异常");
            throw new RuntimeException(e);
        }

        ExecuteMessage executeMessage = new ExecuteMessage();
        if (!completed) {
            executeMessage.setExitValue(-1);
            executeMessage.setErrorMessage("编译超时(compile timeout)");
            return executeMessage;
        }
        Integer exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCode();
        executeMessage.setExitValue(exitCode == null ? -1 : exitCode);
        executeMessage.setMessage(stdout.toString());
        executeMessage.setErrorMessage(stderr.toString());
        return executeMessage;
    }

    /**
     * 5. 在容器内执行单个测试用例：把输入文件重定向到程序 stdin
     */
    private ExecuteMessage runFile(DockerClient dockerClient, String containerId, String runCmd, int caseIndex) {
        String stdinPath = "/in/" + caseIndex + ".in";
        String shellCmd = runCmd + " < " + stdinPath;
        String[] cmdArray = new String[]{"sh", "-c", shellCmd};

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withEnv(Collections.singletonList("HOME=/tmp"))
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();
        String execId = execCreateCmdResponse.getId();

        // 逐帧累加输出，避免只保留最后一帧
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        final boolean[] timedOut = {true};
        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onComplete() {
                timedOut[0] = false;
                super.onComplete();
            }

            @Override
            public void onNext(Frame frame) {
                StreamType streamType = frame.getStreamType();
                if (StreamType.STDERR.equals(streamType)) {
                    stderr.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                } else {
                    stdout.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                }
                super.onNext(frame);
            }
        };

        // 内存统计：容器级 usage 减去页缓存（inactive_file），换算为 KB
        final long[] maxMemoryKb = {0L};
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        statsCmd.exec(new ResultCallback<Statistics>() {
            @Override
            public void onNext(Statistics statistics) {
                maxMemoryKb[0] = Math.max(maxMemoryKb[0], getWorkingSetKb(statistics));
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public void onStart(Closeable closeable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        long time = 0L;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            dockerClient.execStartCmd(execId)
                    .exec(callback)
                    .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
        } catch (InterruptedException e) {
            System.out.println("程序执行异常");
            throw new RuntimeException(e);
        } finally {
            statsCmd.close();
        }

        ExecuteMessage executeMessage = new ExecuteMessage();
        executeMessage.setTime(time);
        executeMessage.setMemory(maxMemoryKb[0]);
        if (timedOut[0]) {
            executeMessage.setErrorMessage("程序执行超时(timeout)");
            // 杀掉容器内的失控进程，避免继续占用资源
            try {
                dockerClient.killContainerCmd(containerId).exec();
            } catch (Exception e) {
                System.out.println("超时后杀容器失败：" + e.getMessage());
            }
        } else {
            Integer exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCode();
            executeMessage.setExitValue(exitCode == null ? -1 : exitCode);
            executeMessage.setMessage(stdout.toString());
            executeMessage.setErrorMessage(stderr.toString());
            if (exitCode != null && exitCode != 0 && StrUtil.isBlank(executeMessage.getErrorMessage())) {
                executeMessage.setErrorMessage("程序运行错误(runtime error)，退出码：" + exitCode);
            }
        }
        return executeMessage;
    }

    /**
     * 计算容器当前工作集内存（usage - inactive_file，KB），过滤页缓存使统计更接近进程真实占用
     */
    private long getWorkingSetKb(Statistics statistics) {
        MemoryStatsConfig memoryStats = statistics.getMemoryStats();
        if (memoryStats == null || memoryStats.getUsage() == null) {
            return 0L;
        }
        long usage = memoryStats.getUsage();
        long inactiveFile = 0L;
        StatsConfig statsConfig = memoryStats.getStats();
        if (statsConfig != null && statsConfig.getInactiveFile() != null) {
            inactiveFile = statsConfig.getInactiveFile();
        }
        return Math.max(0L, (usage - inactiveFile) / 1024);
    }

    /**
     * 6. 收集整理输出结果
     */
    private ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超限
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            if (executeMessage.getTime() != null) {
                maxTime = Math.max(maxTime, executeMessage.getTime());
            }
            if (executeMessage.getMemory() != null) {
                maxMemory = Math.max(maxMemory, executeMessage.getMemory());
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 编译错误响应（消息带 compile 便于判题服务映射为编译错误）
     */
    private ExecuteCodeResponse buildCompileErrorResponse(ExecuteMessage compileMessage) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        String detail = StrUtil.isNotBlank(compileMessage.getErrorMessage())
                ? compileMessage.getErrorMessage() : compileMessage.getMessage();
        executeCodeResponse.setMessage("编译错误(compile): " + detail);
        executeCodeResponse.setStatus(3);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

    /**
     * 7. 删除代码目录
     */
    private boolean deleteFile(File baseDir) {
        if (baseDir != null) {
            boolean del = FileUtil.del(baseDir.getAbsolutePath());
            System.out.println("删除代码目录" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 尽力删除容器（force 会先杀再删）
     */
    private void removeContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            System.out.println("删除容器失败：" + e.getMessage());
        }
    }

    /**
     * 获取错误响应
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
