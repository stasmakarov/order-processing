package com.company.orderprocessing.app;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.BpmnError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "ord_ErrorService")
public class ErrorService {

    public void throwBpmnError(String code, String message) {
        throw new BpmnError(code, message);
    }
}