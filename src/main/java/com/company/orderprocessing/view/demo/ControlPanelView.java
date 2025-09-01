package com.company.orderprocessing.view.demo;


import com.company.orderprocessing.app.FlowableEngineManager;
import com.company.orderprocessing.app.ManufacturingService;
import com.company.orderprocessing.app.ResetService;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ManufacturingProcessStatus;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.*;
import com.company.orderprocessing.rabbit.RabbitService;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.*;

import static java.util.Comparator.comparingInt;

@Route(value = "control-panel-view", layout = MainView.class)
@ViewController(id = "ord_ControlPanelView")
@ViewDescriptor(path = "control-panel-view.xml")
public class ControlPanelView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(ControlPanelView.class);
    private final LinkedHashMap<String, Long> orders = new LinkedHashMap<>();
    private Long totalOrders;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private ManufacturingService manufacturingService;
    @Autowired
    private ResetService resetService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Dialogs dialogs;

    @ViewComponent
    private JmixButton startReadingBtn;
    @ViewComponent
    private JmixButton stopReadingBtn;
    @ViewComponent
    private H1 totalOrdersField;
    @ViewComponent
    private Chart ordersChart;
    @ViewComponent
    private Chart itemsChart;
    @ViewComponent
    private CollectionLoader<Item> itemsDl;
    @ViewComponent
    private HorizontalLayout manufacturingStatusBox;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private FlowableEngineManager flowableEngineManager;

    @Subscribe
    public void onInit(final InitEvent event) {
        updateOrdersChart();
    }

    private void updateOrdersChart() {
        countOrders();
        totalOrdersField.setText(totalOrders != null ? totalOrders.toString() : "0");
        ordersChart.withDataSet(
                new DataSet().withSource(new DataSet.Source<MapDataItem>()
                        .withDataProvider(getOrderCountersMap())
                        .withCategoryField("description")
                        .withValueField("value"))
        );
    }

    private void updateItemsChart() {
        itemsDl.load();
    }

    private void countOrders() {
        List<OrderStatus> sortedStatuses = Arrays.stream(OrderStatus.values())
                .sorted(comparingInt(OrderStatus::getId)).toList();

        for (OrderStatus status : sortedStatuses) {
            long count = dataManager.loadValue(
                            "select count(e) from ord_Order e where e.status = :status", Long.class)
                    .parameter("status", status.getId())
                    .one();
            orders.put(status.name(), count);
        }
        totalOrders = dataManager.loadValue("select count(e) from ord_Order e", Long.class).one();
    }

    private ListChartItems<MapDataItem> getOrderCountersMap() {
        ListChartItems<MapDataItem> mapChartItems = new ListChartItems<>();
        orders.forEach((key, value) ->
                mapChartItems.addItem(new MapDataItem(Map.of("value", value, "description", key))));
        return mapChartItems;
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        if (rabbitService.isRabbitRunning()) {
            startReadingBtn.setEnabled(false);
            stopReadingBtn.setEnabled(true);
        } else {
            startReadingBtn.setEnabled(true);
            stopReadingBtn.setEnabled(false);
        }
        updateItemsChart();
        initiateManufacturingBox();
        manufacturingStatusUpdate();
    }

    @Subscribe(id = "startReadingBtn", subject = "clickListener")
    public void onStartReadingBtnClick(final ClickEvent<JmixButton> event) {
        if (rabbitService.isRabbitAvailable()) {
            boolean started = rabbitService.startListening();
            if (started) {
                startReadingBtn.setEnabled(false);
                stopReadingBtn.setEnabled(true);
            } else {
                notifications.create("Rabbit listener didn't start")
                        .withType(Notifications.Type.ERROR)
                        .show();
            }
        } else {
            notifications.create("Rabbit is unavailable")
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    @Subscribe(id = "stopReadingBtn", subject = "clickListener")
    public void onStopReadingBtnClick(final ClickEvent<JmixButton> event) {
        rabbitService.stopListening();
        startReadingBtn.setEnabled(true);
        stopReadingBtn.setEnabled(false);
    }

    @EventListener
    private void onIncomingOrder(IncomingOrderEvent event) {
        updateOrdersChart();
        String order = event.getOrder();
        notifications.create("Incoming order: " + order)
                .withDuration(3000)
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.DEFAULT)
                .show();
    }
    @EventListener
    private void onDeliveryCompleted(DeliveryCompletedEvent event) {
        updateOrdersChart();
        updateItemsChart();
        notifications.create("Delivery completed: " + event.getDeliveryNumber())
                .withDuration(3000)
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    @EventListener
    private void onItemsProducedEvent(ItemsProducedEvent event) {
        updateOrdersChart();
        manufacturingStatusUpdate();
        Item item = event.getItem();
        notifications.create("Produced items: " + item.getName()
                        + ", now available: " + item.getAvailable())
                .withPosition(Notification.Position.TOP_END)
                .withDuration(3000)
                .show();
    }

    @EventListener
    private void onItemsSuspendedEvent(ItemsSuspendedEvent event) {
        updateOrdersChart();
        manufacturingStatusUpdate();
        Item item = event.getItem();
        notifications.create("Item production suspended: " + item.getName()
                        + ", now available: " + item.getAvailable())
                .withPosition(Notification.Position.TOP_END)
                .withDuration(3000)
                .show();
    }

    @EventListener
    private void onRefreshEvent(RefreshOrderViewEvent event) {
        updateOrdersChart();
        updateItemsChart();
    }

    @EventListener
    private void onRefreshItemsEvent(RefreshItemsEvent event) {
        updateItemsChart();
    }

    @Subscribe(id = "resetAppBtn", subject = "clickListener")
    public void onResetAppBtnClick(final ClickEvent<JmixButton> event) {
        dialogs.createOptionDialog()
                .withHeader("Please confirm")
                .withText("All process instances, orders and deliveries will be deleted. Are you sure?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> resetAddData()),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }

    private void resetAddData() {
        resetService.deleteAllProcessInstances();
        resetService.deleteAllDeliveries();
        resetService.deleteAllOrders();
        resetService.initItems();
        resetService.initNumerators();
        resetService.purgeQueues();
        updateItemsChart();
        updateOrdersChart();
        manufacturingStatusUpdate();
        log.info("🟥Application reset");
    }

    private void initiateManufacturingBox() {
        for (Item item : dataManager.load(Item.class).all().list()) {
            Button button = uiComponents.create(Button.class);
            button.setText(item.getName());
            button.setId(item.getId().toString());
            manufacturingStatusBox.add(button);
        }
    }

    private void manufacturingStatusUpdate() {
        Map<String, ManufacturingProcessStatus> statusMap = manufacturingService.getManufacturingProcessesStatus();
        int count = manufacturingStatusBox.getComponentCount();

        for (int i = 0; i < count; i++) {
            Component component = manufacturingStatusBox.getComponentAt(i);
            if (component instanceof Button button) {
                ManufacturingProcessStatus status = statusMap.get(button.getText());
                updateButton(button, status);
                }
            }
        }

    private void updateButton(Button button, ManufacturingProcessStatus status) {
        button.getStyle().clear();
        switch (status) {
            case PRODUCTION -> { button.addThemeVariants(ButtonVariant.LUMO_SUCCESS);}
            case SUSPENDED -> { button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);}
            case NOT_STARTED -> { button.addThemeVariants(ButtonVariant.LUMO_TERTIARY); }
        }
    }

    @Subscribe(id = "startEngineBtn", subject = "clickListener")
    public void onStartEngineBtnClick(final ClickEvent<JmixButton> event) {
        flowableEngineManager.logProcessEngineStatus();
        flowableEngineManager.startEngine();
    }

    @Subscribe(id = "stopEngineBtn", subject = "clickListener")
    public void onStopEngineBtnClick(final ClickEvent<JmixButton> event) {
        flowableEngineManager.stopEngine();
    }

    @Subscribe(id = "stopAllButton", subject = "clickListener")
    public void onStopAllButtonClick(final ClickEvent<JmixButton> event) {
        manufacturingService.terminateAllItemsProduction();
        notifications.create("All manufacturing process instances terminated")
                .withType(Notifications.Type.SUCCESS)
                .show();
        manufacturingStatusUpdate();
    }

    @Subscribe(id = "startAllButton", subject = "clickListener")
    public void onStartAllButtonClick(final ClickEvent<JmixButton> event) {
        List<Item> items = dataManager.load(Item.class).all().list();
        manufacturingService.startAll(items);
        notifications.create("All manufacturing process instances started")
                .withType(Notifications.Type.SUCCESS)
                .show();
        manufacturingStatusUpdate();
    }

}