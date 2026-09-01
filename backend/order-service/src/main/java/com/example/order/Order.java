package com.example.order;
import java.util.UUID;
public record Order(UUID id, String customerEmail, String product, int quantity) {}
