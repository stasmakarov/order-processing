package com.company.orderprocessing.rabbit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RabbitServiceTest {

    @Autowired
    RabbitService rabbitService;

    @Test
    void isRabbitAvailable() {
        boolean rabbitAvailable = rabbitService.isRabbitAvailable();
        assertTrue(rabbitAvailable);
    }

    @Test
    void startListening() {
    }

    @Test
    void stopListening() {
    }

    @Test
    void isRabbitRunning() {
    }
}