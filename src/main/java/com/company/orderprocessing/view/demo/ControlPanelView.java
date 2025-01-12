package com.company.orderprocessing.view.demo;


import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.DeliveryCompletedEvent;
import com.company.orderprocessing.event.IncomingOrderEvent;
import com.company.orderprocessing.rabbit.RabbitService;
import com.company.orderprocessing.repository.OrderRepository;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private OrderRepository orderRepository;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private Notifications notifications;
    @ViewComponent
    private JmixButton manufacturingStartBtn;
    @ViewComponent
    private JmixButton manufacturingPauseBtn;
    @ViewComponent
    private JmixButton manufacturingResumeBtn;
    @ViewComponent
    private JmixButton manufacturingStopBtn;
    @ViewComponent
    private Chart ordersChart;

    private Map<String, Integer> processes;
    private Map<OrderStatus, Long> orders = new HashMap<>();
    private Long totalOrders;
    @ViewComponent
    private H1 totalOrdersField;

    @Subscribe
    public void onInit(final InitEvent event) {
        updateCharts();
    }

    private void countOrders() {
        for (OrderStatus status : OrderStatus.values()) {
//            long count = orderRepository.countByStatus(status.getId());
            long count = dataManager.loadValue(
                            "select count(e) from ord_Order e where e.status = :status", Long.class)
                    .parameter("status", status.getId())
                    .one();
            orders.put(status, count);
        }
//        totalOrders = orderRepository.countTotal();
        totalOrders = dataManager.loadValue("select count(e) from ord_Order e", Long.class).one();
    }

    private void updateCharts() {
        totalOrdersField.setText(totalOrders != null ? totalOrders.toString() : "0");
        ordersChart.withDataSet(
                new DataSet().withSource(new DataSet.Source<MapDataItem>()
                        .withDataProvider(getOrderCountersMap())
                        .withCategoryField("description")
                        .withValueField("value"))
        );
    }

    private ListChartItems<MapDataItem> getOrderCountersMap() {
        countOrders();
        ListChartItems<MapDataItem> mapChartItems = new ListChartItems<>();
        orders.forEach((key, value) ->
                mapChartItems.addItem(new MapDataItem(Map.of("value", value, "description", key.name()))));
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
    }


    @Subscribe(id = "startReadingBtn", subject = "clickListener")
    public void onStartReadingBtnClick(final ClickEvent<JmixButton> event) {
        rabbitService.startListening();
        startReadingBtn.setEnabled(false);
        stopReadingBtn.setEnabled(true);
    }

    @Subscribe(id = "stopReadingBtn", subject = "clickListener")
    public void onStopReadingBtnClick(final ClickEvent<JmixButton> event) {
        rabbitService.stopListening();
        startReadingBtn.setEnabled(true);
        stopReadingBtn.setEnabled(false);
    }

    private final static String MAN_START_SIGNAL = "Manufacturing start";
    private final static String MAN_PAUSE_SIGNAL = "Manufacturing pause";
    private final static String MAN_RESUME_SIGNAL = "Manufacturing resume";
    private final static String MAN_STOP_SIGNAL = "Manufacturing full stop";

    @Subscribe(id = "manufacturingStartBtn", subject = "clickListener")
    public void onManufacturingStartBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.startProcessInstanceByKey(MANUFACTURING_PROCESS);
        notifications.create("Производственный процесс запущен")
                .withType(Notifications.Type.SUCCESS).show();
    }

    @Subscribe(id = "manufacturingPauseBtn", subject = "clickListener")
    public void onManufacturingPauseBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_PAUSE_SIGNAL);
        notifications.create("Производственный процесс приостановлен")
                .withType(Notifications.Type.WARNING).show();
    }

    @Subscribe(id = "manufacturingResumeBtn", subject = "clickListener")
    public void onManufacturingResumeBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_RESUME_SIGNAL);
        notifications.create("Производственный процесс возобновлен")
                .withType(Notifications.Type.SUCCESS).show();
    }

    @Subscribe(id = "manufacturingStopBtn", subject = "clickListener")
    public void onManufacturingStopBtnClick(final ClickEvent<JmixButton> event) {
        runtimeService.signalEventReceived(MAN_STOP_SIGNAL);
        notifications.create("Производственный процесс полностью остановлен")
                .withType(Notifications.Type.ERROR).show();
    }


    @EventListener
    private void onIncomingOrder(IncomingOrderEvent event) {
        String order = event.getOrder();
        notifications.create("Incoming order: " + order)
                .withDuration(3000)
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
    @EventListener
    private void onDeliveryCompleted(DeliveryCompletedEvent event) {
        notifications.create("Delivery completed: " + event.getDeliveryNumber())
                .withDuration(3000)
                .withPosition(Notification.Position.TOP_END)
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    private void countProcesses() {
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().active().list();
        for (ProcessInstance processInstance : processInstances) {
            String definitionKey = processInstance.getProcessDefinitionKey();
            switch (definitionKey) {
                case ORDER_PROCESSING ->
                        processes.put(ORDER_PROCESSING, processes.get(ORDER_PROCESSING) + 1);
                case ADDRESS_VERIFICATION ->
                        processes.put(ADDRESS_VERIFICATION, processes.get(ADDRESS_VERIFICATION) + 1);
                case PAYMENT_AND_RESERVATION ->
                        processes.put(PAYMENT_AND_RESERVATION, processes.get(PAYMENT_AND_RESERVATION) + 1);
                case DELIVERY_PROCESS ->
                        processes.put(PAYMENT_AND_RESERVATION, processes.get(PAYMENT_AND_RESERVATION) + 1);
                case MANUFACTURING_PROCESS ->
                        processes.put(MANUFACTURING_PROCESS, processes.get(MANUFACTURING_PROCESS) + 1);
                default -> log.warn("Unknown process: {}", definitionKey);
            }
        }
    }
}