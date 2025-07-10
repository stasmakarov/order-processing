package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Numerator;
import io.jmix.core.DataManager;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("ord_NumeratorService")
public class NumeratorService {
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Transactional
    public Integer getNext(String name) {
        systemAuthenticator.begin("admin");
        try {
            Numerator numerator = dataManager.load(Numerator.class)
                    .query("select e from ord_Numerator e where e.name = :name")
                    .parameter("name", name)
                    .one();
            Integer value = numerator.getNumber() + 1;
            numerator.setNumber(value);
            dataManager.save(numerator);
            return value;
        } finally {
            systemAuthenticator.end();
        }
    }
}