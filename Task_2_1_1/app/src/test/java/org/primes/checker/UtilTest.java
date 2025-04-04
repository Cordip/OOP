package org.primes.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 7919, 6997937})
    void testIsPrime_Primes(int prime) {
        assertTrue(Util.isPrime(prime), prime + " should be prime");
    }

    @Test
    void testIsPrime_MaxIntPrime() {
         assertTrue(Util.isPrime(Integer.MAX_VALUE), "Integer.MAX_VALUE should be prime");
    }

    @ParameterizedTest
    @ValueSource(ints = {
            0, 1, 4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 21, 22, 24, 25, 26, 27, 28, 30,
            7919 * 2,
            17 * 19,
            6997937 - 1,
            Integer.MAX_VALUE - 1
    })
    void testIsPrime_NonPrimes(int nonPrime) {
        assertFalse(Util.isPrime(nonPrime), nonPrime + " should not be prime");
    }

     @ParameterizedTest
     @ValueSource(ints = {-1, -2, -5, -10, Integer.MIN_VALUE})
     void testIsPrime_NegativeNumbers(int negative) {
         assertFalse(Util.isPrime(negative), negative + " should not be prime");
     }

     @Test
     void testIsPrime_BoundaryCases() {
         assertFalse(Util.isPrime(0), "0 is not prime");
         assertFalse(Util.isPrime(1), "1 is not prime");
         assertTrue(Util.isPrime(2), "2 is prime");
         assertTrue(Util.isPrime(3), "3 is prime");
         assertFalse(Util.isPrime(4), "4 is not prime");
     }

     @Test
     void testIsPrime_LargeComposites() {
          long largePrime1 = 2147483647;
          long largePrime2 = 2147483629;
          long largeCompositeProduct = largePrime1 * largePrime2;

          int largeCompositeInt = 2147483645; // 5 * 429496729
          assertFalse(Util.isPrime(largeCompositeInt), largeCompositeInt + " should not be prime");

          int compositeFromPrimesNearSqrt = 46337 * 46339;
          assertFalse(Util.isPrime(compositeFromPrimesNearSqrt), compositeFromPrimesNearSqrt + " should not be prime");

          assertFalse(Util.isPrime(25), "25 (5*5) should not be prime");
          assertFalse(Util.isPrime(35), "35 (5*7) should not be prime");
          assertFalse(Util.isPrime(49), "49 (7*7) should not be prime");
     }
}