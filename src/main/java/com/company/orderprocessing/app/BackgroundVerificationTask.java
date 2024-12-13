package com.company.orderprocessing.app;

import com.company.orderprocessing.nominatim.GeoCodingService;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.eventsubscription.api.EventSubscription;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BackgroundVerificationTask extends BackgroundTask<Integer, Void> {
    protected final String address;
    protected final String processInstanceId;
    protected boolean addressVerificationResult;

    private final GeoCodingService geoCodingService;
    private final RuntimeService runtimeService;

    protected BackgroundVerificationTask(String address,
                                         String processInstanceId,
                                         GeoCodingService geoCodingService,
                                         RuntimeService runtimeService) {
        super(30, TimeUnit.SECONDS);
        this.address = address;
        this.processInstanceId = processInstanceId;
        this.geoCodingService = geoCodingService;
        this.runtimeService = runtimeService;
    }

    @Override
    public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
        Point point = geoCodingService.verifyAddress(address);
        addressVerificationResult = (point == null);
        randomDelay();
        return null;
    }

    @Override
    public void done(Void result) {
        if (addressVerificationResult) {
            String executionId = getExecutionId("OK");
            runtimeService.messageEventReceivedAsync("Address OK", executionId);
        }
        else {
            String executionId = getExecutionId("Fake");
            runtimeService.messageEventReceivedAsync("Fake address", executionId);
        }
    }

    private String getExecutionId(String messageName) {
        String executionId = null;
        List<EventSubscription> messageSubscriptions = runtimeService.createEventSubscriptionQuery()
                .list();
        for (EventSubscription subscription : messageSubscriptions) {
            if (subscription.getProcessInstanceId().equals(processInstanceId)
                    && subscription.getEventName().equals(messageName)) {
                executionId = subscription.getExecutionId();
                System.out.println("Found: "+ executionId);
                break;
            }
        }
        return executionId;
    }

    private void randomDelay() {
        Random random = new Random();
        int MAX_DELAY = 20;
        int delay = random.nextInt(MAX_DELAY);
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
