package com.company.orderprocessing.job;

import org.flowable.engine.RuntimeService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ManufacturingJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(ManufacturingJob.class);
    private static final String DELIVERY_PROCESS_DEF_ID = "manufacturing-process";

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        runtimeService.startProcessInstanceById(DELIVERY_PROCESS_DEF_ID);
        log.info("Manufacturing process started");
    }
}
