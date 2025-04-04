package org.primes.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class ThreadChecker implements PrimesChecker {

    private final int threadCount;
    private final ForkJoinPool pool; // Используем ForkJoinPool, т.к. задача хорошо делится

    public ThreadChecker(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive.");
        }
        this.threadCount = threadCount;
        this.pool = new ForkJoinPool(this.threadCount);
    }

    @Override
    public boolean containsNonPrime(int[] arr) {
        Objects.requireNonNull(arr, "Input array cannot be null");
        if (arr.length == 0) {
            return false;
        }

        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(this.pool);
        List<Future<Boolean>> futures = new ArrayList<>();
        int tasksSubmitted = 0;
        boolean nonPrimeFound = false;

        try {
            int chunkSize = (int) Math.ceil((double) arr.length / threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int start = i * chunkSize;
                final int end = Math.min(start + chunkSize, arr.length);

                if (start >= arr.length) {
                    break;
                }

                Callable<Boolean> task = new PrimeCheckTask(arr, start, end);
                futures.add(completionService.submit(task));
                tasksSubmitted++;
            }

            for (int i = 0; i < tasksSubmitted; i++) {
                if (nonPrimeFound) {
                    break;
                }

                try {
                    // Используем poll с таймаутом для быстрой проверки + take для ожидания
                    Future<Boolean> future = completionService.poll(5, TimeUnit.MILLISECONDS);
                    if (future == null) {
                        if (!nonPrimeFound) {
                             future = completionService.take();
                        } else {
                             break;
                        }
                    }

                    boolean taskResult = future.get();

                    if (taskResult) {
                        nonPrimeFound = true;
                         break;
                    }
                } catch (ExecutionException e) {
                     System.err.printf("ERROR: Task execution failed within ThreadChecker(%d): %s%n", threadCount, e.getCause());
                     throw new RuntimeException("Prime checking subtask failed", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                    System.err.printf("WARN: ThreadChecker(%d) interrupted while waiting for task result.%n", threadCount);
                    nonPrimeFound = false;
                    break;
                } catch (CancellationException e){
                    // Задача была отменена (из-за nonPrimeFound=true) - это нормально
                }
            }

        } finally {
            // Отменяем незавершенные задачи, если найдено непростое (ранний выход)
            if (nonPrimeFound) {
                for (Future<Boolean> f : futures) {
                    if (!f.isDone()) {
                        f.cancel(true); // true - прервать выполняющийся поток
                    }
                }
            }
        }
        return nonPrimeFound;
    }

    public void shutdownPool() {
        if (pool != null && !pool.isShutdown()) {
            System.out.printf("Shutting down pool for ThreadChecker (%d threads)...%n", threadCount);
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.printf("Pool for ThreadChecker (%d threads) did not terminate gracefully after 60s, forcing shutdown...%n", threadCount);
                    List<Runnable> droppedTasks = pool.shutdownNow();
                    System.err.printf("Pool for ThreadChecker (%d threads) force shutdown initiated, %d tasks dropped.%n", threadCount, droppedTasks.size());
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.printf("ERROR: Pool for ThreadChecker (%d threads) did not terminate even after force shutdown.%n", threadCount);
                    }
                } else {
                     System.out.printf("Pool for ThreadChecker (%d threads) terminated gracefully.%n", threadCount);
                }
            } catch (InterruptedException ie) {
                System.err.printf("Interrupted during pool shutdown for ThreadChecker (%d threads), forcing shutdown now.%n", threadCount);
                pool.shutdownNow();
                Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            }
        } else if (pool != null) {
             System.out.printf("Pool for ThreadChecker (%d threads) was already shutdown.%n", threadCount);
        }
    }
}