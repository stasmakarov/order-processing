package com.company.orderprocessing.listener;

import com.company.orderprocessing.app.DeliveryService;
import com.company.orderprocessing.app.InventoryService;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.RefreshViewEvent;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.event.AttributeChanges;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component("ord_OrderEventListener")
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private static final String MESSAGE_NAME = "Start delivery";
    private final Random random = new Random();

    private final DataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final RuntimeService runtimeService;
    private final UiEventPublisher uiEventPublisher;
    private final AppSettings appSettings;
    private final DeliveryService deliveryService;


    public OrderEventListener(DataManager dataManager,
                              SystemAuthenticator systemAuthenticator,
                              RuntimeService runtimeService,
                              UiEventPublisher uiEventPublisher,
                              AppSettings appSettings,
                              DeliveryService deliveryService
    ) {
        this.dataManager = dataManager;
        this.systemAuthenticator = systemAuthenticator;
        this.runtimeService = runtimeService;
        this.uiEventPublisher = uiEventPublisher;
        this.appSettings = appSettings;
        this.deliveryService = deliveryService;
    }

    @SuppressWarnings("JmixRuntimeException")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onOrderChangedAfterCommit(final EntityChangedEvent<Order> event) {
        Id<Order> entityId = event.getEntityId();
        AttributeChanges changes = event.getChanges();
        if (!event.getChanges().isChanged("status")
                || event.getType() == EntityChangedEvent.Type.DELETED) return;

        systemAuthenticator.begin("admin");
        try {
            OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
            long count = dataManager.loadValue(
                    "select count(e) from ord_Order e where e.status = :status", Long.class)
                    .parameter("status", OrderStatus.READY)
                    .one();

            Order order = dataManager.load(entityId).one();
            if (count > settings.getDeliveryPackage()) {
                log.info("Number of orders with status READY:{}. The delivery process will be started.", count);
                deliveryService.startDeliveryProcess();
            }
            uiEventPublisher.publishEvent(new RefreshViewEvent(this, order.getNumber()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

}