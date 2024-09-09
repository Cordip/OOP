package task_1_1_1;

/**
 * class for print somethings
 */
public class Print {
    /**
     * useless because all is static here
     */
    public Print() {

    }
    /**
     * prints array of ints
     * @param arr array of ints
     */
    public static void printArrayOfInts(int [] arr) {
        for (int i = 0; i < arr.length; ++i) {
            System.out.print(arr[i] + " ");
        }
    }
}
