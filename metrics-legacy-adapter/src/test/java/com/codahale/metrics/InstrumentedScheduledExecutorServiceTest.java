package com.codahale.metrics;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
public class InstrumentedScheduledExecutorServiceTest {
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @After
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void testCreate() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        InstrumentedScheduledExecutorService instrumentedExecutorService = new InstrumentedScheduledExecutorService(
                executorService, registry, "test-scheduled-instrumented");
        CountDownLatch countDownLatch = new CountDownLatch(10);
        instrumentedExecutorService.scheduleAtFixedRate(countDownLatch::countDown, 0, 1, TimeUnit.MILLISECONDS);
        countDownLatch.await(5, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
        instrumentedExecutorService.shutdown();
        instrumentedExecutorService.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(registry.getMetrics()).containsOnlyKeys("test-scheduled-instrumented.completed",
                "test-scheduled-instrumented.submitted", "test-scheduled-instrumented.duration", "test-scheduled-instrumented.running",
                "test-scheduled-instrumented.scheduled.once", "test-scheduled-instrumented.scheduled.overrun",
                "test-scheduled-instrumented.scheduled.percent-of-period", "test-scheduled-instrumented.scheduled.repetitively");
        assertThat(registry.meter("test-scheduled-instrumented.completed").getCount()).isEqualTo(10);
        assertThat(registry.meter("test-scheduled-instrumented.submitted").getCount()).isEqualTo(0);
        assertThat(registry.counter("test-scheduled-instrumented.running").getCount()).isEqualTo(0);
        assertThat(registry.timer("test-scheduled-instrumented.duration").getCount()).isEqualTo(10);

        assertThat(registry.meter("test-scheduled-instrumented.scheduled.once").getCount()).isEqualTo(0);
        assertThat(registry.meter("test-scheduled-instrumented.scheduled.repetitively").getCount()).isEqualTo(1);
        assertThat(registry.counter("test-scheduled-instrumented.scheduled.overrun").getCount()).isEqualTo(0);
        assertThat(registry.histogram("test-scheduled-instrumented.scheduled.percent-of-period").getCount()).isEqualTo(10);
    }
}
