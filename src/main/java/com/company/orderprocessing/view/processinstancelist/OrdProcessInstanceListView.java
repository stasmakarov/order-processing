package com.company.orderprocessing.view.processinstancelist;

import com.company.orderprocessing.app.ResetService;
import com.company.orderprocessing.event.RefreshViewEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.bpm.entity.ProcessInstanceData;
import io.jmix.bpmflowui.view.processinstance.ProcessInstanceListView;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.job.api.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.List;

@Route(value = "bpm/processinstances", layout = DefaultMainViewParent.class)
@ViewController(id = "bpm_ProcessInstance.list")
@ViewDescriptor(path = "ord-process-instance-list-view.xml")
public class OrdProcessInstanceListView extends ProcessInstanceListView {

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        applyFilter();
    }

    @EventListener
    public void onProcessStarted(final RefreshViewEvent event) {
        applyFilter();
    }

}