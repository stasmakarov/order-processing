package com.company.orderprocessing.quartz;

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

    private static final String DELIVERY_PROCESS_DEF_KEY = "delivery-process";
    private final Random random = new Random();

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private AppSettings appSettings;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int randomValue = random.nextInt(1, maxDelayTimer);
        Map<String, Object> params = new HashMap<>();
        params.put("deliveryTimer", Iso8601Converter.convertSecondsToDuration(randomValue));
        runtimeService.startProcessInstanceByKey(DELIVERY_PROCESS_DEF_KEY, params);
    }
}
