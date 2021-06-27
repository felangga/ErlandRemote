# ErlandRemote - Arduino IR Blaster 
Some android devices included with IR Blaster but my phone doesn't, so I decided to build one 
with Arduino. This IR Blaster controlled using bluetooth from ESP32. ESP32 is powerfull and cheap too, 
so I choose this option aside from using Arduino UNO with Bluetooth module. 

Basically the hardware is very simple, you only need 3 components : 
- ESP32 DEV KIT
- IR Transmitter
- IR Receiver

I'm using parts from broken TV sensor which has 3 legs pin for the receiver sensor. 
Connect IR Receiver to pin 14 and IR Transmitter to pin 4. If your IR Transmitter need 5V, 
you can use transistor to switch from 5V because ESP32 is using 3.3V
![Schematic](https://github.com/felangga/ErlandRemote/blob/master/Schematic.PNG)

For the apps you can compile yourself using AndroidStudio which support from 
Android 4.4 to Android 10 (currently tested)

![Apps](https://github.com/felangga/ErlandRemote/blob/master/apps.jpg)

If you have any question or found some bugs please open issue above. 

Regards,
Felangga
