package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ManufacturingProcessStatus;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.ItemsProducedEvent;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component(value = "ord_ManufacturingService")
public class ManufacturingService {

    private static final Logger log = LoggerFactory.getLogger(ManufacturingService.class);
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;

    private final static String MAN_START_MESSAGE = "Manufacturing start";
    private final static String MAN_SUSPEND_MESSAGE = "Manufacturing pause";
    private final static String MAN_RESUME_MESSAGE = "Manufacturing resume";
    private final static String MAN_STOP_SIGNAL = "Manufacturing full stop";

    private final Random random = new Random();

    public void produceItems(Item item) {
        Integer maxItemsParty = appSettings.load(OrderProcessingSettings.class).getMaxItemsParty();
        int producedQty = random.nextInt(maxItemsParty);
        inventoryService.produceItems(item, producedQty);
    }

    public void startOrResumeItemsProduction(Item item) {
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);

        ProcessInstance processInstance = null;
        try {
            processInstance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(item.getName())
                    .singleResult();
        } catch (FlowableException e) {
            throw new RuntimeException(e);
        }

        if (processInstance == null) {
            //start items production
            Integer manufacturingCycle = appSettings.load(OrderProcessingSettings.class).getManufacturingCycle();
            String timerValue = Iso8601Converter.convertSecondsToDuration(manufacturingCycle);
            params.put("cycleTimer", timerValue);
            runtimeService
                    .startProcessInstanceByMessage(MAN_START_MESSAGE, item.getName(), params);
        } else {
            //resume items production
            EventSubscription subscription = runtimeService.createEventSubscriptionQuery()
                    .processInstanceId(processInstance.getId())
                    .eventName(MAN_RESUME_MESSAGE)
                    .singleResult();
            if (subscription == null) return;
            runtimeService
                    .messageEventReceived(MAN_RESUME_MESSAGE, processInstance.getId(), params);
        }
    }


    public void suspendItemsProduction(Item item) {
        ProcessInstance processInstance = null;
        try {
            processInstance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(item.getName())
                    .singleResult();
        } catch (FlowableException e) {
            log.info("Manufacturing: Query return 2 results instead of max 1. Item: {}", item.getName());
            return;
        }
        if (processInstance != null) {
            runtimeService
                    .messageEventReceived(MAN_SUSPEND_MESSAGE, processInstance.getId());
        }
    }

    public void terminateAllItemsProduction() {
        runtimeService.signalEventReceived(MAN_STOP_SIGNAL);
    }

    public Map<String, ManufacturingProcessStatus> getManufacturingProcessesStatus() {
        Map<String, ManufacturingProcessStatus> map = new HashMap<>();
        systemAuthenticator.begin("admin");
        try {
            List<Item> items = dataManager.load(Item.class).all().list();
            for (Item item : items) {
                ManufacturingProcessStatus status = getProcessStatusByItem(item);
                map.put(item.getName(), status);
            }
        } finally {
            systemAuthenticator.end();
        }
        return map;
    }

    private ManufacturingProcessStatus getProcessStatusByItem(Item item) {
        ProcessInstance processInstance = findProcessInstanceByItem(item);
        if (processInstance != null) {
            return isSuspended(processInstance) ?
                    ManufacturingProcessStatus.SUSPENDED : ManufacturingProcessStatus.PRODUCTION;
        } else {
            return ManufacturingProcessStatus.NOT_STARTED;
        }
    }

    private ProcessInstance findProcessInstanceByItem(Item item) {
        try {
            return runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(item.getName())
                    .singleResult();
        } catch (FlowableException e) {
            return null;
        }
    }

    private boolean isSuspended(ProcessInstance processInstance) {
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .eventName(MAN_SUSPEND_MESSAGE)
                .list();
        return subscriptions != null && !subscriptions.isEmpty();
    }


}