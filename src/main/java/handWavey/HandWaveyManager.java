package handWavey;

import handWavey.Zone;
import handWavey.HandWaveyConfig;
import config.*;
import dataCleaner.MovingMean;
import dataCleaner.History;
import debug.Debug;
import mouseAndKeyboardOutput.*;
import audio.*;
import java.awt.Dimension;
import java.sql.Timestamp;
import java.util.HashMap;
import java.io.File;

public class HandWaveyManager {
    private HandSummary[] handSummaries;
    private HashMap<String, Zone> zones = new HashMap<String, Zone>();
    private MovingMean movingMeanX = new MovingMean(1, 0);
    private MovingMean movingMeanY = new MovingMean(1, 0);
    private Debug debug;
    private GenericOutput output;
    private HandsState handsState;
    private History historyX;
    private History historyY;
    private History historyScroll;
        
    private HashMap<String, String> eventSounds = new HashMap<String, String>();
    private String audioPath;
    
    private String zoneMode = "touchScreen";
    
    private int desktopWidth = 0;
    private int desktopHeight = 0;
    
    private int xOffset = 0;
    private int yOffset = 0;
    private double xMultiplier = 1;
    private double yMultiplier = 1;
    private double zMultiplier = -1;
    
    private double zNoMoveBegin = 0;
    private double zActiveBegin = 0;
    private double zAbsoluteBegin = 0;
    private double zRelativeBegin = 0;
    private double zActionBegin = 0;
    
    private double lastAbsoluteX = 0;
    private double lastAbsoluteY = 0;
    private double diffRemainderY = 0;
    private double diffRemainderX = 0;
    private double diffScrollRemainderY = 0;
    private double diffScrollRemainderX = 0;
    
    private int touchPadX = 0;
    private int touchPadY = 0;
    private double touchPadInputMultiplier = 5;
    private double touchPadOutputMultiplier = 1;
    private double touchPadAcceleration = 2;
    
    private int rewindCursorTime = 300;
    private int cursorLockTime = 400;
    private long cursorLock = 0;
    
    private double scrollInputMultiplier = 5;
    private double scrollOutputMultiplier = 1;
    private double scrollAcceleration = 2;
    
    private int rewindScrollTime = 300;
    
    private double relativeSensitivity = 0.1;
    
    private int maxChange = 200;
    
    private Boolean shouldDiscardOldPosition = true;
    
    public HandWaveyManager() {
        HandWaveyConfig handWaveyConfig = new HandWaveyConfig("handWavey");
        handWaveyConfig.defineGeneralConfig();
        
        this.output = new GenericOutput();
        this.handsState = new HandsState();
        
        reloadConfig();
    }
    
    public void reloadConfig() {
        // This function reloads, and calcuates config based on the settings currently in Config. It does not trigger a reload of the config from file.
        Group handSummaryManager = Config.singleton().getGroup("handSummaryManager");
        
        
        // Set up the debugging.
        int debugLevel = Integer.parseInt(handSummaryManager.getItem("debugLevel").get());
        this.debug = new Debug(debugLevel, "HandWaveyManager");
        
        
        // Set up maxChange.
        this.maxChange = Integer.parseInt(handSummaryManager.getItem("maxChange").get());
        
        
        // Get configured multipliers.
        Group axisOrientation = handSummaryManager.getGroup("axisOrientation");
        int configuredXMultiplier = Integer.parseInt(axisOrientation.getItem("xMultiplier").get());
        int configuredYMultiplier = Integer.parseInt(axisOrientation.getItem("yMultiplier").get());
        int configuredZMultiplier = Integer.parseInt(axisOrientation.getItem("zMultiplier").get());
        this.zMultiplier = configuredZMultiplier;
        
        
        // Get the total desktop resolution.
        this.output.info();
        Dimension desktopResolution = this.output.getDesktopResolution();
        this.desktopWidth = desktopResolution.width;
        this.desktopHeight = desktopResolution.height;
        double desktopAspectRatio = this.desktopWidth/this.desktopHeight;
        
        // Set initial locaiton for touchPad based zoning.
        this.touchPadX = (int) Math.round(this.desktopWidth /3);
        this.touchPadY = (int) Math.round(this.desktopHeight /3);
        
        
        // Figure out how to best fit the desktop into the physical space.
        // TODO This could be abstracted out into testable code.
        Group physicalBoundaries = handSummaryManager.getGroup("physicalBoundaries");
        int pX = Integer.parseInt(physicalBoundaries.getItem("x").get());
        int pXDiff = pX * 2;
        int pYMin = Integer.parseInt(physicalBoundaries.getItem("yMin").get());
        int pYMax = Integer.parseInt(physicalBoundaries.getItem("yMax").get());
        int pYDiff = pYMax - pYMin;
        double physicalAspectRatio = pXDiff / pYDiff;
        
        this.xOffset = pX;
        this.yOffset = pYMin * -1;
        
        if (desktopAspectRatio > physicalAspectRatio) { // desktop is wider
            this.debug.out(1, "Desktop is wider than the cone. Optimising the usable cone for that.");
            this.yMultiplier = configuredYMultiplier * (this.desktopHeight/pYDiff);
            this.xMultiplier = configuredXMultiplier * (this.desktopHeight/pYDiff);
        } else { // desktop is narrower
            this.debug.out(1, "The cone is wider than the desktop. Optimising the usable cone for that.");
            this.xMultiplier = configuredXMultiplier * (this.desktopWidth/pXDiff);
            this.yMultiplier = configuredYMultiplier * (this.desktopWidth/pXDiff);
        }
        
        
        // Configure Z axis thresholds.
        this.zoneMode = handSummaryManager.getItem("zoneMode").get();
        if (this.zoneMode == "touchScreen") {
            Group touchScreen = handSummaryManager.getGroup("zones").getGroup("touchScreen");
            this.zAbsoluteBegin = Double.parseDouble(touchScreen.getGroup("absolute").getItem("threshold").get());
            this.zRelativeBegin = Double.parseDouble(touchScreen.getGroup("relative").getItem("threshold").get());
            this.zActionBegin = Double.parseDouble(touchScreen.getGroup("action").getItem("threshold").get());
            
            this.zones.put("none", new Zone(-999, this.zAbsoluteBegin, 1, 1));
            this.zones.put("absolute", new Zone(
                this.zAbsoluteBegin, this.zRelativeBegin,
                Integer.parseInt(touchScreen.getGroup("absolute").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchScreen.getGroup("absolute").getItem("movingMeanEnd").get())));
            this.zones.put("relative", new Zone(
                this.zRelativeBegin, this.zActionBegin,
                Integer.parseInt(touchScreen.getGroup("relative").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchScreen.getGroup("relative").getItem("movingMeanEnd").get())));
            this.zones.put("action", new Zone(
                this.zActionBegin, this.zActionBegin+50,
                Integer.parseInt(touchScreen.getGroup("action").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchScreen.getGroup("action").getItem("movingMeanEnd").get())));
        } else if (this.zoneMode == "touchPad") {
            Group touchPad = handSummaryManager.getGroup("zones").getGroup("touchPad");
            this.zNoMoveBegin = Double.parseDouble(touchPad.getGroup("noMove").getItem("threshold").get());
            this.zActiveBegin = Double.parseDouble(touchPad.getGroup("active").getItem("threshold").get());
            this.zActionBegin = Double.parseDouble(touchPad.getGroup("action").getItem("threshold").get());
            
            this.zones.put("none", new Zone(-999, this.zNoMoveBegin, 1, 1));
            this.zones.put("noMove", new Zone(this.zNoMoveBegin, this.zActiveBegin, 1, 1));
            this.zones.put("active", new Zone(
                this.zActiveBegin, this.zActionBegin,
                Integer.parseInt(touchPad.getGroup("active").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchPad.getGroup("active").getItem("movingMeanEnd").get())));
            this.zones.put("action", new Zone(
                this.zActionBegin, this.zActionBegin+50,
                Integer.parseInt(touchPad.getGroup("action").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchPad.getGroup("action").getItem("movingMeanEnd").get())));
            this.zones.put("scroll", new Zone(
                9900, 9999,
                Integer.parseInt(touchPad.getGroup("active").getItem("movingMeanBegin").get()),
                Integer.parseInt(touchPad.getGroup("active").getItem("movingMeanEnd").get())));
        } else {
            // TODO This needs to produce some user feedback that the user will see. Once this runs as a service, a debug message won't be sufficient.
            this.debug.out(0, "Unknown zoneMode " + this.zoneMode + ". This will likely cause badness.");
        }
        
        this.debug.out(1, "Moving mean configured for zones:");
        for (String key: this.zones.keySet()) {
            this.debug.out(1, "  " + key + ":  " + this.zones.get(key).toString());
        }
        
        // Get relative sensitivity.
        this.relativeSensitivity = Double.parseDouble(handSummaryManager.getItem("relativeSensitivity").get());
        
        
        // Get Audio path.
        this.audioPath = Config.singleton().getGroup("audioConfig").getItem("pathToAudio").get() + File.separator;;
        
        
        // Get event sounds.
        loadEventSoundFromConfig("zone-none");
        loadEventSoundFromConfig("zone-noMove");
        loadEventSoundFromConfig("zone-active");
        loadEventSoundFromConfig("zone-action");
        
        loadEventSoundFromConfig("zone-absolute");
        loadEventSoundFromConfig("zone-relative");
        
        loadEventSoundFromConfig("zone-scroll");
        
        loadEventSoundFromConfig("mouse-down");
        loadEventSoundFromConfig("mouse-up");
        
        loadEventSoundFromConfig("imposterHand-replace");
        loadEventSoundFromConfig("imposterHand-discard");
        
        
        // Load touchpad mode config.
        Group touchPadConfig = handSummaryManager.getGroup("touchPad");
        this.touchPadInputMultiplier = Double.parseDouble(touchPadConfig.getItem("inputMultiplier").get());
        this.touchPadOutputMultiplier = Double.parseDouble(touchPadConfig.getItem("outputMultiplier").get());
        this.touchPadAcceleration = Double.parseDouble(touchPadConfig.getItem("acceleration").get());
        
        
        // Load click config.
        Group click = handSummaryManager.getGroup("click");
        this.rewindCursorTime = Integer.parseInt(click.getItem("rewindCursorTime").get());
        int cursorHistorySize = Integer.parseInt(click.getItem("historySize").get());
        this.historyX = new History(cursorHistorySize, 0);
        this.historyY = new History(cursorHistorySize, 0);
        
        
        // Load scroll config.
        Group scrollConfig = handSummaryManager.getGroup("scroll");
        this.scrollInputMultiplier = Double.parseDouble(scrollConfig.getItem("inputMultiplier").get());
        this.scrollOutputMultiplier = Double.parseDouble(scrollConfig.getItem("outputMultiplier").get());
        this.scrollAcceleration = Double.parseDouble(scrollConfig.getItem("acceleration").get());
        this.rewindScrollTime = Integer.parseInt(scrollConfig.getItem("rewindScrollTime").get());
        int scrollHistorySize = Integer.parseInt(scrollConfig.getItem("historySize").get());
        this.historyScroll = new History(scrollHistorySize, 0);
        
        
        // Config checks.
        checkZones();
    }
    
    private void checkZones() {
        // Compare every zone to every other zone to make sure that there are no unusable zones.
        
        Group handSummaryManager = Config.singleton().getGroup("handSummaryManager");
        double zoneBuffer = Double.parseDouble(handSummaryManager.getItem("zoneBuffer").get());
        
        for (String outerKey: this.zones.keySet()) {
            Zone outerZone = this.zones.get(outerKey);
            for (String innerKey: this.zones.keySet()) {
                if (outerKey != innerKey) {
                    Zone innerZone = this.zones.get(innerKey);
                    
                    double diff = Math.abs(innerZone.getBegin() - outerZone.getBegin());
                    
                    if (diff <= zoneBuffer) {
                        this.debug.out(0, "Warning: There does not appear to be enough usable space between zones " + outerKey + " and " + innerKey + ". Either reduce the zoneBuffer, or increase the distance between these two zones.");
                    }
                }
            }
        }
    }
    
    private void loadEventSoundFromConfig(String eventID) {
        this.debug.out(3, "Load eventID " + eventID);
        Group audioEvents = Config.singleton().getGroup("audioEvents");
        String filePath = audioEvents.getItem(eventID).get();
        
        this.eventSounds.put(eventID, filePath);
    }
    
    private void moveMouse(int x, int y) {
        if (x < 0) x = 0;
        if (x > this.desktopWidth) x = this.desktopWidth-1;
        
        if (y < 0) y = 0;
        if (y > this.desktopHeight) y = this.desktopHeight-1;
        
        this.historyX.set(x);
        this.historyY.set(y);
        
        this.output.setPosition(x, y);
    }
    
    private void rewindCursorPosition() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long nowMillis = now.getTime();
        long rewindTime = nowMillis - this.rewindCursorTime;
        
        int earlierX = (int) Math.round(this.historyX.get(rewindTime));
        int earlierY = (int) Math.round(this.historyY.get(rewindTime));
        
        this.debug.out(1, "Rewind cursor position by " + this.rewindCursorTime + " milliseconds to around " + String.valueOf(rewindTime) + ", " + String.valueOf(earlierX) + "," + String.valueOf(earlierY) + " for mouse down/up event.");
        
        this.output.setPosition(earlierX, earlierY);
    }
    
    private void rewindScroll() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long nowMillis = now.getTime();
        long rewindTime = nowMillis - this.rewindScrollTime;
        
        int earlierScroll = (int) Math.round(this.historyScroll.getSumFrom(rewindTime));
        
        this.debug.out(1, "Rewind scroll position by " + this.rewindScrollTime + " milliseconds to around " + String.valueOf(rewindTime) + ", " + String.valueOf(earlierScroll) + " for mouse down/up event.");
        
        this.output.scroll(earlierScroll * -1);
    }
    
    private long getNow() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.getTime();
    }
    
    private void setCursorLock() {
        long now = getNow();
        this.cursorLock = now + this.cursorLockTime;
        this.debug.out(1, "Cursor locked at " + String.valueOf(now) + " for " + String.valueOf(this.cursorLockTime) + " milliseconds until " + String.valueOf(this.cursorLock) + ".");
    }
    
    private Boolean cusorIsLocked() {
        if (this.cursorLock > 0) {
            return (getNow() < this.cursorLock);
        } else {
            return false;
        }
    }
    
    private void releaseCursorLock() {
        // Figure out what feedback we are going to give.
        if (this.cursorLock > 0) {
            if (cusorIsLocked()) {
                this.debug.out(1, "Cursor explicitly unlocked.");
            } else {
                this.debug.out(1, "Cursor would have been explicitly unlocked, but the cursorLockTime had already expired.");
            }
        } else {
            this.debug.out(1, "Cursor would have been explicitly unlocked. But was already unlocked.");
        }

        // Do the actual work.
        this.cursorLock = 0;
    }
    
    private int coordToDesktopIntX(double xCoord) {
        return (int) Math.round((xCoord + this.xOffset) * this.xMultiplier);
    }
    
    private int coordToDesktopIntY(double yCoord) {
        return this.desktopHeight - (int) Math.round((yCoord + this.yOffset) * this.yMultiplier);
    }
    
    public void discardOldPosition() {
        this.shouldDiscardOldPosition = true;
    }
    
    private void moveMouseTouchPadFromCoordinates(double xCoord, double yCoord) {
        // Calculate how far the hand has moved since the last iteration.
        double xCoordDiff = 0;
        double yCoordDiff = 0;
        
        if (!this.shouldDiscardOldPosition) {
            xCoordDiff = xCoord - this.lastAbsoluteX + this.diffRemainderX;
            yCoordDiff = yCoord - this.lastAbsoluteY + this.diffRemainderY;
        } else {
            this.shouldDiscardOldPosition = false;
        }
        
        // Calculate the distance we have moved regardless of direction.
        double angularDiff = Math.pow((Math.pow(xCoordDiff, 2) + Math.pow(yCoordDiff, 2)), 0.5);
        
        // Apply maxChange.
        if (angularDiff > this.maxChange) {
            this.debug.out(1, "maxChange has been hit (" + String.valueOf(angularDiff) + " > " + String.valueOf(maxChange) + "), and this frame has been filtered. If this movement was legitimate, consider increasing the maxChange value in the config.");
            this.lastAbsoluteX = xCoord;
            this.lastAbsoluteY = yCoord;
            return;
        }
        
        // Calculate our acceleration.
        double accelerationThreshold = 1;
        double accelerationMultiplier = accelerationThreshold;
        if (angularDiff > accelerationThreshold) {
            accelerationMultiplier = angularDiff * this.touchPadAcceleration;
        }
        
        // Bring everything together to calcuate how far we should move the cursor.
        double xInput = xCoordDiff * this.touchPadInputMultiplier;
        int diffX = (int) Math.round(xInput * accelerationMultiplier * this.touchPadOutputMultiplier);
        double yInput = yCoordDiff * this.touchPadInputMultiplier;
        int diffY = (int) Math.round(yInput * accelerationMultiplier * this.touchPadOutputMultiplier);
        
        // Apply the changes.
        this.touchPadX = this.touchPadX + diffX;
        this.touchPadY = this.touchPadY - diffY;
        
        // Carry over anything that happened, but didn't result in a movement. This means that we can make use of the finer movements without having to move the acceleration and multipliers to extremes.
        this.diffRemainderX = (diffX == 0)?xCoordDiff:0;
        this.diffRemainderY = (diffY == 0)?yCoordDiff:0;
        
        // Not catching OOB here makes the mouse feel sticky on the edges.
        if (this.touchPadX < 0) this.touchPadX = 0;
        if (this.touchPadY < 0) this.touchPadY = 0;
        if (this.touchPadX > this.desktopWidth) this.touchPadX = this.desktopWidth;
        if (this.touchPadY > this.desktopHeight) this.touchPadY = this.desktopHeight;
        
        // Track where we are now so that differences make sense on the next round.
        this.lastAbsoluteX = xCoord;
        this.lastAbsoluteY = yCoord;
        
        moveMouse(this.touchPadX, this.touchPadY);
    }
    
    private void scrollFromCoordinates(double xCoord, double yCoord) {
        // Reset touchPad motion since anything from before the scroll will be meaningless.
        this.diffRemainderX = 0;
        this.diffRemainderY = 0;
        
        // Calculate how far the hand has moved since the last iteration.
        double xCoordDiff = 0;
        double yCoordDiff = 0;
        
        if (!this.shouldDiscardOldPosition) {
            xCoordDiff = xCoord - this.lastAbsoluteX + this.diffScrollRemainderX;
            yCoordDiff = yCoord - this.lastAbsoluteY + this.diffScrollRemainderY;
        } else {
            this.shouldDiscardOldPosition = false;
        }
        
        // Calculate our acceleration.
        double accelerationThreshold = 1;
        double angularDiff = Math.pow((Math.pow(xCoordDiff, 2) + Math.pow(yCoordDiff, 2)), 0.5);
        
        double accelerationMultiplier = accelerationThreshold;
        if (angularDiff > accelerationThreshold) {
            accelerationMultiplier = angularDiff * this.scrollAcceleration;
        }
        
        // Bring everything together to calcuate how far we should move the cursor.
        double xInput = xCoordDiff * this.scrollInputMultiplier;
        double yInput = yCoordDiff * this.scrollInputMultiplier;
        
        int diffX = 0;
        int diffY = 0;
        
        if (this.scrollAcceleration > 1) {
            diffX = (int) Math.round(xInput * accelerationMultiplier * this.scrollOutputMultiplier);
            diffY = (int) Math.round(yInput * accelerationMultiplier * this.scrollOutputMultiplier);
        } else {
            diffX = (int) Math.round(xInput * this.scrollOutputMultiplier);
            diffY = (int) Math.round(yInput * this.scrollOutputMultiplier);
        }
        
        // Record changes.
        this.historyScroll.set(diffY);
        
        // Apply the changes.
        this.output.scroll(diffY);
        
        // Carry over anything that happened, but didn't result in a movement. This means that we can make use of the finer movements without having to move the acceleration and multipliers to extremes.
        this.diffScrollRemainderX = (diffX == 0)?xCoordDiff:0;
        this.diffScrollRemainderY = (diffY == 0)?yCoordDiff:0;
        
        // Track where we are now so that differences make sense on the next round.
        this.lastAbsoluteX = xCoord;
        this.lastAbsoluteY = yCoord;
    }
    
    private void touchPadNone(double xCoord, double yCoord) {
        // This is needed, because otherwise we end up back where we started every time we lift and re-apply.
        this.lastAbsoluteX = xCoord;
        this.lastAbsoluteY = yCoord;
    }
    
    private void moveMouseAbsoluteFromCoordinates(double xCoord, double yCoord) {
        int calculatedX = coordToDesktopIntX(xCoord);
        int calculatedY = coordToDesktopIntY(yCoord);
        
        this.lastAbsoluteX = xCoord;
        this.lastAbsoluteY = yCoord;
        
        moveMouse(calculatedX, calculatedY);
    }
    
    private void moveMouseRelativeFromCoordinates(double xCoord, double yCoord) {
        double xDiff = xCoord - this.lastAbsoluteX;
        double yDiff = yCoord - this.lastAbsoluteY;
        
        int calculatedX = (int) Math.round(coordToDesktopIntX(this.lastAbsoluteX + (xDiff * this.relativeSensitivity)));
        int calculatedY = (int) Math.round(coordToDesktopIntY(this.lastAbsoluteY + (yDiff * this.relativeSensitivity)));
        
        moveMouse(calculatedX, calculatedY);
    }
    
    private void updateMovingMeans(String zone, double handZ) {
        this.movingMeanX.set(this.handSummaries[0].getHandX());
        this.movingMeanY.set(this.handSummaries[0].getHandY());
        this.movingMeanX.resize(this.zones.get(zone).getMovingMeanWidth(handZ));
        this.movingMeanY.resize(this.zones.get(zone).getMovingMeanWidth(handZ));
    }
    
    public void triggerEvent(String eventID) {
        String fileName = this.eventSounds.get(eventID);
        
        if (fileName != "") {
            String fullPath = this.audioPath + fileName;
            this.debug.out(1, "Triggering event " + eventID + " File: " + fullPath);
            
            BackgroundSound.play(fullPath);
        }
    }
    
    private Boolean secondaryHandIsActive() {
        return ((this.handSummaries.length > 1) && (this.handSummaries[1] != null) && (this.handSummaries[1].isValid()));
    }
    
    private String coordsToString(double x, double y) {
        return String.valueOf(Math.round(x)) + ", " + String.valueOf(Math.round(y));
    }
    
    private void handleKeysDowns() {
        for (String key : this.output.getKeysIKnow()) {
            if (this.handsState.shouldKeyDown(key)) {
                this.debug.out(1, "Key down: " + key);
                this.output.keyDown(this.output.getKeyID(key));
            }
        }
    }
    
    private void handleKeyUps() {
        for (String key : this.output.getKeysIKnow()) {
            if (this.handsState.shouldKeyUp(key)) {
                this.debug.out(1, "Key up:   " + key);
                this.output.keyUp(this.output.getKeyID(key));
            }
        }
    }
    
    /* TODO
    
    * Audio feedback on:
        * Hand segment change.
        * Secondary hand in range.
    * Data cleaning:
        * How many frames for a mouse event to be acted on?
    * Acceleration: Add time.
    * Config based mapping to actions.
    * VNC for initial compatibility with wayland?
    * Config to/from disk.
    * Make audio feedback for hands left/right hand aware.
    * Abstract out config into a new class.
    * How to build the final asset?
    * Documentation/automation:
        * Installation.
        * Configuring.
            * touchPad acceleration.
            * scroll acceleration.
            * touchPad vs touchScreen.
            * gestureConfig.
            * audioFeedback.
    * Synth audio feedback for when close to zone boundaries.
    * Occasional freeze:
        * Usually sound stops. But other stuff still works.
        * Check whether threads are lingering.
        * Check GC.
    
    */
    
    // This is where everything gets glued together.
    public void sendHandSummaries(HandSummary[] handSummaries) {
        this.handSummaries = handSummaries;
        
        Double handZ = this.handSummaries[0].getHandZ() * this.zMultiplier;
        String zone = this.handsState.getZone(handZ);
        this.handsState.setHandClosed(!this.handSummaries[0].handIsOpen());
        
        this.handsState.derivePrimaryHandSegment(this.handSummaries[0].getHandRoll(), this.handSummaries[0].handIsLeft());
        if (secondaryHandIsActive()) {
            this.handsState.deriveSecondaryHandSegment(this.handSummaries[1].getHandRoll(), this.handSummaries[1].handIsLeft());
        } else {
            this.handsState.noSecondaryHand();
        }
        
        // Figure out the current state of of gestures.
        this.handsState.figureOutStuff();
        
        // This should happen before any potential de-stabilisation has happened.
        if (this.handsState.shouldMouseUp() == true) {
            this.debug.out(1, "Mouse down at " + coordsToString(this.movingMeanX.get(), this.movingMeanY.get()));
            this.output.mouseUp(this.output.getLastMouseButton());
            
            releaseCursorLock();
            
            triggerEvent("mouse-up");
            if (zone == "scroll") {
                rewindScroll();
            } else  {
                rewindCursorPosition();
            }
        }
        
        handleKeyUps();

        // Move the mouse cursor.
        if (!cusorIsLocked()) {
            if ((zone == "none") || (zone == "noMove")) {
                if (this.zoneMode == "touchPad") {
                    updateMovingMeans(zone, handZ);
                    touchPadNone(this.movingMeanX.get(), this.movingMeanY.get());
                }
            } else if (zone == "active") {
                updateMovingMeans(zone, handZ);
                moveMouseTouchPadFromCoordinates(this.movingMeanX.get(), this.movingMeanY.get());
            } else if (zone == "absolute") {
                updateMovingMeans(zone, handZ);
                moveMouseAbsoluteFromCoordinates(this.movingMeanX.get(), this.movingMeanY.get());
            } else if (zone == "relative") {
                updateMovingMeans(zone, handZ);
                moveMouseRelativeFromCoordinates(this.movingMeanX.get(), this.movingMeanY.get());
            } else if (zone == "action") {
            } else if (zone == "scroll") {
                if (this.handsState.zoneIsNew()) {
                    rewindCursorPosition();
                }
                
                updateMovingMeans(zone, handZ);
                scrollFromCoordinates(this.movingMeanX.get(), this.movingMeanY.get());
            } else {
                this.debug.out(3, "A hand was detected, but it outside of any zones. z=" + String.valueOf(handZ));
            }
        } else {
            // Stop the cursor from jumping around during the beginning of a mouse-down event.
            if (this.zoneMode == "touchPad") {
                updateMovingMeans(zone, handZ);
                touchPadNone(this.movingMeanX.get(), this.movingMeanY.get());
            }
        }
        
        // This should happen after any potential stabilisation has happened.
        if (this.handsState.shouldMouseDown() == true) {
            if (zone == "scroll") {
                rewindScroll();
            } else  {
                rewindCursorPosition();
            }
            
            setCursorLock();
            
            String button = this.handsState.whichMouseButton();
            this.debug.out(1, "Mouse down (" + button + ") at " + coordsToString(this.movingMeanX.get(), this.movingMeanY.get()));
            this.output.mouseDown(this.output.getMouseButtonID(button));
            triggerEvent("mouse-down");
        }
        
        handleKeysDowns();
        
        // Audio events.
        if (this.handsState.zoneIsNew()) {
            String eventID = "zone-" + zone;
            triggerEvent(eventID);
        }
    }
}
