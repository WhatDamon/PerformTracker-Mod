package org.damon233.performtrackermod.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe counter that aggregates values and computes averages.
 * Uses AtomicLong for lock-free, thread-safe operations.
 *
 * <p>This class is designed for performance metric aggregation where
 * multiple threads may update the counter concurrently.
 *
 * @author PerformTracker
 * @since 1.0.0
 */
public class ThreadSafeCounter {
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);

    /**
     * Adds a value to the counter.
     *
     * @param value the value to add
     */
    public void add(long value) {
        sum.addAndGet(value);
        count.incrementAndGet();
    }

    /**
     * Adds a value to the counter.
     *
     * @param value the value to add
     */
    public void add(double value) {
        add((long) value);
    }

    /**
     * Gets the average of all values added.
     *
     * @return the average value, or 0.0 if no values have been added
     */
    public double getAverage() {
        long c = count.get();
        return c == 0 ? 0.0 : (double) sum.get() / c;
    }

    /**
     * Resets the counter to its initial state.
     * Both sum and count are set to zero.
     */
    public void reset() {
        sum.set(0);
        count.set(0);
    }

    /**
     * Returns the number of values that have been added.
     *
     * @return the count of added values
     */
    public long getCount() {
        return count.get();
    }

    /**
     * Returns the sum of all values added.
     *
     * @return the sum of all values
     */
    public long getSum() {
        return sum.get();
    }
}
