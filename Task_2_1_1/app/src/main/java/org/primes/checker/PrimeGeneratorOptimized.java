package org.primes.checker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


public class PrimeGeneratorOptimized {

    // Tunable: Size of segments in bits (representing odd numbers) for cache optimization
    private static final int SEGMENT_BITS = 128 * 1024; // 16KB

    // Default threshold above which base prime generation itself becomes parallel
    private static final int DEFAULT_PARALLEL_BASE_PRIME_THRESHOLD = 10_000_000;

    // Helper structure for returning results from base prime generation
    public static class BasePrimeResult {
        final BitSet compositeInfo; // BitSet where set bit at index i means 2*i+1 is composite
        final List<Integer> primes; // List of odd primes (>=3) found

        BasePrimeResult(BitSet ci, List<Integer> p) {
            this.compositeInfo = ci;
            this.primes = p;
        }
    }

    // Helper structure for parallel base prime generation task results
    private static class SegmentResult {
        final int segmentStartIndex;
        final BitSet compositeInfo;
        final List<Integer> primes;
         SegmentResult(int ssi, BitSet ci, List<Integer> p) {
            this.segmentStartIndex = ssi;
            this.compositeInfo = ci;
            this.primes = p;
         }
    }

    /**
     * Worker class responsible for sieving a single segment of odd numbers.
     */
    private static class SegmentSieveWorker {

        private final int segmentLowIndex;
        private final int bitsInSegment;
        private final List<Integer> basePrimes;
        private final int globalLimit;
        private final long segmentStartNum;
        private final long segmentEndNum;

        SegmentSieveWorker(int segmentLowIndex, int bitsInSegment, List<Integer> basePrimes, int globalLimit) {
            this.segmentLowIndex = segmentLowIndex;
            this.bitsInSegment = bitsInSegment;
            this.basePrimes = basePrimes;
            this.globalLimit = globalLimit;
            this.segmentStartNum = 2L * segmentLowIndex + 1;
            this.segmentEndNum = segmentStartNum + 2L * (bitsInSegment - 1);
        }

        List<Integer> sieveSegment() {
            if (bitsInSegment <= 0) return Collections.emptyList();

            BitSet segmentComposite = new BitSet(bitsInSegment);
            List<Integer> segmentPrimes = new ArrayList<>(Math.min(bitsInSegment, 100));

            if (basePrimes != null) {
                for (int p : basePrimes) {
                    long pSquared = (long) p * p;
                    if (pSquared > segmentEndNum) break; // Optimization

                    long startMultiple;
                    if (pSquared >= segmentStartNum) {
                        startMultiple = pSquared;
                    } else {
                        startMultiple = ((segmentStartNum + p - 1) / p) * p;
                        if (startMultiple % 2 == 0) startMultiple += p;
                        if (startMultiple < pSquared) startMultiple = pSquared;
                    }

                    if (startMultiple <= segmentEndNum) {
                        int startIndexInSegment = (int) ((startMultiple - segmentStartNum) / 2);
                        if (startIndexInSegment >= 0 && startIndexInSegment < bitsInSegment) {
                            for (int idx = startIndexInSegment; idx < bitsInSegment; idx += p) {
                                segmentComposite.set(idx);
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < bitsInSegment; i++) {
                if (!segmentComposite.get(i)) {
                    long primeNumCandidate = segmentStartNum + 2L * i;
                    // Ensure it fits in int and is within the global limit
                    if (primeNumCandidate <= globalLimit && primeNumCandidate <= Integer.MAX_VALUE) {
                         segmentPrimes.add((int) primeNumCandidate);
                    } else if (primeNumCandidate > globalLimit) {
                        break; // Optimization: Numbers will only increase
                    }
                }
            }
            return segmentPrimes;
        }
    }

    public static List<Integer> generatePrimesSequential(int limit) {
        if (limit < 2) {
            return Collections.emptyList();
        }

        BasePrimeResult result = sieveOddSequentialBitSet(limit);
        List<Integer> oddPrimes = result.primes;

        int estimatedSize = (oddPrimes != null ? oddPrimes.size() : 0) + (limit >= 2 ? 1 : 0);
        ArrayList<Integer> allPrimes = new ArrayList<>(estimatedSize);
        if (limit >= 2) {
            allPrimes.add(2);
        }
        if (oddPrimes != null) {
            allPrimes.addAll(oddPrimes);
        }
        return allPrimes;
    }


    public static List<Integer> generatePrimesParallel(int limit, int numThreads) {
        return generatePrimesParallel(limit, numThreads, DEFAULT_PARALLEL_BASE_PRIME_THRESHOLD);
    }

    /**
     * Base prime generation is also parallelized if sqrt(limit) >= basePrimeThreshold.
     */
    public static List<Integer> generatePrimesParallel(int limit, int numThreads, int basePrimeThreshold) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive.");
        }
        if (basePrimeThreshold < 0) {
             throw new IllegalArgumentException("Base prime threshold must be non-negative.");
        }
        return generatePrimesInternal(limit, numThreads, true, basePrimeThreshold);
    }


    public static List<Integer> generatePrimesParallel_ThreadChunks(int limit, int numThreads) {
         return generatePrimesParallel_ThreadChunks(limit, numThreads, DEFAULT_PARALLEL_BASE_PRIME_THRESHOLD);
    }

     /**
     * Splits the main range into exactly numThreads large chunks. Less cache efficient.
     */
    public static List<Integer> generatePrimesParallel_ThreadChunks(int limit, int numThreads, int basePrimeThreshold) {
         if (numThreads <= 0) {
             throw new IllegalArgumentException("Number of threads must be positive.");
         }
         if (basePrimeThreshold < 0) {
             throw new IllegalArgumentException("Base prime threshold must be non-negative.");
         }

         long requiredBits = ((long)limit - 1) / 2 + 1;
         if (numThreads > 0) {
             long bitsPerThread = requiredBits / numThreads;
             if (bitsPerThread > 0 && bitsPerThread / 8.0 * 1.1 > Integer.MAX_VALUE) {
                  System.err.printf("Warning: Thread Chunks strategy might require very large BitSets (approx > %.1f GB per thread), potentially causing OutOfMemoryError.%n",
                                     (bitsPerThread / 8.0 / (1024.0*1024.0*1024.0)));
             }
         }
        return generatePrimesInternal(limit, numThreads, false, basePrimeThreshold);
    }


    private static List<Integer> generatePrimesInternal(int limit, int numThreads, boolean useCacheSegments, int basePrimeThreshold) {
        if (limit < 2) return Collections.emptyList();

        String strategyLabel = useCacheSegments ? "(Cache Segments)" : "(Thread Chunks)";

        int sqrtLimit = (int) Math.sqrt(limit);
        BasePrimeResult baseInfo;
        long baseStart = System.nanoTime();

        // Decide on base prime generation strategy
        if (useCacheSegments && sqrtLimit >= basePrimeThreshold) {
            baseInfo = generateBasePrimesParallelInternal(sqrtLimit, numThreads);
        } else {
            baseInfo = sieveOddSequentialBitSet(sqrtLimit);
        }
        long baseEnd = System.nanoTime();

        if (baseInfo == null || baseInfo.primes == null) {
             System.err.printf("Error: Failed to generate base primes (up to %d) for limit %d. Strategy: %s%n", sqrtLimit, limit, strategyLabel);
             return Collections.emptyList();
        }
        List<Integer> basePrimes = baseInfo.primes;

        ForkJoinPool forkJoinPool = null;
        List<Future<List<Integer>>> futures = new ArrayList<>();
        List<List<Integer>> collectedResults = null;

        int lowNum = sqrtLimit + 1;
        if (lowNum % 2 == 0) lowNum++;
        int lowIndex = (lowNum - 1) / 2;
        int highIndexLimit = (limit - 1) / 2;

        // Handle cases where no main range sieving is needed
        if (lowIndex > highIndexLimit) {
             int estimatedSize = 1 + basePrimes.size();
             ArrayList<Integer> allPrimes = new ArrayList<>(estimatedSize);
             if (limit >= 2) allPrimes.add(2);
             allPrimes.addAll(basePrimes);
             return allPrimes;
        }

        try {
            forkJoinPool = new ForkJoinPool(numThreads);

            if (useCacheSegments) {
                // Strategy 1: Cache-Optimized Segments
                for (int segmentLowIndex = lowIndex; segmentLowIndex <= highIndexLimit; segmentLowIndex += SEGMENT_BITS) {
                    int segmentHighIndex = Math.min(segmentLowIndex + SEGMENT_BITS - 1, highIndexLimit);
                    int bitsInSegment = segmentHighIndex - segmentLowIndex + 1;
                    Callable<List<Integer>> task = createSieveTask(segmentLowIndex, bitsInSegment, basePrimes, limit);
                    futures.add(forkJoinPool.submit(task));
                }
            } else {
                // Strategy 2: Thread Chunks
                long totalIndices = (long) highIndexLimit - lowIndex + 1;
                if (totalIndices > 0) {
                    long indicesPerThread = Math.max(1, totalIndices / numThreads);
                    long remainder = totalIndices % numThreads;
                    int currentChunkLowIndex = lowIndex;
                    int threadsUsed = 0;

                    while (currentChunkLowIndex <= highIndexLimit && threadsUsed < numThreads) {
                        long currentChunkSize = indicesPerThread + (remainder > 0 ? 1 : 0);
                        if (currentChunkSize <= 0) break;

                        int chunkHighIndex = currentChunkLowIndex + (int) currentChunkSize - 1;
                        // Ensure the last chunk doesn't exceed the limit
                        if (threadsUsed == numThreads - 1 || chunkHighIndex > highIndexLimit) {
                           chunkHighIndex = highIndexLimit;
                        }
                        final int bitsInChunk = chunkHighIndex - currentChunkLowIndex + 1;
                        if (bitsInChunk <= 0) break;

                        Callable<List<Integer>> task = createSieveTask(currentChunkLowIndex, bitsInChunk, basePrimes, limit);
                        futures.add(forkJoinPool.submit(task));

                        currentChunkLowIndex = chunkHighIndex + 1;
                        if (remainder > 0) remainder--;
                        threadsUsed++;
                    }
                }
            }

            collectedResults = new ArrayList<>(futures.size());
            for (Future<List<Integer>> future : futures) {
                List<Integer> taskResult = future.get();
                if (taskResult != null) {
                   collectedResults.add(taskResult);
                } else {
                   System.err.printf("Warning: A prime generation task returned null result.%n");
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            handleExecutionError(strategyLabel, e, forkJoinPool);
            return Collections.emptyList();
        } catch (Throwable t) { // Catch OutOfMemoryError etc.
             handleExecutionError(strategyLabel, t, forkJoinPool);
             return Collections.emptyList();
        }
        finally {
            shutdownPool(strategyLabel, forkJoinPool);
        }

        int estimatedSize = 1 + basePrimes.size();
        if (collectedResults != null) {
            for(List<Integer> list : collectedResults) {
                estimatedSize += list.size();
            }
        } else {
             System.err.println("Warning: collectedResults is null during final combination.");
        }

        ArrayList<Integer> allPrimes = new ArrayList<>(estimatedSize);

        if (limit >= 2) {
            allPrimes.add(2);
        }
        allPrimes.addAll(basePrimes);

        if (collectedResults != null) {
            for (List<Integer> list : collectedResults) {
                allPrimes.addAll(list);
            }
        }

        // Final sort is necessary for parallel results
        Collections.sort(allPrimes);
        return allPrimes;
    }

    private static Callable<List<Integer>> createSieveTask(
            int segmentLowIndex, int bitsInSegment, List<Integer> basePrimes, int globalLimit) {
        return () -> {
            SegmentSieveWorker worker = new SegmentSieveWorker(
                    segmentLowIndex, bitsInSegment, basePrimes, globalLimit);
            return worker.sieveSegment();
        };
    }

    private static void handleExecutionError(String label, Throwable t, ForkJoinPool pool) {
         System.err.printf("Error during parallel prime generation %s: %s%n", label, t);
         if (t.getCause() instanceof OutOfMemoryError || t instanceof OutOfMemoryError) {
             System.err.println("!!! OutOfMemoryError likely occurred. Increase heap size (-Xmx) or reduce limit. !!!");
         }
         if (t instanceof InterruptedException) {
             Thread.currentThread().interrupt();
         }
         if (pool != null && !pool.isShutdown()) {
             System.err.println("Attempting emergency shutdown of ForkJoinPool due to error.");
             pool.shutdownNow();
         }
    }

    private static void shutdownPool(String label, ForkJoinPool pool) {
         if (pool != null) {
             pool.shutdown();
             try {
                 if (!pool.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                     pool.shutdownNow();
                 }
             } catch (InterruptedException e) {
                 System.err.printf("Interrupted while waiting for pool %s termination, forcing shutdown.%n", label);
                 pool.shutdownNow();
                 Thread.currentThread().interrupt();
             }
         }
     }


    /**
     * Generates base primes and composite info up to baseLimit using parallel segments.
     * Internal use when sqrt(limit) is large.
     */
    private static BasePrimeResult generateBasePrimesParallelInternal(int baseLimit, int numThreads) {
        if (baseLimit < 3) {
            return sieveOddSequentialBitSet(baseLimit);
        }

        int sqrtBaseLimit = (int) Math.sqrt(baseLimit);
        // Use sequential sieve for the sub-problem (primes up to sqrt(baseLimit))
        BasePrimeResult subBaseInfo = sieveOddSequentialBitSet(sqrtBaseLimit);
        if (subBaseInfo == null || subBaseInfo.primes == null) {
             System.err.printf("Error: Failed to generate sub-base primes (up to %d) for parallel base generation up to %d%n", sqrtBaseLimit, baseLimit);
             return sieveOddSequentialBitSet(baseLimit); // Fallback
        }
        List<Integer> subBasePrimes = subBaseInfo.primes;

        ForkJoinPool forkJoinPool = null;
        List<Future<SegmentResult>> futures = new ArrayList<>();
        int finalBitSetSize = (baseLimit - 1) / 2 + 1;
        BitSet finalCompositeBitSet = new BitSet(finalBitSetSize);
        if (subBaseInfo.compositeInfo != null) {
            subBaseInfo.compositeInfo.stream().forEach(finalCompositeBitSet::set); // Initialize with sub-base info
        }

        // Use CopyOnWriteArrayList for thread-safe adds from tasks
        List<Integer> foundBasePrimesList = new CopyOnWriteArrayList<>(subBasePrimes);

        int lowNum = sqrtBaseLimit + 1;
        if (lowNum % 2 == 0) lowNum++;
        int lowIndex = (lowNum - 1) / 2;
        int highIndexLimit = (baseLimit - 1) / 2;

        if (lowIndex > highIndexLimit) {
             // No segments needed beyond sub-base primes.
             return new BasePrimeResult(finalCompositeBitSet, subBaseInfo.primes);
        }

        try {
            forkJoinPool = new ForkJoinPool(numThreads);
            for (int segmentLowIndex = lowIndex; segmentLowIndex <= highIndexLimit; segmentLowIndex += SEGMENT_BITS) {
                 int segmentHighIndex = Math.min(segmentLowIndex + SEGMENT_BITS - 1, highIndexLimit);
                 int bitsInSegment = segmentHighIndex - segmentLowIndex + 1;
                 final int currentSegmentLowIndex = segmentLowIndex;
                 final List<Integer> currentSubBasePrimes = subBasePrimes;

                 Callable<SegmentResult> task = () -> {
                    SegmentSieveWorker worker = new SegmentSieveWorker(
                        currentSegmentLowIndex, bitsInSegment, currentSubBasePrimes, baseLimit);
                    List<Integer> segmentPrimes = worker.sieveSegment();

                    // Rerun sieve locally to get BitSet (potential redundant work)
                    BitSet segmentComposite = new BitSet(bitsInSegment);
                    long segmentStartNum = 2L * currentSegmentLowIndex + 1;
                    long segmentEndNum = segmentStartNum + 2L * (bitsInSegment - 1);
                    for (int p : currentSubBasePrimes) {
                        long pSquared = (long) p * p; if (pSquared > segmentEndNum) break;
                        long startMultiple;
                        if (pSquared >= segmentStartNum) { startMultiple = pSquared; }
                        else { startMultiple = ((segmentStartNum + p - 1) / p) * p; if (startMultiple % 2 == 0) startMultiple += p; if (startMultiple < pSquared) startMultiple = pSquared; }
                        if (startMultiple <= segmentEndNum) { int startIndexInSegment = (int) ((startMultiple - segmentStartNum) / 2); for (int idx = startIndexInSegment; idx < bitsInSegment; idx += p) { segmentComposite.set(idx); } }
                    }
                    return new SegmentResult(currentSegmentLowIndex, segmentComposite, segmentPrimes);
                 };
                 futures.add(forkJoinPool.submit(task));
            }

            for (Future<SegmentResult> future : futures) {
                 SegmentResult result = future.get();
                 if (result != null) {
                    if (result.primes != null) {
                        foundBasePrimesList.addAll(result.primes);
                    }
                    // Combine segment's composite info into the final BitSet
                    if (result.compositeInfo != null) {
                        result.compositeInfo.stream().forEach(bitIndex -> {
                            int globalIndex = result.segmentStartIndex + bitIndex;
                            if (globalIndex < finalBitSetSize) { // Bounds check
                                finalCompositeBitSet.set(globalIndex);
                            }
                        });
                    }
                 }
            }

        } catch (InterruptedException | ExecutionException e) {
             handleExecutionError("(Parallel Base)", e, forkJoinPool);
             System.err.println("    (Parallel Base) Falling back to sequential base prime generation.");
             return sieveOddSequentialBitSet(baseLimit); // Fallback
        } catch (Throwable t) {
             handleExecutionError("(Parallel Base)", t, forkJoinPool);
             System.err.println("    (Parallel Base) Falling back to sequential base prime generation.");
             return sieveOddSequentialBitSet(baseLimit); // Fallback
        }
        finally {
             shutdownPool("(Parallel Base)", forkJoinPool);
        }

        // Convert COW list to ArrayList for sorting
        ArrayList<Integer> sortedBasePrimes = new ArrayList<>(foundBasePrimesList);
        Collections.sort(sortedBasePrimes);

        return new BasePrimeResult(finalCompositeBitSet, sortedBasePrimes);
    }


    /**
     * Sequential Sieve of Eratosthenes optimized for odd numbers using BitSet.
     * Public for SieveBasedChecker and fallback use.
     */
    public static BasePrimeResult sieveOddSequentialBitSet(int limit) {
        if (limit < 3) {
            int maxIndex = (limit < 1) ? -1 : (limit - 1) / 2;
            BitSet emptyBitSet = new BitSet(Math.max(0, maxIndex + 1));
            return new BasePrimeResult(emptyBitSet, Collections.emptyList());
        }

        int maxIndex = (limit - 1) / 2; // Index represents number 2*i + 1
        BitSet isComposite = new BitSet(maxIndex + 1);
        int sqrtLimit = (int) Math.sqrt(limit);
        int sqrtLimitIndex = (sqrtLimit - 1) / 2;

        for (int p_idx = 1; p_idx <= sqrtLimitIndex; p_idx++) {
            if (!isComposite.get(p_idx)) {
                int p = 2 * p_idx + 1;
                // Avoid potential overflow before calculating p*p
                if (p > Math.sqrt(Integer.MAX_VALUE)) {
                    // Overflow handling is complex, assume limit fits int for standard sieve.
                }

                long pp = (long) p * p;
                if (pp > limit) continue;

                int pp_idx = (int) ((pp - 1) / 2);
                // Mark multiples. Stride is p for indices (skipping evens).
                for (int multiple_idx = pp_idx; multiple_idx <= maxIndex; multiple_idx += p) {
                    isComposite.set(multiple_idx);
                }
            }
        }

        List<Integer> primes = new ArrayList<>(limit > 10 ? (int)(limit / (Math.log(limit) * 2)) : 5);
        for (int i = 1; i <= maxIndex; i++) { // Start from index 1 (number 3)
            if (!isComposite.get(i)) {
                primes.add(2 * i + 1);
            }
        }
        return new BasePrimeResult(isComposite, primes);
    }

}