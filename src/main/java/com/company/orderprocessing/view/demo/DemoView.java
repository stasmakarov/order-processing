package com.company.orderprocessing.view.demo;


import com.company.orderprocessing.rabbit.ListenerControlService;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "demo-view", layout = MainView.class)
@ViewController(id = "ord_DemoView")
@ViewDescriptor(path = "demo-view.xml")
public class DemoView extends StandardView {
    @ViewComponent
    private TypedTextField<String> instanceIdField;
    @ViewComponent
    private TypedTextField<String> eventNameField;
    @ViewComponent
    private TypedTextField<String> targetExecutionField;
    @ViewComponent
    private JmixButton startReadingBtn;
    @ViewComponent
    private JmixButton stopReadingBtn;

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ListenerControlService listenerControlService;
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

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        if (listenerControlService.isListenerRunning()) {
            startReadingBtn.setEnabled(false);
            stopReadingBtn.setEnabled(true);
        } else {
            startReadingBtn.setEnabled(true);
            stopReadingBtn.setEnabled(false);
        }
    }


    @Subscribe(id = "findExecution", subject = "clickListener")
    public void onFindExecutionClick(final ClickEvent<JmixButton> event) {
        String instanceId = instanceIdField.getTypedValue();
        String eventName = eventNameField.getTypedValue();
        List<EventSubscription> messageSubscriptions = runtimeService.createEventSubscriptionQuery()
                .list();
        messageSubscriptions.stream().map(EventSubscription::getExecutionId).forEach(System.out::println);

//        messageSubscriptions.stream()
//                .filter(m -> m.getProcessInstanceId().equals(instanceId) && m.getEventName().equals(eventName))
//                .map(EventSubscription::getExecutionId)
//                .findFirst()
//                .ifPresent(executionId -> {
//                    targetExecutionField.setValue(executionId);
//                    System.out.println("Found: "+executionId);
//                });

        for (EventSubscription subscription : messageSubscriptions) {
            if (subscription.getProcessInstanceId().equals(instanceId)
                    && subscription.getEventName().equals(eventName)) {
                String executionId = subscription.getExecutionId();
                targetExecutionField.setValue(executionId);
                System.out.println("Found: "+executionId);
                break;
            }
        }
    }

    @Subscribe(id = "sendMessageBtn", subject = "clickListener")
    public void onSendMessageBtnClick(final ClickEvent<JmixButton> event) {
        String eventName = "Order delivered";
        String orderNumber = instanceIdField.getValue();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(orderNumber)
                .singleResult();
        if (processInstance == null) return;
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(eventName)
                .parentId(processInstance.getId())
                .singleResult();
        if (execution == null) return;
        runtimeService.messageEventReceivedAsync(eventName, execution.getId());
    }

    @Subscribe(id = "allSubsBtn", subject = "clickListener")
    public void onAllSubsBtnClick(final ClickEvent<JmixButton> event) {
        List<EventSubscription> messageSubscriptions = runtimeService.createEventSubscriptionQuery()
                .list();
        messageSubscriptions.stream().map(EventSubscription::getExecutionId).forEach(System.out::println);
    }

    @Subscribe(id = "startReadingBtn", subject = "clickListener")
    public void onStartReadingBtnClick(final ClickEvent<JmixButton> event) {
        listenerControlService.startListening();
        startReadingBtn.setEnabled(false);
        stopReadingBtn.setEnabled(true);
    }

    @Subscribe(id = "stopReadingBtn", subject = "clickListener")
    public void onStopReadingBtnClick(final ClickEvent<JmixButton> event) {
        listenerControlService.startListening();
        startReadingBtn.setEnabled(true);
        stopReadingBtn.setEnabled(false);
    }

    private final static String MANUFACTURING_PROCESS = "manufacturing-process";
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

    @Subscribe(id = "startById", subject = "clickListener")
    public void onStartByIdClick(final ClickEvent<JmixButton> event) {
        String value = instanceIdField.getTypedValue();
        ProcessInstance instance = runtimeService.startProcessInstanceById(value);
        notifications.create("Process started: " + instance.getName()).show();
    }
}