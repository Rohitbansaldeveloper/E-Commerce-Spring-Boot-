# Inventory Service

Inventory is a separate microservice with its own PostgreSQL database and Redis stock cache.

## Flow
Client -> API Gateway -> Order Service -> Product Service (price/details)
                         |
                         +-> Inventory Service -> PostgreSQL
                                           \-> Redis cache

## Stock reservation
Order creation calls:
POST /inventory/{productId}/reserve?quantity=2

The Inventory Service uses a conditional SQL update:
available_quantity = available_quantity - quantity
WHERE product_id = ? AND available_quantity >= quantity

Only one concurrent reservation can consume the same stock row when the condition is evaluated by PostgreSQL. If the order later fails, Order Service sends a compensating release request.

## Admin stock setup
After creating a product, initialize stock:
POST /inventory/{productId}?quantity=50

This endpoint is intended for ADMIN users through the gateway.

## Read stock
GET /inventory/{productId}

Redis caches stock for 5 minutes. Reserve/release/set operations invalidate the cache.

## Important distributed-systems note
The Order DB and Inventory DB are separate transactions. They are not one distributed ACID transaction. The implementation uses compensation for failed order creation. For a larger production system, persist reservation/compensation commands in an outbox and add reconciliation.
