package handWavey;

import config.*;
import debug.Debug;

public class HandsState {
    private Debug debug;
    
    private double zAbsoluteBegin = 0;
    private double zRelativeBegin = 0;
    private double zActionBegin = 0;
    private double zoneBuffer = 0;
    
    private String zone = "none";
    private String oldZone = "none";
    
    private Boolean zoneMouseDown = false;
    private Boolean gestureMouseDown = false;
    private Boolean resultMouseDownDown = false;
    private Boolean resultMouseDownUp = false;
    
    private Boolean isNew = false;
    
    public HandsState() {
        Config config = Config.singleton();
        Group handSummaryManager = config.getGroup("handSummaryManager");

        // Configure Z axis thresholds.
        Group zones = handSummaryManager.getGroup("zones");
        this.zAbsoluteBegin = Double.parseDouble(zones.getGroup("absolute").getItem("threshold").get());
        this.zRelativeBegin = Double.parseDouble(zones.getGroup("relative").getItem("threshold").get());
        this.zActionBegin = Double.parseDouble(zones.getGroup("action").getItem("threshold").get());
        
        // Configure zone buffer.
        this.zoneBuffer = Double.parseDouble(handSummaryManager.getItem("zoneBuffer").get());
        
        // TODO Give this class its own debug level?
        int debugLevel = Integer.parseInt(handSummaryManager.getItem("debugLevel").get());
        this.debug = new Debug(debugLevel, "HandsState");
    }
    
    private String deriveZone(double handZ) {
        if (handZ > this.zActionBegin) {
            return "action";
        } else if (handZ > this.zRelativeBegin) {
            return "relative";
        } else if (handZ > this.zAbsoluteBegin) {
            return "absolute";
        } else {
            return "none";
        }
    }
    
    
    public String getZone(double handZ) {
        String newZone = deriveZone(handZ);
        String bufferZone = deriveZone(handZ + this.zoneBuffer);
        
        if ((bufferZone == newZone) && (newZone != this.zone)) {
            this.isNew = true;
            this.oldZone = this.zone;
            
            switch(newZone) {
                case "none":
                    this.zoneMouseDown = false;
                    break;
                case "absolute":
                    this.zoneMouseDown = false;
                    break;
                case "relative":
                    this.zoneMouseDown = false;
                    break;
                case "action":
                    this.zoneMouseDown = true;
                    break;
            }
            
            this.zone = newZone;
            this.debug.out(1, "Entered zone " + newZone + "  at depth " + String.valueOf(Math.round(handZ)));
        } else {
            this.isNew = false;
        }
        
        return newZone;
    }
    
    public Boolean zoneIsNew() {
        return this.isNew;
    }
    
    public String getOldZone() {
        return this.oldZone;
    }
    
    private Boolean combinedMouseDown() {
        return ((this.zoneMouseDown || this.gestureMouseDown) && this.zone != "none");
    }
    
    public Boolean shouldMouseDown() {
        Boolean combined = combinedMouseDown();
        if (combined != this.resultMouseDownDown) {
            this.resultMouseDownDown = combined;
            return combined;
        } else {
            return false;
        }
    }
    
    public Boolean shouldMouseUp() {
        Boolean combined = combinedMouseDown();
        if (combined != this.resultMouseDownUp) {
            this.resultMouseDownUp = combined;
            return !combined;
        } else {
            return false;
        }
    }
    
    public void setHandClosed(Boolean handClosed) {
        this.gestureMouseDown = handClosed;
    }
}

