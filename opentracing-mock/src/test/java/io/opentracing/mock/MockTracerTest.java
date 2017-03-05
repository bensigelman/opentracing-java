/**
 * Copyright 2016-2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import io.opentracing.*;
import io.opentracing.concurrent.TracedExecutorService;
import org.junit.Assert;
import org.junit.Test;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class MockTracerTest {
    @Test
    public void testMDCHack() {
        org.apache.log4j.BasicConfigurator.configure();
        final Logger logger = org.slf4j.LoggerFactory.getLogger("hack");
        MDC.put("key1", "val1");
        Map<String, String> ctxMap = MDC.getCopyOfContextMap();
        MDC.put("key2", "val2");
        MDC.setContextMap(ctxMap);
        logger.info("testing: {}", MDC.getCopyOfContextMap().toString());

        final MockTracer tracer = new MockTracer();
        tracer.setSpanManager(new MDCSpanManager());

        /*
        Span par = tracer.buildSpan("parent").start();
        Span childA = tracer.buildSpan("childA").start();
        childA.finish();
        par.finish();

        List<MockSpan> finishedSpans = tracer.finishedSpans();

        for (MockSpan span : finishedSpans) {
            logger.info("finished Span. {} :: {} ({})", span.context().traceId(), span.context().spanId(), span.parentId());
        }
        tracer.reset();
        */

        ExecutorService realExecutor = Executors.newFixedThreadPool(500);
        final ExecutorService otExecutor = new TracedExecutorService(realExecutor, tracer.activeSpanManager());
        Span parent = tracer.buildSpan("parent").start();
        SpanManager.SpanClosure parentSpanClosure = tracer.activeSpanManager().captureWithSpan(parent);
        parentSpanClosure.activate();
        final List<Future<?>> futures = new ArrayList<>();
        final List<Future<?>> subfutures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(otExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    final Span child = tracer.buildSpan("child_" + j).start();
                    SpanManager.SpanClosure childSpanClosure = tracer.activeSpanManager().captureWithSpan(child);
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

    @Test
    public void testRootSpan() {
        // Create and finish a root Span.
        MockTracer tracer = new MockTracer();
        {
            Span span = tracer.buildSpan("tester").withStartTimestamp(1000).start();
            span.setTag("string", "foo");
            span.setTag("int", 7);
            // Old style logging:
            span.log(1001, "event name", tracer);
            // New style logging:
            Map<String, Object> fields = new HashMap<>();
            fields.put("f1", 4);
            fields.put("f2", "two");
            span.log(1002, fields);
            span.log(1003, "event name");
            span.finish(2000);
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        // Check that the Span looks right.
        assertEquals(1, finishedSpans.size());
        MockSpan finishedSpan = finishedSpans.get(0);
        assertEquals("tester", finishedSpan.operationName());
        assertEquals(0, finishedSpan.parentId());
        assertNotEquals(0, finishedSpan.context().traceId());
        assertNotEquals(0, finishedSpan.context().spanId());
        assertEquals(1000, finishedSpan.startMicros());
        assertEquals(2000, finishedSpan.finishMicros());
        Map<String, Object> tags = finishedSpan.tags();
        assertEquals(2, tags.size());
        assertEquals(7, tags.get("int"));
        assertEquals("foo", tags.get("string"));
        List<MockSpan.LogEntry> logs = finishedSpan.logEntries();
        assertEquals(3, logs.size());
        {
            MockSpan.LogEntry log = logs.get(0);
            assertEquals(1001, log.timestampMicros());
            assertEquals("event name", log.fields().get("event"));
            assertEquals(tracer, log.fields().get("payload"));
        }
        {
            MockSpan.LogEntry log = logs.get(1);
            assertEquals(1002, log.timestampMicros());
            assertEquals(4, log.fields().get("f1"));
            assertEquals("two", log.fields().get("f2"));
        }
        {
            MockSpan.LogEntry log = logs.get(2);
            assertEquals(1003, log.timestampMicros());
            assertEquals("event name", log.fields().get("event"));
        }
    }

    @Test
    public void testChildSpan() {
        // Create and finish a root Span.
        MockTracer tracer = new MockTracer();
        {
            Span parent = tracer.buildSpan("parent").withStartTimestamp(1000).start();
            Span child = tracer.buildSpan("child").withStartTimestamp(1100).asChildOf(parent).start();
            child.finish(1900);
            parent.finish(2000);
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        // Check that the Spans look right.
        assertEquals(2, finishedSpans.size());
        MockSpan child = finishedSpans.get(0);
        MockSpan parent = finishedSpans.get(1);
        assertEquals("child", child.operationName());
        assertEquals("parent", parent.operationName());
        assertEquals(parent.context().spanId(), child.parentId());
        assertEquals(parent.context().traceId(), child.context().traceId());

    }

    @Test
    public void testStartTimestamp() throws InterruptedException {
        MockTracer tracer = new MockTracer();
        long startMicros;
        {
            Tracer.SpanBuilder fooSpan = tracer.buildSpan("foo");
            Thread.sleep(2);
            startMicros = System.currentTimeMillis() * 1000;
            fooSpan.start().finish();
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        Assert.assertEquals(1, finishedSpans.size());
        MockSpan span = finishedSpans.get(0);
        Assert.assertTrue(startMicros <= span.startMicros());
        Assert.assertTrue(System.currentTimeMillis() * 1000 >= span.finishMicros());
    }

    @Test
    public void testStartExplicitTimestamp() throws InterruptedException {
        MockTracer tracer = new MockTracer();
        long startMicros = 2000;
        {
            tracer.buildSpan("foo")
                    .withStartTimestamp(startMicros)
                    .start()
                    .finish();
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        Assert.assertEquals(1, finishedSpans.size());
        Assert.assertEquals(startMicros, finishedSpans.get(0).startMicros());
    }

    @Test
    public void testTextMapPropagatorTextMap() {
        MockTracer tracer = new MockTracer(MockTracer.Propagator.TEXT_MAP);
        {
            Span parentSpan = tracer.buildSpan("foo")
                    .start();
            parentSpan.finish();

            HashMap<String, String> injectMap = new HashMap<>();
            tracer.inject(parentSpan.context(), Format.Builtin.TEXT_MAP,
                    new TextMapInjectAdapter(injectMap));

            SpanContext extract = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(injectMap));

            tracer.buildSpan("bar")
                    .asChildOf(extract)
                    .start()
                    .finish();
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        Assert.assertEquals(2, finishedSpans.size());
        Assert.assertEquals(finishedSpans.get(0).context().traceId(), finishedSpans.get(1).context().traceId());
        Assert.assertEquals(finishedSpans.get(0).context().spanId(), finishedSpans.get(1).parentId());
    }

    @Test
    public void testTextMapPropagatorHttpHeaders() {
        MockTracer tracer = new MockTracer(MockTracer.Propagator.TEXT_MAP);
        {
            Span parentSpan = tracer.buildSpan("foo")
                    .start();
            parentSpan.finish();

            HashMap<String, String> injectMap = new HashMap<>();
            tracer.inject(parentSpan.context(), Format.Builtin.HTTP_HEADERS,
                    new TextMapInjectAdapter(injectMap));

            SpanContext extract = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(injectMap));

            tracer.buildSpan("bar")
                    .asChildOf(extract)
                    .start()
                    .finish();
        }
        List<MockSpan> finishedSpans = tracer.finishedSpans();

        Assert.assertEquals(2, finishedSpans.size());
        Assert.assertEquals(finishedSpans.get(0).context().traceId(), finishedSpans.get(1).context().traceId());
        Assert.assertEquals(finishedSpans.get(0).context().spanId(), finishedSpans.get(1).parentId());
    }
}
