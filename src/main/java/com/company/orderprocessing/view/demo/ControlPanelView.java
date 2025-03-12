package com.company.orderprocessing.view.demo;


import com.company.orderprocessing.app.ResetService;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.*;
import com.company.orderprocessing.rabbit.RabbitService;
import com.company.orderprocessing.util.Iso8601Converter;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;
import io.jmix.appsettings.AppSettings;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
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

    private static final String ORDER_PROCESSING = "order-processing";
    private static final String ADDRESS_VERIFICATION = "address-verification";
    private static final String PAYMENT_AND_RESERVATION = "payment-and-reservation";
    private static final String DELIVERY_PROCESS = "delivery-process";
    private static final String MANUFACTURING_PROCESS = "manufacturing-process";

    @ViewComponent
    private JmixButton startReadingBtn;
    @ViewComponent
    private JmixButton stopReadingBtn;

    @Autowired
    private AppSettings appSettings;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private ResetService resetService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiEventPublisher uiEventPublisher;

    @ViewComponent
    private JmixButton manufacturingStartBtn;
    @ViewComponent
    private JmixButton manufacturingPauseBtn;
    @ViewComponent
    private JmixButton manufacturingResumeBtn;
    @ViewComponent
    private JmixButton manufacturingStopBtn;

    private Map<String, Integer> processes;
    private final LinkedHashMap<String, Long> orders = new LinkedHashMap<>();
//    private final LinkedHashMap<String, Long> items = new LinkedHashMap<>();
    private Long totalOrders;
    @ViewComponent
    private H1 totalOrdersField;
    @ViewComponent
    private Chart ordersChart;
    @ViewComponent
    private Chart itemsChart;
    @ViewComponent
    private CollectionLoader<Item> itemsDl;
    @ViewComponent
    private CollectionContainer<Item> itemsDc;

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

private void countItems() {
        List<OrderStatus> sortedStatuses = Arrays.stream(OrderStatus.values())
                .sorted(comparingInt(OrderStatus::getId)).toList();

        for (OrderStatus status : sortedStatuses) {
            long count = dataManager.loadValue(
                            "select count(e) from ord_Item e where e.status = :status", Long.class)
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
        manufacturingButtonsUpdate();
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

    private final static String MAN_START_MESSAGE = "Manufacturing start";
    private final static String MAN_PAUSE_SIGNAL = "Manufacturing pause";
    private final static String MAN_RESUME_SIGNAL = "Manufacturing resume";
    private final static String MAN_STOP_SIGNAL = "Manufacturing full stop";

    private static String manufacturingProcessId;

    @Subscribe(id = "manufacturingStartBtn", subject = "clickListener")
    public void onManufacturingStartBtnClick(final ClickEvent<JmixButton> event) {
        Map<String, Object> params = new HashMap<>();
        Integer manufacturingCycle = appSettings.load(OrderProcessingSettings.class).getManufacturingCycle();
        String timerValue = Iso8601Converter.convertSecondsToDuration(manufacturingCycle);
        params.put("cycleTimer", timerValue);
        ProcessInstance instanceManufacturing = runtimeService.startProcessInstanceByMessage(MAN_START_MESSAGE, params);
        manufacturingProcessId = instanceManufacturing.getId();
        manufacturingButtonsUpdate();
        notifications.create("Manufacturing process started")
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.SUCCESS)
                .withDuration(2000)
                .show();
    }

    @Subscribe(id = "manufacturingPauseBtn", subject = "clickListener")
    public void onManufacturingPauseBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_PAUSE_SIGNAL);
        manufacturingButtonsUpdate();
        notifications.create("Manufacturing process suspended")
                .withThemeVariant(NotificationVariant.LUMO_WARNING)
                .withPosition(Notification.Position.TOP_END)
                .withDuration(2000)
                .show();
    }

    @Subscribe(id = "manufacturingResumeBtn", subject = "clickListener")
    public void onManufacturingResumeBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_RESUME_SIGNAL);
        manufacturingButtonsUpdate();
        notifications.create("Manufacturing process resumed")
                .withType(Notifications.Type.SUCCESS)
                .withPosition(Notification.Position.TOP_END)
                .withDuration(2000)
                .show();
    }

    @Subscribe(id = "manufacturingStopBtn", subject = "clickListener")
    public void onManufacturingStopBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_STOP_SIGNAL); //consider the process definitely stopped by signal
        manufacturingProcessId = null;
        manufacturingButtonsUpdate();
        notifications.create("Manufacturing process stopped")
                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                .withPosition(Notification.Position.TOP_END)
                .withDuration(2000)
                .show();
    }

    private void manufacturingButtonsUpdate() {
        if (manufacturingProcessId == null) {
            manufacturingStartBtn.setEnabled(true);
            manufacturingPauseBtn.setEnabled(false);
            manufacturingResumeBtn.setEnabled(false);
            manufacturingStopBtn.setEnabled(false);
        } else {
            manufacturingStartBtn.setEnabled(false);
            manufacturingStopBtn.setEnabled(true);
            List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                    .processInstanceId(manufacturingProcessId)
                    .list();
            for (EventSubscription subscription : subscriptions) {
                String eventName = subscription.getEventName();
                if (MAN_PAUSE_SIGNAL.equals(eventName)) {
                    manufacturingPauseBtn.setEnabled(true);
                    manufacturingResumeBtn.setEnabled(false);
                    break;
                }
                if (MAN_RESUME_SIGNAL.equals(eventName)) {
                    manufacturingPauseBtn.setEnabled(false);
                    manufacturingResumeBtn.setEnabled(true);
                    break;
                }

            }
        }
    }


    @EventListener
    private void onIncomingOrder(IncomingOrderEvent event) {
        updateOrdersChart();
        String order = event.getOrder();
        notifications.create("Incoming order: " + order)
                .withDuration(3000)
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.SUCCESS)
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
    private void onNewItemsProduced(NewItemsSuppliedEvent event) {
        updateItemsChart();
        notifications.create("New party of items produced")
                .withPosition(Notification.Position.TOP_END)
                .withDuration(3000)
                .show();
    }

    @EventListener
    private void onItemsProducedEvent(ItemsProducedEvent event) {
        updateOrdersChart();
        Item item = event.getItem();
        notifications.create("Produced items: " + item.getName()
                        + ", now available: " + item.getAvailable())
                .withPosition(Notification.Position.TOP_END)
                .withDuration(3000)
                .show();
    }

    @EventListener
    private void onItemsSuspendedEvent(ItemsProducedEvent event) {
        updateOrdersChart();
        Item item = event.getItem();
        notifications.create("Item production suspended: " + item.getName()
                        + ", now available: " + item.getAvailable())
                .withPosition(Notification.Position.TOP_END)
                .withDuration(3000)
                .show();
    }

    @EventListener
    private void onRefreshEvent(RefreshViewEvent event) {
        updateOrdersChart();
        updateItemsChart();
    }

    @EventListener
    private void onReservationErrorEvent(ReservationError event) {
        notifications.create("Reservation error")
                .withType(Notifications.Type.ERROR)
                .show();
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
        updateItemsChart();
        updateOrdersChart();
    }
}