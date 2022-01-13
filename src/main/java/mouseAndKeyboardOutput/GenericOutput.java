package mouseAndKeyboardOutput;

import java.io.IOException;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Point;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.DisplayMode;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class GenericOutput implements Output {
    private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private GraphicsDevice[] gs = this.ge.getScreenDevices();
    private Robot robot = null;
    private int button = 0;
    private String[] keysIKnow = {"ctrl", "alt", "shift"};
    
    public GenericOutput() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public Dimension getDesktopResolution() {
        int x = 0;
        int y = 0;
        int w = 0;
        int h = 0;
        
        for (GraphicsDevice device: this.gs) {
            GraphicsConfiguration[] configuration = device.getConfigurations();
            Rectangle screenBounds = configuration[0].getBounds();
            
            if (screenBounds.x > x) {
                x = screenBounds.x;
                w = x + screenBounds.width;
            }
            
            if (screenBounds.y > y) {
                y = screenBounds.y;
                h = y + screenBounds.height;
            }
            
            if (w == 0) {
                w = screenBounds.width;
            }
            
            if (h == 0) {
                h = screenBounds.height;
            }
        }
        
        return new Dimension(w, h);
    }
    
    public void info() {
        System.out.println("Info.");
        
        Dimension desktopResolution = getDesktopResolution();
        System.out.println("Desktop resolution: " + desktopResolution.width + " " + desktopResolution.height);
        
        Rectangle rectangle = this.ge.getMaximumWindowBounds();
        System.out.println("Primary display: " + rectangle.toString());
        
        Point centerPoint =    this.ge.getCenterPoint();
        System.out.println("    Center: " + centerPoint.toString());
        
        System.out.println("");
        
        GraphicsDevice[] gs = this.ge.getScreenDevices();
        int length = gs.length;
        String len = String.valueOf(length);
        System.out.println("Number of devices: " + len);
        
        for (int i = 0; i < gs.length; i ++) {
            String id = gs[i].getIDstring();
            System.out.println("    ID: " + id);
            
            DisplayMode dm = gs[i].getDisplayMode();
            System.out.println("        Mode via DisplayMode: " + String.valueOf(dm.getWidth()) + " " + String.valueOf(dm.getHeight()));
            
            GraphicsConfiguration[] gc = gs[i].getConfigurations();
            Rectangle gcBounds = gc[0].getBounds();
            System.out.println("        Rectangle(0): " + gcBounds.toString());
            
            System.out.println("");
        }
    }
    
    public void setPosition(int x, int y) {
        this.robot.mouseMove(x, y);
    }
    
    public void click(int button) {
        mouseDown(button);
        mouseUp(button);
    }
    
    public void doubleClick(int button) {
        click(button);
        click(button);
    }
    
    public void mouseDown(int button) {
        try {
            this.robot.mousePress(button);
            this.button = button;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    public void mouseUp(int button) {
        try {
            this.robot.mouseRelease(button);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    public int getLastMouseButton() {
        return this.button;
    }
    
    public void scroll(int amount) {
        this.robot.mouseWheel(amount);
    }
    
    public int getMouseButtonID(String buttonName) {
        int result = InputEvent.BUTTON1_MASK;
        
        // https://docs.oracle.com/javase/7/docs/api/java/awt/event/InputEvent.html
        if (buttonName.equals("left")) {
            result = InputEvent.BUTTON1_MASK;
        } else if (buttonName.equals("middle")) {
            result = InputEvent.BUTTON2_MASK;
        } else if (buttonName.equals("right")) {
            result = InputEvent.BUTTON3_MASK;
        }
        
        return result;
    }
    
    public int getKeyID(String keyName) {
        int result = 0;
        
        // From: https://docs.oracle.com/javase/7/docs/api/java/awt/event/KeyEvent.html
        switch(keyName) {
            case "ctrl":
                result = KeyEvent.VK_CONTROL;
                break;
            case "alt":
                result = KeyEvent.VK_ALT;
                break;
            case "shift":
                result = KeyEvent.VK_SHIFT;
                break;
        }
        
        return result;
    }
    
    public void keyDown(int key) {
        try {
            this.robot.keyPress(key);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    public void keyUp(int key) {
        try {
            this.robot.keyRelease(key);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    public String[] getKeysIKnow() {
        return this.keysIKnow;
    }
    
    public void sleep(int microseconds) {
        try {
            Thread.sleep(microseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public int testInt(String testName) {
        int result = 0;
        
        switch (testName) {
            case "unused":
                break;
        }
        
        return result;
    }
}
