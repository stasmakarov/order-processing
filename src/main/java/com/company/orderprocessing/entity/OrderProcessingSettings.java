package com.company.orderprocessing.entity;

import io.jmix.appsettings.defaults.AppSettingsDefault;
import io.jmix.appsettings.entity.AppSettingsEntity;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@JmixEntity
@Table(name = "ORD_ORDER_PROCESSING_SETTING")
@Entity(name = "ord_OrderProcessingSetting")
public class OrderProcessingSettings extends AppSettingsEntity {
    @JmixGeneratedValue
    @Column(name = "UUID")
    private UUID uuid;

    @AppSettingsDefault("3")
    @Column(name = "DELIVERY_PACKAGE")
    private Integer deliveryPackage;

    @AppSettingsDefault("70")
    @Column(name = "PAYMENT_ERROR_PROBABILITY")
    private Integer paymentErrorProbability;

    @AppSettingsDefault("500")
    @Column(name = "INITIAL_ITEM_QUANTITY")
    private Integer initialItemQuantity;

    public Integer getInitialItemQuantity() {
        return initialItemQuantity;
    }

    public void setInitialItemQuantity(Integer initialItemQuantity) {
        this.initialItemQuantity = initialItemQuantity;
    }


    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getDeliveryPackage() {
        return deliveryPackage;
    }

    public void setDeliveryPackage(Integer deliveryPackage) {
        this.deliveryPackage = deliveryPackage;
    }

    public Integer getPaymentErrorProbability() {
        return paymentErrorProbability;
    }

    public void setPaymentErrorProbability(Integer paymentErrorProbability) {
        this.paymentErrorProbability = paymentErrorProbability;
    }
}
