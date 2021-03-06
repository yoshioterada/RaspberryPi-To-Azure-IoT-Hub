# Intrusion detection Java App on RaspberryPi with Microsoft Azure 

![Confirmation of this demo](https://c1.staticflickr.com/1/387/32519503525_8802a14d47_z.jpg)

This Sample Appliaction get the data of celsius, pressure and humidity data from BME 280 sensor on Raspberry Pi(Raspberry Pi 2 Model B).
After receved it, the application push the data to Azure IoT Hub.I also configure the Stream Analytics and PowerBI on Azure separatelly.As a result, we can confirm the data on PowerBI dashboard.

![Power BI Dashboard](https://c1.staticflickr.com/1/328/32233128852_67c067eb60.jpg)

And also, I put the PIR motion sensor, camera and LED on RasPi. So if something had moved in front of motion sensor, it detected and turn on the LED and automatically take the phone of front of the camera. After take the photo, it upload the image to Azure Storage Blob. 
![Azure Storage Explorer](https://c1.staticflickr.com/1/613/32233420072_a1ed6889b1_c.jpg)
After uploaded the image, it send the URL to Cognitive Services as Face API and Emotion API. After received the result of analysis data, it showes the result to standard output as follow.

![Show Result](https://c1.staticflickr.com/1/541/32005905580_bf2a18799e_c.jpg)

# How to run this Sample Application
* 1. Need to create Azure IoT-Hub, Azure Storage on Azure.
* 2. Need to get the access key for both above services.
* 3. You need to create and get the Subscription ID of Cognitive Services.
* 4. Get the source code from GitHub
* 5. After get both access keys and subscription IDs, [Please Edit the property files?](https://github.com/yoshioterada/RaspberryPi-To-Azure-IoT-Hub/blob/master/src/main/resources/app-resources_ja_JP.properties "Please Edit this property?")
* 6. Build the Application
* 7. Confirm the build binary

```bash
$ cd RaspberryPi-To-Azure-IoT-Hub/target/
```  
```bash
$ ls
```
* 8. Copy the file to Raspberry Pi by scp

```bash
$ scp RaspberryPi-To-Azure-IoT-Hub-1.0-SNAPSHOT.jar  pi@192.168.1.100:/home/pi/
```
* 9. SSH Login to the Raspberry Pi

```bash
$ ssh -l pi 192.168.1.100
```
* 10. Execute the Application on Raspberry Pi

```bash
$ sudo java -jar RaspberryPi-To-Azure-IoT-Hub-1.0-SNAPSHOT.jar  
```  

# How to place the sensors on Raspberry Pi 2.
## How to place the LED

![Connect LED](https://c1.staticflickr.com/1/519/31540433654_972793cf39.jpg)

|LED | Raspberry Pi |
|:-----------|:------------|
|anode (+)|registor (Ω) |
|cathode (-) | Grand |
|registor|GPIO 1(pin12) |

There is no need to connect "5.0 VDC Power (pin2)" in this time.

## How to place the BME 280
![Connect BME 280](https://c1.staticflickr.com/1/499/32232875822_3defde6773.jpg)

|BME 280 | Raspberry Pi |
|:-----------|:------------|
|VIN|5.0 VDC Power (pin2) |
|GND|Ground(pin6)|
|SCK|GPIO 9 SCL1(I2C:pin5)|
|SDO|Ground(pin6)|
|SDI|GPIO 8 SDA1(I2C:pin3)|

## How to place the PIR Motion Sensor
![Connect PIR Motion Sensor](https://c1.staticflickr.com/1/746/32232875872_776592c39b.jpg)

|PIR Motion Sensor | Raspberry Pi |
|:-----------|:------------|
|+DC Voltage|5.0 VDC Power (pin2) |
|Output|GPIO 4(pin16)|
|GND|Ground(pin6)|

###Pleaase note :
For PIR motion sensor, you need to adjst the default sensor setting. 
1. I changed the [Retriggering setting](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/testing-a-pir) from "Low" to "High" by change the jumper pin. Please refer to the [Testing a PIR](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/testing-a-pir) of [https://learn.adafruit.com](https://learn.adafruit.com)?
2. There is two yellow adjuster on the PIR Motion sensor. One is Sensitivity Adjust and the others is Time Delay Adjust. Please adjust by using yellow adjuster for your environment ?

## Placed all sensors
![Connect Sensor to RasPi](https://c1.staticflickr.com/1/721/31540433514_2952ab6f01.jpg)