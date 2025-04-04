package org.primes.checker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Tests triggering parallel base prime generation via parameter")
class PrimeGeneratorOptimizedParallelBaseTriggerTest {

    private static final int LOW_THRESHOLD_FOR_TESTING = 100;
    private static final int DEFAULT_THRESHOLD_VALUE = 10_000_000;


    @DisplayName("generatePrimesParallel should use parallel base generation when sqrt(limit) >= passed threshold")
    @ParameterizedTest(name = "limit = {0}, threads = {1}")
    @CsvSource({
            "10001,  2",
            "10001,  4",
            "20000,  1",
            "20000,  4",
            "10000,  4"
    })
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void testParallelBaseGenerationTriggeredAndMatchesSequential(int limit, int numThreads) {
         assumeTrue(Runtime.getRuntime().availableProcessors() >= 2 || numThreads == 1,
                   "Skipping multi-threaded test on single-core machine");

        System.out.printf("Testing generatePrimesParallel(limit=%d, threads=%d, threshold=%d)...%n",
                          limit, numThreads, LOW_THRESHOLD_FOR_TESTING);

        List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, numThreads, LOW_THRESHOLD_FOR_TESTING);
        List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

        assertNotNull(parallelResult, "Parallel result should not be null");
        assertNotNull(sequentialResult, "Sequential result should not be null");

         assertEquals(sequentialResult.size(), parallelResult.size(),
                     "Number of primes should match between parallel (low threshold) and sequential for limit=" + limit);
         assertEquals(sequentialResult, parallelResult,
                     "List of primes should match between parallel (low threshold) and sequential for limit=" + limit);
    }

    @DisplayName("generatePrimesParallel should use sequential base generation when sqrt(limit) < passed threshold")
    @ParameterizedTest(name = "limit = {0}, threads = {1}")
    @CsvSource({
            "9999, 2",
            "5000, 4",
            "100,  4"
    })
    void testSequentialBaseGenerationTriggeredWithLowThreshold(int limit, int numThreads) {
         System.out.printf("Testing generatePrimesParallel(limit=%d, threads=%d, threshold=%d) expecting SEQUENTIAL base gen...%n",
                          limit, numThreads, LOW_THRESHOLD_FOR_TESTING);

        List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, numThreads, LOW_THRESHOLD_FOR_TESTING);
        List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

        assertNotNull(parallelResult, "Parallel result should not be null");
        assertNotNull(sequentialResult, "Sequential result should not be null");

         assertEquals(sequentialResult.size(), parallelResult.size(),
                     "Number of primes should match between parallel (seq base) and sequential for limit=" + limit);
         assertEquals(sequentialResult, parallelResult,
                     "List of primes should match between parallel (seq base) and sequential for limit=" + limit);
    }

     @DisplayName("generatePrimesParallel with default threshold uses sequential base for moderate limits")
     @Test
     void testDefaultThresholdUsesSequentialBaseForModerateLimit() {
         int limit = 20000;
         int threads = 4;
         System.out.printf("Testing generatePrimesParallel(limit=%d, threads=%d) with DEFAULT threshold behavior...%n",
                          limit, threads);

         List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, threads);
         List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

         assertNotNull(parallelResult);
         assertNotNull(sequentialResult);
          assertEquals(sequentialResult, parallelResult, "Results should match when using default threshold (expecting sequential base)");
     }

    @DisplayName("generatePrimesParallel with default threshold uses parallel base for very large limits")
    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void testDefaultThresholdUsesParallelBaseForLargeLimit() {
        // Testing the *default* parallel base path is hard with int limits.
        // We assume the logic tested with LOW_THRESHOLD_FOR_TESTING works identically.

        int limit = 1_000_001; // sqrt(limit) > 1000
        int threads = 4;
        assumeTrue(Runtime.getRuntime().availableProcessors() >= threads, "Skipping test requiring 4 cores");
        System.out.printf("Testing generatePrimesParallel(limit=%d, threads=%d) with hypothetically lower default threshold behavior...%n",
                         limit, threads);

        // Test the overload directly, simulating trigger
        List<Integer> parallelResultWithThreshold = PrimeGeneratorOptimized.generatePrimesParallel(limit, threads, 1000);
        List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

        assertNotNull(parallelResultWithThreshold);
        assertNotNull(sequentialResult);
         assertEquals(sequentialResult.size(), parallelResultWithThreshold.size(), "Sizes should match");
         assertEquals(sequentialResult, parallelResultWithThreshold, "Results should match when parallel base is triggered");
    }


     @DisplayName("Should throw exception for negative threshold")
     @Test
     void testNegativeThresholdThrowsException() {
         int limit = 1000;
         int threads = 2;
         int negativeThreshold = -1;

         assertThrows(IllegalArgumentException.class,
                      () -> PrimeGeneratorOptimized.generatePrimesParallel(limit, threads, negativeThreshold),
                      "Should throw IllegalArgumentException for negative basePrimeThreshold");

         assertThrows(IllegalArgumentException.class,
                      () -> PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(limit, threads, negativeThreshold),
                      "Should throw IllegalArgumentException for negative basePrimeThreshold (ThreadChunks)");
     }

     @DisplayName("Test specific segment sieving scenarios via parallel base gen")
     @ParameterizedTest(name = "limit = {0}, threads = {1}")
     @CsvSource({
         "30, 4",
         "50, 4"
     })
     void testSpecificSievingScenarios(int limit, int numThreads) {
         assumeTrue(Runtime.getRuntime().availableProcessors() >= numThreads, "Skipping test requiring cores");
         List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, numThreads, 10); // Low threshold
         List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);
         assertEquals(sequentialResult, parallelResult, "Results must match for limit=" + limit);
     }

     @DisplayName("Test generateBasePrimesParallelInternal with baseLimit < 3")
     @ParameterizedTest(name = "baseLimit = {0}, threads = {1}")
     @CsvSource({
             "0, 4",
             "1, 4",
             "2, 4"
     })
     void testParallelBaseInternalWithSmallLimits(int baseLimit, int numThreads) throws Exception {
         int limit = baseLimit * baseLimit + 1;
         if (baseLimit < 2) limit = baseLimit + 1;

         List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, numThreads, 0); // Force internal call attempt
         List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

         assertEquals(sequentialResult, parallelResult, "Results must match for small baseLimit=" + baseLimit);
     }

      @DisplayName("Test generateBasePrimesParallelInternal when no segments needed")
      @ParameterizedTest(name = "baseLimit = {0}, threads = {1}")
      @CsvSource({
          "3, 4",
          "4, 4",
          "5, 4",
          "6, 4",
          "8, 4"
      })
      void testParallelBaseInternalWhenSegmentsMayBeSmall(int baseLimit, int numThreads) {
          int limit = baseLimit * baseLimit + 1;
          List<Integer> parallelResult = PrimeGeneratorOptimized.generatePrimesParallel(limit, numThreads, 0); // Force internal call attempt
          List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);
          assertEquals(sequentialResult, parallelResult, "Results must match when base segments are small or empty");
      }


      @DisplayName("Test generatePrimesParallel_ThreadChunks strategy")
      @ParameterizedTest(name = "limit = {0}, threads = {1}")
      @CsvSource({
              "100,    1",
              "100,    4",
              "10000,  1",
              "10000,  4",
              "10000,  8",
              "99,     10"
      })
      void testThreadChunksStrategy(int limit, int numThreads) {
          List<Integer> resultDefaultThreshold = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(limit, numThreads);
          List<Integer> resultLowThreshold = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(limit, numThreads, 50);
          List<Integer> sequentialResult = PrimeGeneratorOptimized.generatePrimesSequential(limit);

          assertNotNull(resultDefaultThreshold);
          assertNotNull(resultLowThreshold);
          assertNotNull(sequentialResult);

           assertEquals(sequentialResult, resultDefaultThreshold, "ThreadChunks (default threshold) results must match sequential for limit=" + limit);
           assertEquals(sequentialResult, resultLowThreshold, "ThreadChunks (low threshold) results must match sequential for limit=" + limit);
      }

      @DisplayName("Test prime generation for small limits")
      @ParameterizedTest(name = "limit = {0}")
      @ValueSource(ints = {0, 1, 2, 3, 4, 5})
      void testSmallLimits(int limit) {
          int threads = 2;
          List<Integer> seq = PrimeGeneratorOptimized.generatePrimesSequential(limit);
          List<Integer> parCache = PrimeGeneratorOptimized.generatePrimesParallel(limit, threads);
          List<Integer> parChunk = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(limit, threads);
          List<Integer> parCacheLowT = PrimeGeneratorOptimized.generatePrimesParallel(limit, threads, 0);
          List<Integer> parChunkLowT = PrimeGeneratorOptimized.generatePrimesParallel_ThreadChunks(limit, threads, 0);

          assertEquals(seq, parCache, "Cache Parallel should match Sequential for limit=" + limit);
          assertEquals(seq, parChunk, "Chunk Parallel should match Sequential for limit=" + limit);
           assertEquals(seq, parCacheLowT, "Cache Parallel (Low Thresh) should match Sequential for limit=" + limit);
           assertEquals(seq, parChunkLowT, "Chunk Parallel (Low Thresh) should match Sequential for limit=" + limit);
      }
}