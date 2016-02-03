---
layout: post
title: "android property animation"
date: 2016-02-03 20:28:06 +0800
comments: true
categories: android view
published: true
---
本文介绍了属性动画的用法，翻译自[官方文档][ref]。
<!--more-->
# 属性动画
属性动画是一个强壮的框架，允你给所有东西添加动画效果(allows you to animate almost anything)。你可以定义动画，随着时间来改变对象的属性，而不管它是否绘制到屏幕。一个属性动画会在一段时间内改变一个属性(一个对象的字段)。为了添加动画，可以指定想要animate的对象属性，比如对象在屏幕上的位置，动画时长，动画效果值。

属性动画系统允许定义动画的如下特性：

+ 持续时间：可以定义动画持续时间。缺省的时间是300ms
+ 时间插值：可以指定属性如何被计算，计算方法是时间的某个函数
+ 重复次数和方式：可以指定动画持续时间到达后动画是否重复，以及重复的次数。还可以指定动画是以相反的次序回放，还是重新开始。设置为`reverse`会反复地播放和回放(Setting it to reverse plays the animation forwards then backwards repeatedly)，直到达到重复次数
+ Animator集合：可以将一组动画添加到一个分组中，并同时或顺序或以特定的延迟时间播放
+ Frame更新延时：可以指定以多快的频率更新动画帧。缺省值是10ms，但应用中最终的帧更新速度由系统整体负载以及系统可以多快地响应底层的timer来决定

# 属性动画如何工作
首先，我们用一个简单的例子看看动画是如何工作的。图片演示了一个假想的对象对其`x`属性添加动画，`x`属性表示对象在屏幕上的水平位置。动画的持续时间为40ms，移动的距离为40像素。每10ms，即缺省的帧刷新频率，该对象会水平移动10像素。40ms时间动画会结束，对象停在水平方向的40像素的地方。这是线性插值的例子，意味着对象以常速运动。

![animation-linear](http://7xn5nf.com1.z0.glb.clouddn.com/image%2Fblog%2F2016%2F02%2Fanimation-linear.png)

你也可以指定非线性插值的动画。图2演示了一个假想的对象开始时加速运动，快结束时减速运动。该对象仍然在40ms内运动40像素，非线性的。一开始，对象加速到中点，然后从中点开始减速直到结束。如图2所示，该对象在动画开始和结束时移动的距离小于在动画中间运动的距离。

![animation-nonlinear](http://7xn5nf.com1.z0.glb.clouddn.com/image%2Fblog%2F2016%2F02%2Fanimation-nonlinear.png)

现在我们来仔细看看上述过程。图3描述了主要的类。

![valueanimator](http://7xn5nf.com1.z0.glb.clouddn.com/image%2Fblog%2F2016%2F02%2Fvalueanimator.png)

`ValueAnimator`对象记录了对象的时序，比如动画运行了多久，属性的当前值是多少。

`ValueAnimator`封装了`TimeInterpolator`，后者定义如何动画插值，以及一个`TypeEvaluator`，它定义了如何计算动画属性值。比如，在图2中，被使用的`TimeInterpolator`和`TypeEvaluator`分别是`AccelerateDecelerateInterpolator`和`IntEvaluator`。

可以创建`ValueAnimator`并设置属性的开始值和结束值，以及持续时间来启动一个动画。调用`start()`方法时动画将开始运行。在整个动画过程中，`ValueAnimator`计算出一个 *介于0和1之间的动画分数* (elapsed fraction between 0 and 1)，基于动画持续时间和已过去的时间。动画分数表示该动画已完成的比例，0表示0%，1表示100%。比如，在图1中，t=10ms时动画分数为0.25，因为动画持续时间为40ms。

当`ValueAnimator`计算完动画分数后，它会调用`TimeInterpolator`来计算插值分数。插值分数将动画分数映射到一个新的值，映射时会考虑当前的插值方法。比如，在图2中动画缓缓加速，在t=10ms时插值分数约为0.15，比相应的动画分数要小(动画分数为0.25)。图1中，插值分数永远跟动画分数相同。

当插值分数计算完成后，`ValueAnimator`调用相应的`TypeEvaluator`基于动画的插值分数，开始值，结束值来计算属性值。比如，图2中，t=10ms时插值分数为0.15，所以该时刻的属性值为0.15X(40-0)，即6。

API Demos中的`com.example.android.apis.animation`包提供例子用于演示如何使用属性动画。

# 属性动画跟View动画的区别
View动画系统只能给`View`对象添加动画，所以如果想要给非view对象添加动画效果时，你得自己写代码。View动画还受限于仅提供了很少的特性，比如缩放，旋转，但不能修改背景颜色。

View动画另一个问题是它仅仅修改View绘制的位置，而不是View本身。比如你可以使用动画在屏幕上移动button，button可以在正确的位置绘制，但实际上你可点击的位置根本没有变化，所以你必须自己实现代码来处理这种情况。

而使用属性动画，完全没有这些限制。你可以给任意对象的任意属性添加动画(View和非View)，而且对象确实被修改了。属性动画系统在实现动画效果时健壮性更好。在更高层次上，你可以为你想要添加动画效果的属性设置animator，比如颜色，位置，大小等，还可以定义插值和多animator同步等方面。

View动画系统，需要更少的时间开销和代码。如果view动画实现了你想要的效果，又或者已有代码以你想要的方式工作，没必要使用属性动画。如果用例需要，可以为不同的情况使用不同的动画。

# API概述
可以在`android.animation`包中找到大部分属性动画API。因为View动画系统已经在`android.view.animation`包中定义了非常多的插值器，可以直接在属性动画中使用这些插值器。下表描述了属性动画系统中的主要组件。

`Animator`类提供创建动画的基本结构。通常不会直接使用这个类，因为它只提供最小化的功能，需要扩展才能完全支持。下面这些类继承自`Animator`：

表1， Animator

|Class|Description|
|-----|-----------|
|ValueAnimator|属性动画的timing engine，会计算动画值。它包含计算动画值，保存动画时间信息，是否重复动画，接受事件更新的监听器，设置自定义evalute类型等所有的核心功能。动画属性有两个关键，一是计算动画值，二是将这些值设置到相应的对象和属性。ValueAnimator并不处理第二点，所以你要自己监听ValueAnimator计算出来的新值并修改对象属性。|
|ObjectAnimator|ValueAnimator的子类，允许设置目标对象以及属性。这个类计算出新的动画值后会更新相应的属性。大部分时候你会使用ObjectAnimator因为它让处理目标对象的动画值的过程变得简单很多。但有时你想使用ValueAnimator，因为ObjectAnimator有少量限制，比如要求目标对象有特定的setter/getter方法|
|AnimatorSet|提供动画分组机制。可以同时或顺序或以特定延迟运行多个动画。|

表2，Evaluators

|Class/Interface|Description|
|----|-----|
|IntEvaluator|用于计算`int`类型的属性的缺省evaluator|
|FloatEvalutor|用于计算`float`类型的属性的缺省evaluator|
|ArgbEvalutor|用于计算十六进制表示的颜色值类型的属性的缺省evaluator|
|TypeEvalutor|接口，允许自己实现evaluator。如果你对非`int`, `float`或颜色属性添加动画，必须实现`TypeEvaluator`接口以指定如何计算对象的动画值。也可以对`int`,`float`或颜色值设置自定义的`TypeEvaluator`以提供不同的行为|

时间插值器定义以时间为函数如何计算动画值。比如，你可以指定动画以线性方式运行，这意味着动画在整个过程中匀速移动。也可以使用非线性方式，比如在开始时加速，在结束时减速。表3描述了`android.view.animator`中包含的插值器。如果没有适合你的，可以实现`TimeInterpolator`接口来创建自己的插值器。

<表3，略>

# 使用ValueAnimator
The ValueAnimator class lets you animate values of some type for the duration of an animation by specifying a set of int, float, or color values to animate through.通过调用某个工厂方法得到`ValueAnimator`：`ofInt()`, `ofFloat()`, 或`ofObject()`。比如：

```
ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
animation.setDuration(1000);
animation.start();
```

这段代码中，`start()`方法调用后，`ValueAnimator`计算动画值(0到1之间)，持续时间为1000ms。

也可以用下面的方法指定特殊的类型：

```
ValueAnimator animation = ValueAnimator.ofObject(new MyTypeEvaluator(), startPropertyValue, endPropertyValue);
animation.setDuration(1000);
animation.start();
```

这段代码中，`start()`方法调用后，`ValueAnimator`计算动画值(`startPropertyValue`到`endPropertyValue`之间)，使用`MyTypeEvaluator`提供的计算逻辑，持续时间为1000ms。

前面的几段代码，并不会对对象产生任何实际的影响，因为`ValueAnimator`并不会直接操作对象或属性。你最想做的事就是使用计算出来的值修改对象。可以在`ValueAnimator`中定义监听器以在动画的生命周期中恰当地处理事件，比如帧更新。当实现监听器时，可以调用`getAnimatedValue()`为特定的帧更新获取计算好的值。

# 使用ObjectAnimator
`ObjectAnimator`是`ValueAnimator`的子类。它由时间引擎和`ValueAnimator`组成。这种设计下，为任何对象添加动画都容易很多，而你不必实现`ValueAnimator.AnimatorUpdateListener`，因为它会自动更新。

实例化`ObjectAnimator`跟`ValueAnimator`类似，但你同是还要指定对象以及对象的属性名(字符串类型)，以及动画值：

```
ObjectAnimator anim = ObjectAnimator.ofFloat(foo, "alpha", 0f, 1f);
anim.setDuration(1000);
anim.start();
```

想让`ObjectAnimator`正确地更新属性，必须遵守：

+ 对象属性必须有setter方法，形式为`set<propertyName>()`。由于`ObjectAnimator`在动画过程中自动更新属性，它必须能使用这个setter方法访问属性。比如，有个名为`foo`的属性，相应地需要`setFoo()`方法。如果setter方法不存在，有以下办法：
 + 如果你能添加setter方法，添加即可
 + 如果不能添加setter方法，使用wrapper类，wrapper使用有效的setter接受值并将它传到原来的对象
 + 使用`ValueAnimator`代替
+ 如果你只在`ObjectAnimator`工厂方法中的`values...`指定一个值，它假定这个值是结束值。因此，对象属性必须有getter方法，它用于获取动画的开始值。getter方法的形式为`get<propertyName>()`。比如，属性名为`foo`，你需要有一个`getFoo()`方法。
+ The getter (if needed) and setter methods of the property that you are animating must operate on the same type as the starting and ending values that you specify to ObjectAnimator. 比如，假如你构建以下`ObjectAnimator`，你必须有`targetObject.setPropname(float)`和`targetObject.getPropName(float)`：



[ref]: https://developer.android.com/intl/zh-cn/guide/topics/graphics/prop-animation.html#value-animator[]()# 属性动画
属性动画是一个强壮的框架，允你给所有东西添加动画效果(allows you to animate almost anything)。你可以定义动画，随着时间来改变对象的属性，而不管它是否绘制到屏幕。一个属性动画会在一段时间内改变一个属性(一个对象的字段)。为了添加动画，可以指定想要animate的对象属性，比如对象在屏幕上的位置，动画时长，动画效果值。

属性动画系统允许定义动画的如下特性：

+ 持续时间：可以定义动画持续时间。缺省的时间是300ms
+ 时间插值：可以指定属性如何被计算，计算方法是时间的某个函数
+ 重复次数和方式：可以指定动画持续时间到达后动画是否重复，以及重复的次数。还可以指定动画是以相反的次序回放，还是重新开始。设置为`reverse`会反复地播放和回放(Setting it to reverse plays the animation forwards then backwards repeatedly)，直到达到重复次数
+ Animator集合：可以将一组动画添加到一个分组中，并同时或顺序或以特定的延迟时间播放
+ Frame更新延时：可以指定以多快的频率更新动画帧。缺省值是10ms，但应用中最终的帧更新速度由系统整体负载以及系统可以多快地响应底层的timer来决定

# 属性动画如何工作
首先，我们用一个简单的例子看看动画是如何工作的。图片演示了一个假想的对象对其`x`属性添加动画，`x`属性表示对象在屏幕上的水平位置。动画的持续时间为40ms，移动的距离为40像素。每10ms，即缺省的帧刷新频率，该对象会水平移动10像素。40ms时间动画会结束，对象停在水平方向的40像素的地方。这是线性插值的例子，意味着对象以常速运动。

![animation-linear]()

你也可以指定非线性插值的动画。图2演示了一个假想的对象开始时加速运动，快结束时减速运动。该对象仍然在40ms内运动40像素，非线性的。一开始，对象加速到中点，然后从中点开始减速直到结束。如图2所示，该对象在动画开始和结束时移动的距离小于在动画中间运动的距离。

![animation-nonlinear]()

现在我们来仔细看看上述过程。图3描述了主要的类。

![valueanimator]()

`ValueAnimator`对象记录了对象的时序，比如动画运行了多久，属性的当前值是多少。

`ValueAnimator`封装了`TimeInterpolator`，后者定义如何动画插值，以及一个`TypeEvaluator`，它定义了如何计算动画属性值。比如，在图2中，被使用的`TimeInterpolator`和`TypeEvaluator`分别是`AccelerateDecelerateInterpolator`和`IntEvaluator`。

可以创建`ValueAnimator`并设置属性的开始值和结束值，以及持续时间来启动一个动画。调用`start()`方法时动画将开始运行。在整个动画过程中，`ValueAnimator`计算出一个 *介于0和1之间的动画分数* (elapsed fraction between 0 and 1)，基于动画持续时间和已过去的时间。动画分数表示该动画已完成的比例，0表示0%，1表示100%。比如，在图1中，t=10ms时动画分数为0.25，因为动画持续时间为40ms。

当`ValueAnimator`计算完动画分数后，它会调用`TimeInterpolator`来计算插值分数。插值分数将动画分数映射到一个新的值，映射时会考虑当前的插值方法。比如，在图2中动画缓缓加速，在t=10ms时插值分数约为0.15，比相应的动画分数要小(动画分数为0.25)。图1中，插值分数永远跟动画分数相同。

当插值分数计算完成后，`ValueAnimator`调用相应的`TypeEvaluator`基于动画的插值分数，开始值，结束值来计算属性值。比如，图2中，t=10ms时插值分数为0.15，所以该时刻的属性值为0.15X(40-0)，即6。

API Demos中的`com.example.android.apis.animation`包提供例子用于演示如何使用属性动画。

# 属性动画跟View动画的区别
View动画系统只能给`View`对象添加动画，所以如果想要给非view对象添加动画效果时，你得自己写代码。View动画还受限于仅提供了很少的特性，比如缩放，旋转，但不能修改背景颜色。

View动画另一个问题是它仅仅修改View绘制的位置，而不是View本身。比如你可以使用动画在屏幕上移动button，button可以在正确的位置绘制，但实际上你可点击的位置根本没有变化，所以你必须自己实现代码来处理这种情况。

而使用属性动画，完全没有这些限制。你可以给任意对象的任意属性添加动画(View和非View)，而且对象确实被修改了。属性动画系统在实现动画效果时健壮性更好。在更高层次上，你可以为你想要添加动画效果的属性设置animator，比如颜色，位置，大小等，还可以定义插值和多animator同步等方面。

View动画系统，需要更少的时间开销和代码。如果view动画实现了你想要的效果，又或者已有代码以你想要的方式工作，没必要使用属性动画。如果用例需要，可以为不同的情况使用不同的动画。

# API概述
可以在`android.animation`包中找到大部分属性动画API。因为View动画系统已经在`android.view.animation`包中定义了非常多的插值器，可以直接在属性动画中使用这些插值器。下表描述了属性动画系统中的主要组件。

`Animator`类提供创建动画的基本结构。通常不会直接使用这个类，因为它只提供最小化的功能，需要扩展才能完全支持。下面这些类继承自`Animator`：

表1， Animator

|Class|Description|
|-----|-----------|
|ValueAnimator|属性动画的timing engine，会计算动画值。它包含计算动画值，保存动画时间信息，是否重复动画，接受事件更新的监听器，设置自定义evalute类型等所有的核心功能。动画属性有两个关键，一是计算动画值，二是将这些值设置到相应的对象和属性。ValueAnimator并不处理第二点，所以你要自己监听ValueAnimator计算出来的新值并修改对象属性。|
|ObjectAnimator|ValueAnimator的子类，允许设置目标对象以及属性。这个类计算出新的动画值后会更新相应的属性。大部分时候你会使用ObjectAnimator因为它让处理目标对象的动画值的过程变得简单很多。但有时你想使用ValueAnimator，因为ObjectAnimator有少量限制，比如要求目标对象有特定的setter/getter方法|
|AnimatorSet|提供动画分组机制。可以同时或顺序或以特定延迟运行多个动画。|

表2，Evaluators

|Class/Interface|Description|
|----|-----|
|IntEvaluator|用于计算`int`类型的属性的缺省evaluator|
|FloatEvalutor|用于计算`float`类型的属性的缺省evaluator|
|ArgbEvalutor|用于计算十六进制表示的颜色值类型的属性的缺省evaluator|
|TypeEvalutor|接口，允许自己实现evaluator。如果你对非`int`, `float`或颜色属性添加动画，必须实现`TypeEvaluator`接口以指定如何计算对象的动画值。也可以对`int`,`float`或颜色值设置自定义的`TypeEvaluator`以提供不同的行为|

时间插值器定义以时间为函数如何计算动画值。比如，你可以指定动画以线性方式运行，这意味着动画在整个过程中匀速移动。也可以使用非线性方式，比如在开始时加速，在结束时减速。表3描述了`android.view.animator`中包含的插值器。如果没有适合你的，可以实现`TimeInterpolator`接口来创建自己的插值器。

<表3，略>

# 使用ValueAnimator
The ValueAnimator class lets you animate values of some type for the duration of an animation by specifying a set of int, float, or color values to animate through.通过调用某个工厂方法得到`ValueAnimator`：`ofInt()`, `ofFloat()`, 或`ofObject()`。比如：

```
ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
animation.setDuration(1000);
animation.start();
```

这段代码中，`start()`方法调用后，`ValueAnimator`计算动画值(0到1之间)，持续时间为1000ms。

也可以用下面的方法指定特殊的类型：

```
ValueAnimator animation = ValueAnimator.ofObject(new MyTypeEvaluator(), startPropertyValue, endPropertyValue);
animation.setDuration(1000);
animation.start();
```

这段代码中，`start()`方法调用后，`ValueAnimator`计算动画值(`startPropertyValue`到`endPropertyValue`之间)，使用`MyTypeEvaluator`提供的计算逻辑，持续时间为1000ms。

前面的几段代码，并不会对对象产生任何实际的影响，因为`ValueAnimator`并不会直接操作对象或属性。你最想做的事就是使用计算出来的值修改对象。可以在`ValueAnimator`中定义监听器以在动画的生命周期中恰当地处理事件，比如帧更新。当实现监听器时，可以调用`getAnimatedValue()`为特定的帧更新获取计算好的值。

# 使用ObjectAnimator
`ObjectAnimator`是`ValueAnimator`的子类。它由时间引擎和`ValueAnimator`组成。这种设计下，为任何对象添加动画都容易很多，而你不必实现`ValueAnimator.AnimatorUpdateListener`，因为它会自动更新。

实例化`ObjectAnimator`跟`ValueAnimator`类似，但你同是还要指定对象以及对象的属性名(字符串类型)，以及动画值：

```
ObjectAnimator anim = ObjectAnimator.ofFloat(foo, "alpha", 0f, 1f);
anim.setDuration(1000);
anim.start();
```

想让`ObjectAnimator`正确地更新属性，必须遵守：

+ 对象属性必须有setter方法，形式为`set<propertyName>()`。由于`ObjectAnimator`在动画过程中自动更新属性，它必须能使用这个setter方法访问属性。比如，有个名为`foo`的属性，相应地需要`setFoo()`方法。如果setter方法不存在，有以下办法：
 + 如果你能添加setter方法，添加即可
 + 如果不能添加setter方法，使用wrapper类，wrapper使用有效的setter接受值并将它传到原来的对象
 + 使用`ValueAnimator`代替
+ 如果你只在`ObjectAnimator`工厂方法中的`values...`指定一个值，它假定这个值是结束值。因此，对象属性必须有getter方法，它用于获取动画的开始值。getter方法的形式为`get<propertyName>()`。比如，属性名为`foo`，你需要有一个`getFoo()`方法。
+ The getter (if needed) and setter methods of the property that you are animating must operate on the same type as the starting and ending values that you specify to ObjectAnimator. 比如，假如你构建以下`ObjectAnimator`，你必须有`targetObject.setPropname(float)`和`targetObject.getPropName(float)`：

```
ObjectAnimator.ofFloat(targetObject, "propName", 1f)

```
+ 根据对象属性的不同，你可能需要调用`invalidate()`方法以强制使用新的动画值刷新屏幕。在`onAnimationUpdate()`调用中进行这些处理。比如，为Drawable对象的颜色属性添加动画，仅在对象重绘时更新屏幕。View中所有属性的setter，比如`setAlpha()`和`setTranslationX()`会正确地invalidate View，所以你不需要主动调用invalidate。

# AnimationSet
许多情况下，你想根据另一个动画是否启动或结束来启动新的动画。Android系统允许你使用`AnimationSet`打包多个动画，所以你可以指定是否同时，或顺序，或特定延迟来启动多个动画。还可以嵌套AnimatorSet对象。

下面的代码来自[Bouncing Balls](https://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/animation/BouncingBalls.html) (简化过)：

1. 运行`bounceAnim`
2. 同时运行 `squashAnim1`, `squashAnim2`, `stretchAnim1`, 和`stretchAnim2`
3. 运行`bounceBackAnim`
4. 运行`fadeAnim`


```
AnimatorSet bouncer = new AnimatorSet();
bouncer.play(bounceAnim).before(squashAnim1);
bouncer.play(squashAnim1).with(squashAnim2);
bouncer.play(squashAnim1).with(stretchAnim1);
bouncer.play(squashAnim1).with(stretchAnim2);
bouncer.play(bounceBackAnim).after(stretchAnim2);
ValueAnimator fadeAnim = ObjectAnimator.ofFloat(newBall, "alpha", 1f, 0f);
fadeAnim.setDuration(250);
AnimatorSet animatorSet = new AnimatorSet();
animatorSet.play(bouncer).before(fadeAnim);
animatorSet.start();
```

# Animation监听器
可以使用监听器监听动画期间的重要事件：

+ `Animator.AnimatorListener`
 + `onAnimationStart()` - 动画启动时回调
 + `onAnimationEnd()` - 动画结束时回调
 + `onAnimationRepeat()` - 动画重复时回调
 + `onAnimationCancel()` - 动画取消时回调。取消动画时也会调用`onAnimationEnd()`
+ `ValueAnimator.AnimatorUpdateListener`
 + `onAnimationUpdate()` - 每个动画帧时调用。动画期间想使用`ValueAnimator`计算得到的值时可监听这个事件。从传入的`ValueAnimator`对象调用`getAnimatedValue()`获取计算出的值。

当不想实现`Animator.AnimatorListener`的全部方法时，可以继承`AnimatorListenerAdapter`类而不是实现`Animator.AnimatorListener`接口。`AnimatorListenerAdapter`提供全部方法的空实现，你可以有选择地覆盖。

比如，`Bouncing Balls`示例中创建如下只实现`onAnimationEnd()`回调的监听器：

```
ValueAnimatorAnimator fadeAnim = ObjectAnimator.ofFloat(newBall, "alpha", 1f, 0f);
fadeAnim.setDuration(250);
fadeAnim.addListener(new AnimatorListenerAdapter() {
public void onAnimationEnd(Animator animation) {
    balls.remove(((ObjectAnimator)animation).getTarget());
}
```

# Animating Layout Changes to ViewGroups
属性动画系统提供给ViewGroup对象添加动画的能力，就像给View添加动画一样。

可以使用`LayoutTransition`类给ViewGroup设置动画。Views inside a ViewGroup can go through an appearing and disappearing animation when you add them to or remove them from a ViewGroup or when you call a View's setVisibility() method with VISIBLE, INVISIBLE或GONE。


[ref]: https://developer.android.com/intl/zh-cn/guide/topics/graphics/prop-animation.html#value-animator