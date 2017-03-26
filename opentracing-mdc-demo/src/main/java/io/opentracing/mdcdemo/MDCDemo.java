package io.opentracing.mdcdemo;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
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

    public void trivialChild() throws Exception {
        try (ActiveSpanHolder.Continuation c = this.tracer.buildSpan("trivialParent").startAndActivate()) {
            // The child will automatically know about the parent.
            Span child = this.tracer.buildSpan("trivialChild").start();
            child.finish();
        }
    }

    public void asyncSpans() throws Exception {
        final Tracer tracer = this.tracer; // save typing

        // Create an ExecutorService and wrap it in a TracedExecutorService.
        ExecutorService realExecutor = Executors.newFixedThreadPool(500);
        final ExecutorService otExecutor = new TracedExecutorService(realExecutor, tracer.scheduler());

        // Hacky lists of futures we wait for before exiting async Spans.
        final List<Future<?>> futures = new ArrayList<>();
        final List<Future<?>> subfutures = new ArrayList<>();

        /*

         stream of consciousness: could take the wrapped ASH as a param at construction time, then require a bare Span
         instance to refcount, I guess just via the #capture(Span) method. The impl would defer to capture the wrapped
         Continuation and, well, wrap and return it. #activate() would defer --- darn, this breaks in the same way. The
         fundamental problem is that the Tracer's ASH will not know about the wrapper one.

         one option would be to wrap Span itself, though that would require refcount-friendly hooks.
         */

        // Create a parent Continuation for all of the async activity.
        try (final ActiveSpanHolder.Continuation parentContinuation = tracer.buildSpan("parent").startAndActivate();) {

            // Create 10 async children.
            for (int i = 0; i < 10; i++) {
                final int j = i;
                futures.add(otExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        // START child body

                        try (final ActiveSpanHolder.Continuation childContinuation =
                                     tracer.buildSpan("child_" + j).startAndActivate();) {
                            Thread.currentThread().sleep(1000);
                            tracer.scheduler().activeSpan().log("awoke");
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    Span active = tracer.scheduler().activeSpan();
                                    active.log("awoke again");
                                    // Create a grandchild for each child.
                                    Span grandchild = tracer.buildSpan("grandchild_" + j).start();
                                    grandchild.finish();
                                    // active.finish();
                                }
                            };
                            subfutures.add(otExecutor.submit(r));
                        } catch (Exception e) { }

                        // END child body
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
            for (Future<?> f : subfutures) {
                f.get();
            }
        }

        otExecutor.shutdown();
        otExecutor.awaitTermination(3, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        final Logger logger = org.slf4j.LoggerFactory.getLogger("hack");
        MDC.put("mdcKey", "mdcVal");

        final MockTracer tracer = new MockTracer(new MDCActiveSpanHolder());

        // Do stuff with the MockTracer.
        {
            MDCDemo demo = new MDCDemo(tracer);
            demo.trivialSpan();
            demo.trivialChild();
            demo.asyncSpans();
        }

        // Print out all mock-Spans
        List<MockSpan> finishedSpans = tracer.finishedSpans();
        for (MockSpan span : finishedSpans) {
            logger.info("finished Span '{}'. trace={}, span={}, parent={}", span.operationName(), span.context().traceId(), span.context().spanId(), span.parentId());
        }

    }
}
