package task_1_1_1;

import task_1_1_1.Swap;

/**
 * class of all heap sort functions
 */
public class HeapSort {
    /**
     * useless because all is static here
     */
    public HeapSort () {

    }
    /**
     * its sift up
     * takes some array with some length (can be not max of array)
     * on id swaps root with maximal int within kids and root
     *    0
     *   / \
     *  2   6
     * 
     *    to
     * 
     *    6
     *   / \
     *  2   0
     * 
     * @param arr array of ints
     * @param length length of given array (length <= arr.length)
     * @param id  position of root in array
     */
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
    /**
     * where heap sort is located
     * in first cycle we build max heap
     * then in cycle swap root with the last element
     * make length - 1
     * and build max heap again
     * until array is finally sorted
     * @param arr array of ints
     */
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
