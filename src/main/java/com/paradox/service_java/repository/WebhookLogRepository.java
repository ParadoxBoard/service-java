package com.paradox.service_java.repository;

import com.paradox.service_java.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    Optional<WebhookLog> findByDeliveryId(String deliveryId);

    List<WebhookLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    List<WebhookLog> findByProcessedFalseOrderByCreatedAtAsc();

    boolean existsByDeliveryId(String deliveryId);
}

