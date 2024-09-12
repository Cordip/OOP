package sorter;

import java.util.Arrays;
import java.util.Random;
import java.math.*;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import sorter.HeapSort;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class HeapSortTest2 {
    @Test void randomRiseTenArraysWithTimeCalculation() {
        int right = 1;
        int cnt = 0;
        Random rnd = new Random();
        int [] arr;
        int [] clone_array;
        boolean testsSuccesful = true;
        Timer timer;
        for (int i = 0; i < 8; ++i) {
            arr = new int[right*2+100];
            timer = new Timer();
            for (int j = 0; j < right; ++j) {
                arr[j] = rnd.nextInt();
            }
            clone_array = arr.clone();
            System.out.println("elements:" + right*2);
            timer.startTimer();
            HeapSort.sort(arr);
            timer.stopTimer();
            timer.printTimer();
            Arrays.sort(clone_array);
            testsSuccesful = Arrays.equals(arr, clone_array);
            right *= 10;
            cnt = 0;
            if (testsSuccesful == false) {
                assert(false);
            }
        }
        assert(true);
    }
}
