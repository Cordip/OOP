package org.primes.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class UtilCoverageTest {

    @Test
    void testConstructorIsPrivateAndThrowsException() throws Exception {
        Constructor<Util> constructor;
        try {
            constructor = Util.class.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            fail("Class Util should have a zero-argument constructor (possibly private)", e);
            return;
        }

        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
                "Constructor should be private");

        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> {
            constructor.newInstance();
        }, "Expected InvocationTargetException when calling private constructor that throws");

        Throwable cause = thrown.getCause();
        assertNotNull(cause, "InvocationTargetException should have a cause");
        assertTrue(cause instanceof IllegalStateException, "Cause should be IllegalStateException");
        assertEquals("Utility class should not be instantiated", cause.getMessage(), "Correct exception message expected");
    }

    @Test
    void testRangeCheck_Valid() {
        assertDoesNotThrow(() -> Util.rangeCheck(10, 0, 5));
        assertDoesNotThrow(() -> Util.rangeCheck(10, 0, 10));
        assertDoesNotThrow(() -> Util.rangeCheck(10, 5, 5));
    }

    @Test
    void testRangeCheck_InvalidFromIndex() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Util.rangeCheck(10, -1, 5));
    }

    @Test
    void testRangeCheck_InvalidToIndex() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Util.rangeCheck(10, 0, 11));
    }

    @Test
    void testRangeCheck_FromGreaterThanTo() {
        assertThrows(IllegalArgumentException.class, () -> Util.rangeCheck(10, 6, 5));
    }

    @Test
    void testReadIntegersFromFile_ValidFile(@TempDir File tempDir) throws IOException {
        File testFile = new File(tempDir, "numbers.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
            writer.write("10\n");
            writer.write("25\n");
            writer.write("-5\n");
            writer.write(" \n");
            writer.write("abc\n");
            writer.write("999999999999999999\n");
        }

        long[] expected = {10L, 25L, -5L, 999999999999999999L};
        long[] actual = Util.readIntegersFromFile(testFile.getAbsolutePath());
        assertArrayEquals(expected, actual);
    }

     @Test
    void testReadIntegersFromFile_EmptyFile(@TempDir File tempDir) throws IOException {
        File testFile = new File(tempDir, "empty.txt");
        testFile.createNewFile();

        long[] expected = {};
        long[] actual = Util.readIntegersFromFile(testFile.getAbsolutePath());
        assertArrayEquals(expected, actual);
    }

    @Test
    void testReadIntegersFromFile_FileNotFound() {
        String nonExistentPath = "./non_existent_file_for_test.txt";
        long[] expected = {};
        long[] actual = Util.readIntegersFromFile(nonExistentPath);
        assertArrayEquals(expected, actual, "Should return empty array if file not found");
    }
}