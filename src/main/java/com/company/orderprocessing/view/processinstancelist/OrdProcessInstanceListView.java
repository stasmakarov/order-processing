package com.company.orderprocessing.view.processinstancelist;

import com.company.orderprocessing.event.RefreshViewEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import io.jmix.bpmflowui.view.processinstance.ProcessInstanceListView;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Route(value = "bpm/processinstances", layout = DefaultMainViewParent.class)
@ViewController(id = "bpm_ProcessInstance.list")
@ViewDescriptor(path = "ord-process-instance-list-view.xml")
public class OrdProcessInstanceListView extends ProcessInstanceListView {

    private static final Logger log = LoggerFactory.getLogger(OrdProcessInstanceListView.class);

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private Dialogs dialogs;

    @Subscribe(id = "deleteAllBtn", subject = "clickListener")
    public void onDeleteAllBtnClick(final ClickEvent<JmixButton> event) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
        for (ProcessInstance pi : instances) {
            String id = null;
            try {
                id = pi.getId();
                runtimeService.deleteProcessInstance(id, "deleting all action");
                log.info("Process instance deleted: " + id);
            } catch (Exception ignored) {
                log.error("Process instance with ID {} is not deleted", id);
            }
        }

        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .finished().list();
        for (HistoricProcessInstance hpi : historicProcessInstances) {
            String id = hpi.getId();
            try {
                historyService.deleteHistoricProcessInstance(id);
                log.info("Historic process instance deleted: " + id);
            } catch (Exception ignored) {
                log.error("Historic process instance {} not deleted: " , id);
            }
        }
        applyFilter();
        dialogs.createMessageDialog()
                .withHeader("Success")
                .withText("All process instances deleted")
                .open();
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        applyFilter();
    }

    @EventListener
    public void onProcessStarted(final RefreshViewEvent event) {
        applyFilter();
    }

}