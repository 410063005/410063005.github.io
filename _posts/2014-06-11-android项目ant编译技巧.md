---
layout: post
title: android项目ant编译技巧
keywords: Android
description: Android小技巧
categories: [Android]
tags: [Android]
group: archive
icon: globe
---
我们知道，可以通过如下设置将一个普通的Android工程转换成Android Library工程

![android_library_project]({{ site.url }}/assets/20140612/android_library_project.PNG)

设置前后工程变化如下

![project_properties]({{ site.url }}/assets/20140612/project_properties.PNG)

使用Ant编译时(通过android.bat update project 命令生成 build.xml)，普通的Android工程会生成apk文件，而Android Library工程只生成jar文件。由于要生成dex并打包apk资源，前者比后者要耗时不少。

有时我们需要从普通工程中导出部分代码生成jar包，可以手动完成

![export_jar]({{ site.url }}/assets/20140612/export_jar.png)

或者，按照上面的做法，先将一个普通的Android工程转换成Android Library工程，再执行 ant release 或 ant debug

其实，还有一种更简单地方法：使用如下方式执行 ant release 

	ant release -Dandroid.library=true
	
编译后将生成一个classes.jar，编译输出如下

	...
	-compile:
	      [jar] Building jar: F:\xxx\bin\classes.jar
	
	-post-compile:
	
	-obfuscate:
	
	-dex:
	     [echo] Library project: do not convert bytecode...
	...
	
最后，我们还可以通过添加一个 custom_rules.xml 文件来对生成的jar文件进行更灵活地控制：

	<?xml version="1.0" encoding="UTF-8"?>
	<project name="tinyUtils" default="help">
	    <target name="-post-compile">
	        <!-- copy from <sdk>\tools\ant\build.xml '-compile' task -->
	        <if condition="${project.is.library}">
	            <then>
	                <echo level="info">Creating my library output jar file...</echo>
	                <property name="out.mylibrary.jar.file" location="${out.absolute.dir}/my_classes.jar" />
	                <if>
	                    <condition>
	                        <length string="${android.package.excludes}" trim="true" when="greater" length="0" />
	                    </condition>
	                    <then>
	                        <echo level="info">Custom jar packaging exclusion: ${android.package.excludes}</echo>
	                    </then>
	                </if>
	
	                <propertybyreplace name="project.app.package.path" input="${project.app.package}" replace="." with="/" />
	
	                <jar destfile="${out.mylibrary.jar.file}">
						<!-- 自定义 -->
	                </jar>
	            </then>
	        </if>
	    </target>
	</project>

## 总结
1. ant命令中添加 `-Dandroid.library=true` 参数从普通Android项目中导出jar包
2. 添加一个 custom_rules.xml 文件来对生成的jar文件进行更灵活地控制