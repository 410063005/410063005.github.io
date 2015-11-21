---
layout: post
title: "Gradle生成Jar文件"
date: 2015-11-19 19:34:19 +0800
comments: true
categories: gradle
published: true
---
Android Stuio中有一个Android Library Project，如何从这个Library Project生成jar文件呢(对，是jar而不是aar)。这个Android Library Project又依赖于另一个Java Library Project，如果我希望从Android Library Project生成jar文件包括来自其依赖的Java Library Project中的class，又该如何操作呢?
<!-- more -->

# 项目结构
项目结构如下

```
ProjectName
    \- andlib
    |   \- src
    |       \- main
    |           \- java
    \- libraries
        \- javalib
            \- src
                \- main
                    \- java
```

`andlib`这个module除依赖`:libraries:javalib`还依赖其他一些库

```
dependencies {
    compile fileTree(include: ['*.jar'], dir: 'libs')
    compile 'com.android.support:appcompat-v7:23.0.1'
	...
    compile project(':libraries:javalib')
}
```

# 生成jar

这篇[帖子][topic-1]和这篇[帖子][topic-2]中，有人给出了一个不错的方法：

```
def jarName = 'someJarName.jar'

task clearJar(type: Delete) {
    delete "${project.buildDir}/libs/" + jarName
}

task makeJar(type: Copy) {
    from("${project.buildDir}/intermediates/bundles/release/")
    into("${project.buildDir}/libs/")
    include('classes.jar')
    rename('classes.jar', jarName)
}

makeJar.dependsOn(clearJar, build)
```

但是最终生成的jar文件中只包含andlib项目中的class文件。(我想在同一个jar文件中加入`:libraries:javalib`中的class文件，以免作为库使用时依赖多个jar文件)

# FatJar
很自然的想法是找一种方法，能将jar文件的依赖文件也打进去。[Google](https://www.google.com.hk/#newwindow=1&safe=strict&q=gradle+fatjar+)搜索了一下，找到[这个方法][fat-jar]。

```
// Include dependent libraries in archive.
mainClassName = "com.company.application.Main"

jar {
  manifest { 
    attributes "Main-Class": "$mainClassName"
  }  

  from {
    configurations.compile.collect { it.isDirectory() ? it : zipTree(it) }
  }
}
```

果然有效！但这回是将andlib全部的依赖项全部打包进来，比如`appcompat` support库等，生成的jar大小有4MB多。我不过是想自己弄个小且使用顺手的工具库！

![fat-jar](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/fat-jar.PNG)

# 瘦身的FatJar
怎么样才能将指定的而非全部的依赖项打包到jar文件呢。摸索了下。可以使用下面的脚本：

```
task jar(type: Jar, dependsOn: assembleRelease) {

    from 'build/intermediates/classes/release/'

    from zipTree('../javalib/build/libs/simple-commons-1.0.0.jar')
    exclude '**/BuildConfig.class'
    exclude '**/R.class'
    exclude '**/R$*.class'
    archiveName 'simple-commons-' + ANDLIB_VERSION + '.jar'

    manifest {
        attributes 'Manifest-Version': ANDLIB_VERSION
        attributes 'Author': 'cm'
    }
}
```

简单说明下：

1. `exclude`指定了如何排除一些类，Android中一般可排除`BuildConfig`和`R`类
2. `zipTree`是Gradle提供的方法，用于遍历压缩文件中的内容
3. `simple-commons-1.0.0.jar`是`:libraries:javalib`中通过`jar.baseName`指定的名字 (写死文件名不太优雅，但可以粗暴地解决问题)
4. 在`gradle.properties`文件中指定`ANDLIB_VERSION`变量，用于表示生成的jar文件的版本

执行`gradlew jar`后生成`build\libs\simple-commons-1.0.1.jar`文件。看看是不是瘦多了！

![thin-jar](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/thin-jar.PNG)

![fat-jar](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/fat-jar.PNG)

[这篇文章][topic-3]中给出了另一种方式，貌似更优雅，但没试过。

[topic-1]: http://stackoverflow.com/questions/19307341/android-library-gradle-release-jar
[topic-2]: http://stackoverflow.com/questions/19034466/how-to-create-an-android-library-jar-with-gradle-without-publicly-revealing-sour
[fat-jar]: http://stackoverflow.com/questions/4871656/using-gradle-to-build-a-jar-with-dependencies
[topic-3]: http://kennethjorgensen.com/blog/2014/fat-jars-with-excluded-dependencies-in-gradle
