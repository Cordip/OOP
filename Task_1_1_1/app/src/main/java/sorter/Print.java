package sorter;

/**
 *class for print somethings.
 */
public class Print {
    /**
     *useless because all is static here.
     */
    public Print() {

    }
    /**
     *prints array of ints.
     *@param arr array of ints.
     */
    public static void printArrayOfInts(int [] arr) {
        for (int i = 0; i < arr.length; ++i) {
            System.out.print(arr[i] + " ");
        }
        System.out.print(System.lineSeparator());
    }

    public static String arrayToString (int [] arr) {
        String res = "";
        for (int i = 0; i < arr.length; ++i) {
            res = String.format("%s%d ", res, arr[i]);
        }
        return res;
    }
}
