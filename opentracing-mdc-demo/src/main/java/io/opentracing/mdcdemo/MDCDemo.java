package io.opentracing.mdcdemo;

import io.opentracing.Span;
import io.opentracing.SpanManager;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MDCDemo {
    Tracer tracer;

    private MDCDemo(Tracer tracer) {
        this.tracer = tracer;
    }

    public void trivialSpan() {
        Span span = this.tracer.buildSpan("trivial").start();
        span.finish();
    }

    public void trivialChild() {
        Span parent = this.tracer.buildSpan("trivialParent").start();
        // The child will automatically know about the parent.
        Span child = this.tracer.buildSpan("trivialChild").start();
        child.finish();
        parent.finish();
    }

    public void asyncSpans() {
        final Tracer tracer = this.tracer; // save typing

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
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            Span active = tracer.activeSpanManager().active();
                            active.log("awoke again");
                            Span grandchild = tracer.buildSpan("grandchild_" + j).start();
                            grandchild.finish();
                            active.finish();
                        }
                    };
                    subfutures.add(otExecutor.submit(r));
                    childSpanClosure.deactivate(false);
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
    }

    public static void main(String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        final Logger logger = org.slf4j.LoggerFactory.getLogger("hack");
        MDC.put("mdcKey", "mdcVal");

        final MockTracer tracer = new MockTracer(new MDCSpanManager());

        MDCDemo demo = new MDCDemo(tracer);
        demo.trivialSpan();
        demo.trivialChild();
        demo.asyncSpans();

        List<MockSpan> finishedSpans = tracer.finishedSpans();

        logger.info("DONE SLEEPING");
        for (MockSpan span : finishedSpans) {
            logger.info("finished Span '{}'. trace={}, span={}, parent={}", span.operationName(), span.context().traceId(), span.context().spanId(), span.parentId());
        }

    }
}
