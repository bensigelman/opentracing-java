package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.impl.GlobalTracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class TracedExecutorService implements ExecutorService {
    private ExecutorService executor;
    private SpanScheduler manager;

    public TracedExecutorService(ExecutorService executor){
        this(executor, GlobalTracer.get().spanScheduler());
    }

    public TracedExecutorService(ExecutorService executor, SpanScheduler manager) {
        if (executor == null) throw new NullPointerException("Executor is <null>.");
        if (manager == null) throw new NullPointerException("SpanScheduler is <null>.");
        this.executor = executor;
        this.manager = manager;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(new TracedRunnable(command, manager));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(new TracedRunnable(task, manager));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(new TracedRunnable(task, manager), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(new TracedCallable(task, manager));
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
        for (Callable<T> task : tasks) result.add(new TracedCallable(task, manager));
        return result;
    }
}
