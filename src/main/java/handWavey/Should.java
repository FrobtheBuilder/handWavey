package handWavey;

public class Should {
    // For tracking when key/button events should be triggered.
    
    private Boolean value = false;
    private Boolean lastValue = false;
    
    public Should(Boolean defaultValue) {
        value = defaultValue;
        lastValue = defaultValue;
    }
    
    public void set(Boolean value) {
        this.value = value;
    }
    
    public Boolean toTrue() {
        Boolean result = false;
        
        if (this.value != this.lastValue && this.value == true) {
            this.lastValue = this.value;
            result = true;
        }
        
        return result;
    }
    
    public Boolean toFalse() {
        Boolean result = false;
        
        if (this.value != this.lastValue && this.value == false) {
            this.lastValue = this.value;
            result = true;
        }
        
        return result;
    }
}