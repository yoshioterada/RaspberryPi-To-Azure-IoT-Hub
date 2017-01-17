/*
* Copyright 2017 Yoshio Terada
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.yoshio3.services;

import com.yoshio3.services.detectedoperation.CameraOperation;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.yoshio3.services.detectedoperation.FaceAndEmotionalDetectService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yoshio Terada
 */
public class MotionDetectOperation {

    private final static GpioController GPIO = GpioFactory.getInstance();
    private final static GpioPinDigitalOutput LED_SENSOR = GPIO.provisionDigitalOutputPin(RaspiPin.GPIO_01, "GPIO_01", PinState.LOW);
    private final static GpioPinDigitalInput PIR_MOTION_SENSOR = GPIO.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN);

    public void detectMotion() {
        ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
        newSingleThreadExecutor.submit(() -> {
            // create GPIO controller           
            LED_SENSOR.setShutdownOptions(true, PinState.LOW);

            //If motion sensor detect, it change the state to PinState.HIGH
            PIR_MOTION_SENSOR.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
                System.out.println("Motion State Changed : " + event.getState() + ":" + event.getState().getValue());
                boolean onFlag = true;
                if (event.getState() == PinState.HIGH) {
                    CameraOperation cameraSvc = new CameraOperation();
                    try {
                        //LED TURN ON
                        ExecutorService insideThread = Executors.newSingleThreadExecutor();
                        insideThread.submit(() -> {
                            LED_SENSOR.high();
                            LED_SENSOR.pulse(4000, true);
                        });
                        
                        //Take Photo and upload image to Azure Storage
                        String pictURL = cameraSvc.takePhotoAndUploadStorage();

                        //Analysis the image by Cognitive Service(Face&Emotion Detect)
                        FaceAndEmotionalDetectService face = new FaceAndEmotionalDetectService();
                        face.showFaceAndEmotionInfoAsync(pictURL);
                        
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(MotionDetectOperation.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (event.getState() == PinState.LOW) {
                    LED_SENSOR.low();
                }
            });
        });
    }

    public void shutdownMotionDetect() {
        GPIO.shutdown();
    }
}
