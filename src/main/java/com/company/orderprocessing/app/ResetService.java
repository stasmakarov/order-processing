package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.Numerator;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.repository.DeliveryRepository;
import com.company.orderprocessing.repository.OrderRepository;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Component("ord_ResetService")
public class ResetService {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private DeliveryRepository deliveryRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;


    public void deleteAllProcessInstances() {
        try {
            ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("clean_db.sql"));
        } catch (SQLException e) {
            //noinspection JmixRuntimeException
            throw new RuntimeException(e);
        }
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
}