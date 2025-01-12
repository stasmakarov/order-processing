package com.company.orderprocessing.listener;

import com.company.orderprocessing.event.RefreshViewEvent;
import io.jmix.bpm.engine.events.ProcessCompletedEvent;
import io.jmix.bpm.engine.events.ProcessStartedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("ord_ProcessEventListener")
public class ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListener.class);
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private UiEventPublisher uiEventPublisher;

    @EventListener
    public void onProcessStarted(final ProcessStartedEvent event) {
        ProcessDefinition processDefinition = event.getProcessDefinition();
        String name = processDefinition.getName() == null ? processDefinition.getId() : processDefinition.getName();
        log.info("Process started: {}", name);

        systemAuthenticator.begin("admin");
        try {
            uiEventPublisher.publishEvent(new RefreshViewEvent(this));
        } finally {
            systemAuthenticator.end();
        }
    }

    @EventListener
    public void onProcessCompleted(final ProcessCompletedEvent event) {
        ProcessDefinition processDefinition = event.getProcessDefinition();
        String name = processDefinition.getName() == null ? processDefinition.getId() : processDefinition.getName();
        log.info("Process completed: {}", name);
    }

}