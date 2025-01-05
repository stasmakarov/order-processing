package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Numerator;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("ord_NumeratorService")
public class NumeratorService {
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;

    public Integer getNext(String name) {
        Numerator numerator = unconstrainedDataManager.load(Numerator.class)
                .query("select e from ord_Numerator e where e.name = :name")
                .parameter("name", name)
                .one();
        Integer value = numerator.getNumber() + 1;
        numerator.setNumber(value);
        unconstrainedDataManager.save(numerator);
        return value;
    }
}