---
title: check_style
tags:
---


http://gudong.name/2016/04/07/checkstyle.html

http://www.jianshu.com/p/fc2f45a9ee37


https://github.com/ribot/android-boilerplate


如何避免CheckStyle插件检查指定包下的Java文件 http://checkstyle.sourceforge.net/config_filters.html#SuppressionFilter

http://stackoverflow.com/questions/1012407/how-to-suppress-all-checks-for-a-file-in-checkstyle
http://stackoverflow.com/questions/4020478/disable-all-checkstyle-checks-for-a-specific-java-package

checkstyle-config.xml中增加以下配置

```xml
    <module name="SuppressionFilter">
        <property name="file" value="config/suppression.xml"/>
    </module>
```

`config/suppression.xml`的内容如下

```xml
<?xml version="1.0"?>

<!DOCTYPE suppressions PUBLIC
    "-//Puppy Crawl//DTD Suppressions 1.1//EN"
    "http://www.puppycrawl.com/dtds/suppressions_1_1.dtd">

<suppressions>
    <suppress checks=".*" files="com[\\/]tencent[\\/]PmdCampus[\\/]model[\\/]"/>
</suppressions>
```

如何避免Gradle CheckStyle检查指定包下的Java文件

增加 exclude 配置


```
task checkstyle(type: Checkstyle, group: 'Verification', description: 'Runs code style checks') {
    configFile file("$qualityConfigDir/checkstyle/checkstyle-config.xml")
    source 'src'
    include '**/*.java'
    exclude 'com/tencent/PmdCampus/model/*.java'

    reports {
        xml.enabled = true
        xml {
            destination "$reportsDir/checkstyle/checkstyle.xml"
        }
    }

    classpath = files( )
}
```