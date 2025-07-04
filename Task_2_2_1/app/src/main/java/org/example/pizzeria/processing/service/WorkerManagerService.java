package org.example.pizzeria.processing.service;

/**
 * Интерфейс для сервиса, управляющего жизненным циклом потоков работников.
 */
public interface WorkerManagerService {

    /**
     * Возвращает текущее количество активных потоков работников.
     *
     * @return количество живых потоков работников.
     */
    int getActiveWorkerThreadCount();
}
