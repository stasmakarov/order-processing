package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class ReservationError extends ApplicationEvent {
    public ReservationError(Object source) {
        super(source);
    }
}
