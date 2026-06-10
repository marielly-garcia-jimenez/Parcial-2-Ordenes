package com.exam.order_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "product-service", url = "http://product-service:8081")
public interface ProductClient {
    @GetMapping("/productos/{id}")
    Map<String, Object> getProduct(@PathVariable("id") String id);
}
