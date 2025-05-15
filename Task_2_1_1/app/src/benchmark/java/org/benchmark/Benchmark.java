// src/benchmark/java/org/benchmark/Benchmark.java
package org.benchmark;

import org.primes.checker.*;

import java.io.BufferedWriter; // Для CSV
import java.io.IOException; // Для CSV
import java.nio.file.Files; // Для CSV
import java.nio.file.Paths; // Для CSV
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics; // Для статистики
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Для requireNonNull
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Для CSV

public class Benchmark {

    // --- Benchmark Configuration ---
    private static final int ORIGINAL_ARRAY_SIZE = 1_000_000;
    private static final int ORIGINAL_MAX_NUMBER = 20_000_000;
    private static final int WARMUP_RUNS = 5; // Увеличено
    private static final int BENCHMARK_RUNS = 10; // Увеличено
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16, 32};
    private static final int MAX_SIEVE_VALUE_LIMIT = 100_000_000; // Лимит для Sieve (100 млн ~ 12MB BitSet)
    private static final boolean WRITE_CSV_RESULTS = true; // Включить/выключить запись CSV
    private static final String CSV_FILENAME = "benchmark_results.csv";
    // --- End Original Configuration ---

    // --- Configuration for Additional Scenarios ---
    private static final int EARLY_EXIT_ARRAY_SIZE = 1_000_000;
    private static final int EARLY_EXIT_MAX_NUMBER = 20_000_000;
    private static final int EARLY_EXIT_NON_PRIME_INDEX = 10;
    // Новые индексы для других сценариев выхода
    private static final int MIDDLE_EXIT_NON_PRIME_INDEX_DIVISOR = 2; // Поместить в середину
    private static final int END_EXIT_NON_PRIME_INDEX_OFFSET = 10; // Поместить ближе к концу

    private static final int LARGE_NUM_ARRAY_SIZE = 50_000;
    private static final int LARGE_NUM_MAX_VALUE = Integer.MAX_VALUE - 50;

    private static final int[] INCREASING_SIZES = {1_000, 10_000, 100_000, 1_000_000};
    private static final int INCREASING_SIZES_MAX_NUMBER = 20_000_000;
    // --- End Additional Configuration ---

    // --- Checkers (shared instance for stateless checkers) ---
    private static final SequentialChecker sequentialChecker = new SequentialChecker();
    private static final ParallelStreamChecker parallelStreamChecker = new ParallelStreamChecker();
    private static final SieveBasedChecker sieveBasedChecker = new SieveBasedChecker();
    // ThreadCheckers будут создаваться для каждого сценария

    // --- Data Collection for CSV ---
    private static final List<Map<String, Object>> allResultsData = new ArrayList<>();

    // --- Helper Structures ---

    /** Configuration for a benchmark scenario */
    private record ScenarioConfig(
            String label,
            int arraySize,
            int maxNumber, // Target max number for generation
            int[] testArray // The actual data
    ) {
        ScenarioConfig {
            Objects.requireNonNull(label, "Scenario label cannot be null");
            Objects.requireNonNull(testArray, "Scenario test array cannot be null");
        }

        // Helper to get the actual max value present in the generated data
        int getActualMaxNumber() {
            return Arrays.stream(testArray).max().orElse(-1);
        }
    }

    /** Stores detailed statistics for a benchmark run series */
    private static class NamedResult {
        final String name;
        final long avgTimeNs;
        final long minTimeNs;
        final long maxTimeNs;
        final double stdDevNs;
        final int successfulRuns;
        final int totalRuns; // Сколько всего было запусков (включая неудачные)

        NamedResult(String name, long avgTimeNs, long minTimeNs, long maxTimeNs, double stdDevNs, int successfulRuns, int totalRuns) {
            this.name = name;
            this.avgTimeNs = avgTimeNs;
            this.minTimeNs = minTimeNs;
            this.maxTimeNs = maxTimeNs;
            this.stdDevNs = stdDevNs;
            this.successfulRuns = successfulRuns;
            this.totalRuns = totalRuns;
        }

        boolean isSuccessful() {
            // Считаем успешным, если хотя бы один запуск прошел
            return successfulRuns > 0 && avgTimeNs >= 0;
        }

        // Метод для вывода статистики в миллисекундах
        String getStatsStringMs() {
            if (totalRuns == 0) return "No runs";
            if (!isSuccessful()) return String.format("Failed (%d/%d runs)", successfulRuns, totalRuns);

            long avgMs = TimeUnit.NANOSECONDS.toMillis(avgTimeNs);
            long minMs = TimeUnit.NANOSECONDS.toMillis(minTimeNs);
            long maxMs = TimeUnit.NANOSECONDS.toMillis(maxTimeNs);
            // Преобразуем stddev в ms для примерного представления
            double stdDevMs = (double) TimeUnit.NANOSECONDS.toMillis((long) stdDevNs);

            return String.format("avg %4dms (min %4d, max %4d, ±%.2fms | %d/%d runs)",
                    avgMs, minMs, maxMs, stdDevMs, successfulRuns, totalRuns);
        }
    }

    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("Starting Multi-Scenario Benchmark (Task 2.1.1 Checkers)...");
        System.out.println("Warmup Runs: " + WARMUP_RUNS + " | Benchmark Runs: " + BENCHMARK_RUNS);
        System.out.println("Sieve Max Value Limit: " + String.format("%,d", MAX_SIEVE_VALUE_LIMIT));
        System.out.println("NOTE: System.gc() and Thread.sleep() removed for potentially more realistic (but maybe less stable run-to-run) results.");
        System.out.println("Consider using JMH for rigorous benchmarking.");
        System.out.println("======================================================");

        // --- Scenario 1: Original Benchmark (Mostly Large Primes) ---
        runBenchmarkScenario(new ScenarioConfig(
                "Original",
                ORIGINAL_ARRAY_SIZE,
                ORIGINAL_MAX_NUMBER,
                generateMostlyPrimesArray(ORIGINAL_ARRAY_SIZE, ORIGINAL_MAX_NUMBER)
        ));

        // --- Scenario 2: Early Exit ---
        runBenchmarkScenario(new ScenarioConfig(
                "Early Exit",
                EARLY_EXIT_ARRAY_SIZE,
                EARLY_EXIT_MAX_NUMBER,
                generateArrayWithNonPrimeAtIndex(EARLY_EXIT_ARRAY_SIZE, EARLY_EXIT_MAX_NUMBER, EARLY_EXIT_NON_PRIME_INDEX, "Early")
        ));

        // --- Scenario 2b: Middle Exit ---
        int middleIndex = EARLY_EXIT_ARRAY_SIZE / MIDDLE_EXIT_NON_PRIME_INDEX_DIVISOR;
        runBenchmarkScenario(new ScenarioConfig(
                "Middle Exit",
                EARLY_EXIT_ARRAY_SIZE,
                EARLY_EXIT_MAX_NUMBER,
                generateArrayWithNonPrimeAtIndex(EARLY_EXIT_ARRAY_SIZE, EARLY_EXIT_MAX_NUMBER, middleIndex, "Middle")
        ));

        // --- Scenario 2c: End Exit ---
        int endIndex = Math.max(0, EARLY_EXIT_ARRAY_SIZE - END_EXIT_NON_PRIME_INDEX_OFFSET - 1); // -1 т.к. индекс с 0
        runBenchmarkScenario(new ScenarioConfig(
                "End Exit",
                EARLY_EXIT_ARRAY_SIZE,
                EARLY_EXIT_MAX_NUMBER,
                generateArrayWithNonPrimeAtIndex(EARLY_EXIT_ARRAY_SIZE, EARLY_EXIT_MAX_NUMBER, endIndex, "End")
        ));


        // --- Scenario 3: Large Numbers (Near Integer.MAX_VALUE) ---
        runBenchmarkScenario(new ScenarioConfig(
                "Large Numbers",
                LARGE_NUM_ARRAY_SIZE,
                LARGE_NUM_MAX_VALUE, // Target max
                generateLargePrimesArray(LARGE_NUM_ARRAY_SIZE, LARGE_NUM_MAX_VALUE)
        ));

        // --- Scenario 4: Increasing Array Size ---
        System.out.println("\n--- Scenario Group: Increasing Array Size ---");
        for (int currentSize : INCREASING_SIZES) {
            String sizeLabel = String.format("Increasing Size (N=%,d)", currentSize);
            System.out.printf("\n--- Running for Size: %,d ---\n", currentSize);
            runBenchmarkScenario(new ScenarioConfig(
                    sizeLabel,
                    currentSize,
                    INCREASING_SIZES_MAX_NUMBER,
                    generateMostlyPrimesArray(currentSize, INCREASING_SIZES_MAX_NUMBER)
            ));
        }
         System.out.println("\n--- End Scenario Group: Increasing Array Size ---");

        System.out.println("\n======================================================");
        System.out.println("All Benchmark Scenarios Complete.");
        System.out.println("======================================================");

        // --- Write CSV Results ---
        if (WRITE_CSV_RESULTS) {
            writeResultsToCsv(CSV_FILENAME);
        }
    }

    /**
     * Runs a complete benchmark scenario for a given configuration.
     */
    private static void runBenchmarkScenario(ScenarioConfig config) {
        System.out.printf("\n===== Starting Benchmark Scenario: %s =====%n", config.label());
        int actualMaxNumber = config.getActualMaxNumber(); // Get actual max from data
        System.out.printf("Array Size: %,d | Target Max Num: %,d | Actual Max Num: %,d%n",
                config.arraySize(), config.maxNumber(), actualMaxNumber);

        boolean expectedResult = sequentialChecker.containsNonPrime(config.testArray());
        System.out.println("Expected result (contains non-prime?): " + expectedResult);
        validateTestData(config, expectedResult);
        System.out.println("--------------------------------------------------");

        // --- Create ThreadCheckers ONCE for this scenario ---
        List<ThreadChecker> threadCheckers = new ArrayList<>();
        for (int count : THREAD_COUNTS) {
            threadCheckers.add(new ThreadChecker(count));
        }

        // Data structures for timings (List per method per run)
        List<Long> sequentialTimes = new ArrayList<>();
        Map<Integer, List<Long>> threadTimesMap = new HashMap<>(); // Key: thread count
        for (int count : THREAD_COUNTS) threadTimesMap.put(count, new ArrayList<>());
        List<Long> parallelStreamTimes = new ArrayList<>();
        List<Long> sieveBasedTimes = new ArrayList<>();

        try {
            // --- Warmup ---
            System.out.println("Running warmup iterations...");
            for (int i = 0; i < WARMUP_RUNS; i++) {
                System.out.printf(" Warmup Run %d/%d%n", i + 1, WARMUP_RUNS);
                runAllCheckers(config.testArray(), actualMaxNumber, threadCheckers,
                               null, null, null, null, // No recording during warmup
                               expectedResult);
                 // No GC or Sleep
            }
            System.out.println("Warmup complete.");
            System.out.println("--------------------------------------------------");

            // --- Benchmark ---
            System.out.println("Running benchmark iterations...");
            for (int i = 0; i < BENCHMARK_RUNS; i++) {
                System.out.printf(" Benchmark Run %d/%d:%n", i + 1, BENCHMARK_RUNS);
                runAllCheckers(config.testArray(), actualMaxNumber, threadCheckers,
                               sequentialTimes, threadTimesMap, parallelStreamTimes, sieveBasedTimes,
                               expectedResult);
                 // No GC or Sleep
                System.out.println("---");
            }
            System.out.println("Benchmark complete.");
            System.out.println("--------------------------------------------------");

            // --- Calculate Statistics and Store Results ---
            Map<String, NamedResult> resultsMap = new HashMap<>();

            NamedResult seqResult = calculateStatistics("Sequential", sequentialTimes);
            resultsMap.put(seqResult.name, seqResult);

            for (int i = 0; i < THREAD_COUNTS.length; i++) {
                int count = THREAD_COUNTS[i];
                String name = String.format("Thread (%2d threads)", count);
                NamedResult thrResult = calculateStatistics(name, threadTimesMap.get(count));
                resultsMap.put(name, thrResult);
            }

            NamedResult parResult = calculateStatistics("Parallel Stream", parallelStreamTimes);
            resultsMap.put(parResult.name, parResult);

            NamedResult sieveResult = calculateStatistics("Sieve Based", sieveBasedTimes);
            resultsMap.put(sieveResult.name, sieveResult);

            // --- Print Results Table ---
            System.out.printf("\n========= Benchmark Results for %s =========%n", config.label());
            printResult(seqResult, -1); // Sequential is the baseline
            resultsMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("Sequential")) // Skip sequential itself
                    .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Benchmark::getSortOrderForKey))) // Sort nicely
                    .forEach(entry -> printResult(entry.getValue(), seqResult.avgTimeNs));
            System.out.println("==========================================================");

            // --- Print Dynamic Notes ---
            printDynamicNotes(config.label(), resultsMap, seqResult);

            // --- Collect data for CSV ---
            if (WRITE_CSV_RESULTS) {
                 collectResultsForCsv(config, actualMaxNumber, resultsMap);
            }

        } finally {
            // --- Shutdown ThreadChecker pools ---
            System.out.println("Shutting down ThreadChecker pools for scenario: " + config.label());
            for (ThreadChecker checker : threadCheckers) {
                checker.shutdownPool();
            }
            System.out.println("Pools shut down.");
        }
        System.out.println("===== Scenario " + config.label() + " Finished =====");
    }

     /** Assigns a sort order to method names for cleaner table output */
     private static int getSortOrderForKey(String key) {
         if (key.startsWith("Thread")) return 1;
         if (key.equals("Parallel Stream")) return 2;
         if (key.equals("Sieve Based")) return 3;
         return 0; // Sequential (shouldn't be needed here, but...)
     }

    /** Validates generated test data against scenario expectations */
    private static void validateTestData(ScenarioConfig config, boolean expectedResult) {
        // Warnings/Errors remain the same logic
        if (expectedResult && config.label().toLowerCase().contains("original")) {
            System.err.println("WARNING: Original benchmark data contains non-primes. Early exit might affect timings.");
        } else if (expectedResult && config.label().toLowerCase().contains("increasing size")) {
             // It's ok if increasing size data sometimes contains non-primes
        } else if (!expectedResult && (config.label().toLowerCase().contains("exit"))) {
            System.err.printf("ERROR: %s array generation failed - expected non-prime was not found by sequential checker.%n", config.label());
        } else if (expectedResult && config.label().toLowerCase().contains("large number")) {
             System.err.println("WARNING: Large number data generation included non-primes.");
        }
    }

    /** Runs all checker types, handles Sieve limit/OOM, and records timing. */
    private static void runAllCheckers(int[] array,
                                       int actualMaxVal,
                                       List<ThreadChecker> threadCheckers, // Pass list of pre-created checkers
                                       List<Long> seqTimesRec, Map<Integer, List<Long>> thrTimesRecMap,
                                       List<Long> parTimesRec, List<Long> sieveTimesRec,
                                       boolean expectedResult) {
        long start, end;
        boolean result;
        String label;
        long durationNs;

        // 1. Sequential
        label = "Sequential";
        start = System.nanoTime();
        result = sequentialChecker.containsNonPrime(array);
        end = System.nanoTime();
        durationNs = end - start;
        recordTime(durationNs, result, label, seqTimesRec, expectedResult);

        // 2. Threaded (using pre-created checkers)
        for (int i = 0; i < THREAD_COUNTS.length; i++) {
            int count = THREAD_COUNTS[i];
            ThreadChecker thrChecker = threadCheckers.get(i); // Get the existing checker
            label = String.format("Thread (%2d thr)", count);
            start = System.nanoTime();
            result = thrChecker.containsNonPrime(array); // Use the checker's pool
            end = System.nanoTime();
            durationNs = end - start;
             recordTime(durationNs, result, label, thrTimesRecMap == null ? null : thrTimesRecMap.get(count), expectedResult);
        }

        // 3. Parallel Stream
        label = "Parallel Stream";
        start = System.nanoTime();
        result = parallelStreamChecker.containsNonPrime(array);
        end = System.nanoTime();
        durationNs = end - start;
        recordTime(durationNs, result, label, parTimesRec, expectedResult);

        // 4. Sieve Based - with Size Limit Check and OOM Handling
        label = "Sieve Based";
        long sieveStart;
        long sieveDuration = -2L; // Use -2L to indicate "Skipped"
        boolean sieveResult = false; // Default result if skipped/failed

        if (actualMaxVal > MAX_SIEVE_VALUE_LIMIT) {
            System.err.printf("\n!!! Skipping SieveBasedChecker: Actual max value (%,d) exceeds limit (%,d). !!!%n",
                              actualMaxVal, MAX_SIEVE_VALUE_LIMIT);
            // duration remains -2L (Skipped)
        } else {
            sieveStart = System.nanoTime();
            try {
                sieveResult = sieveBasedChecker.containsNonPrime(array);
                long sieveEnd = System.nanoTime();
                sieveDuration = sieveEnd - sieveStart; // Success
            } catch (OutOfMemoryError oom) {
                System.err.printf("\n!!! OutOfMemoryError caught during SieveBasedChecker (maxVal=%,d). Marking as Failed. Increase heap size (-Xmx) or MAX_SIEVE_VALUE_LIMIT. !!!%n", actualMaxVal);
                sieveDuration = -1L; // Mark as Failed
            } catch (Throwable t) {
                 System.err.printf("\n!!! Exception caught during SieveBasedChecker: %s. Marking as Failed. !!!%n", t);
                 sieveDuration = -1L; // Mark as Failed
            }
        }
        recordTime(sieveDuration, sieveResult, label, sieveTimesRec, expectedResult);
    }

    /** Helper to record time, print output, and check result consistency. */
    private static void recordTime(long durationNs, boolean result, String label, List<Long> timeList, boolean expectedResult) {
        if (timeList != null) {
            // Add duration if successful (>=0), or the error code (-1 Failed, -2 Skipped)
            timeList.add(durationNs);
        }

        String timeStr = formatNanosToMsInfo(durationNs); // Use helper
        String resultStr = "---";
        String unexpectedStr = "";

        // Only check result consistency if the run was successful (duration >= 0)
        if (durationNs >= 0) {
            resultStr = String.format("%5b", result);
            if (result != expectedResult) {
                unexpectedStr = " <<< UNEXPECTED";
                System.err.printf("Error: Unexpected result for %s. Expected %b, Got %b%n", label, expectedResult, result);
            }
        } else if (durationNs == -1L){
             resultStr = "FAIL";
        } else if (durationNs == -2L) {
             resultStr = "SKIP";
        }


        System.out.printf("  %-18s: %s ms (Res: %s)%s%n",
                         label, timeStr, resultStr, unexpectedStr);
    }

    /** Helper to print final benchmark results table row with stats and comparison */
    private static void printResult(NamedResult result, long sequentialAvgTimeNs) {
        String statsStr = result.getStatsStringMs();
        System.out.printf("%-20s: %s", result.name, statsStr);

        // Only show comparison if the current result is successful
        if (result.isSuccessful()) {
            if (!result.name.equals("Sequential")) { // Don't compare sequential to itself
                 if (sequentialAvgTimeNs >= 0) { // Only compare if sequential was successful
                     System.out.print(getComparisonString(result.avgTimeNs, sequentialAvgTimeNs, "Seq"));
                 } else {
                     System.out.print(" (vs Seq: N/A - Seq Failed)");
                 }
            }
        } else if (!result.name.equals("Sequential")) {
            // Handle case where current failed, but we still note Seq failure if relevant
            if (sequentialAvgTimeNs < 0) {
                 System.out.print(" (vs Seq: N/A - Both Failed)");
            } else {
                 System.out.print(" (vs Seq: N/A)"); // Current failed
            }
        }
        System.out.printf("%n");
    }

    /** Helper to format nanoseconds to milliseconds string, handling errors */
    private static String formatNanosToMsInfo(long timeNs) {
        if (timeNs >= 0) {
            return String.format("%8d", TimeUnit.NANOSECONDS.toMillis(timeNs));
        } else if (timeNs == -1L) {
            return String.format("%8s", "Failed");
        } else if (timeNs == -2L) {
            return String.format("%8s", "Skipped");
        } else {
            return String.format("%8s", "Error"); // Should not happen
        }
    }

    /** Helper to generate the comparison string (Speedup/Slowdown) */
    private static String getComparisonString(long currentTimeNs, long baselineTimeNs, String baselineName) {
        if (currentTimeNs <= 0 || baselineTimeNs <= 0) {
            return ""; // Cannot compare if either failed
        }
        double ratio = (double) baselineTimeNs / currentTimeNs;
        if (ratio >= 0.995) { // Use a threshold for near-equal performance
            return String.format(" (Speedup vs %s: %.2fx)", baselineName, ratio);
        } else {
            double slowdown = (double) currentTimeNs / baselineTimeNs;
            return String.format(" (Slowdown vs %s: %.2fx)", baselineName, slowdown);
        }
    }


    /**
     * Prints dynamic comparison notes based on the statistics.
     */
    private static void printDynamicNotes(String scenarioLabel, Map<String, NamedResult> resultsMap, NamedResult sequentialResult) {
        System.out.printf("Notes (%s):%n", scenarioLabel);

        // Find fastest, slowest successful runs
        NamedResult fastest = null;
        NamedResult slowest = null;

        List<NamedResult> successfulRuns = resultsMap.values().stream()
                .filter(NamedResult::isSuccessful)
                .toList();

        if (successfulRuns.isEmpty()) {
            System.out.println("  - All runs failed or were skipped, no comparison possible.");
            return;
        }

        fastest = successfulRuns.stream().min(Comparator.comparingLong(r -> r.avgTimeNs)).orElse(null);
        slowest = successfulRuns.stream().max(Comparator.comparingLong(r -> r.avgTimeNs)).orElse(null);

        // --- Print Fastest/Slowest ---
        if (fastest != null) {
            System.out.printf("  - Fastest: %s (%s)%n", fastest.name, fastest.getStatsStringMs());
        }
        if (slowest != null && fastest != null && !slowest.name.equals(fastest.name)) {
            System.out.printf("  - Slowest: %s (%s)", slowest.name, slowest.getStatsStringMs());
            if (fastest.avgTimeNs > 0) {
                System.out.printf(" - %.2fx slower than fastest%n", (double) slowest.avgTimeNs / fastest.avgTimeNs);
            } else { System.out.printf("%n"); }
        } else if (slowest != null && fastest != null && slowest.name.equals(fastest.name) && successfulRuns.size() > 1) {
             System.out.println("  - All successful runs had very similar average times.");
        } else if (slowest != null && fastest != null && slowest.name.equals(fastest.name)){
             System.out.println("  - Only one successful run type.");
        }

        // --- Compare to Sequential ---
        if (sequentialResult != null && sequentialResult.isSuccessful()) {
            if (fastest != null && !fastest.name.equals("Sequential")) {
                System.out.printf("  - Fastest (%s) is %.2f%s faster than Sequential%n",
                                  fastest.name, (double) sequentialResult.avgTimeNs / fastest.avgTimeNs, "x");
            } else if (fastest != null && fastest.name.equals("Sequential")){
                System.out.println("  - Sequential was the fastest.");
            }

            if (slowest != null && !slowest.name.equals("Sequential") && slowest.avgTimeNs > sequentialResult.avgTimeNs) {
                System.out.printf("  - Slowest (%s) is %.2f%s slower than Sequential%n",
                                  slowest.name, (double) slowest.avgTimeNs / sequentialResult.avgTimeNs, "x");
            } else if (slowest != null && slowest.name.equals("Sequential") && fastest != null && !fastest.name.equals("Sequential")) {
                System.out.println("  - Sequential was the slowest successful run.");
            }
        } else {
            System.out.println("  - Sequential run failed or was skipped, cannot compare relative to it.");
        }

        // --- Compare Parallel Stream vs Best Thread ---
        NamedResult parallelStreamResult = resultsMap.get("Parallel Stream");
        NamedResult bestThreadResult = resultsMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("Thread") && entry.getValue().isSuccessful())
                .map(Map.Entry::getValue)
                .min(Comparator.comparingLong(r -> r.avgTimeNs))
                .orElse(null);

        if (parallelStreamResult != null && parallelStreamResult.isSuccessful() && bestThreadResult != null) {
             System.out.printf("  - Parallel Stream vs Best Thread (%s):%s%n",
                 bestThreadResult.name,
                 getComparisonString(parallelStreamResult.avgTimeNs, bestThreadResult.avgTimeNs, "Best Thread")
             );
        } else if (parallelStreamResult != null && parallelStreamResult.isSuccessful() && bestThreadResult == null) {
             System.out.println("  - Parallel Stream succeeded, but all Thread runs failed.");
        } else if (bestThreadResult != null && (parallelStreamResult == null || !parallelStreamResult.isSuccessful())) {
             System.out.println("  - Best Thread run succeeded, but Parallel Stream failed.");
        }
    }

    /** Calculates average, min, max, standard deviation from a list of nanosecond timings. */
    private static NamedResult calculateStatistics(String name, List<Long> times) {
        if (times == null) {
            // Handle case where the list itself might be null (though shouldn't happen with current logic)
            return new NamedResult(name, -1L, -1L, -1L, 0.0, 0, 0);
        }
        int totalRuns = times.size();
        if (totalRuns == 0) {
             return new NamedResult(name, -1L, -1L, -1L, 0.0, 0, 0);
        }

        // Use Stream API for statistics on successful runs (time >= 0)
        DoubleSummaryStatistics stats = times.stream()
                .filter(time -> time >= 0) // Only successful runs
                .mapToDouble(Long::doubleValue)
                .summaryStatistics();

        int successfulRuns = (int) stats.getCount();
        if (successfulRuns == 0) {
            return new NamedResult(name, -1L, -1L, -1L, 0.0, 0, totalRuns); // Failed all runs
        }

        long avgNs = (long) stats.getAverage();
        long minNs = (long) stats.getMin();
        long maxNs = (long) stats.getMax();

        // Calculate standard deviation manually for successful runs
        final double mean = stats.getAverage(); // Use double precision mean for stddev calc
        double variance = times.stream()
                .filter(time -> time >= 0)
                .mapToDouble(Long::doubleValue)
                .map(time -> Math.pow(time - mean, 2))
                .average()
                .orElse(0.0);
        double stdDevNs = Math.sqrt(variance);

        return new NamedResult(name, avgNs, minNs, maxNs, stdDevNs, successfulRuns, totalRuns);
    }

     // --- Data Collection for CSV ---

     /** Collects results from a scenario into the global list for CSV export */
     private static void collectResultsForCsv(ScenarioConfig config, int actualMaxNumber, Map<String, NamedResult> resultsMap) {
         resultsMap.forEach((methodName, result) -> {
             Map<String, Object> row = new HashMap<>();
             row.put("Scenario", config.label());
             row.put("ArraySize", config.arraySize());
             row.put("TargetMaxNumber", config.maxNumber());
             row.put("ActualMaxNumber", actualMaxNumber);
             row.put("Method", methodName);
             // Extract thread count if applicable
             int threads = 1; // Default for Sequential, Parallel Stream, Sieve
             if (methodName.startsWith("Thread")) {
                 try {
                     threads = Integer.parseInt(methodName.replaceAll("[^0-9]", ""));
                 } catch (NumberFormatException e) { /* ignore, use default */ }
             }
             row.put("Threads", threads);
             row.put("AvgTimeNs", result.avgTimeNs); // Store raw Ns
             row.put("MinTimeNs", result.minTimeNs);
             row.put("MaxTimeNs", result.maxTimeNs);
             row.put("StdDevNs", result.stdDevNs);
             row.put("SuccessfulRuns", result.successfulRuns);
             row.put("TotalRuns", result.totalRuns);
             row.put("Status", result.isSuccessful() ? "Success" : (result.totalRuns > 0 ? "Failed" : "NoRuns")); // Add status
             allResultsData.add(row);
         });
     }

     /** Writes the collected benchmark results to a CSV file */
     private static void writeResultsToCsv(String filename) {
         if (allResultsData.isEmpty()) {
             System.out.println("No data collected for CSV export.");
             return;
         }
         System.out.printf("Writing results to %s...%n", filename);

         // Define CSV header order
         List<String> headers = List.of(
                 "Scenario", "ArraySize", "TargetMaxNumber", "ActualMaxNumber",
                 "Method", "Threads", "AvgTimeNs", "MinTimeNs", "MaxTimeNs",
                 "StdDevNs", "SuccessfulRuns", "TotalRuns", "Status"
         );

         try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
             // Write header
             writer.write(String.join(",", headers));
             writer.newLine();

             // Write data rows
             for (Map<String, Object> rowData : allResultsData) {
                 String line = headers.stream()
                         .map(header -> rowData.getOrDefault(header, "").toString()) // Get value or empty string
                         .map(Benchmark::escapeCsvValue) // Escape commas and quotes
                         .collect(Collectors.joining(","));
                 writer.write(line);
                 writer.newLine();
             }
             System.out.println("CSV export complete.");
         } catch (IOException e) {
             System.err.println("Error writing CSV file: " + e.getMessage());
             e.printStackTrace();
         }
     }

     /** Basic CSV value escaping (handles commas and quotes) */
     private static String escapeCsvValue(String value) {
         if (value == null) return "";
         String escaped = value.replace("\"", "\"\""); // Escape quotes
         if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
             escaped = "\"" + escaped + "\""; // Enclose in quotes if needed
         }
         return escaped;
     }


    // --- Data Generation Methods ---

    /** Generates an array containing mostly large prime numbers. */
    private static int[] generateMostlyPrimesArray(int size, int maxNumber) {
        // (Existing code - no changes needed based on requests)
        System.out.printf("Generating mostly primes array (size %,d, max %,d)... ", size, maxNumber);
        long start = System.nanoTime();
        int[] arr = new int[size];
        int count = 0;
        Random random = new Random(0); // Fixed seed for reproducibility
        int attempts = 0;
        int maxAttempts = size * 50; // Increased attempts slightly
        int startSearch = Math.max(2, maxNumber / 2);

        while (count < size && attempts < maxAttempts) {
             // Ensure candidate is within valid range [2, maxNumber]
             int candidate = startSearch + random.nextInt(Math.max(1, maxNumber - startSearch + 1));
             if (candidate <= 1) candidate = 2; // Ensure >= 2

             // Optimization: Skip even numbers > 2 immediately
             if (candidate > 2 && candidate % 2 == 0) {
                 attempts++; // Count this as an attempt
                 continue;
             }

            if (Util.isPrime(candidate)) {
                arr[count++] = candidate;
            }
            attempts++;
        }
        // Fill remaining spots if needed (using the existing prime list)
        int[] knownPrimes = {20319251, 6997901, 6997927, 17858849, 6997937, 6997967, 6998009, 11, 13, 17, 19, 23};
        int primeIdx = 0;
         if (count < size) {
             System.out.printf("Warning: Only generated %d primes randomly, filling %d spots... ", count, size - count);
         }
        while (count < size) {
            int filler = knownPrimes[primeIdx % knownPrimes.length];
             if (filler > maxNumber && maxNumber > 1) {
                  // Try to find *a* prime <= maxNumber if the known one is too large
                  filler = findSmallerPrimeFallback(maxNumber);
             } else if (filler < 2 && maxNumber >= 2) {
                  filler = (maxNumber >= 3) ? 3 : 2;
             } else if (filler < 2) {
                 filler = 2; // Absolute fallback
             } else if (filler > maxNumber) {
                 filler = 2; // Fallback if maxNumber is 1 or less
             }
             arr[count++] = filler;
             primeIdx++;
        }
        long end = System.nanoTime();
        System.out.printf("Done (%d ms).%n", TimeUnit.NANOSECONDS.toMillis(end - start));
        return arr;
    }

    // Helper for generateMostlyPrimesArray
    private static int findSmallerPrimeFallback(int maxNum) {
        if (maxNum >= 3) return 3;
        if (maxNum == 2) return 2;
        // Should not happen if called correctly, but return something safe
        // Consider throwing an error if maxNum <= 1 was possible here
        return 2;
    }

     /** Generates an array like the original, but places a non-prime at a specific index. */
     private static int[] generateArrayWithNonPrimeAtIndex(int size, int maxNumber, int nonPrimeIndex, String exitType) {
         System.out.printf("Generating array with %s non-prime (size %,d, max %,d, nonPrimeIdx %d)... ", exitType, size, maxNumber, nonPrimeIndex);
         long start = System.nanoTime();
         int[] arr = generateMostlyPrimesArray(size, maxNumber); // Start with mostly primes

         if (nonPrimeIndex >= 0 && nonPrimeIndex < size) {
             arr[nonPrimeIndex] = 4; // Use a simple non-prime
         } else {
             System.err.printf("Warning: %s nonPrimeIndex %d out of bounds for size %d, placing at index 0.%n", exitType, nonPrimeIndex, size);
             if (size > 0) arr[0] = 4;
         }
         long end = System.nanoTime();
         System.out.printf("Done (%d ms).%n", TimeUnit.NANOSECONDS.toMillis(end - start));
         return arr;
     }

     /** Generates an array with primes close to Integer.MAX_VALUE. */
     private static int[] generateLargePrimesArray(int size, int startMaxNumber) {
        // (Existing code - seems reasonable, maybe add more warnings)
        System.out.printf("Generating large primes array (size %,d, max ~%,d)... ", size, startMaxNumber);
        long start = System.nanoTime();
        if (startMaxNumber < 2) throw new IllegalArgumentException("maxNumber must be at least 2");

        int[] arr = new int[size];
        int count = 0;
        int attempts = 0;
        int maxAttempts = size * 250; // Allow more attempts for large primes
        int currentNum = startMaxNumber;

        // Ensure starting point is odd or 2
        if (currentNum > 2 && currentNum % 2 == 0) currentNum--;
        if (currentNum < 2) currentNum = 2; // Handle edge case if startMaxNumber was < 2

        while (count < size && attempts < maxAttempts && currentNum >= 2) {
            if (Util.isPrime(currentNum)) {
                arr[count++] = currentNum;
            }
            // Move to the next candidate (decrement by 2 to check only odds, or handle the '2' case)
            if (currentNum == 3) currentNum = 2; // Check 2 next
            else if (currentNum == 2) break; // Already checked 2
            else currentNum -= 2;

            attempts++;
        }

        if (count < size) {
            System.err.printf("Warning: Only found %d large primes near %d after %d attempts. Filling remaining %d spots with smaller primes... ", count, startMaxNumber, attempts, size-count);
            // Use the same known primes filler as in the other generator
             int[] knownPrimes = {20319251, 6997901, 6997927, 17858849, 6997937, 6997967, 6998009, 11, 13, 17, 19, 23};
             int primeIdx = 0;
            while(count < size) {
                 // No need to check against startMaxNumber here, just fill
                 arr[count++] = knownPrimes[primeIdx % knownPrimes.length];
                 primeIdx++;
            }
             System.out.print("Fill complete. ");
        }
        long end = System.nanoTime();
        System.out.printf("Done (%d ms).%n", TimeUnit.NANOSECONDS.toMillis(end - start));
        return arr;
    }
}