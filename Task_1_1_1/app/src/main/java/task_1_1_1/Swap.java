package task_1_1_1;


/**
 *some util class.
 */
public class Swap {
    /**
     *useless because all is static here.
     */
    public Swap () {

    }
    /**
     *swaps two ints in array.
     *@param arr array of ints.
     *@param first first becomes second.
     *@param second second becomes first.
     */
    public static void swapArrayInts(int [] arr, int first, int second) {
        int temp = arr[first];
        arr[first] = arr[second];
        arr[second] = temp;
    }
}
