package com.company.orderprocessing.view.order;

import com.company.orderprocessing.configuration.WebinarConfig;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.RefreshOrderViewEvent;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.model.KeyValueContainer;
import io.jmix.flowui.view.*;
import io.jmix.mapsflowui.component.GeoMap;
import io.jmix.mapsflowui.component.data.DataVectorSourceItems;
import io.jmix.mapsflowui.component.model.GeoMapView;
import io.jmix.mapsflowui.component.model.source.DataVectorSource;
import io.jmix.mapsflowui.component.model.source.GeoObjectClickNotifier;
import io.jmix.mapsflowui.kit.component.model.Extent;
import io.jmix.mapsflowui.kit.component.model.Padding;
import io.jmix.mapsflowui.kit.component.model.style.Fill;
import io.jmix.mapsflowui.kit.component.model.style.Style;
import io.jmix.mapsflowui.kit.component.model.style.image.Anchor;
import io.jmix.mapsflowui.kit.component.model.style.image.CircleStyle;
import io.jmix.mapsflowui.kit.component.model.style.image.IconOrigin;
import io.jmix.mapsflowui.kit.component.model.style.image.IconStyle;
import io.jmix.mapsflowui.kit.component.model.style.stroke.Stroke;
import io.jmix.mapsflowui.kit.component.model.style.text.TextStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private WebinarConfig webinarConfig;

    @ViewComponent
    private GeoMap map;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;
    @ViewComponent("map.ordersLayer.dataVectorSource1")
    private DataVectorSource<Order> dataVectorSource;
    @ViewComponent("map.boundsLayer.boundsSource")
    private DataVectorSource<KeyValueEntity> boundsSource;

    @Subscribe
    public void onInit(final InitEvent event) {
        applyMapParamsFromConfig();
        dataVectorSource.addGeoObjectClickListener(this::onGeoObjectClick);
        dataVectorSource.setStyleProvider(this::createStyleForOrder);
        if (boundsSource != null) {
            boundsSource.setStyleProvider(kv -> createBoundsStyle());
        }
        saveContext = new SaveContext();
    }

    // comments in English
    private io.jmix.mapsflowui.kit.component.model.style.Style createBoundsStyle() {
        var style = new io.jmix.mapsflowui.kit.component.model.style.Style();

        // более прозрачный прямоугольник: alpha = 0.04 (4%)
        style.setFill(new io.jmix.mapsflowui.kit.component.model.style.Fill("rgba(33, 125, 255, 0.02)"));

        var stroke = new Stroke();
        stroke.setColor("rgba(33, 125, 255, 0.6)"); // полупрозрачная обводка
        stroke.setWidth(1.25);
        style.setStroke(stroke);

        return style;
    }


    // Keep center at region center; make a fixed visible area expanded by 'outside' buffer
    private void applyMapParamsFromConfig() {
        // Region bounds (degrees)
        double minLat = webinarConfig.getMinLat();
        double maxLat = webinarConfig.getMaxLat();
        double minLon = webinarConfig.getMinLon();
        double maxLon = webinarConfig.getMaxLon();

        // Region center
        double cx = (minLon + maxLon) / 2d; // lon
        double cy = (minLat + maxLat) / 2d; // lat

        // Fixed outside buffer around region (meters)
        // TODO: при желании вынеси в конфиг, напр. webinar.outsideBufferMeters
        final double OUTSIDE_BUFFER_METERS = 20_000d; // 20 км
        final double METERS_PER_DEG_LAT = 111_320d;   // среднее метров в 1° широты
        double metersPerDegLon = Math.cos(Math.toRadians(cy)) * METERS_PER_DEG_LAT;
        if (metersPerDegLon <= 0) metersPerDegLon = 1; // guard near poles

        // Переводим метры → градусы на широте региона
        double dLat = OUTSIDE_BUFFER_METERS / METERS_PER_DEG_LAT;
        double dLon = OUTSIDE_BUFFER_METERS / metersPerDegLon;

        // Расширяем регион на буфер по всем сторонам (симметрично относительно центра региона)
        double eMinX = minLon - dLon;
        double eMaxX = maxLon + dLon;
        double eMinY = minLat - dLat;
        double eMaxY = maxLat + dLat;

        // Небольшой визуальный паддинг (5%), чтобы рамка не прилипала к краю
        double padX = (eMaxX - eMinX) * 0.05;
        double padY = (eMaxY - eMinY) * 0.05;
        eMinX -= padX; eMaxX += padX; eMinY -= padY; eMaxY += padY;

        var view = new GeoMapView();
        view.setCenter(new Coordinate(cx, cy)); // центр = центр региона
        view.setExtent(new Extent(eMinX, eMinY, eMaxX, eMaxY)); // фиксированная зона
        map.setMapView(view);

        // Полигон региона (как и раньше)
        drawBoundsPolygon(minLon, minLat, maxLon, maxLat);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
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
            case OUT_OF_ZONE -> "GRAY";
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
                .withSrc("/icons/marker-6.png")
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

    @EventListener
    public void onOrderChanged(RefreshOrderViewEvent event) {
        ordersDl.load();
        // include outside orders in visible area after reload
        applyMapParamsFromConfig();
    }

    /**
     * Put rectangle polygon matching configured bounds into a KeyValue container (if present).
     * Requires a vector layer bound to a KeyValueCollectionContainer with id 'boundsDc' and property 'geometry'.
     * If such container is absent, this method is a no-op.
     */
    private void drawBoundsPolygon(double minLon, double minLat, double maxLon, double maxLat) {
        try {
            var viewData = getViewData();
            var container = viewData.getContainer("boundsDc");
            if (container == null) return; // no container configured

            // Build polygon (x=lon, y=lat, SRID=4326)
            var gf = new GeometryFactory(new PrecisionModel(), 4326);
            var ring = new Coordinate[]{
                    new Coordinate(minLon, minLat),
                    new Coordinate(maxLon, minLat),
                    new Coordinate(maxLon, maxLat),
                    new Coordinate(minLon, maxLat),
                    new Coordinate(minLon, minLat)
            };
            var shell = gf.createLinearRing(ring);
            var polygon = gf.createPolygon(shell, null);

            var kv = new KeyValueEntity();
            kv.setValue("geometry", polygon);

            if (container instanceof KeyValueCollectionContainer coll) {
                List<KeyValueEntity> items = new ArrayList<>();
                items.add(kv);
                coll.setItems(items);
            } else if (container instanceof KeyValueContainer one) {
                one.setItem(kv);
            }
        } catch (Exception ex) {
            log.debug("Bounds polygon skipped: {}", ex.getMessage());
        }
    }

    @Subscribe(id = "cancelButton", subject = "clickListener")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        
    }
}
