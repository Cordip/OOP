package task_1_1_1;

import task_1_1_1.Swap;

public class HeapSort {
    private static void heapify(int [] arr, int length, int id) {
        int large = id;
        int left = 2 * id + 1;
        int right = 2 * id + 2;

        if (left < length && arr[left] > arr[large]) {
            large = left;
        }

        if (right < length && arr[right] > arr[large]) {
            large = right;
        }

        if (large != id) {
            Swap.swapArrayInts(arr, large, id);
            heapify(arr, length, large);
        }
    }

    public static void sort(int [] arr) {
        System.out.println("sorting in process...");
        int length = arr.length;
        
        for (int i = length / 2 - 1; i >= 0; --i) {
            heapify(arr, length, i);
        }

        for (int i = length - 1; i > 0; --i) {
            Swap.swapArrayInts(arr, i, 0);

            heapify(arr, i, 0);
        }
        System.out.println("array is sorted");
    }
}
