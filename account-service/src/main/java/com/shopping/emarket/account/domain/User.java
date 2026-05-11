package com.shopping.emarket.account.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1",      column = @Column(name = "shipping_line1")),
            @AttributeOverride(name = "line2",      column = @Column(name = "shipping_line2")),
            @AttributeOverride(name = "city",       column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "region",     column = @Column(name = "shipping_region")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal")),
            @AttributeOverride(name = "country",    column = @Column(name = "shipping_country"))
    })
    private Address shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1",      column = @Column(name = "billing_line1")),
            @AttributeOverride(name = "line2",      column = @Column(name = "billing_line2")),
            @AttributeOverride(name = "city",       column = @Column(name = "billing_city")),
            @AttributeOverride(name = "region",     column = @Column(name = "billing_region")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal")),
            @AttributeOverride(name = "country",    column = @Column(name = "billing_country"))
    })
    private Address billingAddress;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(UUID id, String email, String username, String passwordHash) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String username, Address shipping, Address billing) {
        if (username != null) this.username = username;
        if (shipping != null) this.shippingAddress = shipping;
        if (billing != null) this.billingAddress = billing;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Address getShippingAddress() { return shippingAddress; }
    public Address getBillingAddress() { return billingAddress; }
    public List<PaymentMethod> getPaymentMethods() { return paymentMethods; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
