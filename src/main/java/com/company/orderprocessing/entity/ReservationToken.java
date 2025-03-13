package com.company.orderprocessing.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@JmixEntity
@Table(name = "ORD_RESERVATION_TOKEN")
@Entity(name = "ord_ReservationToken")
public class ReservationToken {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private Long id;

    @JmixGeneratedValue
    @Column(name = "UUID")
    private UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}