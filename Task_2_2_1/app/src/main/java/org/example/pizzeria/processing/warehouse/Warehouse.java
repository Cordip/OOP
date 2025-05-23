// ./Task_2_2_1_1/app/src/main/java/org/example/pizzeria/processing/warehouse/Warehouse.java
package org.example.pizzeria.processing.warehouse;

import java.util.Collection;
import java.util.List;
import org.example.pizzeria.domain.pizza.Pizza;

/**
 * Интерфейс для потокобезопасного склада пицц с ограниченной вместимостью.
 */
public interface Warehouse {
     /**
     * Помещает готовую пиццу на склад. Блокируется, если склад полон.
     * @param pizza Пицца для добавления.
     * @throws InterruptedException если поток был прерван во время ожидания.
     */
    void put(Pizza pizza) throws InterruptedException;

    /**
     * Забирает со склада до `maxAmount` пицц.
     * Блокируется, если склад пуст. Если пицц меньше, чем `maxAmount`, забирает все доступные.
     * @param maxAmount Максимальное количество пицц для взятия. Должно быть > 0.
     * @return Список пицц (может быть пустым, если поток прерван).
     * @throws InterruptedException если поток был прерван во время ожидания.
     */
    List<Pizza> take(int maxAmount) throws InterruptedException;

     /**
     * Извлекает все оставшиеся пиццы со склада без блокировки.
     * @return Список оставшихся на складе пицц.
     */
    List<Pizza> drainRemaining();

    /**
     * Атомарно удаляет все доступные пиццы со склада и добавляет их в заданную коллекцию.
     * Эффективнее многократного вызова {@link #take(int)}.
     *
     * @param collection Коллекция для добавления удаленных пицц.
     * @return Количество переданных пицц.
     * @throws NullPointerException если collection null.
     * @throws IllegalArgumentException если collection - это сам этот склад.
     */
    int drainTo(Collection<? super Pizza> collection);

     /**
     * Возвращает текущее количество пицц на складе (приблизительное).
     *
     * @return Текущее количество пицц.
     */
    int size();

    /**
     * Возвращает максимальную вместимость склада.
     *
     * @return Максимальная вместимость.
     */
    int capacity();

    /**
     * Проверяет, пуст ли склад.
     * @return {@code true}, если склад пуст, иначе {@code false}.
     */
    boolean isEmpty();
}