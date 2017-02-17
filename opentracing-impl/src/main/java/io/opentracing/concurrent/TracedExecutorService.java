package io.opentracing.concurrent;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.impl.GlobalTracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class TracedExecutorService implements ExecutorService {
    private ExecutorService executor;
    private Span span;
    private Tracer tracer;

    public TracedExecutorService(ExecutorService executor){
        this(executor, GlobalTracer.get());
    }

    public TracedExecutorService(ExecutorService executor, Tracer tracer){
        this(executor, tracer.active(), tracer);
    }

    public TracedExecutorService(ExecutorService executor, Span span, Tracer tracer){
        if (executor == null) throw new NullPointerException("Executor is <null>.");
        if (tracer == null) throw new NullPointerException("Tracer is <null>.");
        this.executor = executor;
        this.span = span;
        this.tracer = tracer;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(new TracedRunnable(command, span, tracer));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(new TracedRunnable(task, span, tracer));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(new TracedRunnable(task, span, tracer), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(new TracedCallable(task, span, tracer));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasksWithTracing(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        return executor.invokeAll(tasksWithTracing(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasksWithTracing(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasksWithTracing(tasks), timeout, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> tasksWithTracing(
        Collection<? extends Callable<T>> tasks) {
        if (tasks == null) throw new NullPointerException("Collection of tasks is <null>.");
        Collection<Callable<T>> result = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> task : tasks) result.add(new TracedCallable(task, span, tracer));
        return result;
    }
}
