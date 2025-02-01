package com.company.orderprocessing.view.order;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.RefreshViewEvent;
import com.company.orderprocessing.repository.OrderRepository;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import io.jmix.mapsflowui.component.GeoMap;
import io.jmix.mapsflowui.component.data.DataVectorSourceItems;
import io.jmix.mapsflowui.component.model.source.ClusterSource;
import io.jmix.mapsflowui.component.model.source.DataVectorSource;
import io.jmix.mapsflowui.component.model.source.GeoObjectClickNotifier;
import io.jmix.mapsflowui.kit.component.model.Padding;
import io.jmix.mapsflowui.kit.component.model.style.Fill;
import io.jmix.mapsflowui.kit.component.model.style.Style;
import io.jmix.mapsflowui.kit.component.model.style.image.Anchor;
import io.jmix.mapsflowui.kit.component.model.style.image.CircleStyle;
import io.jmix.mapsflowui.kit.component.model.style.image.IconOrigin;
import io.jmix.mapsflowui.kit.component.model.style.image.IconStyle;
import io.jmix.mapsflowui.kit.component.model.style.text.TextStyle;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OrderListView.class);
    protected SaveContext saveContext;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private OrderRepository orderRepository;

    @ViewComponent
    private GeoMap map;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;
    @ViewComponent("map.ordersLayer.cluster.dataVectorSource1")
    private DataVectorSource<Order> dataVectorSource;
    @ViewComponent("map.ordersLayer.cluster")
    private ClusterSource cluster;
    @ViewComponent
    private CollectionContainer<Order> ordersDc;

    @Subscribe
    public void onInit(final InitEvent event) {
        saveContext = new SaveContext();
        dataVectorSource.addGeoObjectClickListener(this::onGeoObjectClick);
        dataVectorSource.setStyleProvider(this::createStyleForOrder);
        cluster.addPointStyles(createClusterStyle());
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        System.out.println("check items");
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        System.out.println("check items");
    }

    @Subscribe
    public void onBeforeClose(final BeforeCloseEvent event) {
        dataManager.save(saveContext);
    }


    private String getPointColor(Order order) {
        OrderStatus status = order.getStatus();

        // Orders in status NEW are not displayed on the map because they don't have a 'location' attribute
        // As well, CANCELLED orders may not have location
        return switch (status) {
            case VERIFIED ->    "LIMEGREEN";
            case READY ->       "YELLOW";
            case IN_DELIVERY -> "ORANGE";
            case COMPLETED ->   "PLUM";
            case CANCELLED ->   "SLATEGREY";
            default ->          "#000000";
        };
    }

    private Style createStyleForOrder(Order order) {
        return new Style()
                .withImage(createIconStyle(order))
                .withText(createTextStyle(order));
    }

    private IconStyle createIconStyle(Order order) {
        return new IconStyle()
                .withScale(0.5)
                .withAnchorOrigin(IconOrigin.BOTTOM_LEFT)
                .withAnchor(new Anchor(0.49, 0.12))
                .withSrc("icons/marker-6.png")
                .withColor(getPointColor(order));
    }

    private TextStyle createTextStyle(Order order) {
        return new TextStyle()
                .withBackgroundFill(new Fill("rgba(255, 255, 255, 0.6)"))
                .withPadding(new Padding(5, 5, 5, 5))
                .withOffsetY(15)
                .withFont("11px sans-serif")
                .withText(order.getNumber());
    }

    private Style createClusterStyle() {
        CircleStyle circleStyle = new CircleStyle();
        circleStyle.setRadius(12);
        circleStyle.setFill(new Fill("NAVY"));
        Style style = new Style();
        style.setImage(circleStyle);
        return style;
    }

    private void onGeoObjectClick(GeoObjectClickNotifier.GeoObjectClickEvent<Order> event) {
        Order orderMarker = event.getItem();
        DataGridItems<Order> items = ordersDataGrid.getItems();
        if (items != null) {
            Collection<Order> orders = items.getItems();
            for (Order order : orders) {
                if (order.getId().equals(orderMarker.getId())) {
                    ordersDataGrid.getItems().setSelectedItem(order);
                    break;
                }
            }
        }
    }


    @Subscribe(id = "removeAllButton", subject = "clickListener")
    public void onRemoveAllButtonClick(final ClickEvent<JmixButton> event) {
        orderRepository.deleteAll();
        ordersDl.load();
    }

    @EventListener
    public void onOrderChanged(RefreshViewEvent event) {
        ordersDc.getMutableItems().clear();
        ordersDl.load();
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        String orderNumber = event.getOrderNumber();
        log.info("Changed order: {}", orderNumber);
    }

    @Subscribe(id = "refreshMapBtn", subject = "clickListener")
    public void onRefreshMapBtnClick(final ClickEvent<JmixButton> event) {
        ordersDc.getMutableItems().clear();
        ordersDl.load();
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        dataVectorSource.setStyleProvider(this::createStyleForOrder);
        System.out.println("refresh map");
    }

}