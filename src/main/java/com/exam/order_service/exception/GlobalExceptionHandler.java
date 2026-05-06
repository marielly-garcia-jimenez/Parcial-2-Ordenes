package com.exam.order_service.exception;

import com.exam.order_service.dto.RetryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "order_retry_jobs";

    public GlobalExceptionHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Error detectado en Order Service, enviando a Kafka para reintento: {}", ex.getMessage());
        
        Object body = request.getAttribute("failedObject", WebRequest.SCOPE_REQUEST);
        
        // Envolver el body en un mapa con la llave "data" como espera el Broker
        java.util.Map<String, Object> payloadWrapper = new java.util.HashMap<>();
        payloadWrapper.put("data", body);
        
        RetryMessage<java.util.Map<String, Object>> retryMessage = new RetryMessage<>(
                payloadWrapper,
                new RetryMessage.StepStatus("PENDING", "Pendiente de enviar correo"),
                new RetryMessage.StepStatus("PENDING", "Pendiente de actualizar")
        );

        kafkaTemplate.send(TOPIC, retryMessage);
        log.info("Mensaje de reintento enviado al tópico: {}", TOPIC);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", "RETRY_QUEUED");
        response.put("message", "La orden no pudo procesarse inmediatamente y ha sido enviada al sistema de reintentos.");
        response.put("error", ex.getMessage());
        
        return org.springframework.http.ResponseEntity.ok(response);
    }
}
