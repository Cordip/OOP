package org.primes.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.primes.checker.PrimeCheckTask;
import org.primes.checker.Util;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PrimeCheckTaskInterruptTest {

    @Test
    @Timeout(10)
    void testTaskCancellation() throws InterruptedException {
        System.out.println("Generating large array...");
        int[] array = IntStream.rangeClosed(1_000_001, 2_010_001)
                .filter(Util::isPrime)  
                .toArray();
        System.out.println("Array generated with length: " + array.length);

        PrimeCheckTask task = new PrimeCheckTask(array, 0, array.length);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = null;

        try {
            future = executor.submit(task);
            Thread.sleep(20);

            System.out.println("Calling future.cancel(true)...");
            boolean cancelResult = future.cancel(true);
            System.out.println("Future.cancel(true) returned: " + cancelResult);

            System.out.println("Attempting future.get()...");
            try {
                Boolean result = future.get();
                System.err.println("Future.get() returned normally with: " + result);
                System.err.println("State: isCancelled=" + future.isCancelled() + ", isDone=" + future.isDone());
                fail("Future.get() should have thrown CancellationException, but returned normally. cancel(true) result was: " + cancelResult);
            } catch (CancellationException e) {
                System.out.println("Successfully caught CancellationException.");
                System.out.println("Checking state within CancellationException catch block...");
                System.out.println("State: isCancelled=" + future.isCancelled() + ", isDone=" + future.isDone());
                assertTrue(future.isCancelled(), "In CancellationException catch: Future should be marked as cancelled.");
                assertTrue(future.isDone(), "In CancellationException catch: Future should be marked as done.");
            } catch (ExecutionException e) {
                System.err.println("Future.get() threw ExecutionException (Task failed): " + e.getCause());
                e.printStackTrace();
                fail("Future.get() threw ExecutionException, expected CancellationException. Task likely failed before cancel.", e);
            }

            System.out.println("Test logic after try-catch for future.get() completed.");

        } finally {
            if (executor != null) {
                System.out.println("Shutting down executor...");
                List<Runnable> unfinished = executor.shutdownNow();
                if (!unfinished.isEmpty()) {
                    System.err.println("Warning: Executor shutdownNow found " + unfinished.size() + " unfinished tasks.");
                }
                 if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                     System.err.println("Executor did not terminate within 1 second.");
                 }
                 System.out.println("Executor shutdown complete.");
            }
            if (future != null) {
                System.out.println("Final Future state in finally: isCancelled=" + future.isCancelled() + ", isDone=" + future.isDone());
            } else {
                System.out.println("Final Future state in finally: Future was null.");
            }
        }
    }
}