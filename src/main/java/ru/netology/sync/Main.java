package ru.netology.sync;

import java.util.*;
import java.util.concurrent.*;
public class Main {
    public static final Map<Integer, Integer> sizeToFreq = new HashMap<>();
    private static final Object monitor = new Object();
    private static volatile boolean updateNeeded = false;

    public static final int THREADS_COUNT = 1000;
    public static void main(String[] args) throws InterruptedException {
        int threadsCount = THREADS_COUNT;
        Thread[] threads = new Thread[threadsCount];

        // Создаем и запускаем поток для вывода лидера
        Thread leaderThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                synchronized (monitor) {
                    try {
                        while (!updateNeeded && !Thread.interrupted()) {
                            monitor.wait();
                        }
                        if (Thread.interrupted()) break;

                        printCurrentLeader();
                        updateNeeded = false;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            System.out.println("Поток вывода лидера завершен");
        });
        leaderThread.start();

        // Создаем и запускаем потоки
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                String route = generateRoute("RLRFR", 100);
                int rCount = countChar(route, 'R');

                // Синхронизированное обновление мапы
                synchronized (sizeToFreq) {
                    sizeToFreq.put(rCount, sizeToFreq.getOrDefault(rCount, 0) + 1);
                }

                System.out.println("Количество 'R': " + rCount);

                // Уведомляем поток вывода о новом обновлении
                synchronized (monitor) {
                    updateNeeded = true;
                    monitor.notify();
                }
            });
            threads[i].start();
        }

        // Ожидаем завершения всех потоков
        for (Thread thread : threads) {
            thread.join();
        }

        // Прерываем поток вывода лидера
        leaderThread.interrupt();
        leaderThread.join();

        // Выводим статистику
        printStatistics();
    }

    // Генерация маршрута
    public static String generateRoute(String letters, int length) {
        Random random = new Random();
        StringBuilder route = new StringBuilder();
        for (int i = 0; i < length; i++) {
            route.append(letters.charAt(random.nextInt(letters.length())));
        }
        return route.toString();
    }

    // Подсчет символов в строке
    private static int countChar(String str, char ch) {
        return (int) str.chars().filter(c -> c == ch).count();
    }

    // Вывод текущего лидера
    private static void printCurrentLeader() {
        synchronized (sizeToFreq) {
            if (sizeToFreq.isEmpty()) {
                System.out.println("Лидер: данных еще нет");
                return;
            }

            Map.Entry<Integer, Integer> maxEntry = Collections.max(
                    sizeToFreq.entrySet(),
                    Map.Entry.comparingByValue()
            );

            System.out.println("Текущий лидер: " + maxEntry.getKey() +
                    " (встретилось " + maxEntry.getValue() + " раз)");
        }
    }

    // Вывод статистики
    private static void printStatistics() {
        if (sizeToFreq.isEmpty()) {
            System.out.println("Нет данных для статистики");
            return;
        }

        // Находим наиболее часто встречающуюся частоту
        Map.Entry<Integer, Integer> maxEntry = Collections.max(
                sizeToFreq.entrySet(),
                Map.Entry.comparingByValue()
        );

        System.out.printf("Самое частое количество повторений %d  (встретилось %d раз)\n", maxEntry.getKey(), maxEntry.getValue());

        System.out.println("Другие размеры:");
        sizeToFreq.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!entry.getKey().equals(maxEntry.getKey())) {
                        System.out.printf("- %d (%d раз)\n", entry.getKey(), entry.getValue());
                    }
                });
    }
}
