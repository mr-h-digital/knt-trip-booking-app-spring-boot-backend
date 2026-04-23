package com.kntransport.backend.service;

import com.kntransport.backend.dto.NotificationDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.entity.Notification;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public PagedResponse<NotificationDto> getNotifications(String email, int page, int size) {
        User user = userService.getByEmail(email);
        var pageResult = notificationRepository.findByUserOrderByTimestampDesc(
                user, PageRequest.of(page, size));
        return PagedResponse.from(pageResult, NotificationDto::from);
    }

    @Transactional
    public void markRead(String email, String id) {
        Notification notification = notificationRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        User user = userService.getByEmail(email);
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(String email) {
        User user = userService.getByEmail(email);
        notificationRepository.markAllReadByUser(user);
    }

    /**
     * Creates and persists a notification for a user, optionally linked to a
     * specific entity (trip, lift club, or quote) so the Android app can
     * deep-link to it from the notification detail screen.
     */
    @Transactional
    public void createNotification(
            User user,
            Notification.NotifType type,
            String title,
            String body,
            UUID referenceId,
            Notification.ReferenceType referenceType) {

        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setTimestamp(Instant.now());
        n.setReferenceId(referenceId);
        n.setReferenceType(referenceType);
        notificationRepository.save(n);
    }
}
