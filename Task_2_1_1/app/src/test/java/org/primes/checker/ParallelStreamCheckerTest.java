package org.primes.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


class ParallelStreamCheckerTest {

    private ParallelStreamChecker checker;

     @BeforeEach
    void setUp() {
        checker = new ParallelStreamChecker();
    }

     private int[] generatePrimesArray(int count, int min, int max) {
        int[] primes = new int[count];
        int found = 0;
        int currentNum = (max % 2 == 0) ? max - 1 : max;
        if (currentNum < 2) currentNum = 2;
        while (found < count && currentNum >= min) {
            if (Util.isPrime(currentNum)) primes[found++] = currentNum;
            if (currentNum == 3) currentNum = 2; else if (currentNum == 2) currentNum = 1; else if (currentNum % 2 != 0) currentNum -= 2; else currentNum--;
        }
        if (found < count) { System.err.println("Warning: Could not generate enough primes for test array."); Arrays.fill(primes, found, count, 7); }
        return primes;
     }

    @Test
    void testContainsNonPrime_Example1_HasNonPrimes() {
        int[] arr = {6, 8, 7, 13, 5, 9, 4};
        assertTrue(checker.containsNonPrime(arr));
    }

    @Test
    void testContainsNonPrime_Example2_AllPrimes() {
        int[] arr = {20319251, 6997901, 6997927, 6997937, 17858849, 6997967,
                     6998009, 6998029, 6998039, 20165149, 6998051, 6998053};
        assertFalse(checker.containsNonPrime(arr));
    }

     @Test
    void testContainsNonPrime_OnlyNonPrimes() {
        int[] arr = {4, 6, 8, 9, 10, 12, 14, 15, 1};
        assertTrue(checker.containsNonPrime(arr));
    }

     @Test
    void testContainsNonPrime_Mixed_NonPrimeAtStart() {
        int[] arr = {4, 5, 7, 11, 13};
        assertTrue(checker.containsNonPrime(arr));
    }

     @Test
     void testContainsNonPrime_Mixed_NonPrimeInMiddle() {
         int[] arr = {5, 7, 10, 11, 13};
         assertTrue(checker.containsNonPrime(arr));
     }

     @Test
     void testContainsNonPrime_Mixed_NonPrimeAtEnd() {
         int[] arr = {5, 7, 11, 13, 15};
         assertTrue(checker.containsNonPrime(arr));
     }

    @Test
    void testContainsNonPrime_ContainsOne() {
        int[] arr = {2, 3, 5, 1, 7};
        assertTrue(checker.containsNonPrime(arr));
    }

    @Test
    void testContainsNonPrime_ContainsZero() {
        int[] arr = {2, 3, 0, 5, 7};
        assertTrue(checker.containsNonPrime(arr));
    }

     @Test
     void testContainsNonPrime_ContainsNegative() {
         int[] arr = {2, 3, -5, 7};
         assertTrue(checker.containsNonPrime(arr));
     }

    @Test
    void testContainsNonPrime_EmptyArray() {
        int[] arr = {};
        assertFalse(checker.containsNonPrime(arr));
    }

     @Test
     void testContainsNonPrime_SinglePrime() {
         int[] arr = {17};
         assertFalse(checker.containsNonPrime(arr));
     }

     @Test
     void testContainsNonPrime_SingleNonPrime_Small() {
         int[] arr = {10};
         assertTrue(checker.containsNonPrime(arr));
     }

    @Test
    void testContainsNonPrime_NullArray() {
        assertThrows(NullPointerException.class, () -> checker.containsNonPrime(null));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testContainsNonPrime_LargeArrayAllPrimes() {
        assumeTrue(Runtime.getRuntime().availableProcessors() >= 2, "Skipping intensive test on single core machine");
        int size = 15000;
        int[] arr = generatePrimesArray(size, 1_000_000, 5_000_000);
        assertFalse(checker.containsNonPrime(arr));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testContainsNonPrime_LargeArrayOneNonPrimeAtEnd() {
         assumeTrue(Runtime.getRuntime().availableProcessors() >= 2, "Skipping intensive test on single core machine");
         int size = 15000;
         int[] arr = generatePrimesArray(size, 1_000_000, 5_000_000);
         arr[size - 1] = 999999;
         assertTrue(checker.containsNonPrime(arr));
    }

     @Test
     @Timeout(value = 5, unit = TimeUnit.SECONDS)
     void testContainsNonPrime_LargeArrayOneNonPrimeAtStart() {
          assumeTrue(Runtime.getRuntime().availableProcessors() >= 2, "Skipping intensive test on single core machine");
          int size = 15000;
          int[] arr = generatePrimesArray(size, 1_000_000, 5_000_000);
          arr[0] = 4;
          assertTrue(checker.containsNonPrime(arr));
     }
}