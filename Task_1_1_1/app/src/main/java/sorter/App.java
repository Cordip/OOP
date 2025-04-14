/**
 *programm for sorting array of ints with heap sort.
 *@autor Maxim Gavrilev
 *@version 1.0
*/

 
package sorter;

import sorter.HeapSort;
import sorter.Print;
import sorter.Receiver;

import java.util.Arrays;


/**
 *main class of this application.
 *@author Maxim Gavrilev.
 */
public class App {

    /**
     *main function, programm starts here.
     *
     *@param args not used.
     */
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        int length = receiver.collectLengthForArrayFromCmd();
        int [] arr = receiver.collectArrayFromCmd(length);
        
        Timer timer = new Timer();
        timer.startTimer();
        HeapSort.sort(arr);
        timer.stopTimer();
        timer.printTimer();
        Print.printArrayOfInts(arr); 

        receiver.closeReceiver();


        
    }
}
