package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<ApiDtos.NotificationResponse> list(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(DtoMapper::notification);
    }

    @Transactional
    public ApiDtos.NotificationResponse read(UUID userId, UUID id) {
        Notification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Notificação não encontrada."));
        notification.markRead();
        return DtoMapper.notification(notification);
    }

    @Transactional
    public int readAll(UUID userId) { return repository.markAllRead(userId); }
}

