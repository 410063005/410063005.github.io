---
title: bug-notificaion-bar-icon
tags:
 - android
---

> I have an app showing custom notifications. The problem is that when running in Android 5 the small icon in the Notification bar shows white. How can I fix this?

我的应用会弹出自定义notification。问题是，当应用运行在Android 5.0和Android 6.0上时notification中的small icon会变成白色的

更正：确认这个问题出现在Android 5.0以及之后的版本上，包括Android 6.0和Android 7.0。提上述问题时，Android 5.0是当时最新的版本。

更正：更准确地说这个问题出现在API Level 21 (Android 5.0), API Level 22 (Android 5.1), API Level 23 (Android 6.0)等等。我在API Level 22和API Level 23上测试过notifcaion small icon，发现它们有所不同。

![](notification_small_icon.jpg)

解决方法：将targetVersion从23改成20后，问题解决！

但显然我们不能这么处理

---

-

# Notification规范
[android 5.0的变化](https://developer.android.com/about/versions/android-5.0-changes.html)

> alpha-channel is the only data of the image that Android uses for notification icons:
> + alpha == 1: pixels show white
> + alpha == 0: pixels show as the color you chose at Notification.Builder#setColor(int)

[规范参考](https://material.io/guidelines/patterns/notifications.html#notifications-anatomy-of-a-notification)




[ref]: http://stackoverflow.com/questions/28387602/notification-bar-icon-turns-white-in-android-5-lollipop
[ref2]: http://stackoverflow.com/questions/30795431/icon-not-displaying-in-notification-white-square-shown-instead/33608653#33608653