---
layout: post
title: "Reading and Writing Logs"
date: 2015-12-18 17:33:06 +0800
comments: true
categories: android 翻译
published: false
---
本文介绍了如何读写Android日志。翻译自[Reading and Writing Logs](http://developer.android.com/intl/zh-cn/tools/debugging/debugging-log.html)
<!--more-->

Android日志系统提供收集和查看系统调试日志的机制。Logcat会输出系统信息，包括模拟器或设备出错时的栈信息以及应用通过`Log`类输出的信息。可以通过ADB或DDMS运行Logcat，Logcat允许实时查看日志。

# Log类
`Log`是日志类，可在代码中用于输出信息到LogCat。常用的输出日志方法包括：

+ `v(String, String)` (verbose)
+ `d(String, String)` (debug)
+ `i(String, String)` (information)
+ `w(String, String)` (warning)
+ `e(String, String)` (error)

比如：

```
Log.i("MyActivity", "MyClass.getView() — get item number " + position);
```

LogCat会输出类似这样的信息：

```
I/MyActivity( 1557): MyClass.getView() — get item number 1
```

# 使用LogCat
可以在DDMS中使用LogCat或在ADB shell中调用它。如何在DDMS中使用LogCat请参考[Using DDMS][Using DDMS]。ADB shell中运行LogCat的通常方法是：

```
[adb] logcat [<option>] ... [<filter-spec>] ...
```

可以从开发机或模拟器或设备的远程adb shell上使用`logcat`命令。在开发机上查看日志输出，可使用：

```
$ adb logcat
```

从远程adb shell则使用：

```
# logcat
```

下表描述了`logcat`命令行选项：

|   |   |
|---|---|
|-c |清空缓冲区并退出|
|-d |输出日志并退出|
|-g |打印日志缓冲区大小并退出|
|-n <count>|Sets the maximum number of rotated logs to <count>. The default value is 4. Requires the -r option.
|-r <kbytes>|Rotates the log file every <kbytes> of output. The default value is 16. Requires the -f option.
|-f <filename>|将日志写入到<filename>。缺省写入到`stdout`|
|-s  |缺省不输出日志|
|-v <format>|设置日志输出格式。缺省是`brief`格式|

# 过虑日志
每条Android日志信息有一个 *tag* 和一个 *priority*。

+ tag - 一个字符串，用于表示日志从哪个系统组件产生(比如，"View"表示来自view system)
+ priority - 下列某个字符，优先级从低到高：
 + `V` - Verbose(最低优先级)
 + `D` - Debug
 + `I` - Info
 + `W` - Warning
 + `E` - Error
 + `F` - Fatal
 + `S` - Silent(最高优先级，这种优先级不输出任何信息)

运行LogCat并观察每条日志信息的前两列(分别是`<priority>/<tag>`)，你可以得到系统使用的tag和priority。

下面是logcat输出的一个例子，priority是"I"，tag是"Activitymanager"：

```
I/ActivityManager(  585): Starting activity: Intent { action=android.intent.action...}
```

为减少日志输出以便于控制，可以使用 *filter expressions* 以限制日志。 使用日志过滤器可以让系统只输出那些你感兴趣的日志，而忽略其他日志。

过滤器格式是: `tag:priority ...`，`tag`表示感兴趣的tag，而`priority`表示这个tag下输出的日志的最低优先级。Messages for that tag at or above the specified priority are written to the log。可以在一个过滤器指定提供任意数量的`tag:priority`，以空格分隔。

下面的例子只输出"ActivityManager"对应的日志，级别为"Info"及以上，以及"MyApp"对应的日志，级别为"Debug"及以上：

```
adb logcat ActivityManager:I MyApp:D *:S
```

上述过滤器最后的`*:S`，将所有tag的级别设置为"silent"，以保证只有"ActivityManager"和"MyApp"对应的日志才输出。使用"*:S"是保证只输出你明确指定的过滤器下的日志的好办法——你的过滤器变成了"白名单"。

下面的过滤器表达器显示所有的tag上的"warning"及以上级别的日志：

```
adb logcat *:W
```

如果你在开发机上运行LogCat(而不是在远程adb shell上运行)，也可以通过导入`ANDROID_LOG_TAGS`环境变量的方式设置缺省的过滤表达器：

```
export ANDROID_LOG_TAG="ActivityManager:I MyApp:D *:S"
```

注意远程shell或使用`adb shell logcat`时`ANDROID_LOG_TAGS`并不可用。

# 控制日志格式
除了tag和priority，日志信息中还包含若干元数据字段。使用`-v`参数，可以修改日志输出格式以显示特定的元数据字段。

+ `brief` - 显示priority/tag和生成日志的进程的PID(缺省的格式)
+ `process` - 只显示PID
+ `tag` - 只显示priority/tag
+ `raw` - 显示原始的日志信息，没有其他元数据字段
+ `time` - 显示日期，时间，priority/tag，生成日志的进程的PID
+ `threadtime` - 显示日期，时间，priority,tag，以及生成日志的线程的PID和TID
+ `long` - 显示所有的元数据信息并以空行分隔信息

启动LogCat时，可以用以下方式指定你期望的输出格式：

```
[adb] logcat [-v <format>]
```

以下例子演示如何使用`thread`格式输出日志信息：

```
adb logcat -v thread
```

注意，只能使用`-v`参数指定一种输出格式。

# 查看日志缓冲区
Android日志系统保留多个环形日志缓冲区，并不是所有的日志都发往缺省的缓冲区。想查看其他的日志信息，可以使用`-b`参数运行`logcat`命令来查看另外的日志缓冲区。包括以下几种：

+ `radio` - 查看包括radio/telephony相关信息的日志缓冲区
+ `events` - 查看包括事件相关的信息的日志缓冲区
+ `main` - 查看主日志缓冲区(缺省)

`-b`参数的用法如下：

```
[adb] logcat [-b <buffer>]
```

下面这个例子演示了如何查看radio和telephony相关的日志：

```
adb logcat -b radio
```

# 查看stdout和stderr
缺省时Android系统将`stdout`和`stderr`(`System.out`和`System.err`)的输出发送到`/dev/null`。在运行Davik VM的进程中，可以让系统将这些日志输出到日志文件中。这种情况下，系统使用`stdout`和`stderr`输出所有的`I`级别的日志。

To route the output in this way, you stop a running emulator/device instance and then use the shell command setprop to enable the redirection of output. Here's how you do it:

```
$ adb shell stop
$ adb shell setprop log.redirect-stdio true
$ adb shell start
```

系统会一直保持以上设置直到结束模拟器/设备实例。想将其作为默认值的话，可以在设备的`/data/local.prop`添加相应设置。

# 调试网页
略

[Using DDMS]: http://developer.android.com/tools/debugging/ddms.html#logcat