package com.exam.order_service.controller;

import com.exam.order_service.model.Order;
import com.exam.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
@RequestMapping("/ordenes")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderRepository orderRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public OrderController(OrderRepository orderRepository, org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order, WebRequest request) {
        log.info("Intentando crear orden para el usuario: {}", order.getUsuarioId());
        request.setAttribute("failedObject", order, WebRequest.SCOPE_REQUEST);
        
        // Disparar reintento si es "fail" o "fail_permanent"
        if (order.getUsuarioId() == null || 
            order.getUsuarioId().equalsIgnoreCase("fail") || 
            order.getUsuarioId().equalsIgnoreCase("fail_permanent")) {
            
            throw new RuntimeException("Error simulado para iniciar ciclo de reintentos");
        }
        
        order.setEstado("PENDIENTE");
        Order savedOrder = orderRepository.save(order);
        
        // Paso 1: ACTUALIZAR INVENTARIO DE PRODUCTOS
        kafkaTemplate.send("inventory_update_events", savedOrder);
        log.info("Evento inventory_update_events enviado para orden {}", savedOrder.getId());
        
        return savedOrder;
    }

    @PostMapping("/retry")
    public Order saveRetry(@RequestBody Order order) {
        log.info("Reintentando guardar orden desde Broker: {}", order.getId());
        if (order.getUsuarioId() != null && order.getUsuarioId().equalsIgnoreCase("fail_permanent")) {
            log.warn("Simulando fallo permanente en Orden para prueba de 5 intentos");
            throw new RuntimeException("Fallo simulado permanentemente en orden");
        }
        Order savedOrder = orderRepository.save(order);
        
        // También disparar inventario en el reintento exitoso
        kafkaTemplate.send("inventory_update_events", savedOrder);
        
        return savedOrder;
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable String id) {
        log.info("Obteniendo orden con id: {}", id);
        return orderRepository.findById(id).orElse(null);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Order> getOrdersByUser(@PathVariable String usuarioId) {
        log.info("Obteniendo ordenes para el usuario: {}", usuarioId);
        return orderRepository.findByUsuarioId(usuarioId);
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id, @RequestBody String status) {
        log.info("Actualizando estado para la orden {}: {}", id, status);
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setEstado(status);
            Order updatedOrder = orderRepository.save(order);
            
            // Paso 1: ACTUALIZAR ESTATUS DE ORDEN
            kafkaTemplate.send("order_status_changed_events", updatedOrder);
            log.info("Evento order_status_changed_events enviado para orden {}", updatedOrder.getId());
            
            return updatedOrder;
        }
        return null;
    }

    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable String id, @RequestBody Order orderUpdate) {
        log.info("Actualizando orden {}", id);
        return orderRepository.findById(id).map(existingOrder -> {
            boolean productsChanged = !existingOrder.getProductoIds().equals(orderUpdate.getProductoIds());
            existingOrder.setProductoIds(orderUpdate.getProductoIds());
            existingOrder.setTotal(orderUpdate.getTotal());
            Order saved = orderRepository.save(existingOrder);
            
            if (productsChanged) {
                // Paso 1.1: ACTUALIZAR INVENTARIO DE PRODUCTOS
                kafkaTemplate.send("inventory_update_events", saved);
            }
            return saved;
        }).orElse(null);
    }
}
