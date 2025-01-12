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
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import io.jmix.maps.utils.GeometryUtils;
import io.jmix.mapsflowui.component.GeoMap;
import io.jmix.mapsflowui.component.data.DataVectorSourceItems;
import io.jmix.mapsflowui.component.model.source.DataVectorSource;
import io.jmix.mapsflowui.component.model.source.GeoObjectClickNotifier;
import io.jmix.mapsflowui.kit.component.model.Padding;
import io.jmix.mapsflowui.kit.component.model.style.Fill;
import io.jmix.mapsflowui.kit.component.model.style.Style;
import io.jmix.mapsflowui.kit.component.model.style.image.Anchor;
import io.jmix.mapsflowui.kit.component.model.style.image.IconOrigin;
import io.jmix.mapsflowui.kit.component.model.style.image.IconStyle;
import io.jmix.mapsflowui.kit.component.model.style.text.TextStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.Optional;


@Route(value = "orders", layout = MainView.class)
@ViewController(id = "ord_Order.list")
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
public class OrderListView extends StandardListView<Order> {

    protected SaveContext saveContext;
    protected GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    @Autowired
    private DataManager dataManager;
    @Autowired
    private OrderRepository orderRepository;

    @ViewComponent
    private GeoMap map;
    @ViewComponent("map.ordersLayer.dataVectorSource1")
    private DataVectorSource<Order> dataVectorSource;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;

    @Subscribe
    public void onInit(final InitEvent event) {
        saveContext = new SaveContext();
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        ordersDl.load();
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        if (items != null) {
            Collection<Order> orders = items.getItems();
            System.out.println("Before");
        }
        initBuildingSource();
    }

    @Subscribe
    public void onBeforeClose(final BeforeCloseEvent event) {
        dataManager.save(saveContext);
    }


    private String getPointColor(Order order) {
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.SECRET) {
            return "#0000FF";
        } if (status == OrderStatus.VERIFIED) {
            return "#008000";
        }

        return switch (status) {
            case NEW -> "#0000FF"; // Blue
            case VERIFIED -> "#008000"; // Green
            case READY -> "#FFFF00"; // Yellow
            case IN_DELIVERY -> "#FFA500"; // Orange
            case COMPLETED -> "#A52A2A"; // Brown
            case CANCELLED -> "#000000"; // Black
            default -> "#000FFF";
        };
    }


    private void initBuildingSource(){
        dataVectorSource.setStyleProvider(order -> new Style()
                .withImage(new IconStyle()
                        .withScale(0.5)
                        .withAnchorOrigin(IconOrigin.BOTTOM_LEFT)
                        .withAnchor(new Anchor(0.49, 0.12))
                        .withSrc("icons/marker-6.png")
                        .withColor(getPointColor(order)))
                .withText(new TextStyle()
                        .withBackgroundFill(new Fill("rgba(255, 255, 255, 0.6)"))
                        .withPadding(new Padding(5, 5, 5, 5))
                        .withOffsetY(15)
                        .withFont("11px sans-serif")
                        .withText(order.getNumber()))
        );
        dataVectorSource.addGeoObjectClickListener(this::onGeoObjectClick);
    }

    private void onGeoObjectClick(GeoObjectClickNotifier.GeoObjectClickEvent<Order> event) {
        Order orderMarker = event.getItem();
        DataGridItems<Order> items = ordersDataGrid.getItems();
        if (items != null) {
            Collection<Order> orders = items.getItems();
            Optional<Order> orderOptional = orders.stream()
                    .filter(o -> o.getId().equals(orderMarker.getId()))
                    .findFirst();
            orderOptional.ifPresent(items::setSelectedItem);
        }
    }


    @Subscribe(id = "removeAllButton", subject = "clickListener")
    public void onRemoveAllButtonClick(final ClickEvent<JmixButton> event) {
        orderRepository.deleteAll();
        ordersDl.load();
    }

    @EventListener
    public void onOrderChanged(RefreshViewEvent event) {
            ordersDl.load();
            refreshMap();
    }

    private void refreshMap() {
        initBuildingSource();
        DataVectorSourceItems<Order> items = dataVectorSource.getItems();
        Coordinate center = new Coordinate(37.617664, 55.752121);
        map.setCenter(center);
    }

}