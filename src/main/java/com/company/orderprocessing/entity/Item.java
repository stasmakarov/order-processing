package com.company.orderprocessing.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "ORD_ITEM")
@Entity(name = "ord_Item")
public class Item {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "AVAILABLE")
    private Integer available = 0;

    @Column(name = "RESERVED")
    private Integer reserved = 0;

    @Column(name = "DELIVERED")
    private Integer delivered = 0;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getDelivered() {
        return delivered;
    }

    public void setDelivered(Integer delivered) {
        this.delivered = delivered;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        if (reserved < 0)
            return;
        this.reserved = reserved;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}