package org.example.ClassiUtente;

import java.util.Timer;

public class TimerHandler {

    private Timer timer;
    private boolean reset;

    public TimerHandler(Timer timer, boolean reset) {
        timer = new Timer();
        reset = false;
    }


}
