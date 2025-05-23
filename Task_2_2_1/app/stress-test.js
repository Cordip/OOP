import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// --- Метрики ---
const createOrderLatency = new Trend('create_order_latency');
const getOrderStatusLatency = new Trend('get_order_status_latency');
const errorRate = new Rate('errors');
const createdOrdersCounter = new Counter('created_orders');

// --- Опции теста ---
export const options = {
    // Количество виртуальных пользователей (VU) и продолжительность
    stages: [
        { duration: '30s', target: 20 }, // Плавный старт до 20 пользователей за 30 секунд
        { duration: '1m', target: 20 }, // Держать 20 пользователей 1 минуту
        { duration: '30s', target: 50 }, // Увеличить до 50 пользователей за 30 секунд
        { duration: '2m', target: 50 }, // Держать 50 пользователей 2 минуты
        { duration: '30s', target: 100 },// Увеличить до 100 пользователей за 30 секунд
        { duration: '3m', target: 100 },// Держать 100 пользователей 3 минуты
        { duration: '1m', target: 0 },   // Плавное снижение до 0
    ],
    thresholds: {
        'http_req_failed': ['rate<0.01'], // <1% запросов должны падать
        'http_req_duration': ['p(95)<500'], // 95% запросов должны быть быстрее 500ms
        'create_order_latency': ['p(90)<400'], // 90% создания заказов быстрее 400ms
        'get_order_status_latency': ['p(95)<200'], // 95% получения статуса быстрее 200ms
        'errors': ['rate<0.02'], // общая частота ошибок < 2%
    },
    // Не обязательно, но может помочь если приложение отвечает медленно и k6 начинает таймаутить раньше
    // discardResponseBodies: true, // Если не нужны тела ответов
};

const BASE_URL = 'http://localhost:8080/api/orders';
let orderIds = []; // Будем хранить ID созданных заказов для GET запросов

// --- Основная функция теста, которую выполняет каждый VU ---
export default function () {
    // 70% времени - создаем заказы, 30% - проверяем статус
    if (Math.random() < 0.7) {
        group('Create Order', function () {
            const payload = JSON.stringify({
                pizzaDetails: `Пепперони стресс-тест ${Math.random()}`,
            });
            const params = {
                headers: {
                    'Content-Type': 'application/json',
                },
            };
            const res = http.post(BASE_URL, payload, params);

            const success = check(res, {
                'POST /api/orders status is 201': (r) => r.status === 201,
                'POST /api/orders has orderId': (r) => r.json('orderId') !== null,
            });

            createOrderLatency.add(res.timings.duration);
            errorRate.add(!success);

            if (success && res.json('orderId')) {
                const orderId = res.json('orderId');
                orderIds.push(orderId); // Сохраняем ID
                createdOrdersCounter.add(1);
            }
            sleep(Math.random() * 2 + 0.5); // Пауза между запросами от 0.5 до 2.5 секунд
        });
    } else {
        if (orderIds.length > 0) {
            group('Get Order Status', function () {
                // Берем случайный ID из ранее созданных
                const randomOrderId = orderIds[Math.floor(Math.random() * orderIds.length)];

                const res = http.get(`${BASE_URL}/${randomOrderId}`);
                const success = check(res, {
                    'GET /api/orders/{id} status is 200 or 404': (r) => r.status === 200 || r.status === 404,
                });
                // 404 не считаем ошибкой в данном контексте, т.к. заказ мог быть завершен
                // но если хотим считать ошибкой, то: (r) => r.status === 200

                getOrderStatusLatency.add(res.timings.duration);
                errorRate.add(res.status !== 200 && res.status !== 404); // Ошибка если не 200 и не 404
                 sleep(Math.random() * 1 + 0.2); // Пауза от 0.2 до 1.2 секунд
            });
        } else {
            // Если еще нет созданных orderId, просто делаем паузу
            sleep(1);
        }
    }
}

// (Необязательно) Код, который выполняется один раз при завершении теста
export function teardown(data) {
    // console.log(`Test finished. Total created orders: ${data.metrics.created_orders.values.count}`);
    // Здесь можно было бы, например, удалить все тестовые заказы, если бы API это позволял
}