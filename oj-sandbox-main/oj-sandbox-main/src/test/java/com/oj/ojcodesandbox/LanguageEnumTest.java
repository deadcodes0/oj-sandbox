package com.oj.ojcodesandbox;

import com.oj.ojcodesandbox.enums.LanguageEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LanguageEnumTest {

    @Test
    void shouldMapJavaConfig() {
        LanguageEnum java = LanguageEnum.getEnumByValue("java");
        assertNotNull(java);
        assertEquals("Main.java", java.getSourceFileName());
        assertEquals("openjdk:8-alpine", java.getDockerImage());
        assertEquals("javac -encoding utf-8 -d /app /code/*.java", java.getCompileCmd());
        assertEquals("java -cp /app Main", java.getRunCmd());
    }

    @Test
    void shouldMapCConfig() {
        LanguageEnum c = LanguageEnum.getEnumByValue("c");
        assertNotNull(c);
        assertEquals("Main.c", c.getSourceFileName());
        assertEquals("gcc:latest", c.getDockerImage());
        assertEquals("gcc /code/Main.c -o /app/Main", c.getCompileCmd());
        assertEquals("/app/Main", c.getRunCmd());
    }

    @Test
    void shouldMapCppConfig() {
        LanguageEnum cpp = LanguageEnum.getEnumByValue("cpp");
        assertNotNull(cpp);
        assertEquals("Main.cpp", cpp.getSourceFileName());
        assertEquals("gcc:latest", cpp.getDockerImage());
        assertEquals("g++ /code/Main.cpp -o /app/Main", cpp.getCompileCmd());
        assertEquals("/app/Main", cpp.getRunCmd());
    }

    @Test
    void shouldReturnNullForUnknownLanguage() {
        assertNull(LanguageEnum.getEnumByValue("go"));
        assertNull(LanguageEnum.getEnumByValue(null));
        assertNull(LanguageEnum.getEnumByValue(""));
    }
}
