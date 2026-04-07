package com.exam.order_service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    
    @JsonProperty("usuarioId")
    @JsonAlias({"userId", "usuarioId"})
    @Field("usuarioId")
    private String usuarioId;
    
    @JsonProperty("productoIds")
    @JsonAlias({"productIds", "productoIds"})
    @Field("productoIds")
    private List<String> productoIds;
    
    @JsonProperty("total")
    @Field("total")
    private Double total;
    
    @JsonProperty("estado")
    @JsonAlias({"status", "estado"})
    @Field("estado")
    private String estado;

    public Order() {}

    public Order(String id, String usuarioId, List<String> productoIds, Double total, String estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.productoIds = productoIds;
        this.total = total;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<String> getProductoIds() {
        return productoIds;
    }

    public void setProductoIds(List<String> productoIds) {
        this.productoIds = productoIds;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
