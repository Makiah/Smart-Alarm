# The Smart Alarm Project

## Motivation
As a student, there have been countless times in which I've gone to bed, ready to get in a good 9 hours, and woken up exhausted, with just enough energy to slog through the day before hitting the sack once more.  However, there have also been countless times in which I've gone to bed expecting to wake up 7 hours later, certain of the migraine I'll have the following day, only to be proven hopelessly wrong by waking full of energy.  What gives?!

## Principle
With the advent of machine learning, computer vision has advanced leaps and bounds.  I plan to use my phone camera to  detect my movement while I sleep using OpenCV (rather than taking 8 hours of video and doing it manually), then have it respond by moving my alarm forward and backward (to a point, understandably, since I need to wake up before a certain point).  I can then use ML a second time to analyze the boost or drop or energy I receive as a result of each adjustment, and magnify or reduce that adjustment in response.  What could go wrong?!  

## Technology
I plan to develop this application first for Android, using the OpenCV vision libraries.  If it's effective/popular, I'll re-do it in Swift/Objective-C for iOS.
