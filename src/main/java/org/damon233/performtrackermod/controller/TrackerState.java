package org.damon233.performtrackermod.controller;

/**
 * Represents the operational state of the performance tracker.
 *
 * <p>The tracker operates as a simple state machine with two states:
 * <ul>
 *   <li>{@link #IDLE} - Tracker is not running, no data collection or output</li>
 *   <li>{@link #RUNNING} - Tracker is actively collecting metrics and outputting data</li>
 * </ul>
 *
 * <p>State transitions:
 * <ul>
 *   <li>IDLE {@code ->} RUNNING via {@link TrackerController#start()}</li>
 *   <li>RUNNING {@code ->} IDLE via {@link TrackerController#stop()}</li>
 * </ul>
 *
 * @author PerformTracker
 * @since 1.0.0
 */
public enum TrackerState {
    /** Tracker is not running. No data collection or output occurs. */
    IDLE,

    /** Tracker is actively collecting and outputting performance data. */
    RUNNING
}
