---
title: 如何在ActionBar上显示引导提示
tags:
  - Android
toc: true
date: 2018-01-11 21:04:51
---
你一定在微信朋友圈见过弹出来指向右上角的箭头，"诱导"你去分享。通常来说，弹出来的箭头只能指向右上角，没法指"进"右上角。有没有办法实现实现指"进"右上角的效果呢？微信中h5应该是没有办法的，但App原生代码中是可以实现的，来看看怎么做的吧。

<!--more-->

# 问题
视觉上要求在ActionBar的OptionMenu上弹出一个提示：

![](/images/1515671077336.webp)

内心的感受是~!^%$#@!!!#))*。 但抛开需求或设计上的合理性不谈，那到底如何实现这种效果呢？要知道，那个小圆点可是跑到ActionBar上面去了啊！

# 解决办法
实际项目中，一般都会定义BaseActivity用于统一控制页面结构。比如我们项目的BaseActivity结构如下：

```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/common_base_activity_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!--action bar-->
    <android.support.v7.widget.Toolbar
        android:id="@+id/common_base_activity_toolbar"
        android:layout_width="fill_parent"
        android:layout_height="50dp" />

    <!--content被添加到这里了-->
    </>
</LinearLayout>
```

id为`common_base_activity_toolbar`的ToolBar会添加如下View作为ActionBar(标题栏)，而布局文件中定义的内容会添加到标题栏下方。

![](/images/1515673781733.webp)

BaseActivity统一处理上述逻辑。通常情况下这种方式工作得很好，大家都很happy，因为继承自BaseActivity的页面处理标题栏很省事。但同时，由于BaseActivity加载了上述结构的布局文件，设计缺少灵活性，如果有页面要使用特殊的标题栏就非常麻烦了。

## 方案一

考虑到BaseActivity的限制，一种容易想到的解决办法是绕开BaseActivity另起炉灶。由于不继承BaseActivity，一切从头开始，所以上述实现上述视觉效果并不存在困难。但不继承BaseActivity可能给项目造成混乱。

## 方案二

Android设置`android:clipChildren`可以让View超出父View的边界，从而达到的特殊UI效果。这里是一个[例子](http://blog.csdn.net/zhangphil/article/details/48655411)。简单尝试后失败，可能的原因如下：

+ 要让`andorid:clipChildren`属性生效，必须设置在准确的View上，而我们的布局比例子中要复杂，这个准确的节点应该是位置BaseActivity的布局中
+ 只有固定大小的View才可以超出父View (?待求证)

## 方案三

BaseActivity的布局文件中的`LinearLayout`其实并不是页面最终的根节点，真正的根节点是Android系统添加的`FrameLayout`。这个特别的`FrameLayout` id是`android.R.id.content`。通过`findViewById(android.R.id.content)`不难找到这个`FrameLayout`，我们可以将视觉需求中的那个提示控件添加到`FrameLayout`上，从而实现要求的效果。关键代码如下：

```java
    void showHint(int topMargin, int layoutId) {
        // 获取FrameLayout
        ViewGroup activityContent = (ViewGroup) findViewById(android.R.id.content);
        // 生成hint
        View hintView = getLayoutInflater().inflate(layoutId, mActivityContent, false);
        // 调整布局参数
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) hintView.getLayoutParams();
        if (params != null) {
            params.topMargin = topMargin;
            hintView.setLayoutParams(params);
        }
        // 添加并显示hint
        activityContent.addView(hintView);
    }
```

在Layout Inspector可以很直观地看到上述代码对布局的修改。

![](/images/1515672774540.webp)

## 方案四

我还脑洞大开地想出了这个方案，

![](/images/1515674263401.webp)

![](/images/1515674523010.webp)

弹出提示和没有弹出提示时分别使用不同的图标，通过"欺骗"用户的方式实现最终效果，只是代码会写得很扯。

# 总结
方案三以比较简单低成本的方式实现了需求。暂时没想到更好的实现方法。如果你有，可以告诉我。


如果BaseActivity一开始是这样设计的，可能具有更多的灵活性：

+ BaseActivity是抽象的，统一完成对标题栏的处理
+ BaseActivity不直接加载布局，由子类加载布局
+ 子类加载的布局文件可以使用include方式使用公共的ActionBar，也可以自定义不同的ActionBar，只要保证ActionBar中View的id一致即可

Java代码和布局文件分别如下：

```java
public abstract class BaseActivity extends AppCompatActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		contentView();
		setupActionBar();
	}
	
	protected void setupActionBar() {
        // 设置标题栏
        // ...
	}
	
	public abstract void contentView();
}

public class DemoActivity extends BaseActivity {
	
	@Override
	public void contentView() {
		setContentView(R.layout.activity_demo);
	}
}
```

```xml
<!-- activity_demo.xml -->

<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/common_base_activity_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!--action bar-->
	<!-- include公共的ActionBar 也可以自己自定义ActionBar -->
    <include layout="@layout/common_toolbar" />

    <!--content-->
	<LinearLayout />
	
</LinearLayout>
```