package com.company.orderprocessing.view.order;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.*;
import io.jmix.maps.utils.GeometryUtils;
import io.jmix.mapsflowui.component.GeoMap;
import io.jmix.mapsflowui.component.event.MapClickEvent;
import io.jmix.mapsflowui.component.event.MapMoveEndEvent;
import io.jmix.mapsflowui.component.event.MapZoomChangedEvent;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "orders", layout = MainView.class)
@ViewController(id = "ord_Order.list")
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
public class OrderListView extends StandardListView<Order> {
    protected GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    protected SaveContext saveContext;

    @Autowired
    private Notifications notifications;
    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private GeoMap map;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;

    @Subscribe
    public void onInit(final InitEvent event) {
        map.addClickListener(this::onMapClick);
//        map.addZoomChangedListener(this::onMapZoom);
//        map.addMoveEndListener(this::onMapMoveEnd);
        saveContext = new SaveContext();
    }

    @Subscribe
    public void onBeforeClose(final BeforeCloseEvent event) {
        dataManager.save(saveContext);
    }



    private void onMapClick(MapClickEvent event) {
        Point point = geometryFactory.createPoint(event.getCoordinate());
        Order order = ordersDataGrid.getSingleSelectedItem();
        if (order != null) {
            order.setLocation(point);
            saveContext.saving(order);
        }
        notifications.show(String.format("Map click: %.2f, %2f",
                event.getCoordinate().x, event.getCoordinate().y));
    }

    private void onMapZoom(MapZoomChangedEvent event) {
        notifications.show("Map has been zoomed: " + event.getZoom());
    }

    private void onMapMoveEnd(MapMoveEndEvent event) {
        notifications.create("Map has been moved",
                        String.format("""
                                        Map has been moved:
                                        Center: %.2f, %.2f
                                        Zoom: %.2f
                                        Rotation: %.2f
                                        North-east point: %.8f, %.8f
                                        South-west point: %.8f, %.8f
                                        """,
                                event.getCenter().getX(),
                                event.getCenter().getY(),
                                event.getZoom(),
                                event.getRotation(),
                                event.getExtent().getMaxX(),
                                event.getExtent().getMaxY(),
                                event.getExtent().getMinX(),
                                event.getExtent().getMinY()))
                .withPosition(Notification.Position.BOTTOM_END)
                .show();
    }

}