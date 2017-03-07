package io.opentracing.mdcdemo;

import io.opentracing.Span;
import io.opentracing.SpanManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MDCDemo {
    public static void main(String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        final Logger logger = org.slf4j.LoggerFactory.getLogger("hack");
        MDC.put("mdcKey", "mdcVal");

        final MockTracer tracer = new MockTracer();
        tracer.setSpanManager(new MDCSpanManager());

        ExecutorService realExecutor = Executors.newFixedThreadPool(500);
        final ExecutorService otExecutor = new TracedExecutorService(realExecutor, tracer.activeSpanManager());
        Span parent = tracer.buildSpan("parent").start();
        SpanManager.SpanClosure parentSpanClosure = tracer.activeSpanManager().captureActive();
        parentSpanClosure.activate();
        final List<Future<?>> futures = new ArrayList<>();
        final List<Future<?>> subfutures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(otExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    final Span child = tracer.buildSpan("child_" + j).start();
                    SpanManager.SpanClosure childSpanClosure = tracer.activeSpanManager().captureActive();
                    childSpanClosure.activate();
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    child.log("awoke");
                    logger.info("defining");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            Span active = tracer.activeSpanManager().active();
                            active.log("awoke again");
                            Span grandchild = tracer.buildSpan("grandchild").start();
                            grandchild.finish();
                        }
                    };
                    logger.info("submitting");
                    subfutures.add(otExecutor.submit(r));
                    logger.info("deactivating");
                    childSpanClosure.deactivate(true);
                }
            }));
        }
        try {
            for (Future<?> f : futures) {
                f.get();
            }
            for (Future<?> f : subfutures) {
                f.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        otExecutor.shutdown();
        try {
            otExecutor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parentSpanClosure.deactivate(true);

        List<MockSpan> finishedSpans = tracer.finishedSpans();

        logger.info("DONE SLEEPING");
        for (MockSpan span : finishedSpans) {
            logger.info("finished Span '{}'. trace={}, span={}, parent={}", span.operationName(), span.context().traceId(), span.context().spanId(), span.parentId());
        }

    }
}
