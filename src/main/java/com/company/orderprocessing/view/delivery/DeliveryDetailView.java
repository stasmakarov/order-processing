package com.company.orderprocessing.view.delivery;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "deliveries/:id", layout = MainView.class)
@ViewController(id = "ord_Delivery.detail")
@ViewDescriptor(path = "delivery-detail-view.xml")
@EditedEntityContainer("deliveryDc")
public class DeliveryDetailView extends StandardDetailView<Delivery> {
    @Autowired
    private DataManager dataManager;
    @ViewComponent
    private CollectionPropertyContainer<Order> ordersDc;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        List<Order> orders = dataManager.load(Order.class)
                .query("select o from ord_Order o where o.delivery = :delivery")
                .parameter("delivery", getEditedEntity())
                .list();
        ordersDc.setItems(orders);
    }


}