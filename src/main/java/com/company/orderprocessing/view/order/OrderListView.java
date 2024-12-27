package com.company.orderprocessing.view.order;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.repository.OrderRepository;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.pagination.SimplePagination;
import io.jmix.flowui.data.BindingState;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import io.jmix.maps.utils.GeometryUtils;
import io.jmix.mapsflowui.component.GeoMap;
import io.jmix.mapsflowui.component.data.DataVectorSourceItems;
import io.jmix.mapsflowui.component.event.MapClickEvent;
import io.jmix.mapsflowui.component.model.feature.MarkerFeature;
import io.jmix.mapsflowui.component.model.layer.VectorLayer;
import io.jmix.mapsflowui.component.model.source.DataVectorSource;
import io.jmix.mapsflowui.component.model.source.VectorSource;
import io.jmix.mapsflowui.kit.component.model.Padding;
import io.jmix.mapsflowui.kit.component.model.source.AbstractVectorSource;
import io.jmix.mapsflowui.kit.component.model.style.Fill;
import io.jmix.mapsflowui.kit.component.model.feature.*;
import io.jmix.mapsflowui.kit.component.model.style.MarkerStyle;
import io.jmix.mapsflowui.kit.component.model.style.Style;
import io.jmix.mapsflowui.kit.component.model.style.image.Anchor;
import io.jmix.mapsflowui.kit.component.model.style.image.IconOrigin;
import io.jmix.mapsflowui.kit.component.model.style.image.IconStyle;
import io.jmix.mapsflowui.kit.component.model.style.text.TextStyle;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;


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
    @Autowired
    private OrderRepository orderRepository;

    @ViewComponent
    private GeoMap map;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;
    @ViewComponent("map.ordersLayer")
    private VectorLayer mapOrdersLayer;
    @ViewComponent("map.ordersLayer.dataVectorSource")
    private DataVectorSource<Order> dataVectorSource;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;

    @Subscribe
    public void onInit(final InitEvent event) {
        map.addClickListener(this::onMapClick);
        saveContext = new SaveContext();

        initBuildingSource();

//        MarkerFeature markerFeature = getMarkerFeature("#FF0000");
//        AbstractVectorSource source = mapOrdersLayer.getSource();
//        ((VectorSource) source).addFeature(markerFeature);
//        System.out.println("Init");

    }

    private static MarkerFeature getMarkerFeature(String color) {
        // Create a styled marker
        MarkerFeature marker = new MarkerFeature(GeometryUtils.createPoint(20, 20));
        marker.removeAllStyles();
        marker.withStyles(new Style()
                .withImage(new IconStyle()
                        .withSrc("icons/marker-icon.png")
                        .withScale(0.1)
                        .withColor(color))); // Set the marker color to red
        return marker;
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {

        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        Collection<Order> orders = items.getItems();
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

    private String getPointColor(Order order) {
        OrderStatus status = order.getStatus();
        return switch (status) {
            case NEW -> "#0000FF"; // Blue
            case VERIFIED -> "#008000"; // Green
            case READY -> "#FFFF00"; // Yellow
            case IN_DELIVERY -> "#FFA500"; // Orange
            case COMPLETED -> "#A52A2A"; // Brown
            case CANCELLED -> "#000000"; // Black
        };
    }


    private void initBuildingSource(){
        dataVectorSource.setStyleProvider(order -> new Style()
                .withImage(new IconStyle()
                        .withColor(getPointColor(order))));
    }

    @Subscribe(id = "removeAllButton", subject = "clickListener")
    public void onRemoveAllButtonClick(final ClickEvent<JmixButton> event) {
        orderRepository.deleteAll();
    }

    @EventListener
    public void onMyEntityChanged(EntityChangedEvent<Order> event) {
        if (event.getType() == EntityChangedEvent.Type.CREATED) {
            ordersDl.load();
        }
    }

}