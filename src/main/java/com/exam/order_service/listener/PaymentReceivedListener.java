package com.exam.order_service.listener;

import com.exam.order_service.model.Order;
import com.exam.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentReceivedListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentReceivedListener.class);
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentReceivedListener(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment_received_events", groupId = "order-group")
    public void handlePaymentReceived(Object message) {
        log.info("KAFKA: Recibido evento de pago recibido: {}", message);
        try {
            // El mensaje puede venir como un Map si se usa JsonDeserializer genérico
            Map<String, Object> payment = objectMapper.convertValue(message, Map.class);
            String orderId = (String) payment.get("ordenId");
            
            if (orderId != null) {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setEstado("PAGADA");
                    orderRepository.save(order);
                    log.info("Orden {} actualizada a estado PAGADA", orderId);
                    
                    // Notificar cambio de estatus
                    kafkaTemplate.send("order_status_changed_events", order);
                    log.info("Evento de cambio de estatus de orden enviado para: {}", orderId);
                });
            }
        } catch (Exception e) {
            log.error("Error al procesar evento de pago: {}", e.getMessage());
        }
    }
}
