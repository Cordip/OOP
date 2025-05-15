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
    public Boolean call() throws InterruptedException {
        for (int j = startIndex; j < endIndex; j++) {
            if (Thread.currentThread().isInterrupted()) {
                 throw new InterruptedException("PrimeCheckTask execution was interrupted.");
            }

            if (!Util.isPrime(array[j])) {
                return true;
            }
        }
        return false;
    }
}