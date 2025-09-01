package com.company.orderprocessing.view.processinstancelist;

import com.company.orderprocessing.event.RefreshOrderViewEvent;
import com.vaadin.flow.router.Route;
import io.jmix.bpmflowui.view.processinstance.ProcessInstanceListView;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.context.event.EventListener;

@Route(value = "bpm/processinstances", layout = DefaultMainViewParent.class)
@ViewController(id = "bpm_ProcessInstance.list")
@ViewDescriptor(path = "ord-process-instance-list-view.xml")
public class OrdProcessInstanceListView extends ProcessInstanceListView {

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        applyFilter();
    }

    @EventListener
    public void onProcessStarted(final RefreshOrderViewEvent event) {
        applyFilter();
    }

}