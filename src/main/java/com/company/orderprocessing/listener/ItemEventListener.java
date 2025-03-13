package com.company.orderprocessing.listener;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.ItemsProducedEvent;
import com.company.orderprocessing.event.ItemsSuspendedEvent;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component("cst_ItemEventListener")
public class ItemEventListener {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private RuntimeService runtimeService;

    private final static String MAN_START_MESSAGE = "Manufacturing start";
    private final static String MAN_SUSPEND_MESSAGE = "Manufacturing pause";
    private final static String MAN_RESUME_MESSAGE = "Manufacturing resume";
    private final static String MAN_STOP_SIGNAL = "Manufacturing full stop";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onItemChangedBeforeCommit(final EntityChangedEvent<Item> event) {
        OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
        Integer minItemsAvailable = settings.getMinItemsAvailable();
        Integer initialItemQuantity = settings.getInitialItemQuantity();
        systemAuthenticator.begin("admin");
        try {
            Id<Item> entityId = event.getEntityId();
            Item item = dataManager.load(entityId).one();

            if (item.getAvailable() < minItemsAvailable) {
                produceItems(item);
                uiEventPublisher.publishEvent(new ItemsProducedEvent(this, item));
            }

            if (item.getAvailable() >= initialItemQuantity) {
                suspendItemsProduction(item);
                uiEventPublisher.publishEvent(new ItemsSuspendedEvent(this, item));
            }

            Integer reserved = item.getReserved();
            if (reserved < 0) {
                System.out.println("reservation error");
            }
        } finally {
            systemAuthenticator.end();
        }
    }

    private void produceItems(Item item) {
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(item.getName())
                .singleResult();

        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        if (processInstance == null) {
            //start items production
            Integer manufacturingCycle = appSettings.load(OrderProcessingSettings.class).getManufacturingCycle();
            String timerValue = Iso8601Converter.convertSecondsToDuration(manufacturingCycle);
            params.put("cycleTimer", timerValue);
            runtimeService
                    .startProcessInstanceByMessage(MAN_START_MESSAGE, item.getName(), params);
        } else {
            //resume items production
            runtimeService
                    .messageEventReceived(MAN_RESUME_MESSAGE, processInstance.getId(), params);
        }
    }

    private void suspendItemsProduction(Item item) {
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(item.getName())
                .singleResult();
        if (processInstance != null) {
            runtimeService
                    .messageEventReceived(MAN_SUSPEND_MESSAGE, processInstance.getId());
        }
    }
}