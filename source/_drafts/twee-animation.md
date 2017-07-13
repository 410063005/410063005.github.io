---
title: 彻底弄懂tween动画
tags:
 - android
---
通过官方文档和实例彻底弄懂tween动画。
<!-- more -->

# TextSwitcher
[Android TextSwitcher][ref]这个例子演示了如何在TextSwitcher中使用文本变换动画。TextSwitcher是一个特殊类型的[ViewSwitcher][ref2]，当`TextSwitcher.setText()`被调用时会以动画方式移除当前文字并显示新的文字。

关键的代码如下：

```java
/*
 * Set the in and out animations. Using the fade_in/out animations
 * provided by the framework.
 */
Animation in = AnimationUtils.loadAnimation(this,
        android.R.anim.slide_in_left);
Animation out = AnimationUtils.loadAnimation(this,
        android.R.anim.slide_out_right);
mSwitcher.setInAnimation(in);
mSwitcher.setOutAnimation(out);
```

通过TextSwitcher设置不同的InAnimation和OutAnimation，可以看到不同的动画效果。所以，这个例子是我们学习tween动画的一个好案例。

可以在`<SDK目录>\platforms\android-25\data\res\anim`下找到`fade_in.xml`和`fade_out.xml`两个资源。

```xml
<!-- fade_in.xml -->
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
        android:interpolator="@interpolator/decelerate_quad"
        android:fromAlpha="0.0" android:toAlpha="1.0"
        android:duration="@android:integer/config_longAnimTime" />
        
<!-- fade_out.xml -->
<alpha xmlns:android="http://schemas.android.com/apk/res/android" android:interpolator="@interpolator/accelerate_quad" 
    android:fromAlpha="1.0"
    android:toAlpha="0.0"
    android:duration="@android:integer/config_mediumAnimTime" 
/>
```

这动画非常简单，很容易理解。再看一个。

```xml
<!-- slide_in_left.xml -->
<set xmlns:android="http://schemas.android.com/apk/res/android">
	<translate android:fromXDelta="-50%p" android:toXDelta="0"
            android:duration="@android:integer/config_mediumAnimTime"/>
	<alpha android:fromAlpha="0.0" android:toAlpha="1.0"
            android:duration="@android:integer/config_mediumAnimTime" />
</set>

<!-- slide_out_right.xml -->
<set xmlns:android="http://schemas.android.com/apk/res/android">
	<translate android:fromXDelta="0" android:toXDelta="50%p"
            android:duration="@android:integer/config_mediumAnimTime"/>
	<alpha android:fromAlpha="1.0" android:toAlpha="0.0"
            android:duration="@android:integer/config_mediumAnimTime" />
</set>
```

即便对着动画效果看，我也没太明白android:fromXDelta="-50%p"是什么意思。翻文档吧。

# 文档
以下内容翻译自[这里][ref3]。

animation资源可以定义两种类型的动画。

+ 属性动画 - 在一段时间内通过animator修改对象的属性来创建动画效果
+ View动画 - 有两类view动画
 + 通过[Animation][ref4]对view进行一系列的变换来创建动画
 + 通过[AnimationDrawable][ref6]依次显示一系列图片来显示动画

## 属性动画
一种在xml中定义的动画，可以修改一段时间内目标对象的属性，比如背景色或alpha值。
 
文件位置：

    `res/animator/filename.xml`
    
    文件名作为资源ID
    
编译后的资源类型：

    Java代码：`R.animator.filename`
    
    XML：`@[package:]animator/filename`

语法：

```xml
<set
  android:ordering=["together" | "sequentially"]>

    <objectAnimator
        android:propertyName="string"
        android:duration="int"
        android:valueFrom="float | int | color"
        android:valueTo="float | int | color"
        android:startOffset="int"
        android:repeatCount="int"
        android:repeatMode=["repeat" | "reverse"]
        android:valueType=["intType" | "floatType"]/>

    <animator
        android:duration="int"
        android:valueFrom="float | int | color"
        android:valueTo="float | int | color"
        android:startOffset="int"
        android:repeatCount="int"
        android:repeatMode=["repeat" | "reverse"]
        android:valueType=["intType" | "floatType"]/>

    <set>
        ...
    </set>
</set>
```

    文件必须要有一个根元素：`<set>`, `<objectAnimator>`, 或`<animator>`.  你可以将一组动画元素或`<set>`元素放到`<set>`中。
    
元素：

    `<set>`
    
    一个可以包含其他动画元素(`<objectAnimator>`， `<animator>`， 或`<set>`)的容器。 它代表AnimatorSet
    
    可以指定嵌套的`<set>`。 每个`<set>`可以定义自己的`ordering`属性。
    
    元素的属性如下：
    
    `android:ordering`
    
        关键字。它指定了动画的顺序。
        
        |Value|Description|
        |-----|-----------|
        |sequentially|顺序播放当前set中的动画|
        |together (缺省)|同时播放当前set中的动画|	
    
    `<objectAnimator>`
    
        在一段时间内给对象上指定的属性进行动画。表示[ObjectAnimator](https://developer.android.com/reference/android/animation/ObjectAnimator.html)
    
        元素的属性如下：
        
        `android:propertyName`
        
            字符串。**必须**。通过名字指定了将将要显示动画的属性。比如，你可以为view对象指定`"alpha"`或`"backgroundColor"`。`objectAnimator`元素并不暴露`target`属性，所以你不能在XML中指定要显示动画的对象。通过调用`loadAnimation()`方法来加载动画XML资源，然后调用`setTarget()`来设置目标对象。
            
        `android:valueTo`
        
            float, int或color。必须。动画属性的结束值。color值由6位十六进制数表示，如#333333
            
        `android:duration`
        
            int.  动画持续时间，单位为毫秒。缺省值是300毫秒
            
        `android:startOffset`
        
            int. `start()`调用后开始播放动画的延迟时间
            
        `android:repeatCount`
        
            int. 重复动画的次数。`"-1"`表示无限重复，也可设置为其他正数。比如，`"1"`表示首次运行动画后还将再重复一次，所以动画一共运行两次。缺省值是`"0"`，表示不重复。
            
        `android:repeatMode`
        
            int. 动画结束后的行为。 `android:repeatCount`设置为`"-1"`或正数时这个属性才生效。`"reverse"`会以相反顺序显示动画，而`"repeat"`以原来顺序显示动画。
            
        `android:valueType`
        
            关键字. 如果当前值是color，不需要指定这个属性。动画框架自动处理color值。
            
            |value|Description|
            |-----|-----------|
            |intType|指定的动画值为int类型|
            |floatType(缺省)|指定的动画值为float类型|
        
    `<animator>`
    
            在一段时间内显示动画。表示[ValueAnimator](https://developer.android.com/reference/android/animation/ValueAnimator.html)
        
            元素的属性如下：
    
            `android:valueTo`

                float, int或color。必须。动画属性的结束值。color值由6位十六进制数表示，如#333333
                
            `android:valueFrom`
            
                float, int或color。必须。动画属性的起始值。color值由6位十六进制数表示，如#333333

            其他属性同<objectAnimator>，略
            
例子：

假设XML文件是`res/animator/property_animator.xml`

```xml
<set android:ordering="sequentially">
    <set>
        <objectAnimator
            android:propertyName="x"
            android:duration="500"
            android:valueTo="400"
            android:valueType="intType"/>
        <objectAnimator
            android:propertyName="y"
            android:duration="500"
            android:valueTo="300"
            android:valueType="intType"/>
    </set>
    <objectAnimator
        android:propertyName="alpha"
        android:duration="500"
        android:valueTo="1f"/>
</set>
```

为了运行这个动画，必须在代码中将XML资源加载为`AnimatiorSet`，然后在开始动画前设置目标对象。

```java
AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(myContext,
    R.anim.property_animator);
set.setTarget(myObject);
set.start();
```
    
## View动画
View动画框架既支持tween动画也支持frame动画，两者都能在XML中声明。下面描述了如何使用。

### Tween动画
tween动画在XML中定义了如何对图像进行旋转，平移，透明度以及拉伸变换。

文件位置：

    `res/anim/filename.xml`
    
    文件名作为资源ID
    
编译后的资源类型：

    编译后的类型为Animation
    
资源引用：

    Java代码：`R.anim.filename`
    
    XML：`@[package:]anim/filename`
   
语法：

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@[package:]anim/interpolator_resource"
    android:shareInterpolator=["true" | "false"] >
    <alpha
        android:fromAlpha="float"
        android:toAlpha="float" />
    <scale
        android:fromXScale="float"
        android:toXScale="float"
        android:fromYScale="float"
        android:toYScale="float"
        android:pivotX="float"
        android:pivotY="float" />
    <translate
        android:fromXDelta="float"
        android:toXDelta="float"
        android:fromYDelta="float"
        android:toYDelta="float" />
    <rotate
        android:fromDegrees="float"
        android:toDegrees="float"
        android:pivotX="float"
        android:pivotY="float" />
    <set>
        ...
    </set>
</set>
```

文件必须要有一个根元素：`<alpha>`, `<scale>`, `<translate>`, `<rotate>`或`<set>`

元素：

    `<set>`
    
    一个可以包含其他动画元素(`<alpha>`, `<scale>`, `<translate>`, `<rotate>`)的容器。代表[AnimationSet](https://developer.android.com/reference/android/view/animation/AnimationSet.html)
    
    属性：
    
    `android:interpolator`
    
        插值器。应用于当前动画的[Interpolator](https://developer.android.com/reference/android/view/animation/Interpolator.html)。它的值必须为指定某个插值器资源的引用(注意不是插值器类名)。平台中提供默认的插值器资源，你也可创建自己的插值器资源。
        
    `android:shareInterpolator`
    
        布尔值。如果想在所有子元素间共用相同插值器，置为"true"
        
    `<alpha>`
    
        渐隐渐现动画。表示[AlphaAnimation](https://developer.android.com/reference/android/view/animation/AlphaAnimation.html)
        
        属性：
        
        `android:fromAlpha`
        
            float类型。初始不透明度，0.0表示透明，1.0表示不透明
            
        `android:toAlpha`
        
            float类型。结束时的不透明度，0.0表示透明，1.0表示不透明
            
        `<alpha>`元素更多的属性见[Animation](https://developer.android.com/reference/android/view/animation/Animation.html)类
        
    `<scale>`
    
        缩放动画。你可以通过`pivotX`和`pivotY`指定图像的中心点，以指明它从哪里扩大或缩小。比如，如果这两个值分别是0和0(左上角)，则所有的扩大或缩小都会往右下方。表示[ScaleAnimation](https://developer.android.com/reference/android/view/animation/ScaleAnimation.html)
        
        属性：
        
        `android:fromXScale`
        
            float. x方向上的初始放大因子，1.0表示无变化
            
        `android:toXScale`
        
            float. x方向上的结束放大因子，1.0表示无变化
            
        `android:fromYScale`
        
            float. y方向上的初始放大因子，1.0表示无变化
            
        `android:toYScale`
        
            float. y方向上的结束放大因子，1.0表示无变化
            
        `android:pivotX`
        
            float. 当对象缩放时保持固定的x坐标
            
        `android:pivotY`
        
            float. 当对象缩放时保持固定的y坐标
            
        `<scale>`元素更多的属性见[Animation](https://developer.android.com/reference/android/view/animation/Animation.html)类
        
    `<translate>`

        垂直或水平方向的移动。支持三种格式的属性：-100%到100%的值，表示相对于自己的百分比；-100%p到100%p的值，表示相对于自己父元素的百分比；浮点数，表示绝对值。表示[TranslateAnimation](https://developer.android.com/reference/android/view/animation/TranslateAnimation.html)
        
        `android:fromXDelta`
        
            float或百分比。x方向起始值。可以几种方式表示：相对于正常位置的像素值(比如5)，相对于元素宽度的百分比(比如5%)，或相对于父元素宽度的百分比(比如5%p)
            
        `android:toXDelta`
        
            float或百分比。x方向结束值。可以几种方式表示：相对于正常位置的像素值(比如5)，相对于元素宽度的百分比(比如5%)，或相对于父元素宽度的百分比(比如5%p)
            
        `android:fromYDelta`和`android:toYDelta`都是类似的，略
        
    `<rotate>`
    
        旋转动画。表示[RotateAnimation](https://developer.android.com/reference/android/view/animation/RotateAnimation.html)
        
        `android:fromDegrees`
        
            float. 起始角位置，单位度
            
        `android:toDegrees`
         
            float. 结束角位置，单位度
            
        `android:pivotX`
            float或百分比。中心点的x坐标。可以几种方式表示：相对于对象左边缘的像素值(比如5)，相对于对象左边缘的百分比(比如5%)，相对于父容器的左边缘(比如5%p)
            
        `android:pivotY`
            float或百分比。中心点的y坐标。可以几种方式表示：相对于对象左边缘的像素值(比如5)，相对于对象左边缘的百分比(比如5%)，相对于父容器的左边缘(比如5%p)
            
例子：
    
假设XML文件是`res/anim/hyperspace_jump.xml`

```xml
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:shareInterpolator="false">
    <scale
        android:interpolator="@android:anim/accelerate_decelerate_interpolator"
        android:fromXScale="1.0"
        android:toXScale="1.4"
        android:fromYScale="1.0"
        android:toYScale="0.6"
        android:pivotX="50%"
        android:pivotY="50%"
        android:fillAfter="false"
        android:duration="700" />
    <set
        android:interpolator="@android:anim/accelerate_interpolator"
        android:startOffset="700">
        <scale
            android:fromXScale="1.4"
            android:toXScale="0.0"
            android:fromYScale="0.6"
            android:toYScale="0.0"
            android:pivotX="50%"
            android:pivotY="50%"
            android:duration="400" />
        <rotate
            android:fromDegrees="0"
            android:toDegrees="-45"
            android:toYScale="0.0"
            android:pivotX="50%"
            android:pivotY="50%"
            android:duration="400" />
    </set>
</set>
```
   
下面的代码将对ImageView应用以上动画：

```java
ImageView image = (ImageView) findViewById(R.id.image);
Animation hyperspaceJump = AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump);
image.startAnimation(hyperspaceJump);
```

## 插值器
插值器是XML中定义的动画修改器，它可以影响动画的变化速度。它允许现有的动画效果加速，减速，重复，弹回等等。

使用`android:interpolator`属性将插值器应用于动画元素，其值引用interpolator资源。

Android中所有的interpolator都是interpolator的子类。对每个interpolator类，Android都包含一个公开的资源供引用，可以使用`android:interpolator`属性应用该interpolator。

插值器表，略

这样使用`android:interpolator`属性。

```xml
<set android:interpolator="@android:anim/accelerate_interpolator">
    ...
</set>
```

自定义插值器

如果你对系统提供的插值器不满意，也可以修改属性以创建自己的插值器。比如，你可以调整AnticipateInterpolator的加速度，或者调整CycleInterpolator的cycle数。你需要在XML文件中创建自己的插值器。

    文件位置：

        `res/anim/filename.xml`
        
        文件名作为资源ID
        
    编译后的资源类型：

        指向对应interpolator对象的资源
        
    语法：

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <InterpolatorName xmlns:android="http://schemas.android.com/apk/res/android"
        android:attribute_name="value"
        />
    ```

        如果你不使用任何属性，则你定义的插值器跟系统提供的完全一样。
        
    元素：
    
        注意XML中定义的每个插值器，名字都以小写字母开头
        
        `<accelerateDecelerateInterpolator>`
        
            开头和结束的变化速度都很慢，而中间阶段会加速。这个元素没有属性
            
        `<accelerateInterpolator>`
        
            变换速度一开始很快，然后加速
            
            属性：
            
            `android:factor`
            
                浮点数。加速速度(缺省是1)
                
        `<anticipateInterpolator>`
        
            这个变换是先后退然后前进。
            
            属性：
            
            `android:tension`
            
                浮点数。拉力值(缺省是2)
                
        `<anticipateOvershootInterpolator>`
        
            这个变换是先后退，然后前进并且超过终点值，最后回到终点值
            
            属性：
            
            `android:tension`
            
                浮点数。拉力值(缺省是2)
                
            `android:extraTension`
            
                浮点数。The amount by which to multiply the tension (缺省是1.5)
                
        `<bounceInterpolator>`
        
            这个变换在结束时有弹回效果。没有属性
            
        `<cycleInterpolator>`
        
            重复动画效果。变换效果遵守正弦函数。
            
            属性：
            
            `android:cycles`
            
                整数。次数(缺省是1)
                
        `<decelerateInterpolator>`
        
            这个变换一开始很快，然后减速。
            
            属性：
            
            `android:factor`

                浮点数。减速速度(缺省是1)
                
        `<linearInterpolator>`
        
            变换速度是恒定的。没有属性。
            
        `<overshootInterpolator>`
        
            一直前进并超过终点值，然后回到终点值
            
            属性：
            
            `android:tension`
            
                浮点数。拉力值(缺省是2)
            
    例子：
    
        文件位置：`res/anim/my_overshoot_interpolator.xml`
        
        ```xml
        <?xml version="1.0" encoding="utf-8"?>
        <overshootInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
        android:tension="7.0"
        />
        ```

        这个动画使用了interpolator:
        
        ```xml
        <scale xmlns:android="http://schemas.android.com/apk/res/android"
            android:interpolator="@anim/my_overshoot_interpolator"
            android:fromXScale="1.0"
            android:toXScale="3.0"
            android:fromYScale="1.0"
            android:toYScale="3.0"
            android:pivotX="50%"
            android:pivotY="50%"
            android:duration="700" />        
        ```

### Frame动画
略

---
        
---
[view动画](https://developer.android.com/guide/topics/graphics/view-animation.html#tween-animation)

可以使用view动画系统在view上执行tween动画。tween动画通过起始点，终止点，大小，旋转及其他信息计算动画效果。

tween动画可以对view对象执行一系列简单的转换(位置，大小，角度，透明度)。如果你有一个TextView对象，你可以移动、旋转、缩放文字。如果它有背景图，则背景图也会跟着文字一起变化。[animation package](https://developer.android.com/reference/android/view/animation/package-summary.html)提供了tween动画用到的类。

一系列的动画指令定义tween动画。可以是XML或Android代码。就像定义布局一样，推荐使用XML文件。因为它比代码有更好的可读性，易重用，易替换。下面的例子中我们使用XML。(想学习如何通过代码定义动画，请参考AnimationSet和其他Animation的子类)

动画指令定义了你想要的变换，何时发生，以及持续时间。变换可以同时发生或连续发生。比如，你可以让TextView从左边移动到右边，然后旋转180度，或者你也可以移动的同时进行旋转。每种变换都有特定变换参数(对大小变换而言，是起始大小和结束大小；对旋转变换而言，是起始角度和结束角度)，以及通用变换参数(比如，起始时间和持续时间)。想让多个变换同时发生，让它们有相同的起始时间即可。想让他们连续发生，让起始时间加上前一个动画的持续时间即可。

动画的XML文件位于Android工程下的`res/anim`目录。文件必须有一个根元素：可以是`<alpha>`, `<scale>`, `<translate>`, `<rotate>`，或`<set>`(它可以包含一组其他的元素)。缺省情况下所有的动画指令同时执行。想让它们连续发生，必须指定`startOffset`属性。

以下XML用于先拉伸view，接着缩小并旋转。

```xml
<set android:shareInterpolator="false">
    <scale
        android:interpolator="@android:anim/accelerate_decelerate_interpolator"
        android:fromXScale="1.0"
        android:toXScale="1.4"
        android:fromYScale="1.0"
        android:toYScale="0.6"
        android:pivotX="50%"
        android:pivotY="50%"
        android:fillAfter="false"
        android:duration="700" />
    <set android:interpolator="@android:anim/decelerate_interpolator">
        <scale
           android:fromXScale="1.4"
           android:toXScale="0.0"
           android:fromYScale="0.6"
           android:toYScale="0.0"
           android:pivotX="50%"
           android:pivotY="50%"
           android:startOffset="700"
           android:duration="400"
           android:fillBefore="false" />
        <rotate
           android:fromDegrees="0"
           android:toDegrees="-45"
           android:toYScale="0.0"
           android:pivotX="50%"
           android:pivotY="50%"
           android:startOffset="700"
           android:duration="400" />
    </set>
</set>
```
   
屏幕坐标(0, 0)位于左上角，往右和往下坐标都会增大。

某些值，比如pivotX，可以相对于对象本身或相对于对象父节点。注意使用正确的格式(50%p是相对于父节点，50%是相对于自己)。

你能通过设置Interpolator指定变换如何随着时间变化。Android包含若干Interpolator子类，这些子类有不同的速度曲线。比如，AccelerateInterpolator会让一个变换先慢后快。

> 注意：不管动画如何移动或缩放，view的边界并不会自动变化以适应动画。尽管如此，动画仍然可以在view的边界外绘制，而且不会被剪裁。当然，如果动画超过了父view的边界，则会被剪裁。

[ref]: https://github.com/googlesamples/android-TextSwitcher
[ref2]: http://developer.android.com/reference/android/widget/ViewSwitcher.html
[ref3]: https://developer.android.com/guide/topics/resources/animation-resource.html#Twee
[ref4]: https://developer.android.com/reference/android/view/animation/Animation.html
[ref5]: https://developer.android.com/reference/android/graphics/drawable/AnimationDrawable.html