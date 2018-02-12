---
title: coordinate_layout的使用
tags:
---
# 问题描述


# 解决办法

## 1
[android - ScrollingViewBehavior for ListView - Stack Overflow](https://stackoverflow.com/questions/30612453/scrollingviewbehavior-for-listview)

<Nst>

# 参考

## 2

```java
ListView lvUser = (ListView) view.findViewById(R.id.common_load_activity_lv_list);
ViewCompat.setNestedScrollingEnabled(lvUser, true);
```

Of course nested scrolling behavior will only work from Lollipop.


```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
     listView.setNestedScrollingEnabled(true);
}
```

It will obviously only work on Lollipop.


> 	
CoordinatorLayout works with RecyclerView and any other scrolling view that support NestedScrollView. However, there seem to be classes that provide this behavior to extend to more scrolling classes like ListView, I'm hoping someone might be able to show a working code for this. So far the accepted answer shows how to make ListView work with Coordinator for API >= Lollipop. 

解决办法来自这里[android - ScrollingViewBehavior for ListView - Stack Overflow](https://stackoverflow.com/questions/30612453/scrollingviewbehavior-for-listview)


## 3

```
public class NestedScrollingListView extends ListView implements NestedScrollingChild {
private NestedScrollingChildHelper mNestedScrollingChildHelper;

public NestedScrollingListView(final Context context) {
    super(context);
    initHelper();
}

public NestedScrollingListView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    initHelper();
}

public NestedScrollingListView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initHelper();
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public NestedScrollingListView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initHelper();
}

private void initHelper() {
    mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
    setNestedScrollingEnabled(true);
}

@Override
public void setNestedScrollingEnabled(final boolean enabled) {
    mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
}

@Override
public boolean isNestedScrollingEnabled() {
    return mNestedScrollingChildHelper.isNestedScrollingEnabled();
}

@Override
public boolean startNestedScroll(final int axes) {
    return mNestedScrollingChildHelper.startNestedScroll(axes);
}

@Override
public void stopNestedScroll() {
    mNestedScrollingChildHelper.stopNestedScroll();
}

@Override
public boolean hasNestedScrollingParent() {
    return mNestedScrollingChildHelper.hasNestedScrollingParent();
}

@Override
public boolean dispatchNestedScroll(final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed, final int[] offsetInWindow) {
    return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
}

@Override
public boolean dispatchNestedPreScroll(final int dx, final int dy, final int[] consumed, final int[] offsetInWindow) {
    return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
}

@Override
public boolean dispatchNestedFling(final float velocityX, final float velocityY, final boolean consumed) {
    return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
}

@Override
public boolean dispatchNestedPreFling(final float velocityX, final float velocityY) {
    return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
}

```

## 4
将ListView切换为RecyclerView


[saulmm/CoordinatorExamples: Different CoordinatorLayout usages, tips & examples](https://github.com/saulmm/CoordinatorExamples)

[CoordinatorLayout/activity_main.xml at master · stayinxing/CoordinatorLayout](https://github.com/stayinxing/CoordinatorLayout/blob/master/MyApplication/app/src/main/res/layout/activity_main.xml)

[探索新的Android Material Design支持库 - APP开发者 - SegmentFault](https://segmentfault.com/a/1190000002976409#articleHeader7)

https://developer.android.com/reference/android/support/design/widget/AppBarLayout.html

[cheesesquare/include_list_viewpager.xml at master · chrisbanes/cheesesquare](https://github.com/chrisbanes/cheesesquare/blob/master/app/src/main/res/layout/include_list_viewpager.xml)

[一步一步深入理解CoordinatorLayout][ref1]

[CoordinatorLayout与滚动的处理][ref2]

[cheesesquare][ref3]

[ref1]: http://www.jianshu.com/p/8c92d0a1e591
[ref2]: http://www.open-open.com/lib/view/open1437312265428.html
[ref3]: https://github.com/chrisbanes/cheesesquare
[ref4]: https://material.io/guidelines/patterns/scrolling-techniques.html#