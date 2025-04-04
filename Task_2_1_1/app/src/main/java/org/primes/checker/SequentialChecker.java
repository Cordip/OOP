package org.primes.checker;

import java.util.Objects;

public class SequentialChecker implements PrimesChecker {

    @Override
    public boolean containsNonPrime(int[] arr) {
        Objects.requireNonNull(arr, "Input array cannot be null");
        for (int num : arr) {
            if (!Util.isPrime(num)) {
                return true;
            }
        }
        return false;
    }
}