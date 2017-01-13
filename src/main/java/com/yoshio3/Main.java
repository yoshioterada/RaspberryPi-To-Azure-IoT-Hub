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
package com.yoshio3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Yoshio Terada
 */
public class Main {

    public static void main(String... args) {
        try (BufferedReader stdReader
                = new BufferedReader(new InputStreamReader(System.in));) {

            TempAndPressureService tempSvc = new TempAndPressureService();
            tempSvc.execToGetTempAndPressure();

            LEDService ledSvc = new LEDService();
            ledSvc.blinkLED();

            String line;
            System.out.println("Enter [exit] to stop the App.");
            System.out.println("---------------------------------");
            while ((line = stdReader.readLine()) != null) {
                if (line.equals("exit")) {
                    tempSvc.disable();
                    ledSvc.disable();
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            System.exit(-1);
        }
    }
}
