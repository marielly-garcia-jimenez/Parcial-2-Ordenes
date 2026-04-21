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

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
        return orderRepository.save(order);
    }

    @PostMapping("/retry")
    public Order saveRetry(@RequestBody Order order) {
        log.info("Reintentando guardar orden desde Broker: {}", order.getId());
        if (order.getUsuarioId() != null && order.getUsuarioId().equalsIgnoreCase("fail_permanent")) {
            log.warn("Simulando fallo permanente en Orden para prueba de 5 intentos");
            throw new RuntimeException("Fallo simulado permanentemente en orden");
        }
        return orderRepository.save(order);
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
            return orderRepository.save(order);
        }
        return null;
    }
}
