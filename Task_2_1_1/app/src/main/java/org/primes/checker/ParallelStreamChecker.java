package org.primes.checker;

import java.util.Arrays;
import java.util.Objects;

public class ParallelStreamChecker implements PrimesChecker {

    @Override
    public boolean containsNonPrime(int[] arr) {
        Objects.requireNonNull(arr, "Input array cannot be null");
        if (arr.length == 0) {
            return false;
        }

        // Use parallel stream and short-circuiting anyMatch
        return Arrays.stream(arr)
                     .parallel()
                     .anyMatch(num -> !Util.isPrime(num));
    }
}