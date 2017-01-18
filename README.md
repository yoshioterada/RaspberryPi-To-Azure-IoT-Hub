# RaspberryPi Java IoT Sample with Microsoft Azure 

![Confirmation of this demo](https://c1.staticflickr.com/1/381/31540920304_a8372eb32c_z.jpg)

This Sample Appliaction get the data of celsius, pressure and humidity data from BME 280 sensor on Raspberry Pi(Raspberry Pi 2 Model B).
After receved it, the application push the data to Azure IoT Hub.I also configure the Stream Analytics and PowerBI on Azure separatelly.As a result, we can confirm the data on PowerBI dashboard.

![Power BI Dashboard](https://c1.staticflickr.com/1/328/32233128852_67c067eb60.jpg)

And also, I put the PIR motion sensor, camera and LED on RasPi. So if something had moved in front of motion sensor, it detected and turn on the LED and automatically take the phone of front of the camera. After take the photo, it upload the image to Azure Storage Blob. After uploaded the image, it send the URL to Cognitive Services as Face API and Emotion API. After received the result of analysis data, it showes the result to standard output as follow.

![Show Result](https://c1.staticflickr.com/1/541/32005905580_bf2a18799e_c.jpg)

# How to place the sensors on Raspberry Pi 2.
![Connect LED](https://c1.staticflickr.com/1/519/31540433654_972793cf39.jpg)

![Connect BME 280](https://c1.staticflickr.com/1/499/32232875822_3defde6773.jpg)

![Connect PIR Motion Sensor](https://c1.staticflickr.com/1/746/32232875872_776592c39b.jpg)

![Connect Sensor to RasPi](https://c1.staticflickr.com/1/721/31540433514_2952ab6f01.jpg)
