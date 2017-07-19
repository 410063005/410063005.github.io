---
title: Android矢量图
tags:
 - android
---
总结了Android矢量图相关的知识
<!--more-->

# 介绍
矢量图适用于简单的图标。而应用的icon包含许多细节，不适合使用矢量图。与栅格图相比，矢量图首次加载时可能消耗更多CPU。之后，二者的内存占用和性能不相上下。建议将矢量图像限制为最大200x200dp，否则可能需要耗费很长的绘制时间。最好将图标设置为黑色，`android:fillColor="#FF000000"`，然后在布局中为矢量图设置`tint`属性，图标颜色将随之变为tint颜色。 见[添加多密度矢量图形][添加多密度矢量图形]

VectorDrawable定义静态图，而AnimatedVectorDrawable定义动态矢量图。

## [VectorDrawable][vector_drawable]
> VectorDrawable允许基于XML创建矢量图
> 注意：为了优化重绘性能，会给每个VectorDrawable创建bitmap缓存。因此引用相同的VectorDrawable时会共享相同的bitmap缓存。如果使用时要求的尺寸不一致，会重新生成bitmap，并且每次尺寸发生变化时会重绘。换句话说，如果VectrorDrawable用于不同的尺寸，为每个尺寸使用一个VectorDrawable会更加高效。

+ <vector> - 定义矢量图
 + android:name - 定义矢量图的名字
 + android:width - drawable的固有宽度
 + android:height - drawable的固有高度
 + android:viewportWidth - viewport的宽度
 + android:viewportHeight - viewport的高度
 + android:tint - The color to apply to the drawable as a tint. By default, no tint is applied.
 + android:tintMode - The Porter-Duff blending mode for the tint color. Default is src_in.
 + android:autoMirrored - 当布局方向是RTL时是否进行镜像处理。缺省是false
 + android:alpha - drawable的不透明度。缺省是1.0
+ <group> - 定义一组path或子组，以及变换信息。以viewport相同的坐标系来定义变换。变换的顺序是缩放，旋转，平移。
 + android:name - 定义组的名字
 + android:rotation - 当前组的旋转角度
 + android:pivotX - 当前组的缩放或旋转中枢点的x坐标。在viewport空间中定义。缺省是0
 + android:pivotY - 当前组的缩放或旋转中枢点的y坐标。在viewport空间中定义。缺省是0
 + android:scaleX - x坐标上的缩放值。缺省是1
 + android:scaleY - y坐标上的缩放值。缺省是1
 + android:translateX - x坐标上的平移值。在viewport空间中定义。缺省是0
 + android:translateY - y坐标上的平移值。在viewport空间中定义。缺省是0
+ <path> - 定义绘制路径
 + android:name - 路径名
 + android:pathData - 以SVG路径数据中的"d"属性相同的格式定义的路径数据。在viewport空间中定义
 + android:fillColor - 用于填充路径的颜色。可以是色值，也可以是color state list或gradient color(SDK 24以上，见GradientColor或GradientColorItem)。如果属性是动画属性，动画中的颜色会覆盖原有的值。如果不指定这个值，将不会绘制路径。
 + android:strokeColor - 用于绘制路径轮廓的颜色。可以是色值，也可以是color state list或gradient color(SDK 24以上，见GradientColor或GradientColorItem)。如果属性是动画属性，动画中的颜色会覆盖原有的值。如果不指定这个值，将不会绘制路径轮廓。
 + android:strokeWidth - path stroke的宽度。缺省是0
 + android:strokeAlpha - path stroke的不透明度。缺省是1
 + android:fillAlpha - path的不透明度。缺省是1
 <这里略过了几个属性>
 
## [AnimatedVectorDrawable][animated_vector_drawable]
> 这个类使用`ObjectAnimator`或`AnimatorSet`中定义的动画对`VectorDrawable`做属性动画。
> 从API 25开始，AnimatedVectorDrawable在RenderThread(不同于之前版本中的UI Thread)运行。这意味着即使UI线程的负载很重，AnimatedVectorDrawable仍然能保持平滑。注意：如果UI线程不响应，RenderThread也可能继续运行动画直到UI线程可以处理下一帧。因此不可能精确协调RenderThread-enabled的AnimatedVectorDrawable跟UI线程动画。另外，`onAnimationEnd(Drawable)`将在AnimatedVectorDrawable运行完成后被回调。

可以使用三个独立的XML文件定义AnimatedVectorDrawable，也可以使用单独的XML文件定义。

第一种方式包括以下文件：

+ VectorDrawable XML文件
+ AnimatedVectorDrawable XML文件，定义了目标VectorDrawable和animator
+ animator XML文件

VectorDrawable XML文件

可以对VectorDrawable中支持动画的属性进行动画操作。这些属性由ObjectAnimator操作。ObjectAnimator的目标可以是根元素，组元素或者path元素。目标元素的名字必须跟VectorDrawable中的保持一致。不需要动画的元素不必命名。

以下是VectorDrawable中支持动画的属性：

|元素名字|支持动画的属性名|
|--------|----------------|
|vector|alpha           |
|group |rotation, pivotX, pivotY, scaleX, scaleY, translateX, translateY|
|path  |pathData, fillColor, strokeColor, strokeWidth, strokeAlpha, fillAlpha, trimPathStart, trimPathOffset|
|clip-path|pathData|

下面是`vectordrawable.xml`中定义的一个VectorDrawable。在`AnimatedVectorDrawable` XML文件中使用文件名(不包括后缀)来引起这里的VectorDrawable。

```xml
 <vector xmlns:android="http://schemas.android.com/apk/res/android"
     android:height="64dp"
     android:width="64dp"
     android:viewportHeight="600"
     android:viewportWidth="600" >
     <group
         android:name="rotationGroup"
         android:pivotX="300.0"
         android:pivotY="300.0"
         android:rotation="45.0" >
         <path
             android:name="v"
             android:fillColor="#000000"
             android:pathData="M300,70 l 0,-70 70,70 0,0 -70,70z" />
     </group>
 </vector>
```
AnimatedVectorDrawable XML文件

AnimatedVectorDrawable元素有一个VectorDrawable属性，以及一个或多个目标元素。目标元素可以使用`android:name`属性指定其目标，使用`android:animation`属性指定ObjectAnimator或AnimatorSet。

下面的示例代码定义了AnimatedVectorDrawable。注意，这里的名字指定了上述VectorDrawable文件中的group和path。

```xml
 <animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
     android:drawable="@drawable/vectordrawable" >
     <target
         android:name="rotationGroup"
         android:animation="@anim/rotation" />
     <target
         android:name="v"
         android:animation="@anim/path_morph" />
 </animated-vector>
```

`ObjectAnimator`或`AnimatorSet`的XML文件

上述AnimatedVectorDrawable例子中使用了`rotation.xml`和`path_morph`两个文件。前者在6000ms内将目标group从0度旋转到360度，

```xml
 <objectAnimator
     android:duration="6000"
     android:propertyName="rotation"
     android:valueFrom="0"
     android:valueTo="360" />
```

后者将path从当前形状变换成另一个形状。注意这里的path必须适合变换。特别是path需要有同样的命令，相同的顺序，每个命令有相同的参数。建议将path中的string保存为资源以便复用。

```xml
 <set xmlns:android="http://schemas.android.com/apk/res/android">
     <objectAnimator
         android:duration="3000"
         android:propertyName="pathData"
         android:valueFrom="M300,70 l 0,-70 70,70 0,0 -70,70z"
         android:valueTo="M300,70 l 0,-70 70,0  0,140 -70,0 z"
         android:valueType="pathType"/>
 </set>
```

第二种方式是在一个XML文件中定义AnimatedVectorDrawable。

AAPT工具(Build Tools 24或以上版本)支持一种新的、可将相关XML文件打包到同一个XML的格式。所以可以将一个例子中的多个XML文件合并到同一个。

<具体XML文件略>

# 官方博客消息
Android Studio 1.4就开始通过构建时生成png的方式对矢量图提供有限的支持。

[Android Developers Blog][blog] Android Support Library 23.2开始支持VectorDrawable和AnimatedVectorDrawable。矢量图允许使用XML定义的单个矢量图形替换原来的多个png资源。之前只有Android 5.0及以上设备才支持VectorDrawable和AnimatedVectorDrawable。现在可以分别通过两个新的支持库在旧设备上支持矢量图：support-vector-drawable和animated-vector-drawable。

并不是所有的接受drawable id的地方都可以加载矢量图。但可以使用`app:srcCompat`属性来加载矢量图。运行时加载矢量图的话，直接像以前一样使用`setImageResource()`方法即可。

# [矢量图][vector_drawable2]
VectorDrawable是由XML文件中定义的一系列点，直线，曲线及颜色信息定义的矢量图。使用矢量图最大的优势是易于缩放。它的显示质量可以无损缩放，意味着在不同屏幕尺寸上可以使用相同的文件而不会导致图片质量下降。可以减少APK包大小，减少开发者维护成本。可以使用多个XML文件来定义多个矢量图来为不同分辨率屏幕实现动画效果，从而取代原来的使用多个图片的做法。

通过一个例子来理解矢量图的好处。一张100x100dp的图片可能在低分辨率上展示出好的质量，而在大屏幕设备上则需要400x400dp的图片。通常开发者创建一个资源的多个版本，以适应不同的分辨率。这种方式导致更多开发成本，更大的APK包，占用更多的设备空间。

Android 5.0(API Level 21)开始VectorDrawable和AnimatedVectorDrawable两个类能支持矢量图。而23.2支持库也能对矢量图和动画矢量图提供完整支持。

VectorDrawable定义了一个静态的绘制对象。类似于SVG格式，每个矢量图形以树状结构定义，由`path`和`group`对象组成。每个`path`包含对象轮廓的几何信息，而`group`则包含具体的变换信息。所有的path以它们出现在XML中的顺序绘制。

![](https://developer.android.com/images/guide/topics/graphics/vectorpath.png)

Android Stuiok中的Vector Asset Studio工具可以很方便地向项目中添加矢量图XML文件。

定义VectorDrawable比较简单，而定义AnimatedVectorDrawable则相对麻烦。需要VectorDrawable XML文件，AnimatedVectorDrawable XML文件以及animator XML文件共3个文件。

使用如下配置可以在项目中引入vectorDrawable支持库：

```gradle
//For Gradle Plugin 2.0+
 android {
   defaultConfig {
     vectorDrawables.useSupportLibrary = true
    }
 }
```

新的support库中添加了新的内容：

+ 25.4.0及以上版本 - 支持Path Morphing, Path Interpolation
+ 26.0.0-beta1版本 - 支持Move along path


# 参考资料
相当赞的一篇文章 [AnimatedVectorDrawableCompat](https://android.jlelse.eu/animatedvectordrawablecompat-3d9568727c53)

> 这篇文章引述[另一个文章](https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88)，提到了AnimatedVectorDrawableCompat的一些限制。

注：这些限制可能已经在新的support库中被实现。

[VectorDrawable：适应不同分辨率的drawable资源](http://jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0123/2346.html)
[VectorDrawable-第一章](http://jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0201/2396.html)
[VectorDrawable-第二章](http://jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0301/2514.html)
[VectorDrawable-第三章](http://jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0301/2515.html)
[VectorDrawable-第四章](http://jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0306/2553.html)
[图标动画技术介绍](http://www.jcodecraeer.com/a/specialarc/2017/0104/6928.html)
[8000个已分类好的扁平化图标](http://www.shejidaren.com/8000-flat-icons.html)
[SVG path ref](http://www.w3.org/TR/SVG11/paths.html#PathData)

[SVG path命令](http://blog.csdn.net/xu_fu/article/details/44004841)。每个命令都有大小写形式，大写代表后面的参数是绝对坐标，小写表示相对坐标。参数之间用空格或逗号隔开

+ M (x y) 移动到x,y
+ L (x y) 直线连到x,y，还有简化命令H(x) 水平连接、V(y)垂直连接
+ Z，没有参数，连接起点和终点
+ C(x1 y1 x2 y2 x y)，控制点x1,y1 x2,y2，终点x,y
+ Q(x1 y1 x y)，控制点x1,y1，终点x,y
+ A(rx ry x-axis-rotation large-arc-flag sweep-flag x y) 

rx ry 椭圆半径 
x-axis-rotation x轴旋转角度 
large-arc-flag 为0时表示取小弧度，1时取大弧度 
sweep-flag 0取逆时针方向，1取顺时针方向 

可以对照着[Android矢量图形之VectorDrawable研究](http://www.cnblogs.com/bvin/p/4317852.html)这个博客中的例子来加深对命令的认识。

# demo
[AnimatedVectorDrawable](https://gist.github.com/nickbutcher/b3962f0d14913e9746f2) [设计图](https://dribbble.com/shots/1945376-Search)
[VectorDrawableDemo][vector_drawable_demo]
[AnimatedVectorDrawableCompat-play-to-reset-button](https://github.com/lewismcgeary/AnimatedVectorDrawableCompat-play-to-reset-button)

灵感：实现一个旋转的时钟，以及一个笑脸 [来源](http://blog.sqisland.com/2014/10/first-look-at-animated-vector-drawable.html)

# 工具
1. Vector Drawable Studio
2. [AndroidIconAnimator](https://romannurik.github.io/AndroidIconAnimator/) (已废弃)
3. [Shape Shifter](https://shapeshifter.design/) [Shape Shifter wiki](http://www.androiddesignpatterns.com/2016/11/introduction-to-icon-animation-techniques.html)
4. [Android SVG to VectorDrawable](http://inloop.github.io/svg2android/)

[添加多密度矢量图形]: https://developer.android.com/studio/write/vector-asset-studio.html
[vector_drawable]: https://developer.android.com/reference/android/graphics/drawable/VectorDrawable.html
[animated_vector_drawable]: https://developer.android.com/reference/android/graphics/drawable/AnimatedVectorDrawable.html
[vector_drawable2]: https://developer.android.com/guide/topics/graphics/vector-drawable-resources.html
[vector_drawable_demo]: https://github.com/Damonzh/VectorDrawableDemo
[blog]: https://android-developers.googleblog.com/2016/02/android-support-library-232.html