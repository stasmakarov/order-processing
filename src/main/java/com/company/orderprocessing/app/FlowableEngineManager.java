package com.company.orderprocessing.app;

import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FlowableEngineManager {

    private ProcessEngine processEngine;

    @Autowired
    private ManagementService managementService;

    public void startEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration
                    .createStandaloneProcessEngineConfiguration()
                    .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1") // Adjust DB settings
                    .setJdbcDriver("org.h2.Driver")
                    .setJdbcUsername("sa")
                    .setJdbcPassword("")
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                    .buildProcessEngine();

            System.out.println("Flowable engine started manually.");
        }
    }

    public void stopEngine() {
        if (processEngine != null) {
            processEngine.close();
            processEngine = null;
            System.out.println("Flowable engine stopped.");
        }
    }

    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    public void logProcessEngineStatus() {
        Map<String, String> properties = managementService.getProperties();
        AsyncExecutor asyncExecutor = processEngine.getProcessEngineConfiguration().getAsyncExecutor();
        System.out.println("Check async");
    }
}
