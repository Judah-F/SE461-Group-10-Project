package org.psnbtech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

public class ClockTest {

    private Clock clock;

    @Before
    public void setUp() {
        clock = new Clock(9.0f);
    }

    // ============== PC on Clock predicates ==============

    @Test
    public void newlyConstructedClockIsNotPaused() {
        assertFalse(clock.isPaused());
    }

    @Test
    public void setPausedTogglesIsPausedFlag() {
        clock.setPaused(true);
        assertTrue(clock.isPaused());
        clock.setPaused(false);
        assertFalse(clock.isPaused());
    }

    @Test
    public void hasElapsedCycleReturnsFalseWhenNoCyclesAccumulated() {
        assertFalse(clock.hasElapsedCycle());
    }

    @Test
    public void hasElapsedCycleConsumesAccumulatedCycle() throws Exception {
        seedElapsedCycles(3);
        assertTrue(clock.hasElapsedCycle());
        assertTrue(clock.hasElapsedCycle());
        assertTrue(clock.hasElapsedCycle());
        assertFalse(clock.hasElapsedCycle());
    }

    @Test
    public void peekElapsedCycleDoesNotConsume() throws Exception {
        seedElapsedCycles(2);
        assertTrue(clock.peekElapsedCycle());
        assertTrue(clock.peekElapsedCycle());
        assertTrue(clock.peekElapsedCycle());
    }

    @Test
    public void resetClearsAccumulatedCyclesAndPausedFlag() throws Exception {
        seedElapsedCycles(5);
        clock.setPaused(true);
        clock.reset();
        assertFalse(clock.hasElapsedCycle());
        assertFalse(clock.isPaused());
    }

    // ============== update() — accumulation while running / paused ==============
    @Test
    public void updateAccumulatesCyclesOverTime() throws Exception {
        Clock fast = new Clock(1000.0f);
        fast.update();
        Thread.sleep(50);
        fast.update();
        assertTrue(fast.peekElapsedCycle());
    }

    @Test
    public void updateDoesNotAccumulateCyclesWhilePaused() throws Exception {
        Clock fast = new Clock(1000.0f);
        fast.update();
        fast.setPaused(true);
        Thread.sleep(50);
        fast.update();
        assertFalse(fast.peekElapsedCycle());
    }

    // ============== B3 — pause leak on hasElapsedCycle ==============
    // Predicate is `elapsedCycles > 0` with no `&& !isPaused`, so cycles
    // accumulated before a pause leak through. Fix: add `&& !isPaused`.

    @Test
    public void pausedClockShouldNotReportElapsedCyclesEvenIfAccumulated() throws Exception {
        seedElapsedCycles(3);
        clock.setPaused(true);
        assertFalse("B3: paused clock should not report elapsed cycles", clock.hasElapsedCycle());
    }

    // ============== Helper ==============

    private void seedElapsedCycles(int value) throws Exception {
        Field f = Clock.class.getDeclaredField("elapsedCycles");
        f.setAccessible(true);
        f.setInt(clock, value);
    }
}
