package org.simplenumbers;

import java.util.Arrays;
import java.lang.Math;

public class SimpleNumbers {
    private long [] array;

    public SimpleNumbers () {
        this.array = null;
    }

    public SimpleNumbers (long [] array) {
        Arrays.parallelSort(array);
        this.array = array;
    }

    public void setArray(long [] array) {
        Arrays.parallelSort(array);
        this.array = array;
    }

    public Boolean checkArray() {
        for (int i = 0; i < array.length; i++) {
            

            if (array[i] == 0 || array[i] == 1) {
                return true;
            }
            if (array[i] != 2 && array[i] % 2 == 0) {
                return true;
            }
            if (array[i] != 3 && array[i] % 3 == 0) {
                return true;
            }
            for (int j = 5; j <= Math.sqrt(array[i]); j += 6) {
                if (array[i] % j == 0 || array[i] % (j + 2) == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    // public Boolean checkArray(int start, int end) {
    //     Util.rangeCheck(array.length, start, end);
    //     for (int i = start; i < end ; i++) {

    //         if (array[i] == 0 || array[i] == 1) {
    //             return true;
    //         }
    //         if (array[i] != 2 && array[i] % 2 == 0) {
    //             return true;
    //         }
    //         if (array[i] != 3 && array[i] % 3 == 0) {
    //             return true;
    //         }
    //         for (int j = 5; j <= Math.sqrt(array[i]); j += 6) {
    //             if (array[i] % j == 0 || array[i] % (j + 2) == 0) {
    //                 return true;
    //             }
    //         }
    //     }

    //     return false;
    // }

    /*
     * Bool checkArray
     *  нужно провериить на существование простого числа хотя бы одного
     *  true когда есть простое число в массиве
     * 1) проверить каждое число по отдельности
     * 2) сделать маску и бегать каждой по каждому
     * 
     * 
     * 
     * 
     * 
     * 
     * interrput, thread, threadpool, база про многопоточность
     * нужен интерфейс
     * 
     * хочу для большей эффективности иметь два метода
     * когда колличество чиселок в чанке больше 4_000
     * буду применять решето эратосфена
     * а если меньше то проще в ручную
     * 
     * РЭ = n * (ln (ln (n))) - тут у нас n = 100_000 (размер чанка)
     * Ручками = n * sqrt(n) - тут n = колличеству циферок из массива,
     *                         которые попали в чанк
     * 
     * 
     */
}
