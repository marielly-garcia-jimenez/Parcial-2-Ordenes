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
    private final com.exam.order_service.feign.ProductClient productClient;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public OrderController(OrderRepository orderRepository, com.exam.order_service.feign.ProductClient productClient, org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping
    public List<Order> findAll() {
        log.info("Obteniendo todas las ordenes");
        return orderRepository.findAll();
    }

    @PostMapping
    public org.springframework.http.ResponseEntity<?> createOrder(@RequestBody Order order, WebRequest request) {
        log.info("Intentando crear orden para el usuario: {}", order.getUsuarioId());
        request.setAttribute("failedObject", order, WebRequest.SCOPE_REQUEST);

        // Disparar reintento si es "fail" o "fail_permanent"
        if (order.getUsuarioId() == null || 
            order.getUsuarioId().equalsIgnoreCase("fail") || 
            order.getUsuarioId().equalsIgnoreCase("fail_permanent")) {

            throw new RuntimeException("Error simulado para iniciar ciclo de reintentos");
        }

        // VALIDACIÓN DE STOCK
        if (order.getProductoIds() != null) {
            for (String pid : order.getProductoIds()) {
                try {
                    java.util.Map<String, Object> product = productClient.getProduct(pid);
                    if (product == null) {
                        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                                .body(java.util.Map.of("message", "Producto no encontrado: " + pid));
                    }
                    Integer stock = (Integer) product.get("stock");
                    if (stock == null || stock <= 0) {
                        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                                .body(java.util.Map.of("message", "Stock insuficiente para el producto: " + product.get("nombre")));
                    }
                } catch (Exception e) {
                    log.warn("Error al validar stock para producto {}: {}", pid, e.getMessage());
                }
            }
        }

        order.setEstado("PENDIENTE");
        Order savedOrder = orderRepository.save(order);

        // Paso 1: ACTUALIZAR INVENTARIO DE PRODUCTOS
        kafkaTemplate.send("inventory_update_events", savedOrder);
        log.info("Evento inventory_update_events enviado para orden {}", savedOrder.getId());

        // ADICIONAL: Notificar cambio de estado inicial para disparar correo
        kafkaTemplate.send("order_status_changed_events", savedOrder);
        log.info("Evento order_status_changed_events enviado para orden {}", savedOrder.getId());

        return org.springframework.http.ResponseEntity.ok(savedOrder);
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
        
        // ADICIONAL: Notificar cambio de estado en reintento exitoso
        kafkaTemplate.send("order_status_changed_events", savedOrder);
        
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

    @GetMapping("/producto/{productId}")
    public List<Order> getOrdersByProduct(@PathVariable String productId) {
        log.info("Obteniendo ordenes asociadas al producto: {}", productId);
        return orderRepository.findByProductoIdsContaining(productId);
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id, @RequestBody String status) {
        log.info("Actualizando estado para la orden {}: {}", id, status);
        // Limpiar el status por si viene con comillas de JSON
        String cleanStatus = status.replace("\"", "");
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setEstado(cleanStatus);
            Order updatedOrder = orderRepository.save(order);
            
            // Paso 1: ACTUALIZAR ESTATUS DE ORDEN
            kafkaTemplate.send("order_status_changed_events", updatedOrder);
            log.info("Evento order_status_changed_events enviado para orden {}", updatedOrder.getId());
            
            return updatedOrder;
        }
        return null;
    }

    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable String id, @RequestBody java.util.Map<String, Object> updateMap) {
        log.info("Actualizando orden {} con mapa: {}", id, updateMap);
        return orderRepository.findById(id).map(existingOrder -> {
            boolean statusChanged = false;

            if (updateMap.containsKey("total")) {
                Object totalObj = updateMap.get("total");
                if (totalObj != null) {
                    try {
                        existingOrder.setTotal(Double.valueOf(totalObj.toString()));
                    } catch (Exception e) {
                        log.error("Error al convertir total");
                    }
                }
            }
            
            String newStatus = null;
            if (updateMap.containsKey("estado")) {
                newStatus = (String) updateMap.get("estado");
            } else if (updateMap.containsKey("status")) {
                newStatus = (String) updateMap.get("status");
            }

            if (newStatus != null) {
                existingOrder.setEstado(newStatus.toUpperCase());
                statusChanged = true;
            }
            
            Order saved = orderRepository.save(existingOrder);
            
            if (statusChanged) {
                kafkaTemplate.send("order_status_changed_events", saved);
                log.info("Evento order_status_changed_events enviado para orden {}", saved.getId());
            }
            
            return saved;
        }).orElse(null);
    }
}
