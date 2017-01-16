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
package com.yoshio3.services.motiondetectedoperation;

import com.hopding.jrpicam.RPiCamera;
import com.hopding.jrpicam.enums.AWB;
import com.hopding.jrpicam.enums.DRC;
import com.hopding.jrpicam.enums.Encoding;
import com.hopding.jrpicam.exceptions.FailedToRunRaspistillException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yoterada
 */
public class CameraOperation {

    private final static Logger LOGGER = Logger.getLogger(CameraOperation.class.getName());
    private volatile boolean flag;
    private RPiCamera piCamera;
    private final static int INTERVAL = 1; //1 sec

    public CameraOperation() {
        try {
            String saveDir = "/home/pi/Pictures";
            piCamera = new RPiCamera(saveDir);
            flag = true;
        } catch (FailedToRunRaspistillException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    public void disable() {
        this.flag = false;
    }

    public void takePhotoAndUploadStorage() {
        ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
        newSingleThreadExecutor.submit(() -> {
            try {
                String fileName = takePhotoAndGetFileName();
                Path upFileBytes = Paths.get(fileName);
                StorageService storage = new StorageService();
                storage.uploadFile(Files.readAllBytes(upFileBytes), upFileBytes.toString());
                System.out.println(upFileBytes.toString() + " is uploaded.");
                Files.delete(upFileBytes);
                Thread.sleep(INTERVAL * 1000);
            } catch (IOException | InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        });
    }

    private String takePhotoAndGetFileName() {
        piCamera.setAWB(AWB.AUTO); // Change Automatic White Balance setting to automatic
        piCamera.setDRC(DRC.OFF); // Turn off Dynamic Range Compression
        piCamera.setISO(400);
        piCamera.setWidth(512);
        piCamera.setHeight(384);
        piCamera.setContrast(100); // Set maximum contrast
        piCamera.setSharpness(100); // Set maximum sharpness
        piCamera.setQuality(75); // Set maximum quality
        piCamera.setTimeout(1000); // Wait 1 second to take the image
        piCamera.turnOnPreview(); // Turn on image preview
        piCamera.setEncoding(Encoding.PNG); // Change encoding of images to PNG
        try {
            //Create new file name
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            UUID uuid = UUID.randomUUID();
            String fileName = currentDateTime + "_" + uuid.toString() + ".png";

            File takeStill = piCamera.takeStill(fileName, 512, 384);
            return takeStill.getAbsolutePath();
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "";
        }
    }
}
