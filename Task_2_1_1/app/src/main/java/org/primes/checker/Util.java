package org.primes.checker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Util {

    private Util() {
        // Utility class should not be instantiated
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    public static boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        // Check only divisors of the form 6k ± 1 up to sqrt(n)
        for (long i = 5; (i * i) <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    public static long[] readIntegersFromFile(String fileName) {
        List<Long> numbersList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    long number = Long.parseLong(line.trim()); // Trim whitespace
                    numbersList.add(number);
                } catch (NumberFormatException e) {
                    // Skipping invalid number
                    if (!line.trim().isEmpty()) { // Avoid warning for blank lines
                       System.err.println("Skipping invalid number format: " + line);
                    }
                }
            }
        } catch (IOException e) {
            // Error reading the file
            System.err.println("Error reading the file: " + e.getMessage());
        }

        long[] numbersArray = new long[numbersList.size()];
        for (int i = 0; i < numbersList.size(); i++) {
            numbersArray[i] = numbersList.get(i);
        }
        return numbersArray;
    }

    public static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

}