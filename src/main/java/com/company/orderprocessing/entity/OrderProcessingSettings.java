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

    @AppSettingsDefault("orders")
    @Column(name = "ORDER_QUEUE")
    private String orderQueue;

    @AppSettingsDefault("inventory")
    @Column(name = "INVENTORY_QUEUE")
    private String inventoryQueue;

    @AppSettingsDefault("3")
    @Column(name = "DELIVERY_PACKAGE")
    private Integer deliveryPackage;

    @AppSettingsDefault("70")
    @Column(name = "PAYMENT_ERROR_PROBABILITY")
    private Integer paymentErrorProbability;

    @AppSettingsDefault("500")
    @Column(name = "INITIAL_ITEM_QUANTITY")
    private Integer initialItemQuantity;

    @AppSettingsDefault("60")
    @Column(name = "MAX_DELAY_TIMER")
    private Integer maxDelayTimer;

    @AppSettingsDefault("10")
    @Column(name = "MAX_ITEMS_PARTY")
    private Integer maxItemsParty;

    @AppSettingsDefault("50")
    @Column(name = "MIN_ITEMS_AVAILABLE")
    private Integer minItemsAvailable;

    @AppSettingsDefault("300")
    @Column(name = "MANUFACTURING_CYCLE")
    private Integer manufacturingCycle;

    public String getInventoryQueue() {
        return inventoryQueue;
    }

    public void setInventoryQueue(String inventoryQueue) {
        this.inventoryQueue = inventoryQueue;
    }

    public Integer getMinItemsAvailable() {
        return minItemsAvailable;
    }

    public void setMinItemsAvailable(Integer minItemsAvailable) {
        this.minItemsAvailable = minItemsAvailable;
    }

    public String getOrderQueue() {
        return orderQueue;
    }

    public void setOrderQueue(String orderQueue) {
        this.orderQueue = orderQueue;
    }

    public Integer getMaxDelayTimer() {
        return maxDelayTimer;
    }

    public void setMaxDelayTimer(Integer maxDelayTimer) {
        this.maxDelayTimer = maxDelayTimer;
    }

    public void setManufacturingCycle(Integer manufacturingCycle) {
        this.manufacturingCycle = manufacturingCycle;
    }

    public Integer getManufacturingCycle() {
        return manufacturingCycle;
    }

    public Integer getMaxItemsParty() {
        return maxItemsParty;
    }

    public void setMaxItemsParty(Integer maxItemsParty) {
        this.maxItemsParty = maxItemsParty;
    }

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
