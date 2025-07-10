package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.Numerator;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.rabbit.RabbitService;
import com.company.orderprocessing.repository.DeliveryRepository;
import com.company.orderprocessing.repository.OrderRepository;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component("ord_ResetService")
public class ResetService {

    @Autowired
    private DeliveryRepository deliveryRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RabbitService rabbitService;


    public void deleteAllProcessInstances() {
//        try {
//            ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("clean_db.sql"));
//        } catch (SQLException e) {
//            //noinspection JmixRuntimeException
//            throw new RuntimeException(e);
//        }
        deleteCompletedProcessInstances();
        terminateActiveProcessInstances();
    }

    @Transactional
    private void deleteCompletedProcessInstances() {
        int batchSize = 50;

        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .finished(); // Only get completed processes

        List<String> instanceIds;
        do {
            instanceIds = query.listPage(0, batchSize)
                    .stream()
                    .map(HistoricProcessInstance::getId)
                    .toList();

            for (String instanceId : instanceIds) {
                deleteInstanceWithRetry(instanceId); // Retry deletion if needed
            }
        } while (!instanceIds.isEmpty());
    }
    private static final int MAX_RETRIES = 3;

    private void deleteInstanceWithRetry(String instanceId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                historyService.deleteHistoricProcessInstance(instanceId);
                return;
            } catch (Exception e) {
                attempt++;
                System.out.println("Retrying deletion: " + instanceId + " (Attempt " + attempt + ")");
            }
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void terminateActiveProcessInstances() {
        ProcessInstanceQuery activeQuery = runtimeService.createProcessInstanceQuery();
        activeQuery.list()
                .forEach(instance -> runtimeService.deleteProcessInstance(
                        instance.getId(),
                        "Bulk deletion requested"
                ));
    }


    public void deleteAllDeliveries() {
        deliveryRepository.deleteAll();
    }

    public void deleteAllOrders() {
        orderRepository.deleteAll();
    }

    public void initItems() {
        SaveContext saveContext = new SaveContext();
        OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
        List<Item> items = unconstrainedDataManager.load(Item.class).all().list();
        for (Item item : items) {
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
        rabbitService.purgeQueue(settings.getOrderQueue());
        rabbitService.purgeQueue(settings.getInventoryQueue());
        rabbitService.purgeQueue(settings.getReplyQueue());
    }
}