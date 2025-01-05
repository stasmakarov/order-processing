package com.company.orderprocessing.view.delivery;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "deliveries/:id", layout = MainView.class)
@ViewController(id = "ord_Delivery.detail")
@ViewDescriptor(path = "delivery-detail-view.xml")
@EditedEntityContainer("deliveryDc")
public class DeliveryDetailView extends StandardDetailView<Delivery> {
}