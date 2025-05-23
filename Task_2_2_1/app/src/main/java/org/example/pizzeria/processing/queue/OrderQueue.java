package org.example.pizzeria.processing.queue;

import java.util.Collection;
import java.util.List;
import org.example.pizzeria.domain.order.Order;

/**
 * Интерфейс для потокобезопасной очереди заказов с ограниченной вместимостью.
 */
public interface OrderQueue {

    /**
     * Помещает заказ в хвост очереди. Блокируется, если очередь полна.
     *
     * @param order Заказ для добавления.
     * @throws InterruptedException если поток был прерван во время ожидания.
     * @throws NullPointerException если order null.
     */
    void put(Order order) throws InterruptedException;

    /**
     * Извлекает заказ из головы очереди. Блокируется, если очередь пуста.
     *
     * @return Следующий заказ из очереди.
     * @throws InterruptedException если поток был прерван во время ожидания.
     */
    Order take() throws InterruptedException;

    /**
     * Атомарно удаляет все доступные элементы из очереди и добавляет их в заданную коллекцию.
     * Эффективнее многократного вызова {@link #take()}.
     *
     * @param collection Коллекция для добавления удаленных элементов.
     * @return Количество переданных элементов.
     * @throws NullPointerException если collection null.
     * @throws IllegalArgumentException если collection - это сама эта очередь.
     */
    int drainTo(Collection<? super Order> collection);

    /**
     * Извлекает все оставшиеся заказы из очереди без блокировки.
     *
     * @deprecated Рекомендуется использовать {@link #drainTo(Collection)}.
     * @return Новый список, содержащий все оставшиеся в очереди заказы.
     */
    @Deprecated
    List<Order> drainRemaining();

    /**
     * Возвращает текущее количество заказов в очереди (приблизительное).
     *
     * @return Текущее количество заказов.
     */
     int size();

     /**
      * Проверяет, пуста ли очередь.
      *
      * @return {@code true}, если очередь пуста, иначе {@code false}.
      */
     boolean isEmpty();

     /**
      * Возвращает максимальную вместимость этой очереди.
      * @return Максимальная вместимость.
      */
     int capacity();
}