package org.primes.checker;

import java.util.concurrent.Callable;

public class PrimeCheckTask implements Callable<Boolean> {

    private final int[] array;
    private final int startIndex;
    private final int endIndex;

    public PrimeCheckTask(int[] array, int startIndex, int endIndex) {
        this.array = array;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public Boolean call() throws Exception {
        for (int j = startIndex; j < endIndex; j++) {
            if (!Util.isPrime(array[j])) {
                return true;
            }
            // Check for interruption, important for early exit
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
        }
        return false;
    }
}