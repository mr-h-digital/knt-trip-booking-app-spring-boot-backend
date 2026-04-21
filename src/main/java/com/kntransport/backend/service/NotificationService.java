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
}
