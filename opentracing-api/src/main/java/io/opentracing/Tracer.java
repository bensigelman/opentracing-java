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
package io.opentracing;

import io.opentracing.propagation.Format;

/**
 * Tracer is a simple, thin interface for Span creation and propagation across arbitrary transports.
 */
public interface Tracer {

    /**
     * Return a new SpanBuilder for a Span with the given `operationName`.
     *
     * <p>If there is an active Span according to the {@link Tracer#spanScheduler()}'s
     * {@link SpanScheduler#activeContext}, buildSpan will automatically build a {@link References#INFERRED_CHILD_OF}
     * reference to same.
     *
     * <p>You can override the operationName later via {@link Span#setOperationName(String)}.
     *
     * <p>A contrived example:
     * <pre>{@code
         Tracer tracer = ...

         // Note: if there is an {@link SpanScheduler#active()} Span, it will be treated as the parent of workSpan.
         Span workSpan = tracer.buildSpan("DoWork")
                               .start();

         Span http = tracer.buildSpan("HandleHTTPRequest")
                           .asChildOf(workSpan.context())
                           .withTag("user_agent", req.UserAgent)
                           .withTag("lucky_number", 42)
                           .start();
     }</pre>
     */
    SpanBuilder buildSpan(String operationName);

    /**
     * @return the SpanScheduler associated with this Tracer. Must not be null.
     * @see SpanScheduler
     * @see ThreadLocalScheduler a simple built-in thread-local-storage SpanScheduler
     */
    SpanScheduler spanScheduler();

    /**
     * Inject a SpanContext into a `carrier` of a given type, presumably for propagation across process boundaries.
     *
     * <p>Example:
     * <pre>{@code
     *     Tracer tracer = ...
     *     Span clientSpan = ...
     *     TextMap httpHeadersCarrier = new AnHttpHeaderCarrier(httpRequest);
     *     tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, httpHeadersCarrier);
     * }</pre>
     *
     * @param <C> the carrier type, which also parametrizes the Format.
     * @param spanContext the SpanContext instance to inject into the carrier
     * @param format the Format of the carrier
     * @param carrier the carrier for the SpanContext state. All Tracer.inject() implementations must support io.opentracing.propagation.TextMap and java.nio.ByteBuffer.
     *
     * @see io.opentracing.propagation.Format
     * @see io.opentracing.propagation.Format.Builtin
     */
    <C> void inject(SpanContext spanContext, Format<C> format, C carrier);

    /**
     * Extract a SpanContext from a `carrier` of a given type, presumably after propagation across a process boundary.
     *
     * <p>Example:
     * <pre>{@code
     *     Tracer tracer = ...
     *     TextMap httpHeadersCarrier = new AnHttpHeaderCarrier(httpRequest);
     *     SpanContext spanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, httpHeadersCarrier);
     *     tracer.buildSpan('...').asChildOf(spanCtx).start();
     * }</pre>
     *
     * If the span serialized state is invalid (corrupt, wrong version, etc) inside the carrier this will result in an
     * IllegalArgumentException.
     *
     * @param <C> the carrier type, which also parametrizes the Format.
     * @param format the Format of the carrier
     * @param carrier the carrier for the SpanContext state. All Tracer.extract() implementations must support
     *                io.opentracing.propagation.TextMap and java.nio.ByteBuffer.
     *
     * @return the SpanContext instance holding context to create a Span.
     *
     * @see io.opentracing.propagation.Format
     * @see io.opentracing.propagation.Format.Builtin
     */
    <C> SpanContext extract(Format<C> format, C carrier);

    interface SpanBuilder extends SpanContext {

        /**
         * A shorthand for addReference(References.CHILD_OF, parent).
         */
        SpanBuilder asChildOf(SpanContext parent);

        /**
         * A shorthand for addReference(References.CHILD_OF, parent.context()).
         */
        SpanBuilder asChildOf(Span parent);

        /**
         * Add a reference from the Span being built to a distinct (usually parent) Span. May be called multiple times to
         * represent multiple such References.
         * <p>
         * If no references are added manually before {@link SpanBuilder#start()} is invoked, an
         * {@link References#INFERRED_CHILD_OF} reference is created to any {@link SpanScheduler#activeContext()}
         * context.
         *
         * @param referenceType the reference type, typically one of the constants defined in References
         * @param referencedContext the SpanContext being referenced; e.g., for a References.CHILD_OF referenceType, the
         *                          referencedContext is the parent
         *
         * @see io.opentracing.References
         */
        SpanBuilder addReference(String referenceType, SpanContext referencedContext);

        /** Same as {@link Span#setTag(String, String)}, but for the span being built. */
        SpanBuilder withTag(String key, String value);

        /** Same as {@link Span#setTag(String, boolean)}, but for the span being built. */
        SpanBuilder withTag(String key, boolean value);

        /** Same as {@link Span#setTag(String, Number)}, but for the span being built. */
        SpanBuilder withTag(String key, Number value);

        /** Specify a timestamp of when the Span was started, represented in microseconds since epoch. */
        SpanBuilder withStartTimestamp(long microseconds);

        /**
         * @return the newly-started Span instance
         */
        Span start();

        /**
         * Returns a newly started and {@linkshort SpanScheduler.ActivationState#activate(boolean) activated}
         * {@link SpanScheduler.ActivationState}.
         *
         * @param autoFinish if true, the {@link Span} encapsulated by the {@link SpanScheduler.ActivationState} will
         *                   finish() upon invocation of {@link SpanScheduler.ActivationState#deactivate()}.
         * @return a pre-activated {@link SpanScheduler.ActivationState}
         */
        SpanScheduler.ActivationState startAndActivate(boolean autoFinish);

    }
}
