# 🛒 Microservicio de Órdenes - Gestión de Pedidos

El **Servicio de Órdenes** es la pieza central del flujo transaccional. Registra las intenciones de compra y gestiona el estado vital de cada pedido.

---

## 🛠️ Stack Tecnológico
- **Base de Datos:** MongoDB (Colección `ordenes`).
- **Framework:** Spring Boot 3.2.5.
- **Registro:** Cliente de Eureka Service.
- **Logs:** Centralizados en CloudWatch (LocalStack).

---

## 📋 Endpoints Principales (vía Gateway: 8080)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `POST` | `/ordenes` | Crea una nueva orden de compra. |
| `GET` | `/ordenes/{id}` | Busca una orden por su ID. |
| `GET` | `/ordenes/usuario/{uid}` | Lista todas las órdenes de un usuario. |
| `PUT` | `/ordenes/{id}/status` | Actualiza el estado de la orden (PENDIENTE/PAGADA/CANCELADA). |

---

## 📑 Estados de la Orden
- **PENDIENTE:** Orden recién creada.
- **PAGADA:** Confirmación recibida del Servicio de Pagos.
- **CANCELADA:** Orden no procesada o anulada.

---

## 🔗 Ecosistema Completo
Consulta el repositorio de [Infraestructura y Guías](https://github.com/marielly-garcia-jimenez/Infraestructura-Examen) para más detalles.

---
<p align="center"> Servicio Core de Microservicios - 2026 </p>
