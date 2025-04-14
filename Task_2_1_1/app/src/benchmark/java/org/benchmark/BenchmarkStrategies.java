package org.benchmark;

import org.primes.checker.PrimeGeneratorOptimized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BenchmarkStrategies {

    // --- Benchmark Configuration ---
    private static final int LIMIT = 100_000_000; // Upper bound (10^8)
    // private static final int LIMIT = 500_000_000; // Needs -Xmx adjustments

    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final int WARMUP_RUNS = 1;
    private static final int BENCHMARK_RUNS = 3;
    // --- End Configuration ---

    public static void main(String[] args) {
        System.out.printf("Starting Benchmark: Limit=%,d, Threads=%d%n", LIMIT, THREADS);
        System.out.printf("Warmup Runs: %d, Benchmark Runs: %d%n", WARMUP_RUNS, BENCHMARK_RUNS);
        System.out.println("Comparing Strategies: Sequential vs. Cache Segments vs. Thread Chunks");
        System.out.println("--------------------------------------------------");

        List<Long> sequentialTimes = new ArrayList<>();
        List<Long> cacheSegmentTimes = new ArrayList<>();
        List<Long> threadChunkTimes = new ArrayList<>();
        long lastSequentialResultSize = -1;
        long lastCacheSegmentResultSize = -1;
        long lastThreadChunkResultSize = -1;

        System.out.println("Running warmup iterations...");
        for (int i = 0; i < WARMUP_RUNS; i++) {
            System.out.printf(" Warmup Run %d/%d%n", i + 1, WARMUP_RUNS);

            System.out.println("  Running Sequential (warmup)...");
            List<Integer> res0 = PrimeGeneratorOptimized.generatePrimesSequential(LIMIT);

            System.out.println("  Running Cache Segments (warmup)...");
            List<Integer> res1 = PrimeGeneratorOptimized.generatePrimesParallel(LIMIT, THREADS);

            System.out.println("  Running Thread Chunks (warmup)...");
            List<Integer> res2 = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(LIMIT, THREADS);

            // Verify consistency
            if (res0 == null || res1 == null || res2 == null) {
                 System.err.println("!!! WARMUP ERROR: One or more results are null!");
            } else if (res0.size() != res1.size() || res1.size() != res2.size()) {
                 System.err.printf("!!! WARMUP ERROR: Result sizes differ! Seq=%d, Cache=%d, Chunks=%d%n",
                                   res0.size(), res1.size(), res2.size());
            }
             System.gc(); // Suggest GC
             try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        System.out.println("Warmup complete.");
        System.out.println("--------------------------------------------------");

        System.out.println("Running benchmark iterations...");
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            System.out.printf(" Benchmark Run %d/%d:%n", i + 1, BENCHMARK_RUNS);

            System.out.println("  Running Sequential...");
            long startTime0 = System.nanoTime();
            List<Integer> resultSeq = PrimeGeneratorOptimized.generatePrimesSequential(LIMIT);
            long endTime0 = System.nanoTime();
             if (resultSeq != null) {
                 sequentialTimes.add(endTime0 - startTime0);
                 lastSequentialResultSize = resultSeq.size();
                 System.out.printf("  -> Sequential time:     %,d ms (%,d primes)%n",
                                   TimeUnit.NANOSECONDS.toMillis(endTime0 - startTime0), resultSeq.size());
             } else {
                 System.out.println("  -> Sequential run FAILED.");
                 sequentialTimes.add(-1L); // Mark as failed
             }

            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            System.out.println("  Running Cache Segments...");
            long startTime1 = System.nanoTime();
            List<Integer> resultCache = PrimeGeneratorOptimized.generatePrimesParallel(LIMIT, THREADS);
            long endTime1 = System.nanoTime();
            if (resultCache != null) {
                cacheSegmentTimes.add(endTime1 - startTime1);
                lastCacheSegmentResultSize = resultCache.size();
                System.out.printf("  -> Cache Segments time: %,d ms (%,d primes)%n",
                                  TimeUnit.NANOSECONDS.toMillis(endTime1 - startTime1), resultCache.size());
            } else {
                System.out.println("  -> Cache Segments run FAILED.");
                cacheSegmentTimes.add(-1L);
            }

            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            System.out.println("  Running Thread Chunks...");
            long startTime2 = System.nanoTime();
            List<Integer> resultChunk = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(LIMIT, THREADS);
            long endTime2 = System.nanoTime();
             if (resultChunk != null) {
                 threadChunkTimes.add(endTime2 - startTime2);
                 lastThreadChunkResultSize = resultChunk.size();
                 System.out.printf("  -> Thread Chunks time:  %,d ms (%,d primes)%n",
                                   TimeUnit.NANOSECONDS.toMillis(endTime2 - startTime2), resultChunk.size());
             } else {
                 System.out.println("  -> Thread Chunks run FAILED (check -Xmx, OutOfMemoryError possible).");
                 threadChunkTimes.add(-1L);
             }

             // Check consistency
             if (lastSequentialResultSize != -1 && lastCacheSegmentResultSize != -1 && lastThreadChunkResultSize != -1) {
                  if (lastSequentialResultSize != lastCacheSegmentResultSize || lastCacheSegmentResultSize != lastThreadChunkResultSize) {
                       System.err.printf("!!! BENCHMARK ERROR: Result sizes differ in run %d! Seq=%d, Cache=%d, Chunks=%d%n",
                                         i + 1, lastSequentialResultSize, lastCacheSegmentResultSize, lastThreadChunkResultSize);
                  }
             } else if (lastSequentialResultSize == -1 || lastCacheSegmentResultSize == -1 || lastThreadChunkResultSize == -1) {
                  System.err.println("!!! BENCHMARK WARNING: At least one run failed in iteration " + (i+1));
             }

            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            System.out.println("---");
        }
        System.out.println("Benchmark complete.");
        System.out.println("--------------------------------------------------");

        long avgSequential = calculateAverage(sequentialTimes);
        long avgCacheSegments = calculateAverage(cacheSegmentTimes);
        long avgThreadChunks = calculateAverage(threadChunkTimes);

        System.out.println("\n========= Benchmark Results (Average Time) =========");
        printFinalResult("Sequential", avgSequential);
        printFinalResult("Cache Segments", avgCacheSegments);
        printFinalResult("Thread Chunks", avgThreadChunks);
        System.out.println("--------------------------------------------------");

        if (avgSequential > 0) {
            if (avgCacheSegments > 0) {
                System.out.printf("Cache Segments Speedup vs Sequential: %.2fx%n", (double) avgSequential / avgCacheSegments);
            } else {
                System.out.println("Cache Segments failed, cannot calculate speedup vs Sequential.");
            }
            if (avgThreadChunks > 0) {
                 System.out.printf("Thread Chunks Speedup vs Sequential:  %.2fx%n", (double) avgSequential / avgThreadChunks);
            } else {
                 System.out.println("Thread Chunks failed, cannot calculate speedup vs Sequential.");
            }
        } else {
             System.out.println("Sequential run failed, cannot calculate speedups.");
        }

         if (avgCacheSegments > 0 && avgThreadChunks > 0) {
             double speedupCacheVsChunks = (double) avgThreadChunks / avgCacheSegments;
             System.out.printf("Cache Segments Speedup vs Thread Chunks: %.2fx%n", speedupCacheVsChunks);
         }

        System.out.println("====================================================");
    }

    private static long calculateAverage(List<Long> times) {
        long sum = 0;
        int count = 0;
        for (long time : times) {
            if (time >= 0) { // Ignore failures
                sum += time;
                count++;
            }
        }
        return count > 0 ? sum / count : -1; // Return -1 if no successful runs
    }

     private static void printFinalResult(String label, long avgTimeNs) {
         System.out.printf("%-20s: ", label);
         if (avgTimeNs >= 0) {
             System.out.printf("%,d ms%n", TimeUnit.NANOSECONDS.toMillis(avgTimeNs));
         } else {
             System.out.println("Failed");
         }
     }
}