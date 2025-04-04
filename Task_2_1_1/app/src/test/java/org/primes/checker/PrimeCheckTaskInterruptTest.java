package org.primes.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PrimeCheckTaskInterruptTest {

    @Test
    @Timeout(5)
    void testTaskCancellation() throws Exception {
        int[] array = IntStream.rangeClosed(1_000_001, 1_010_001).filter(Util::isPrime).toArray();
        PrimeCheckTask task = new PrimeCheckTask(array, 0, array.length);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final AtomicReference<Future<Boolean>> futureRef = new AtomicReference<>();

        try {
            futureRef.set(executor.submit(task));
            Thread.sleep(1);

            Future<Boolean> future = futureRef.get();
            if (future == null) {
                fail("Future was not assigned");
                return;
            }

            boolean cancelResult = future.cancel(true);
            System.out.println("Future.cancel(true) returned: " + cancelResult);

            assertThrows(CancellationException.class, () -> {
                 future.get(100, TimeUnit.MILLISECONDS);
            }, "Getting result after cancellation should throw CancellationException");

            assertTrue(future.isDone(), "Future should be marked as done after cancellation or completion");

        } catch (CancellationException e) {
            System.out.println("Test PASSED: Task cancellation was successful and CancellationException was thrown.");
            Future<Boolean> future = futureRef.get();
            if (future != null) {
                assertTrue(future.isCancelled(), "In CancellationException case, Future should be cancelled");
                assertTrue(future.isDone(), "In CancellationException case, Future should be done");
            }

        } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             fail("Test thread was interrupted unexpectedly during sleep");
        } finally {
             if (executor != null) {
                List<Runnable> unfinished = executor.shutdownNow();
                if (!unfinished.isEmpty()) {
                    System.err.println("Warning: Executor shutdownNow found " + unfinished.size() + " unfinished tasks.");
                }
             }
             Future<Boolean> finalFuture = futureRef.get();
             if (finalFuture != null) {
                 System.out.println("Final Future state: isCancelled=" + finalFuture.isCancelled() + ", isDone=" + finalFuture.isDone());
             } else {
                 System.out.println("Final Future state: Future was null.");
             }
        }
    }
}