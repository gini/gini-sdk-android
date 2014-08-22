package net.gini.android;


import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class ConstantFuture<T> implements Future<T> {
    private final T value;

    public ConstantFuture(final T value) {
        this.value = value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public T get(final long timeout, final TimeUnit unit) {
        return null;
    }
}