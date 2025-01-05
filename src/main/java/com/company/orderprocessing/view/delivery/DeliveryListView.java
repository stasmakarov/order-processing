package com.company.orderprocessing.view.delivery;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "deliveries", layout = MainView.class)
@ViewController(id = "ord_Delivery.list")
@ViewDescriptor(path = "delivery-list-view.xml")
@LookupComponent("deliveriesDataGrid")
@DialogMode(width = "64em")
public class DeliveryListView extends StandardListView<Delivery> {
}