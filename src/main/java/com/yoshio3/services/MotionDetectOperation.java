/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.yoshio3.services;

import com.yoshio3.services.motiondetectedoperation.CameraOperation;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RCMPin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yoterada
 */
public class MotionDetectOperation {

    private volatile boolean flag;

    public MotionDetectOperation() {
        this.flag = true;
    }

    public void disable() {
        this.flag = false;
    }

    public void enable() {
        ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
        newSingleThreadExecutor.submit(() -> {
            System.out.println("Starting Pi4J Motion Sensor Example");
            // create gpio controller           
            final GpioController gpio = GpioFactory.getInstance();
            // I placed motion sensor on GPIO 4 on RasPi.
            GpioPinDigitalInput[] pins = {
                gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN)
            };

            //If motion sensor detect, it change the state to PinState.HIGH
            //Then 
            GpioPinListenerDigital listener = (GpioPinDigitalStateChangeEvent event) -> {
                if (event.getState() == PinState.HIGH) {
                    CameraOperation cameraSvc = new CameraOperation();
                    cameraSvc.takePhotoAndUploadStorage();
                }
            };

            gpio.addListener(listener, pins);
            while (flag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MotionDetectOperation.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            pins[0].setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
            gpio.shutdown();
        });
    }
}
