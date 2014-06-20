---
layout: post
title: 从web页面上启动android应用
keywords: Android
description: Android小技巧
categories: [Android,备忘]
tags: [Android]
group: archive
icon: globe
---
如果android手机上已经安装了某个app，我们有时候会希望可以从web页面上调起这个app。比如，从网页版本的地图启动地图app。虽然这不是一个常见的需求，但这个调起方式挺有意思，可以结合web页和app各自的优点，所以这里做一个备忘。

最基本的调起其实非常简单。首先，在app的manifest中定义自己的scheme，不妨定义为`my.special.scheme`。代码如下：

	<intent-filter>
		<data android:scheme="my.special.scheme" />
		<action android:name="android.intent.action.VIEW" />
		<category android:name="android.intent.category.DEFAULT"/>
		<category android:name="android.intent.category.BROWSABLE"/>
	</intent-filter>

然后在服务器上编写一个静态页面，代码如下：

	<!DOCTYPE html>
	<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
	<html>
		<head>
			<title>launch android app</title>
		</head>
		<body>
			<a href="my.special.scheme://other/parameters/here">launch_app</a>
		</body>
	</html>

页面截图：

![launch_app]({{ site.url }}/assets/20140620/launch_app.png)

点击链接后，如果安装了app，系统会启动相应的app

参考:

1. [Launch custom android application from android browser](http://stackoverflow.com/questions/2958701/launch-custom-android-application-from-android-browser)
2. [Make a link in the Android browser start up my app](http://stackoverflow.com/questions/3469908/make-a-link-in-the-android-browser-start-up-my-app)


[代码]({{ site.url }}/assets/20140620/src.zip)
