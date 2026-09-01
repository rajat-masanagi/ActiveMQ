package com.example.order;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
public record CreateOrderRequest(@NotBlank @Email String customerEmail, @NotBlank String product, @Positive int quantity) {}
