package com.company.orderprocessing.view.order;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.nominatim.GeoCodingService;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController(id = "ord_Order.detail")
@ViewDescriptor(path = "order-detail-view.xml")
@EditedEntityContainer("orderDc")
public class OrderDetailView extends StandardDetailView<Order> {
    @Autowired
    private GeoCodingService geoCodingService;
    @Autowired
    private Notifications notifications;

    @Subscribe(id = "verifyAddressBtn", subject = "clickListener")
    public void onVerifyAddressBtnClick(final ClickEvent<JmixButton> event) {
        String address = getEditedEntity().getAddress();
        Point point = geoCodingService.verifyAddress(address);
        if (point != null) {
            getEditedEntity().setLocation(point);
            notifications.create("Address verified")
                    .show();
        } else {
            getEditedEntity().setLocation(null);
            notifications.create("Address do not exist")
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }
}