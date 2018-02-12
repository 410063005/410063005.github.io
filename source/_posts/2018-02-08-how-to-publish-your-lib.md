---
title: 如何发布自己的Java库
toc: true
date: 2018-02-08 15:27:54
tags:
 - Gradle
---

使用Gradle构建工具非常方便，我们可以快速地添加第三方库依赖。那么如何发布自己的库？

<!--more-->
实际项目中又分成两种情况。一种是将自己的库发布到第三方搭建的Maven库(比如公司内部Maven库)，另一种情况是发布到官网(比如jCenter)。下面分别讨论。

# 发布到第三方库
这里以发布到我们公司内部的Maven库为例，发布到其他第三方Maven库应该类似。

## 发布
### 使用maven-publish插件发布
在待发布的module的`build.gradle`中添加配置：

```groovy
// 1. 添加maven-publish plugin
apply plugin: 'maven-publish'

publishing {
    publications {
        // 2. 指定要发布的内容
        mavenJava(MavenPublication) {
            from components.java
        }
    }

    repositories {
        maven {
            // 3. 指定Maven库的用户名密码
            credentials {
                username "$mavenUser"
                password "$mavenPassword"
            }

            // change to point to your repo, e.g. http://my.org/repo
            url {
                // 4. 指定Maven库的地址 你甚至可以在这里指定为本地文件 "$buildDir/repo"
                "http://my.org/repo"
            }
        }
    }
}
```

+ 第3点，[参考](https://docs.gradle.org/current/userguide/publishing_maven.html) [参考](https://stackoverflow.com/questions/12749225/where-to-put-gradle-configuration-i-e-credentials-that-should-not-be-committe)
+ 第4点，[参考](http://km.oa.com/group/29073/articles/show/299976)

配好之后运行`gradle publish`即可将生成的内容上传到指定的Maven库。

```
gradlew publish
Parallel execution is an incubating feature.

Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.jar
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.jar.sha1
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.jar.md5
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.pom
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.pom.sha1
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.111742-6.pom.md5
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/maven-metadata.xml
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/maven-metadata.xml.sha1
Upload http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/maven-metadata.xml.md5
Upload http://my.org/repo/com/test/viky-test/maven-metadata.xml
Upload http://my.org/repo/com/test/viky-test/maven-metadata.xml.sha1
Upload http://my.org/repo/com/test/viky-test/maven-metadata.xml.md5

BUILD SUCCESSFUL in 3s
4 actionable tasks: 4 executed
```

注意：不要忘记在`gradle.properties`中配置Maven库的用户名密码:

```groovy
mavenUser=your_username
mavenPassword=your_password
```

否则可能出现类似下面这样的未授权问题：

```
* What went wrong:
Execution failed for task ':publishMavenJavaPublicationToMavenRepository'.
> Failed to publish publication 'mavenJava' to repository 'maven'
   > Could not write to resource 'http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.093919-5.jar'.
      > Could not PUT 'http://my.org/repo/com/test/viky-test/1.0-SNAPSHOT/viky-test-1.0-20180206.093919-5.jar'. Received status code 401 from server: Unauthorized
```

### 使用maven插件发布
也可以直接使用maven插件而不是maven-publish插件。maven插件配置起来可能相对更简单。

首先在module的`build.gradle`中添加maven plugin。

```groovy
apply plugin: 'maven-publish'
```

然后在`build.gradle`中添加一个新的task`uploadArchives`。

```groovy
uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: 'http://my.org/repo'){
                authentication(userName: 'your_username', password: 'your_password')
                pom.groupId = 'com.yourcompany.yourpackage'
                pom.artifactId = 'yourart'
                pom.version = '1.0.0-SNAPSHOT'
            }
        }
    }
}
```

运行`gradle uploadArchives`将生成的内容上传到指定的Maven库。

## 使用自己发布的库
在`repositories`中添加了Maven库地址后，我们就可以像使用第三方库一样引用自己发布的库了。

添加仓库：
```groovy
allprojects {
    repositories {
        maven {
            // 注意, 这里的地址仅用于演示, 实际的下载地址可能跟上传时指定的地址有所不同
            url "http://my.org/repo"
        }
    }
}
```

添加依赖：
```
compile 'com.yourcompany.yourpackage:yourart:1.0.0-SNAPSHOT'
```

# 发布到jCenter
jCenter是由bintray.com维护的Maven仓库。

[如何使用Android Studio把自己的Android library分发到jCenter和Maven Central | 开发技术前线](http://www.devtf.cn/?p=760)一文中有比较详情的描述，但是我按照文中提到的方法尝试很久仍然不能成功将自己的库发布到jCenter。所以建议直接使用[bintray-plugin](https://github.com/bintray/bintray-examples)提供的例子来实践，但操作前一定要把[README](https://github.com/bintray/gradle-bintray-plugin#readme)多看几遍，否则可能踩坑。

## 下载代码
首先下载[bintray-examples](https://github.com/bintray/bintray-examples/blob/master/gradle-aar-example/build.gradle)代码。

```
git clone https://github.com/bintray/bintray-examples.git
```

注意，官方的例子更新并不及时。以examples中的`gradle-aar-example`为例，它使用的gradle版本是2.2.1，所以按照[官网说明](https://github.com/bintray/gradle-bintray-plugin#step-2-apply-the-plugin-to-your-gradle-build-script)我们应当将对`build.gradle`脚本的buildscript部分用到`bintray-plugin`的地方进行修改，否则后续操作可能失败。

修改前的代码
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:1.0.0'
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.0'
        classpath 'com.github.dcendents:android-maven-plugin:1.2'

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
```

修改后的代码
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:1.0.0'
        classpath 'com.github.dcendents:android-maven-plugin:1.2'

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id "com.jfrog.bintray" version "1.7.3"
}
```

## 申请bintray账号
具体步骤在些略过，可以参考[如何使用Android Studio把自己的Android library分发到jCenter和Maven Central | 开发技术前线](http://www.devtf.cn/?p=760)第一部分。

我们会用到bintray的账号名以及API Key。可以在 Edit Profit 中找到 API Key：

![](/images/1518062768362.webp)

## 配置bintray插件
修改`build.gradle`中的`bintray`部分。

```groovy
bintray {
    user = "chen410063005"
    key = "**************"

    configurations = ['archives'] //When uploading configuration files
    pkg {
        repo = 'tt'
        name = 'wee'
        userOrg = 'sunmoon'
        desc = 'Bintray aar publishing example'
        websiteUrl = siteUrl
        issueTrackerUrl = 'https://github.com/bintray/bintray-examples/issues'
        vcsUrl = gitUrl
        licenses = ['Apache-2.0']
        labels = ['aar', 'android', 'example']
        publicDownloadNumbers = true
    }
}
```

+ user - 你的bintray用户名
+ key - 你的API Key
+ repo - Repo name
+ name - Package name
+ userOrg - 你的bintray账号所属的组织

以上几个字段需要跟你的bintray账号相对应。以我的账号为例，截图如下：

![](/images/1518063073227.webp)

## 发布
配置无误后`gradle bintrayUpload`即可将aar文件上传到bintray

![](/images/1518063238354.webp)

记得要在bintray中将新上传的文件状态修改为发布状态才能正常下载。

## 使用

添加仓库：
```groovy
repositories {
    maven {
        url  "https://sunmoon.bintray.com/tt" 
    }
}
```

添加依赖：
`compile 'com.bintray.example:gradle-aar-example:1.1'`

容易忘记如何添加仓库和添加依赖。不用担心，bintray网站中有详情的引导告诉我们如何操作，见这里的截图。

+ 添加仓库。点"set me up" -> "Resolving artifacts using Gradle"

![](/images/1518063711773.webp)

+ 添加依赖。在"Maven build settings"中将Maven切换到Gradle
![](/images/1518063762982.webp)

这个进度条是不是很熟悉？`build.gradle`脚本变动后gradle正在下载我自己上传的库文件，说明我们可以从jCenter找到自己发布的库了！

![](/images/1518064033209.webp)

# 参考
+ [如何使用Android Studio把自己的Android library分发到jCenter和Maven Central | 开发技术前线](http://www.devtf.cn/?p=760)