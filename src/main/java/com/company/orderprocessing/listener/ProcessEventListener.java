package com.company.orderprocessing.listener;

import io.jmix.bpm.engine.events.ProcessCompletedEvent;
import io.jmix.bpm.engine.events.ProcessStartedEvent;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component("ord_ProcessEventListener")
public class ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListener.class);

    @EventListener
    public void onProcessStarted(final ProcessStartedEvent event) {
        ProcessDefinition processDefinition = event.getProcessDefinition();
        String name = processDefinition.getName() == null ?
                processDefinition.getId() : processDefinition.getName();
        log.info("Process started: {}", name);
    }

    @EventListener
    public void onProcessCompleted(final ProcessCompletedEvent event) {
        ProcessDefinition processDefinition = event.getProcessDefinition();
        String name = processDefinition.getName() == null ?
                processDefinition.getId() : processDefinition.getName();
        log.info("Process completed: {}", name);
    }

}