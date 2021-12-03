package handWavey;

import handWavey.HandsState;
import handWavey.*;
import config.Config;
import config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;


// TODO We need to handle the case of the hand no longer being seen while a mouse down state is present. The mouse should be released so that the desktop returns to being usable.


class TestState {
    HandsState handsState;

    @BeforeEach
    void setUp() {
        Config.setSingletonFilename("handWaveyConfigTest.yml");
        Config config = Config.singleton();

        Group handSummaryManager = config.newGroup("handSummaryManager");
        handSummaryManager.newItem(
            "debugLevel",
            "2",
            "Int: Sensible numbers are 0-5, where 0 is no debugging, and 5  is probably more detail than you'll ever want.");
        handSummaryManager.newItem(
            "rangeMethod",
            "manual",
            "How the range of possible hand positions is configured. Current possible values are: manual.");
        
        Group axisOrientation = handSummaryManager.newGroup("axisOrientation");
        axisOrientation.newItem(
            "xMultiplier",
            "1",
            "Set this to -1 when you need to invert X (side to side). You'll typicall only need to do this if your device is upside down. On newer LeapSDK versions, this may become obsolete.");
        axisOrientation.newItem(
            "yMultiplier",
            "1",
            "Set this to -1 when you need to invert Y (up and down). You'll typicall only need to do this if your device is upside down. On newer LeapSDK versions, this may become obsolete.");
        axisOrientation.newItem(
            "zMultiplier",
            "-1",
            "Set this to -1 when you need to invert Z (how far away from you your hand goes). UltraMotion takes care of this for you. So I can't currently think of a use-case for it, but am including it for completness.");
        
        Group physicalBoundaries = handSummaryManager.newGroup("physicalBoundaries");
        physicalBoundaries.newItem(
            "x",
            "100",
            "+ and - this value horizontally from the center of the visible cone above the device.");
        physicalBoundaries.newItem(
            "yMin",
            "200",
            "Minimum value of height above the device.");
        physicalBoundaries.newItem(
            "yMax",
            "400",
            "Maximum value of height above the device.");
        physicalBoundaries.newItem(
            "z",
            "120",
            "+ and - this value in depth from the center of the visible cone above the device.");
        
        Group zoneThresholds = handSummaryManager.newGroup("zoneThresholds");
        zoneThresholds.newItem(
            "absolute",
            "-150",
            "Greater than ___ denotes the beginning of the absolute zone.");
        zoneThresholds.newItem(
            "relative",
            "50",
            "Greater than ___ denotes the beginning of the relative zone.");
        zoneThresholds.newItem(
            "action",
            "100",
            "Greater than ___ denotes the beginning of the action zone.");

        handSummaryManager.newItem(
            "relativeSensitivity",
            "0.1",
            "How sensitive is the relative zone compared to the absolute zone? Decimal between 0 and 1.");
        
        this.handsState = new HandsState();
    }

    @AfterEach
    void destroy() {
        this.handsState = null;
    }

    @Test
    public void testZones() {
        /* In this test we test a sequence of getZone()s by changing the z axis, and then testing the results.
        
        * We should get different zones depending on where are are on the Z axis.
        * We should get mouse down and up events when entering and exiting the action zone.
        */
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("none", this.handsState.getZone(-151));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("none", this.handsState.getZone(-150));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(10));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(50));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(51));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(100));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("action", this.handsState.getZone(101));
        
        assertEquals(true, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("action", this.handsState.getZone(102));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("action", this.handsState.getZone(103));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(true, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
    }

    @Test
    public void testEraticZones() {
        /* This is a clone of the testZones test, but with slightly more eratic data.
        
        * There should be no broken assumptions from skipping a zone. It should simply do the right thing based on where we are now.
        */
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("none", this.handsState.getZone(-151));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("action", this.handsState.getZone(101));
        
        assertEquals(true, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(true, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
    }
    
    @Test
    public void testHandClosedGesture() {
        /* Here we want to test that the states work correctly with the hand being open and closed.
        */
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("none", this.handsState.getZone(-151));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        this.handsState.setHandClosed(true);
        
        assertEquals(true, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        this.handsState.setHandClosed(true);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(true, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
    }
    
    @Test
    public void testHandClosedGestureWithZones() {
        /* Here we want to test that the states work correctly with the hand being open and closed while moving through zones.
        */
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("none", this.handsState.getZone(-151));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("absolute", this.handsState.getZone(-149));
        this.handsState.setHandClosed(true);
        
        assertEquals(true, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("action", this.handsState.getZone(101));
        this.handsState.setHandClosed(true);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        this.handsState.setHandClosed(true);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(true, this.handsState.shouldMouseUp());
        
        assertEquals("relative", this.handsState.getZone(99));
        this.handsState.setHandClosed(false);
        
        assertEquals(false, this.handsState.shouldMouseDown());
        assertEquals(false, this.handsState.shouldMouseUp());
    }
}

