package sorter;

import java.util.Scanner;

/**
 *this classes is used for receiving from cmd.
 */
public class Receiver {
    /**
     *scanner here for optimisation.
     */
    private Scanner scannerIn;

    public Receiver () {
        scannerIn = new Scanner(System.in);
    }

    public void closeReceiver () {
        scannerIn.close();
    }

    /**
     *function that collects int (length of future array).
     *@return length of array.
     */
    public int collectLengthForArrayFromCmd() {
        int length = -1;
        System.out.println("Write length of the array");
        while(length == -1) {
            if (scannerIn.hasNextInt()) {
                length = scannerIn.nextInt();
                System.out.println("inputted length " + length);
            } else {
                scannerIn.next();
                System.out.println("Writed not int");

            }
        }

        return length;
    }
    /**
     *collects array with given length.
     *@param length max elements in array.
     *@return some array with his length = length.
     */
    public int[] collectArrayFromCmd(int length) {
        int[] arr = new int[length];
        System.out.println("Write array of integers");
        for (int i = 0; i < length; ++i) {
            arr[i] = -1;
            while(arr[i] == -1) {
                if (scannerIn.hasNextInt()) {
                    arr[i] = scannerIn.nextInt();
                } else {
                    scannerIn.next();
                    System.out.println("Writed not int");
    
                }
            }
                
            
        }
        System.out.println("array collected");
        
        return arr;
    }
}
