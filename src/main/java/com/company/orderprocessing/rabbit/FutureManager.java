package com.company.orderprocessing.rabbit;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FutureManager {
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> futures = new ConcurrentHashMap<>();

    public void addFuture(String correlationId, CompletableFuture<Boolean> future) {
        futures.put(correlationId, future);
    }

    public CompletableFuture<Boolean> getFuture(String correlationId) {
        return futures.get(correlationId);
    }

    public void removeFuture(String correlationId) {
        futures.remove(correlationId);
    }
}

