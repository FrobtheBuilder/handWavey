// (c) 2022 Kevin Sandom under the GPL v3. See LICENSE for details.

/*
For figuring out what events should be triggered when something changes.
*/

package handWavey;

import dataCleaner.Changed;
import java.util.List;
import java.util.ArrayList;

import debug.Debug;

public class HandStateEvents {
    private Debug debug;
    private Changed zoneChanged = new Changed("OOB");
    private Changed segmentChanged = new Changed(0);
    private Changed stateChanged = new Changed(Gesture.absent);
    
    private List<String> anyChangeEvents = new ArrayList<String>();
    private List<String> enterEvents = new ArrayList<String>();
    private List<String> exitEvents = new ArrayList<String>();
    
    private Gesture gesture;
    
    private String handLetter ="p";
    
    public HandStateEvents(Boolean isPrimary) {
        this.debug = Debug.getDebug("HandStateEvents");
        
        this.handLetter = (isPrimary)?"p":"s";
        this.gesture = new Gesture();
        this.gesture.generateConfig();
    }
    
    public void setZone(String zone) {
        this.zoneChanged.set(zone);
        
        if (this.zoneChanged.hasChanged()) {
            this.debug.out(1, this.handLetter + " hand has changed zone to " + zone);
        }
    }
    
    public void setSegment(int segment) {
        this.segmentChanged.set(segment);
        
        if (this.segmentChanged.hasChanged()) {
            this.debug.out(1, this.handLetter + " hand has changed segment to " + String.valueOf(segment));
        }
    }
    
    public void setState(int state) {
        this.stateChanged.set(state);
        
        if (this.stateChanged.hasChanged()) {
            this.debug.out(1, this.handLetter + " hand has changed state to " + String.valueOf(state));
        }
    }
    
    public Boolean freshlyAbsent() {
        return (this.stateChanged.toInt() == Gesture.absent && this.stateChanged.hasChanged());
    }
    
    public Boolean somethingChanged() {
        return (this.zoneChanged.hasChanged() || this.segmentChanged.hasChanged() || this.stateChanged.hasChanged());
    }
    
    public List<String> getAnyChangeEvents() {
        return this.anyChangeEvents;
    }
    
    public List<String> getExitEvents() {
        return this.exitEvents;
    }
    
    public List<String> getEnterEvents() {
        return this.enterEvents;
    }
    
    public void deriveEvents() { // Normal use-case.
        this.anyChangeEvents = new ArrayList<String>();
        this.exitEvents = new ArrayList<String>();
        this.enterEvents = new ArrayList<String>();
        
        if (somethingChanged()) {
            this.exitEvents.add(this.gesture.gestureName(
                this.handLetter,
                this.zoneChanged.fromStr(),
                this.segmentChanged.fromInt(),
                this.stateChanged.fromInt()) + "-exit");
            
            if (nonOOBExit()) {
                this.exitEvents.add(this.gesture.gestureName(
                    this.handLetter,
                    "nonOOB",
                    this.segmentChanged.fromInt(),
                    this.stateChanged.fromInt()) + "-exit");
            }
            
            this.enterEvents.add(this.gesture.gestureName(
                this.handLetter,
                this.zoneChanged.toStr(),
                this.segmentChanged.toInt(),
                this.stateChanged.toInt()) + "-enter");
            
            if (nonOOBEnter()) {
                this.enterEvents.add(this.gesture.gestureName(
                    this.handLetter,
                    "nonOOB",
                    this.segmentChanged.fromInt(),
                    this.stateChanged.fromInt()) + "-enter");
            }
        } else {
            return; // Return quickly if nothing has changed.
        }
        
        if (this.zoneChanged.hasChanged() || reIntroduced()){
            this.anyChangeEvents.add("general-zone-" + this.handLetter + "AnyChange");
            this.exitEvents.add("general-zone-" + this.handLetter + this.gesture.capitalise(this.zoneChanged.fromStr()) + "-exit");
            this.enterEvents.add("general-zone-" + this.handLetter + this.gesture.capitalise(this.zoneChanged.toStr()) + "-enter");
        }
        
        if (this.segmentChanged.hasChanged() || reIntroduced()){
            this.anyChangeEvents.add("general-segment-" + this.handLetter + "AnyChange");
            this.exitEvents.add("general-segment-" + this.handLetter + this.segmentChanged.fromInt() + "-exit");
            this.enterEvents.add("general-segment-" + this.handLetter + this.segmentChanged.toInt() + "-enter");
        }
        
        if (this.stateChanged.hasChanged()){
            String fromState = this.gesture.capitalise(this.gesture.handState(this.stateChanged.fromInt()));
            String toState = this.gesture.capitalise(this.gesture.handState(this.stateChanged.toInt()));
            
            this.anyChangeEvents.add("general-state-" + this.handLetter + "AnyChange");
            this.exitEvents.add("general-state-" + this.handLetter + fromState + "-exit");
            this.enterEvents.add("general-state-" + this.handLetter + toState + "-enter");
        }
    }
    
    public Boolean nonOOBEnter() {
        return (this.zoneChanged.hasChanged() && this.zoneChanged.fromStr() == "OOB");
    }
    
    public Boolean nonOOBExit() {
        return (this.zoneChanged.hasChanged() && this.zoneChanged.toStr() == "OOB");
    }
    
    private Boolean reIntroduced() {
        return (this.stateChanged.hasChanged() && this.stateChanged.fromInt() == Gesture.absent);
    }
    
    public List<String> getCurrentEvents() { // For when events have been lost during a click freeze.
        List<String> result = new ArrayList<String>();
        
        result.add(this.gesture.gestureName(
            this.handLetter,
            this.zoneChanged.toStr(),
            this.segmentChanged.toInt(),
            this.stateChanged.toInt()) + "-enter");
        
        result.add("general-zone-" + this.handLetter + "AnyChange");
        result.add("general-zone-" + this.handLetter + this.gesture.capitalise(this.zoneChanged.toStr()) + "-enter");
        
        result.add("general-segment-" + this.handLetter + "AnyChange");
        result.add("general-segment-" + this.handLetter + this.segmentChanged.toInt() + "-enter");
        
        String fromState = this.gesture.capitalise(this.gesture.handState(this.stateChanged.fromInt()));
        String toState = this.gesture.capitalise(this.gesture.handState(this.stateChanged.toInt()));
        
        result.add("general-state-" + this.handLetter + "AnyChange");
        result.add("general-state-" + this.handLetter + toState + "-enter");
        
        return result;
    }
    
    public String getIndividualExitEvent() {
        return this.gesture.generateSingleHandGestureName(
            this.handLetter,
            this.zoneChanged.fromStr(),
            this.segmentChanged.fromInt(),
            this.stateChanged.fromInt());
    }
    
    public String getIndividualEnterEvent() {
        return this.gesture.generateSingleHandGestureName(
            this.handLetter,
            this.zoneChanged.toStr(),
            this.segmentChanged.toInt(),
            this.stateChanged.toInt());
    }
    
    public String getNonOOBExitEvent() {
        return this.gesture.generateSingleHandGestureName(
            this.handLetter,
            "nonOOB",
            this.segmentChanged.fromInt(),
            this.stateChanged.fromInt());
    }
    
    public String getNonOOBEnterEvent() {
        return this.gesture.generateSingleHandGestureName(
            this.handLetter,
            "nonOOB",
            this.segmentChanged.toInt(),
            this.stateChanged.toInt());
    }
}
