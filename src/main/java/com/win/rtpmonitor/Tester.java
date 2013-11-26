package com.win.rtpmonitor;

/**
 *
 * @author ourasov
 */
public class Tester {

    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        monitor.setSpinCount(10000000);
        monitor.startEngineVerification("Dracula", 5);
    }
}