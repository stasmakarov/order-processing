package com.company.orderprocessing.quartz;

import com.company.orderprocessing.app.DeliveryService;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
import org.flowable.engine.RuntimeService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DeliveryJob implements Job {

    @Autowired
    private DeliveryService deliveryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        deliveryService.startDeliveryProcess();
    }
}
