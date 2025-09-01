package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.Numerator;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.rabbit.RabbitService;
import com.company.orderprocessing.repository.DeliveryRepository;
import com.company.orderprocessing.repository.ItemRepository;
import com.company.orderprocessing.repository.OrderRepository;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("ord_ResetService")
public class ResetService {

    private static final Logger log = LoggerFactory.getLogger(ResetService.class);

    // --- Deps via constructor injection to avoid nulls in tests
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final AppSettings appSettings;
    private final UnconstrainedDataManager unconstrainedDataManager;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final RabbitService rabbitService;

    public ResetService(DeliveryRepository deliveryRepository,
                        OrderRepository orderRepository, ItemRepository itemRepository,
                        AppSettings appSettings,
                        UnconstrainedDataManager unconstrainedDataManager,
                        HistoryService historyService,
                        RuntimeService runtimeService, RepositoryService repositoryService,
                        RabbitService rabbitService) {
        this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.itemRepository = itemRepository;
        this.appSettings = Objects.requireNonNull(appSettings);
        this.unconstrainedDataManager = Objects.requireNonNull(unconstrainedDataManager);
        this.historyService = Objects.requireNonNull(historyService);
        this.runtimeService = Objects.requireNonNull(runtimeService);
        this.repositoryService = repositoryService;
        this.rabbitService = Objects.requireNonNull(rabbitService);
    }

    /**
     * Full cleanup: terminate active runtime instances first, then delete historic ones.
     * This order avoids race conditions and foreign key issues.
     */
    public void deleteAllProcessInstances() {
        pauseInbound();
        try {
            terminateActiveProcessInstances(200);
            deleteHistoricProcessInstances(200);
        } finally {
            resumeInbound();
        }
    }

    // ---------------------------------------------------------------------
    // Runtime
    // ---------------------------------------------------------------------

    // Pause inbound sources & new process starts
    private void pauseInbound() {
        try {
            // Stop Rabbit listeners (no new external messages)
            rabbitService.stopListening();
        } catch (Exception e) {
            log.warn("Failed to stop Rabbit listeners: {}", e.toString());
        }


        // Suspend all active process definitions (block new instance starts)
        repositoryService.createProcessDefinitionQuery()
                .active()
                .list()
                .forEach(pd -> {
                    try {
                        // includeProcessInstances=false -> only block new starts
                        repositoryService.suspendProcessDefinitionById(pd.getId(), false, null);
                    } catch (Exception e) {
                        log.warn("Could not suspend PD {}: {}", pd.getKey(), e.getMessage());
                    }
                });
    }

    private void resumeInbound() {
        // 1) activate process definitions back
        repositoryService.createProcessDefinitionQuery()
                .suspended()
                .list()
                .forEach(pd -> {
                    try {
                        repositoryService.activateProcessDefinitionById(pd.getId(), false, null);
                    } catch (Exception e) {
                        log.warn("Could not activate PD {}: {}", pd.getKey(), e.getMessage());
                    }
                });

        try {
            // 2) start Rabbit listeners again
            rabbitService.startListening();
        } catch (Exception e) {
            log.warn("Failed to start Rabbit listeners: {}", e.toString());
        }
    }


    /**
     * Delete active runtime instances in batches. Uses new transactions to ensure progress.
     */
    public void terminateActiveProcessInstances(int batchSize) {
        if (batchSize <= 0) batchSize = 200;
        while (true) {
            List<String> ids = fetchActiveRuntimeIds(batchSize);
            if (ids.isEmpty()) break;
            terminateBatch(ids);
        }
    }

    private List<String> fetchActiveRuntimeIds(int batchSize) {
        ProcessInstanceQuery q = runtimeService.createProcessInstanceQuery();
        List<ProcessInstance> page = q.listPage(0, batchSize);
        List<String> ids = new ArrayList<>(page.size());
        for (ProcessInstance pi : page) {
            ids.add(pi.getId());
        }
        return ids;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void terminateBatch(List<String> instanceIds) {
        for (String id : instanceIds) {
            try {
                // comment: Cascade deletes runtime execution and related jobs/tasks
                runtimeService.deleteProcessInstance(id, "Bulk deletion requested");
            } catch (Exception e) {
                log.warn("Failed to delete runtime instance {}: {}", id, e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------------
    // History
    // ---------------------------------------------------------------------

    /**
     * Delete finished/deleted historic instances in batches.
     * Uses new transactions per batch to avoid long-running transactions and to actually release rows for next page.
     */
    public void deleteHistoricProcessInstances(int batchSize) {
        if (batchSize <= 0) batchSize = 200;
        while (true) {
            List<String> ids = fetchHistoricFinishedIds(batchSize);
            if (ids.isEmpty()) break;
            deleteHistoricBatch(ids);
        }
    }

    private List<String> fetchHistoricFinishedIds(int batchSize) {
        HistoricProcessInstanceQuery q = historyService.createHistoricProcessInstanceQuery().finished();
        List<HistoricProcessInstance> page = q.listPage(0, batchSize);
        List<String> ids = new ArrayList<>(page.size());
        for (HistoricProcessInstance hpi : page) {
            ids.add(hpi.getId());
        }
        return ids;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void deleteHistoricBatch(List<String> instanceIds) {
        for (String id : instanceIds) {
            int attempt = 0;
            while (true) {
                try {
                    // comment: Cascade deletes all historic details, activities, vars, incidents
                    historyService.deleteHistoricProcessInstance(id);
                    break;
                } catch (Exception e) {
                    attempt++;
                    if (attempt >= 3) {
                        log.warn("Giving up deleting historic instance {} after {} attempts: {}", id, attempt, e.getMessage());
                        break;
                    }
                    try { Thread.sleep(Duration.ofMillis(150).toMillis()); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Domain helpers
    // ---------------------------------------------------------------------

    public void deleteAllDeliveries() {
        deliveryRepository.deleteAll();
    }

    public void deleteAllOrders() {
        orderRepository.deleteAll();
    }

    public void initItems() {
        itemRepository.deleteAll();
        List<String> itemNames = List.of("Boo", "Foo", "Moo");
        SaveContext saveContext = new SaveContext();
        OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);

        for (String itemName : itemNames) {
            Item item = unconstrainedDataManager.create(Item.class);
            item.setName(itemName);
            item.setAvailable(settings.getInitialItemQuantity());
            item.setReserved(0);
            item.setDelivered(0);
            saveContext.saving(item);
        }
        unconstrainedDataManager.save(saveContext);
    }

    public void initNumerators() {
        SaveContext saveContext = new SaveContext();
        List<Numerator> numerators = unconstrainedDataManager.load(Numerator.class).all().list();
        for (Numerator numerator : numerators) {
            numerator.setNumber(0);
            saveContext.saving(numerator);
        }
        unconstrainedDataManager.save(saveContext);
    }

    public void purgeQueues() {
        OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
        rabbitService.safePurge(settings.getOrderQueue());
        rabbitService.safePurge(settings.getInventoryQueue());
        rabbitService.safePurge(settings.getReplyQueue());
    }
}
