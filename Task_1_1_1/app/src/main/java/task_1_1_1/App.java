/** 
 * programm for sorting array of ints with heap sort.
 * @autor Maxim Gavrilev
 * @version 1.0
*/

 
package task_1_1_1;

import task_1_1_1.HeapSort;
import task_1_1_1.Receiver;
import task_1_1_1.Print;

public class App {

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


        
    }
}
