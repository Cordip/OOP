package org.primes.checker;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.OptionalInt;


/**
 * Checker using Sieve of Eratosthenes.
 * NOTE: Generally INEFFICIENT for this task due to high sieve generation cost.
 */
public class SieveBasedChecker implements PrimesChecker {

    @Override
    public boolean containsNonPrime(int[] arr) {
        Objects.requireNonNull(arr, "Input array cannot be null");
        if (arr.length == 0) {
            return false;
        }

        OptionalInt maxOptional = Arrays.stream(arr).max();
        if (!maxOptional.isPresent()) {
            return false;
        }
        int maxVal = maxOptional.getAsInt();

        if (maxVal < 2) {
           return Arrays.stream(arr).anyMatch(num -> true);
        }

        // Generate sieve info up to maxVal - major overhead here.
        PrimeGeneratorOptimized.BasePrimeResult sieveResult = PrimeGeneratorOptimized.sieveOddSequentialBitSet(maxVal);
        BitSet isCompositeOdd = sieveResult.compositeInfo; // isCompositeOdd.get(i) => 2*i+1 is composite

        for (int num : arr) {
            if (!isNumberPrimeAccordingToSieve(num, isCompositeOdd, maxVal)) {
                return true;
            }
        }

        return false;
    }

    /** Checks primality using the pre-calculated Sieve BitSet. */
    private boolean isNumberPrimeAccordingToSieve(int number, BitSet isCompositeOdd, int sieveLimit) {
        if (number <= 1) return false;
        if (number == 2) return true;
        if (number % 2 == 0) return false;

        if (number > sieveLimit) {
             // Number outside sieve range. Fallback to direct check (highlights sieve limitation).
             return Util.isPrime(number);
        }

        int index = (number - 1) / 2;
        if (index >= isCompositeOdd.size() || isCompositeOdd.get(index)) {
            return false; // Composite or out of bounds
        }

        return true; // Prime according to sieve
    }
}