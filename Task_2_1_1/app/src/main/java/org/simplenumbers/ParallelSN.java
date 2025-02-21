package org.simplenumbers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

public class ParallelSN{
    long [] array;
    final int numThreads = Runtime.getRuntime().availableProcessors();
    final int limit;
    final int chunkSize = 100_000;

    public ParallelSN(long [] array) {
        this.array = array;
        limit = array.length;
    }

    public Boolean ParallelCheck () {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int sqrtLimit = (int) Math.sqrt(limit);
        Boolean res = false;

        for (int start = sqrtLimit + 1; start <= limit; start += chunkSize) {
            int chunkStart = start;
            int chunkEnd = Math.min(chunkStart + chunkSize - 1, limit);
            // Future<?> future = executor.submit(() -> processChunk(array, chunkStart, chunkEnd));
            // Future<Boolean> future = executor.submit(() -> new CallableSN(array).checkArray(chunkStart, chunkEnd));
            Future<Boolean> future = executor.submit(new CallableSN(array, chunkStart, chunkEnd));
            // Future<Boolean> future = executor.submit(() -> new CallableSN(array, chunkStart, chunkEnd));

            try {
                res |= future.get();
                // System.out.println(res);

            } catch (Exception e) {
                // TODO: handle exception
            }

            if (res) {
                break;
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES); // Wait for all tasks to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread pool interrupted");
        }

        return res;
    }

    private class CallableSN implements Callable<Boolean> {
        long [] array;
        int start;
        int end;

        CallableSN(long [] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        
        public Boolean call() throws Exception {
            for (int i = 0; i < array.length; i++) {
            

                if (array[i] == 0 || array[i] == 1) {
                    return true;
                }
                if (array[i] != 2 && array[i] % 2 == 0) {
                    return true;
                }
                if (array[i] != 3 && array[i] % 3 == 0) {
                    return true;
                }
                for (int j = 5; j <= Math.sqrt(array[i]); j += 6) {
                    if (array[i] % j == 0 || array[i] % (j + 2) == 0) {
                        return true;
                    }
                }
            }
    
            return false;
        }
    }
}
