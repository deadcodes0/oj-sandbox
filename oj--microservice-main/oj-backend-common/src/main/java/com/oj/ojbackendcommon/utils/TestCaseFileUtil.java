package com.oj.ojbackendcommon.utils;

import cn.hutool.core.io.FileUtil;
import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.exception.BusinessException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判题用例文件读写工具
 *
 * <p>目录布局：{basePath}/{questionId}/in_{i}.txt、out_{i}.txt（i 从 0 起）。
 * 不依赖 model，只操作字符串列表，供题目服务写入、判题服务读取。
 */
public class TestCaseFileUtil {

    private static final String INPUT_PREFIX = "in_";

    private static final String OUTPUT_PREFIX = "out_";

    private static final String SUFFIX = ".txt";

    private static final Pattern INPUT_PATTERN = Pattern.compile("^in_(\\d+)\\.txt$");

    private TestCaseFileUtil() {
    }

    /**
     * 写入一组用例文件（输入输出数量必须一致）
     */
    public static void writeCaseFiles(String basePath, long questionId, List<String> inputs, List<String> outputs) {
        validateQuestionId(questionId);
        if (inputs == null || outputs == null || inputs.size() != outputs.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用例输入输出数量不一致");
        }
        File dir = FileUtil.mkdir(dirPath(basePath, questionId));
        for (int i = 0; i < inputs.size(); i++) {
            File inFile = new File(dir, INPUT_PREFIX + i + SUFFIX);
            File outFile = new File(dir, OUTPUT_PREFIX + i + SUFFIX);
            try {
                FileUtil.writeUtf8String(inputs.get(i) == null ? "" : inputs.get(i), inFile);
                FileUtil.writeUtf8String(outputs.get(i) == null ? "" : outputs.get(i), outFile);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用例文件写入失败");
            }
        }
    }

    /**
     * 读取一组用例文件；目录不存在或无用例文件返回 null
     */
    public static CaseFiles readCaseFiles(String basePath, long questionId) {
        validateQuestionId(questionId);
        File dir = FileUtil.file(dirPath(basePath, questionId));
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }
        List<Integer> indexes = new ArrayList<>();
        for (File file : files) {
            Matcher matcher = INPUT_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                indexes.add(Integer.parseInt(matcher.group(1)));
            }
        }
        if (indexes.isEmpty()) {
            return null;
        }
        Collections.sort(indexes);
        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();
        for (Integer index : indexes) {
            File inFile = new File(dir, INPUT_PREFIX + index + SUFFIX);
            File outFile = new File(dir, OUTPUT_PREFIX + index + SUFFIX);
            if (!inFile.exists() || !outFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用例文件不完整，缺少第 " + index + " 个用例");
            }
            try {
                inputs.add(FileUtil.readUtf8String(inFile));
                outputs.add(FileUtil.readUtf8String(outFile));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用例文件读取失败");
            }
        }
        CaseFiles caseFiles = new CaseFiles();
        caseFiles.setInputs(inputs);
        caseFiles.setOutputs(outputs);
        return caseFiles;
    }

    /**
     * 删除某题目的用例目录，目录不存在时忽略
     */
    public static void deleteCaseFiles(String basePath, long questionId) {
        validateQuestionId(questionId);
        File dir = FileUtil.file(dirPath(basePath, questionId));
        if (dir.exists()) {
            FileUtil.del(dir);
        }
    }

    private static String dirPath(String basePath, long questionId) {
        return basePath + File.separator + questionId;
    }

    private static void validateQuestionId(long questionId) {
        if (questionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目 id 不合法");
        }
    }

    /**
     * 用例文件读取结果
     */
    public static class CaseFiles {

        private List<String> inputs;

        private List<String> outputs;

        public List<String> getInputs() {
            return inputs;
        }

        public void setInputs(List<String> inputs) {
            this.inputs = inputs;
        }

        public List<String> getOutputs() {
            return outputs;
        }

        public void setOutputs(List<String> outputs) {
            this.outputs = outputs;
        }
    }
}
