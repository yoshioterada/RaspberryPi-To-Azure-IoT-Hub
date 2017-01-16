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

import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import com.microsoft.azure.iothub.IotHubMessageResult;
import com.microsoft.azure.iothub.IotHubStatusCode;
import com.microsoft.azure.iothub.Message;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.yoshio3.Main;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

/**
 *
 * @author Yoshio Terada
 */
public class TempAndPressureService {

    private static final String DEVICE_ID = "yoshio-raspi";
    private final static int BME280_ADDRESS = 0x76;
    private static final String CONNECTION_STRING = "HostName=yoshio3-iot-hub.azure-devices.net;DeviceId=" + DEVICE_ID + ";SharedAccessKey=2GGgEBpDhgdps3YLhcNkzUnLOiZIe/Tt2hgebMj+UIg=";
    private volatile boolean flag;

    public TempAndPressureService() {
        this.flag = true;
    }

    public void disable() {
        this.flag = false;
    }

    public void execToGetTempAndPressure() throws IOException {

        ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
        newSingleThreadExecutor.submit(() -> {
            //Create instance of Microsoft Azure IoT Hub Client.
            //To avoid the creation of multiple DeviceClient instances and invoke open(),
            //it is needed to write the following code outside of while statement.
            try (DeviceClient client = new DeviceClient(CONNECTION_STRING, IotHubClientProtocol.AMQPS)) {
                client.setMessageCallback((Message message, Object object) -> {
                    return IotHubMessageResult.COMPLETE;
                }, null);
                client.open();

                //Data Sheet of the BME 280
                //https://s3.amazonaws.com/controleverything.media/controleverything/Production%20Run%204/14_BME280_I2CS/Datasheets/BME280.pdf
                // Create I2C bus
                I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
                // Get I2C device, BME280 I2C address is 0x76(108)
                I2CDevice device = bus.getDevice(BME280_ADDRESS);
                while (flag) {
                    // Read 24 bytes of data from address 0x88(136)
                    byte[] calibration = new byte[24];
                    device.read(0x88, calibration, 0, 24);
                    // Read 8 bytes of data from address 0xF7(247)
                    // pressure msb1, pressure msb, pressure lsb, temp msb1, temp msb, temp lsb, humidity lsb, humidity msb
                    byte[] data = new byte[8];
                    device.read(0xF7, data, 0, 8);

                    double celsius = getCelsiusValue(device, calibration, data);
                    double pressure = getPressureValue(device, calibration, data);
                    double humidity = getHumidityValue(device, calibration, data);

                    pushTempDataToAzureIoTHub(client, celsius, pressure, humidity);
                    Thread.sleep(20000);
                }
                bus.close();
            } catch (IOException | InterruptedException | URISyntaxException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        );
    }

    /* push the data to Azure IoT Hub*/
    private void pushTempDataToAzureIoTHub(DeviceClient client, double celsius, double pressure, double humidity) throws IOException, URISyntaxException {
        //Create JSON messages for sending IoT Hub
        
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        JsonObject jsonValue = Json.createObjectBuilder()
                .add("date", currentDateTime)
                .add("device", "DEVICE_ID")
                .add("celsius", celsius)
                .add("pressure", pressure)
                .add("humidity", humidity)
                .build();
        //Create message for Azure IoT Hub
        Message message = new Message(jsonValue.toString());
        message.setExpiryTime(5000);

        //Send async message to Azure IoT Hub
        client.sendEventAsync(message, (IotHubStatusCode status, Object context) -> {
            if(!(status == IotHubStatusCode.OK || status == IotHubStatusCode.OK_EMPTY)){
                Logger.getLogger(TempAndPressureService.class.getName()).log(Level.SEVERE, "Failed due to :{0} : {1}", new Object[]{status.name(), context});
            }
        }, jsonValue.toString());
                    System.out.println("日時(Date&Time) : " + currentDateTime);
                    System.out.printf("温度(Celsius) : %.2f ℃ %n", celsius);
                    System.out.printf("気圧(Pressure) : %.2f hPa %n", pressure);
                    System.out.printf("相対湿度(Humidity) : %.2f %% RH %n", humidity);
                    System.out.println("---------------------------------");
        
    }

    /* Get Celsius value from BME 280 on Raspbery Pi */
    private double getCelsiusValue(I2CDevice device, byte[] calibration, byte[] data) throws IOException {
        int dig_T1 = (calibration[0] & 0xFF) + ((calibration[1] & 0xFF) * 256);

        int dig_T2 = (calibration[2] & 0xFF) + ((calibration[3] & 0xFF) * 256);
        if (dig_T2 > 32767) {
            dig_T2 -= 65536;
        }
        int dig_T3 = (calibration[4] & 0xFF) + ((calibration[5] & 0xFF) * 256);
        if (dig_T3 > 32767) {
            dig_T3 -= 65536;
        }
        long adc_t = (((long) (data[3] & 0xFF) * 65536) + ((long) (data[4] & 0xFF) * 256) + (long) (data[5] & 0xF0)) / 16;
        double var1 = (((double) adc_t) / 16384.0 - ((double) dig_T1) / 1024.0) * ((double) dig_T2);
        double var2 = ((((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)
                * (((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)) * ((double) dig_T3);
        double t_fine = (long) (var1 + var2);
        double celsius = (var1 + var2) / 5120.0;
        return celsius;
    }

    /* Get Pressure value from BME 280 on Raspbery Pi */
    private double getPressureValue(I2CDevice device, byte[] calibration, byte[] data) throws IOException {
        int dig_T1 = (calibration[0] & 0xFF) + ((calibration[1] & 0xFF) * 256);

        int dig_T2 = (calibration[2] & 0xFF) + ((calibration[3] & 0xFF) * 256);
        if (dig_T2 > 32767) {
            dig_T2 -= 65536;
        }
        int dig_T3 = (calibration[4] & 0xFF) + ((calibration[5] & 0xFF) * 256);
        if (dig_T3 > 32767) {
            dig_T3 -= 65536;
        }

        // Pressure coefficients
        int dig_P1 = (calibration[6] & 0xFF) + ((calibration[7] & 0xFF) * 256);
        int dig_P2 = (calibration[8] & 0xFF) + ((calibration[9] & 0xFF) * 256);
        if (dig_P2 > 32767) {
            dig_P2 -= 65536;
        }
        int dig_P3 = (calibration[10] & 0xFF) + ((calibration[11] & 0xFF) * 256);
        if (dig_P3 > 32767) {
            dig_P3 -= 65536;
        }
        int dig_P4 = (calibration[12] & 0xFF) + ((calibration[13] & 0xFF) * 256);
        if (dig_P4 > 32767) {
            dig_P4 -= 65536;
        }
        int dig_P5 = (calibration[14] & 0xFF) + ((calibration[15] & 0xFF) * 256);
        if (dig_P5 > 32767) {
            dig_P5 -= 65536;
        }
        int dig_P6 = (calibration[16] & 0xFF) + ((calibration[17] & 0xFF) * 256);
        if (dig_P6 > 32767) {
            dig_P6 -= 65536;
        }
        int dig_P7 = (calibration[18] & 0xFF) + ((calibration[19] & 0xFF) * 256);
        if (dig_P7 > 32767) {
            dig_P7 -= 65536;
        }
        int dig_P8 = (calibration[20] & 0xFF) + ((calibration[21] & 0xFF) * 256);
        if (dig_P8 > 32767) {
            dig_P8 -= 65536;
        }
        int dig_P9 = (calibration[22] & 0xFF) + ((calibration[23] & 0xFF) * 256);
        if (dig_P9 > 32767) {
            dig_P9 -= 65536;
        }

        long adc_t = (((long) (data[3] & 0xFF) * 65536) + ((long) (data[4] & 0xFF) * 256) + (long) (data[5] & 0xF0)) / 16;
        long adc_p = (((long) (data[0] & 0xFF) * 65536) + ((long) (data[1] & 0xFF) * 256) + (long) (data[2] & 0xF0)) / 16;

        double var1 = (((double) adc_t) / 16384.0 - ((double) dig_T1) / 1024.0) * ((double) dig_T2);
        double var2 = ((((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)
                * (((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)) * ((double) dig_T3);
        double t_fine = (long) (var1 + var2);

        var1 = ((double) t_fine / 2.0) - 64000.0;
        var2 = var1 * var1 * ((double) dig_P6) / 32768.0;
        var2 = var2 + var1 * ((double) dig_P5) * 2.0;
        var2 = (var2 / 4.0) + (((double) dig_P4) * 65536.0);
        var1 = (((double) dig_P3) * var1 * var1 / 524288.0 + ((double) dig_P2) * var1) / 524288.0;
        var1 = (1.0 + var1 / 32768.0) * ((double) dig_P1);
        double p = 1048576.0 - (double) adc_p;
        p = (p - (var2 / 4096.0)) * 6250.0 / var1;
        var1 = ((double) dig_P9) * p * p / 2147483648.0;
        var2 = p * ((double) dig_P8) / 32768.0;
        double pressure = (p + (var1 + var2 + ((double) dig_P7)) / 16.0) / 100;
        return pressure;
    }

    /* Get Humidity value from BME 280 on Raspbery Pi */
    private double getHumidityValue(I2CDevice device, byte[] calibration, byte[] data) throws IOException {
        int dig_T1 = (calibration[0] & 0xFF) + ((calibration[1] & 0xFF) * 256);

        int dig_T2 = (calibration[2] & 0xFF) + ((calibration[3] & 0xFF) * 256);
        if (dig_T2 > 32767) {
            dig_T2 -= 65536;
        }
        int dig_T3 = (calibration[4] & 0xFF) + ((calibration[5] & 0xFF) * 256);
        if (dig_T3 > 32767) {
            dig_T3 -= 65536;
        }
        // Read 1 byte of data from address 0xA1(161)
        int dig_H1 = ((byte) device.read(0xA1) & 0xFF);

        // Read 7 bytes of data from address 0xE1(225)
        device.read(0xE1, calibration, 0, 7);

        // Convert the data
        // humidity coefficients
        int dig_H2 = (calibration[0] & 0xFF) + (calibration[1] * 256);
        if (dig_H2 > 32767) {
            dig_H2 -= 65536;
        }
        int dig_H3 = calibration[2] & 0xFF;
        int dig_H4 = ((calibration[3] & 0xFF) * 16) + (calibration[4] & 0xF);
        if (dig_H4 > 32767) {
            dig_H4 -= 65536;
        }
        int dig_H5 = ((calibration[4] & 0xFF) / 16) + ((calibration[5] & 0xFF) * 16);
        if (dig_H5 > 32767) {
            dig_H5 -= 65536;
        }
        int dig_H6 = calibration[6] & 0xFF;
        if (dig_H6 > 127) {
            dig_H6 -= 256;
        }

        // Select control humidity register
        // Humidity over sampling rate = 1
        device.write(0xF2, (byte) 0x01);
        // Select control measurement register
        // Normal mode, temp and pressure over sampling rate = 1
        device.write(0xF4, (byte) 0x27);
        // Select config register
        // Stand_by time = 1000 ms
        device.write(0xF5, (byte) 0xA0);

        // Convert pressure and temperature data to 19-bits
        long adc_p = (((long) (data[0] & 0xFF) * 65536) + ((long) (data[1] & 0xFF) * 256) + (long) (data[2] & 0xF0)) / 16;

        long adc_t = (((long) (data[3] & 0xFF) * 65536) + ((long) (data[4] & 0xFF) * 256) + (long) (data[5] & 0xF0)) / 16;

        // Convert the humidity data
        long adc_h = ((long) (data[6] & 0xFF) * 256 + (long) (data[7] & 0xFF));

        // Temperature offset calculations
        double var1 = (((double) adc_t) / 16384.0 - ((double) dig_T1) / 1024.0) * ((double) dig_T2);
        double var2 = ((((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)
                * (((double) adc_t) / 131072.0 - ((double) dig_T1) / 8192.0)) * ((double) dig_T3);
        double t_fine = (long) (var1 + var2);

        // Humidity offset calculations
        double var_H = (((double) t_fine) - 76800.0);
        var_H = (adc_h - (dig_H4 * 64.0 + dig_H5 / 16384.0 * var_H)) * (dig_H2 / 65536.0 * (1.0 + dig_H6 / 67108864.0 * var_H * (1.0 + dig_H3 / 67108864.0 * var_H)));
        double humidity = var_H * (1.0 - dig_H1 * var_H / 524288.0);
        if (humidity > 100.0) {
            humidity = 100.0;
        } else if (humidity < 0.0) {
            humidity = 0.0;
        }
        return humidity;
    }
}
