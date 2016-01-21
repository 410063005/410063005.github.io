---
layout: post
title: "动态生成productFlavors"
date: 2015-11-26 15:40:06 +0800
comments: true
categories: gradle
published: true
---
本文介绍通过动态生成productFlavors的方式构建多渠道包
<!--more-->
最近项目有打渠道包的需求。一开始的想法是，既然我们的项目已经在RDM上用gradle构建，而RDM是支持多渠道构建的，直接使用RDM提供的打渠道包功能不就行了。至于使用方式，应该是启动RDM构建时填入渠道号列表就可以了，见下图。
![rdm-channel-apk](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/rdm-channel-apk.PNG)
但满心欢喜点"帮助"却发现打不开链接。又找到官方的帮助链接[RDM持续集成之 渠道包打包 功能说明][rdm]，发现没权限打开。大家能打开吗？好吧，我没权限不折腾了。不折腾的另一个原因是，KM上找到关于RDM上如何打渠道包的文章大多是一年前的，不太详细，不好操作。

所以自己参考网上资料摸索出如何基于gradle打渠道包。具体涉及到三个关键点：

1. manifest placeholders的使用。个人比较习惯将渠道号放在manifest的meta-data中，所以需要有办法修改或替换manifest中的meta-data。幸好gradle直接支持[manifest placeholders功能][manifest-placeholders]。
2. product flavors的使用。不同flavor对应不同版本，不同版本填相应渠道名即可得到渠道包。关于product flavors的用法可以见我之前写的[Gradle自动化多渠道包构建初探](http://km.oa.com/articles/show/246012)，里面主要讲如何使用product flavors配置不同后台参数。
3. [如何动态创建product flavors][ref]。之所以要动态创建，是因为给40个以上渠道逐个写40个product flavors，虽然可行，但是显得特臃肿。想想你的代码不能有循环操作(for或while)该有多可怕！

好吧，不多说，直接上代码。
# 配置manifest
`AndroidManifest.xml`中配置上报渠道号到[灯塔][dengta]和[MTA][mta]，这里不写具体渠道号，而是使用自定义的`CHANNEL_DENGTA`占位符。`build.gradle`中会用到这个占位符。关于manifest placeholders的用法可以参考[这篇文章][manifest-placeholders]。

manifest代码如下：

```
<manifest>
    <uses-permission android:name="android.permission.INTERNET"/>
    ...
    <application>
        <activity>
        ...
        </activity>

        <receiver>
        ...
        </receiver>
        ...

        <meta-data
            android:name="CHANNEL_DENGTA"
            android:value="${CHANNEL_DENGTA}"/>

        <meta-data
            android:name="InstallChannel"
            android:value="${CHANNEL_DENGTA}"/>
    </application>
</manifest>
```
# 配置build.gradle
`build.gradle`中`android{}`动态生成 **一部分** productFlavors。注意强调了只是一部分动态生成，另外几个是直接写好的。具体见代码：

```
android {

    // 应用市场渠道
    def marketFlavors = [
            "baidu", "360", "wandoujia", "anzhi",
            "xiaomi", "91", "pp", "anzhuomarket",
            "wo", "tianyi", "oppo", "huawei",
            "meizu", "vivo", "igame", "tgp", "qqwb", "qqtips"
    ]

    // 学校推广渠道
    def schoolFlavors = [
            "yzzya", "yzzyb", "zjgya", "zjgyb",
            "znlya", "znlyb", "szxya", "szxyb",
            "ptxya", "ptxyb", "dbnya", "dbnyb",
            "szgza", "szgzb", "gdgya", "gdgyb",
            "dndxa", "dndxb", "hbjma", "hbjmb", "hzsfa", "hzsfb"
    ]


    productFlavors {
        // 分别是开发版本, 内部检验版本, 测试版本, 正式发布版本. 用于配置各种SDK的key, 简单起见这里略过
        dev {
        }
        internalUse {
         }
        forTesting { // ProductFlavor names cannot start with 'test'
        }
        releaseDomain {
        }

        // 根据应用市场渠道动态生成productFlavors, 并将`CHANNEL_DENGTA`占位符设置为相应的渠道名
        marketFlavors.each { name ->
            "channelMarket$name" {
                manifestPlaceholders = [CHANNEL_DENGTA: name]
            }
        }

        // 根据学校推广渠道动态生成productFlavors, 并将`CHANNEL_DENGTA`占位符设置为相应的渠道名
        schoolFlavors.each { name ->
            "channelSchool$name" {
                manifestPlaceholders = [CHANNEL_DENGTA: name]
            }
        }
    }

    // 应用市场渠道生成的apk放到`build/outputs/apk/market/`目录
    // 学校推广渠道生成的apk放到`build/outputs/apk/school/`目录
    applicationVariants.all { variant ->

        // 只在构建release版本时才将apk放到相应的目录, debug或其他版本时apk路径保持缺省值
        if (variant.buildType.name == 'release') {

            variant.outputs.each { output ->
                def pn = variant.productFlavors[0].name
                if (pn.contains('channelMarket')) {
                    // 避免apk文件名中产生 "channelMarket" 字样
                    def originName = pn.replace("channelMarket", "")
                    output.outputFile = new File(output.outputFile.parent + "/market",
                        "com.tencent.pmdcampus-${originName}-${variant.versionName}.apk".toLowerCase())

                } else if (pn.contains('channelSchool')) {
                    // 避免apk文件名中产生 "channelSchool" 字样
                    def originName = pn.replace("channelSchool", "")
                    output.outputFile = new File(output.outputFile.parent + "/school",
                        "com.tencent.pmdcampus-${originName}-${variant.versionName}.apk".toLowerCase())

                } else {
                    // 略过其他的渠道, 如dev, internalUse等
                    // NO OP
                }
            }
        }
    }

}
```

注意：最关键的在于`marketFlavors.each`和`schoolFlavors.each`两部分。而`applicationVariants.all`这部分是可选的，仅仅用于将渠道包分类存放。

# 生成渠道包
执行`gradlew assembleRelease`生成渠道包(为方便演示，这里只生成了两个渠道包)
![gradlew-result](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/gradlew-assemble.PNG)
渠道包的目录结构如下
![gradlew-assemble](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/gradlew-result.PNG)

至此，gradle就能在本地构建多渠道包了。遗留的一个问题是，我们项目在RDM上构建的，生成的apk包不能留在原来目录，否则无法下载。对单个apk还是比较好操作的，直接拷贝到可下载的目录就行了。而对类似`school`或`market`这样的目录则需要我们在拷贝前先处理下。

# 支持RDM构建
如上所说，我们项目在RDM上构建的，生成的apk包不能留在原来目录，否则无法下载。对单个apk文件可以从原目录拷贝到`bin/`目录即可。对目录则最先打成单个压缩包再拷贝。直接贡献出我们项目的`build.sh`吧：

```
export ANDROID_HOME=$ANDROID_SDK
export JAVA_HOME=$JDK7
export PATH=$JDK7/bin:$GRADLE_HOME/bin:$PATH
if [ "$channel" = "" ]; then
    gradle assembleReleaseDomainRelease assembleForTestingRelease assembleInternalUseRelease assembleDevRelease
else
    gradle assembleRelease
fi
cp build/outputs/apk/10052-dev-release.apk bin/$BaseLine-develop.apk
cp build/outputs/apk/10052-internalUse-release.apk bin/$BaseLine-for-internal-use-only.apk
cp build/outputs/apk/10052-forTesting-release.apk bin/$BaseLine-for-testing.apk
cp build/outputs/apk/10052-releaseDomain-release.apk bin/$BaseLine-release.apk
cp build/outputs/mapping/releaseDomain/release/mapping.txt bin/$BaseLine-mapping-release.txt

if [ "$channel" = "" ]; then
    echo
else
    zip bin/market.zip build/outputs/apk/market/*.apk
    zip bin/school.zip build/outputs/apk/school/*.apk
fi
```

上面脚本中的`channel`是RDM自定义参数，关于RDM自定义参数用法可参考[这里篇文章][ref2]。在RDM上启动构建时输入`channel=<非空值>`即表示构建多渠道包，无输入或输入`channel=`时只构建平常需要的四个渠道包。

![rdm-channel-param](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/rdm-channel-param.PNG)

看看我们的构建结果吧，`market.zip`和`school.zip`正是我们在`build.sh`中打包生成的。

![rdm-result](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/rdm-results.PNG)

# 总结
虽然没体验过RDM官方多渠道包构建，但还是可以简单跟其比较下。

1. 不足：RDM启动构建时可输入"渠道号列表"参数，看起来相当灵活，非开发人员也可操作；而本文中的方案脚本中写死渠道号，不是很灵活，加渠道得修改代码
2. 优势：基于gradle的productFlavor，在本地或RDM上都可运行(哥哥姐姐们没体验过配置RDM的痛苦过程?相比之下本地运行时排错就简单多了)。本文中的方案直接生成渠道apk，而不是修改原始apk中特定文件方式[1][1],[2][2]来得到渠道包(据说RDM也是这么实现的?见[这里](http://km.oa.com/group/18155/articles/show/210291))，不存在apk包被损坏的风险

[ref]: http://stackoverflow.com/questions/20976946/dynamically-generating-product-flavors/
[ref2]: http://km.oa.com/group/18155/articles/show/132758
[1]: http://km.oa.com/articles/show/225120
[2]: http://km.oa.com/group/22112/articles/show/216716X
[manifest-placeholders]: http://relex.me/using-manifestplaceholders/
[rdm]: http://km.oa.com/group/18155/articles/show/145930
[dengta]: http://beacon.tencent.com/
[mta]: http://mta.oa.com/