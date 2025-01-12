package com.company.orderprocessing.view.processdefinitionlist;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.bpmflowui.view.processdefinition.ProcessDefinitionListView;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "bpm/processdefinitions", layout = DefaultMainViewParent.class)
@ViewController(id = "bpm_ProcessDefinition.list")
@ViewDescriptor(path = "ord-process-definition-list-view.xml")
public class OrdProcessDefinitionListView extends ProcessDefinitionListView {
    private static final Logger log = LoggerFactory.getLogger(OrdProcessDefinitionListView.class);

    @Autowired
    private RepositoryService repositoryService;

    @Subscribe(id = "deleteAllBtn", subject = "clickListener")
    public void onDeleteAllBtnClick(final ClickEvent<JmixButton> event) {
        DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        List<Deployment> deployments = deploymentQuery.list();

        for (Deployment deployment : deployments) {
            String deploymentId = deployment.getId();
            try {
                repositoryService.deleteDeployment(deploymentId, true);
                log.info("Deleted deployment: " + deploymentId);
            } catch (Exception e) {
                log.error("Failed to delete deployment with ID " + deploymentId + ": " + e.getMessage());
            }
        }
        applyFilter();
    }
}