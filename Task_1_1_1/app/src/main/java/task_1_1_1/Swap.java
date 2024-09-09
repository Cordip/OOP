package task_1_1_1;

public class Swap {
    public static void swapArrayInts(int [] arr, int first, int second) {
        int temp = arr[first];
        arr[first] = arr[second];
        arr[second] = temp;
    }
}
