package com.semd.backend.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class SimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SimulationScheduler.class);

    private final ScheduledThreadPoolExecutor executor;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> registry = new ConcurrentHashMap<>();

    public SimulationScheduler(
            @Value("${app.simulation.scheduler-pool-size:4}") int poolSize) {
        this.executor = new ScheduledThreadPoolExecutor(poolSize);
        this.executor.setRemoveOnCancelPolicy(true);
    }

    public void schedule(Long simulationId, Runnable tick, long intervalMs) {
        // Dùng compute để tránh đăng ký trùng
        registry.compute(simulationId, (id, existing) -> {
            if (existing != null && !existing.isDone()) {
                log.warn("Simulation {} đã có task đang chạy, bỏ qua", simulationId);
                return existing;
            }
            ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
                    tick, 0, intervalMs, TimeUnit.MILLISECONDS);
            log.info("Scheduled simulation {}", simulationId);
            return future;
        });
    }

    public void cancel(Long simulationId) {
        ScheduledFuture<?> future = registry.remove(simulationId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled simulation {}", simulationId);
        }
    }

    public boolean isScheduled(Long simulationId) {
        ScheduledFuture<?> f = registry.get(simulationId);
        return f != null && !f.isDone();
    }

    // Chạy 1 lần sau delay (dùng cho chờ ở hiện trường)
    public void scheduleOnce(Long simulationId, Runnable task, long delayMs) {
        ScheduledFuture<?> future = executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        registry.put(simulationId, future);
        log.info("Scheduled once simulation {} after {}ms", simulationId, delayMs);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SimulationScheduler, cancelling {} tasks", registry.size());
        registry.values().forEach(f -> f.cancel(false));
        registry.clear();
        executor.shutdown();
    }
}