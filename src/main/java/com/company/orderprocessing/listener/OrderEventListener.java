package com.company.orderprocessing.listener;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.RefreshViewEvent;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
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

@Component("ord_OrderEventListener")
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private static final String MESSAGE_NAME = "Start delivery";

    private final DataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final RuntimeService runtimeService;
    private final UiEventPublisher uiEventPublisher;
    private final AppSettings appSettings;


    public OrderEventListener(DataManager dataManager,
                              SystemAuthenticator systemAuthenticator,
                              RuntimeService runtimeService,
                              UiEventPublisher uiEventPublisher,
                              AppSettings appSettings) {
        this.dataManager = dataManager;
        this.systemAuthenticator = systemAuthenticator;
        this.runtimeService = runtimeService;
        this.uiEventPublisher = uiEventPublisher;
        this.appSettings = appSettings;
    }

    /**
     * Listens for changes to the {@link Order} entity and triggers actions based on the order's status.
     * If the status has changed to READY, it counts the number of orders with that status.
     * If the count exceeds a predefined limit, it starts a process instance by sending a message.
     *
     * @param event The {@link EntityChangedEvent} containing details about the changes made to the
     *              {@link Order} entity. This event includes information about the type of change
     *              (e.g., created, updated, deleted) and specific field changes.
     *
     * @throws RuntimeException if an error occurs while querying the database or starting the process
     *                          instance. The exception is wrapped in a RuntimeException to indicate
     *                          that it should be handled at a higher level.
     *
     * <p>
     * Note: This method is intended to be used within a transactional context. It relies on the
     * {@link DataManager} to perform database operations and may affect system behavior based on
     * order status changes.
     * </p>
     */

    @SuppressWarnings("JmixRuntimeException")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onOrderChangedAfterCommit(final EntityChangedEvent<Order> event) {
        if (!event.getChanges().isChanged("status")
                || event.getType() == EntityChangedEvent.Type.DELETED) return;

        systemAuthenticator.begin("admin");
        try {
            OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
            long count = dataManager.loadValue(
                    "select count(e) from ord_Order e where e.status = :status", Long.class)
                    .parameter("status", OrderStatus.READY)
                    .one();

            if (count > settings.getDeliveryPackage()) {
                log.info("Number of orders with status READY:{}. The delivery process will be started.", count);
                runtimeService.startProcessInstanceByMessage(MESSAGE_NAME);
            }
            uiEventPublisher.publishEvent(new RefreshViewEvent(this));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }
}