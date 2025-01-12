package com.company.orderprocessing.view.processinstancelist;

import com.company.orderprocessing.event.RefreshViewEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
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
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.job.api.Job;
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
    private ManagementService managementService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private Dialogs dialogs;

    @Subscribe(id = "deleteAllBtn", subject = "clickListener")
    public void onDeleteAllBtnClick(final ClickEvent<JmixButton> event) {
        suspendActiveProcesses();
        deletedSuspendedProcesses();
        deleteHistoricProcesses();
        applyFilter();
        dialogs.createMessageDialog()
                .withHeader("Success")
                .withText("Process instances deleted")
                .open();
    }

    private void suspendActiveProcesses() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
        for (ProcessInstance pi : instances) {
            String id = pi.getId();
            try {
                runtimeService.suspendProcessInstanceById(id);
                log.info("Process instance suspended: {}", id);
            } catch (Exception e) {
                log.error("Failed to suspend process instance with ID {}: {}", id, e.getMessage());
            }
        }
    }

    private void deletedSuspendedProcesses() {
        List<ProcessInstance> suspendedInstances = runtimeService.createProcessInstanceQuery().suspended().list();
        for (ProcessInstance pi : suspendedInstances) {
            String id = pi.getId();
            try {
// Delete identity links associated with the process instance
                List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(id);
                for (IdentityLink identityLink : identityLinks) {
                    if (identityLink.getUserId() != null) {
                        runtimeService.deleteUserIdentityLink(id, identityLink.getUserId(), identityLink.getType());
                        log.info("Deleted user identity link for process instance {}: {}", id, identityLink.getUserId());
                    }
                    if (identityLink.getGroupId() != null) {
                        runtimeService.deleteGroupIdentityLink(id, identityLink.getGroupId(), identityLink.getType());
                        log.info("Deleted group identity link for process instance {}: {}", id, identityLink.getGroupId());
                    }
                }

                List<Job> jobs = managementService.createJobQuery().processInstanceId(id).list();
                for (Job job : jobs) {
                    managementService.deleteJob(job.getId());
                    log.info("Deleted job associated with process instance {}: {}", id, job.getId());
                }

                runtimeService.deleteProcessInstance(id, "deleting all action");
                log.info("Process instance deleted: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete process instance with ID {}: {}", id, e.getMessage());
            }
        }
    }

    private void deleteHistoricProcesses() {
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .list();
        int deletedCount = 0;
        for (HistoricProcessInstance hpi : historicProcessInstances) {
            String id = hpi.getId();
            try {
                historyService.deleteHistoricProcessInstance(id);
                log.trace("Historic process instance deleted: " + id);
                deletedCount++;
            } catch (Exception ignored) {
                log.error("Historic process instance {} not deleted: " , id);
            }
        }
        log.info("{} historic process instances deleted.", deletedCount);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        List<ProcessInstanceData> items = processInstancesDc.getItems();
        DataGridItems<ProcessInstanceData> gridItems = processInstancesGrid.getItems();
        applyFilter();
    }

    @EventListener
    public void onProcessStarted(final RefreshViewEvent event) {
        applyFilter();
    }

}