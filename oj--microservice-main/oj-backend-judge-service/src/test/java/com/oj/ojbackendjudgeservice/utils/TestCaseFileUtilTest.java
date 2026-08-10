package com.oj.ojbackendjudgeservice.utils;

import cn.hutool.core.io.FileUtil;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendcommon.utils.TestCaseFileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestCaseFileUtilTest {

    private Path tempDir;

    @AfterEach
    void cleanup() {
        if (tempDir != null) {
            FileUtil.del(tempDir.toFile());
        }
    }

    @Test
    void writeAndReadRoundTrip() throws IOException {
        tempDir = Files.createTempDirectory("oj-case-test-");
        long questionId = 1001L;
        List<String> inputs = Arrays.asList("1 2\n", "3 4\n");
        List<String> outputs = Arrays.asList("3\n", "7\n");

        TestCaseFileUtil.writeCaseFiles(tempDir.toString(), questionId, inputs, outputs);

        TestCaseFileUtil.CaseFiles caseFiles = TestCaseFileUtil.readCaseFiles(tempDir.toString(), questionId);
        assertNotNull(caseFiles);
        assertEquals(inputs, caseFiles.getInputs());
        assertEquals(outputs, caseFiles.getOutputs());
    }

    @Test
    void readReturnsNullWhenDirectoryMissing() throws IOException {
        tempDir = Files.createTempDirectory("oj-case-test-");
        assertNull(TestCaseFileUtil.readCaseFiles(tempDir.toString(), 9999L));
    }

    @Test
    void readThrowsWhenOutputFileMissing() throws IOException {
        tempDir = Files.createTempDirectory("oj-case-test-");
        long questionId = 1002L;
        TestCaseFileUtil.writeCaseFiles(tempDir.toString(), questionId,
                Collections.singletonList("1"), Collections.singletonList("2"));
        File outFile = new File(tempDir.toFile(), questionId + File.separator + "out_0.txt");
        FileUtil.del(outFile);
        assertThrows(BusinessException.class,
                () -> TestCaseFileUtil.readCaseFiles(tempDir.toString(), questionId));
    }

    @Test
    void deleteRemovesDirectory() throws IOException {
        tempDir = Files.createTempDirectory("oj-case-test-");
        long questionId = 1003L;
        TestCaseFileUtil.writeCaseFiles(tempDir.toString(), questionId,
                Collections.singletonList("1"), Collections.singletonList("2"));
        TestCaseFileUtil.deleteCaseFiles(tempDir.toString(), questionId);
        assertNull(TestCaseFileUtil.readCaseFiles(tempDir.toString(), questionId));
    }

    @Test
    void writeRejectsSizeMismatch() {
        assertThrows(BusinessException.class,
                () -> TestCaseFileUtil.writeCaseFiles("tmp", 1L,
                        Collections.singletonList("1"), Arrays.asList("2", "3")));
    }
}
