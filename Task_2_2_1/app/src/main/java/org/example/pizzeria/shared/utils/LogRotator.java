package org.example.pizzeria.shared.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилитарный класс для выполнения ротации лог-файлов по размеру.
 */
public class LogRotator {
    private static final Logger log = LoggerFactory.getLogger(LogRotator.class);

    /**
     * Проверяет размер файла и выполняет ротацию, если он превышает лимит.
     *
     * @param filePath         Путь к файлу, который нужно проверить.
     * @param maxSizeMb        Максимальный размер в мегабайтах, после которого нужна ротация.
     * @param rotationEnabled  Включен ли механизм ротации.
     * @param onBeforeRotation Действие, которое нужно выполнить перед ротацией (например, закрыть файл).
     *                         Может быть null.
     * @return {@code true}, если ротация была выполнена, иначе {@code false}.
     */
    public boolean rotate(Path filePath, long maxSizeMb, boolean rotationEnabled, Runnable onBeforeRotation) {
        if (!rotationEnabled) {
            log.info("Log rotation is disabled for {}.", filePath);
            return false;
        }
        if (!Files.exists(filePath)) {
            log.debug("Log file {} does not exist. No rotation needed.", filePath);
            return false;
        }

        try {
            long fileSize = Files.size(filePath);
            long maxSizeInBytes = maxSizeMb * 1024 * 1024;

            if (fileSize >= maxSizeInBytes) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path archivePath = filePath.resolveSibling(filePath.getFileName().toString() + "." + timestamp + ".archive");
                log.warn("Log file {} size {} bytes >= limit {} bytes. Rotating to {}", filePath, fileSize, maxSizeInBytes, archivePath);

                if (onBeforeRotation != null) {
                    log.debug("Executing pre-rotation action for {}.", filePath);
                    onBeforeRotation.run();
                }

                Files.move(filePath, archivePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Log file rotated successfully to {}", archivePath);
                return true;
            } else {
                log.debug("Log file {} size {} bytes is within limit {} bytes. No rotation needed.", filePath, fileSize, maxSizeInBytes);
                return false;
            }
        } catch (IOException e) {
            log.error("Error during log rotation for {}. Continuing...", filePath, e);
            return false;
        }
    }
}