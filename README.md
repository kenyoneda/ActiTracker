# ActiTracker

An Android application for logging sensor data from a smartwatch and smartphone, primarily intended for researchers collecting activity data from which they can build a model on. Various sensors can be selected from a list of all available sensors. This depends on the model of the device. There are two modes for recording data - timed (in minutes) and manual. Once logging begins, the watch and phone display a timer/stopwatch (depending on the logging mode). Once logging is completed, there is the option to view the files.

*Note: There is a 5 second delay before the application starts collecting data. This is to allow for the researcher to place the devices in the necessary position.*

######To pull files (all data is stored on the phone):
Connect phone via USB to computer.
\*OSX users will need to install Android File Transfer. 
https://www.android.com/filetransfer/

######Available Settings:
* Selection of available sensors from the phone
* Selection of available sensors from the watch
* Sampling Rate
* Filename Formatting

######Troubleshooting:
If the app is not working as intended, ensure the following applications are up to date:
* Android Wear
* Google Play Services (Play Store -> Settings -> Tap on Build Version)
* Google Application (App with the Google "G" logo)

and the following settings are set on the phone:
* Turn `OFF` tilt to wake screen (Android Wear -> Settings Icon -> G Watch)
* Turn `ON` always-on screen (Android Wear -> Settings Icon -> G Watch)

and the following settings are set on the watch:
* Turn `OFF` wrist gestures (Settings -> Gestures)
* `DISABLE` stay awake when charging (Settings -> Developer Options)

Please report issues to GitHub or email comments and suggestions to kyoneda at fordham.edu

This application is licensed under the MIT license.
