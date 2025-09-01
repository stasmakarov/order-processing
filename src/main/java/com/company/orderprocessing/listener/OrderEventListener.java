package com.company.orderprocessing.listener;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.event.RefreshOrderViewEvent;
import io.jmix.core.DataManager;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("ord_OrderEventListener")
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final DataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final UiEventPublisher uiEventPublisher;

    public OrderEventListener(DataManager dataManager,
                              SystemAuthenticator systemAuthenticator,
                              UiEventPublisher uiEventPublisher

    ) {
        this.dataManager = dataManager;
        this.systemAuthenticator = systemAuthenticator;
        this.uiEventPublisher = uiEventPublisher;
    }

    @SuppressWarnings("JmixRuntimeException")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onOrderChangedAfterCommit(final EntityChangedEvent<Order> event) {
        if (EntityChangedEvent.Type.DELETED.equals(event.getType())) return;
        if (!event.getChanges().isChanged("status")) return;

        systemAuthenticator.begin("admin");
        try {
            String number = dataManager.loadValue(
                            "select e.number from ord_Order e where e.id = :id", String.class)
                    .parameter("id", event.getEntityId().getValue())
                    .one();
            uiEventPublisher.publishEvent(new RefreshOrderViewEvent(this, number));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }
}