APP DEFINITION STATEMENT
--
"Messi" is an incoming SMS text message management mobile application to be used together with Ford Motor Company's SYNC® system on vehicle's head unit.
Messi implements Ford's SYNC® AppLink™ APIs, allowing vehicle occupants to have control of Messi's features through the use of in-vehicle Human Machine Interfaces (HMI).

ABSTRACT
--
This application runs on Android phones.
It serves as a simple temporary incoming SMS text message management application while connecting to the SYNC® system in vehicle. Any messages received during this period would not be stored in the application in a persistent manner.

********************
Messages in the application contain the following properties:

• Sender of the message

• Body of the message

• Index of the message in current message list

********************
Features: 


• Alerting user to an incoming SMS text message immediately, if no other alert is in progress, via HMI;

• Viewing a particular message’s details via HMI;

• Viewing the next message's details, via HMI;

• Reading an incoming message's body aloud via HMI;

• Quick replying to a particular message via HMI;

• Calling the sender of a particular message via HMI;

• Viewing all incoming SMS text messages in a list, sorted by receiving time, via HMI.

USAGE
---
This application requires running on an actual Android phone. Bluetooth access is essential.

BUILD REQUIREMENTS
---
API level 19 or later


RUNTIME REQUIREMENTS
---
Android 4.4 or later
