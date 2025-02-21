package org.simplenumbers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static long[] readIntegersFromFile(String fileName) {
        List<Long> numbersList = new ArrayList<>();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            
            while ((line = br.readLine()) != null) {
                try {
                    long number = Long.parseLong(line);
                    numbersList.add(number);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid number: " + line);
                }
            }
            
            br.close();
        } catch (IOException e) {
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
