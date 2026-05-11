package com.shopping.emarket.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    public enum Type { CARD, PAYPAL }

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    private String brand;

    @Column(length = 4)
    private String last4;

    @Column(name = "token_ref", length = 128)
    private String tokenRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentMethod() {}

    public PaymentMethod(UUID id, Type type, String brand, String last4, String tokenRef) {
        this.id = id;
        this.type = type;
        this.brand = brand;
        this.last4 = last4;
        this.tokenRef = tokenRef;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Type getType() { return type; }
    public String getBrand() { return brand; }
    public String getLast4() { return last4; }
    public String getTokenRef() { return tokenRef; }
    public Instant getCreatedAt() { return createdAt; }
}
