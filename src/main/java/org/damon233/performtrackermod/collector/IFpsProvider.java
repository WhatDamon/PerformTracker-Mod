package org.damon233.performtrackermod.collector;

public interface IFpsProvider {
    double getAverageFps();
    void reset();
}
