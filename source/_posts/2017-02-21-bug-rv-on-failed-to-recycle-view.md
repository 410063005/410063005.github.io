---
title: Bug系列之onFailedToRecycleView引发的血案
tags:
  - android
  - bug
date: 2017-02-21 20:15:11
---


# 问题现象
页面结构如下：

```xml
<XRecyclerView>
    <RefreshHeader />
    
    <ContentHeader>
        <View />
        <View />
        <弹幕 />
    </ContentHeader>
    
    <RecyclerViewItem />

    <RecyclerViewItem />
</XRecyclerView>
```

1. 外层的[`XRecyclerView`](https://github.com/jianghejie/XRecyclerView)继承自`RecyclerView`，支持下拉刷新
2. 这里的`RefreshHeader`是由`XRecyclerView`自动添加以支持下拉刷新的
3. `ContentHeader`中有一个使用`RecyclerView`实现的弹幕。弹幕条数多于4条时会循环播放，少于3条时播放完成后逐渐隐藏

注意，使用嵌套`RecyclerView`。问题现象是，当滚动列表到底部再回到顶部时，`<ContentHeader>`代表的View错乱，变成了一个空的`RecyclerViewItem`。但也不一定要滚动到底部，只要弹幕移出可见区域，问题就可能发生。

<video src='demo.mp4' type='video/mp4' controls='controls'  width='30%' height='30%'>
</video>

一开始百思不得奇解，凭经验认为应该是`RecyclerView`中View复用的问题。但怪就怪在为什么`ContentHeader`会变成`RecyclerViewItem`。还有几个诡异的地方：

1. 弹幕循环播放时，问题不复现
2. `RecyclerViewItem`数量为0时，问题不复现

# 问题分析
打印日志看一下。(好吧，其实在准确的位置打印日志并展开分析之前我各种折腾浪费了不少时间)

```java
@Override
public int getItemViewType(int position) {
    ...
}

@Override
public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    System.out.println("onCreateViewHolder viewType=" + viewType);
}
```

发现两个不符合预期的地方：

1. 日志输出为`onCreateViewHolder viewType=10002`，而`10002`并不是我们自己在`getItemViewType(int position)`中定义的。`10002`从哪里来？
2. `onCreateViewHolder viewType=10002`会输出两次。为什么`onCreateViewHolder`调用两次？

## 10002从哪里来
这要从`XRecyclerView`特性说起来。`XRecyclerView`继承自`RecyclerView`，支持`header`和`footer`，有点类似于`ListView`。

Google官方明确地从`RecyclerView`中移除了对`header`和`footer`的支持，不知到底是为了保持API简单(移除长尾特性)，还是说明`header`和`footer`是不好的做法，因为特殊item完全也能实现`header`和`footer`，当然实现起来不如直接支持`header`和`footer`来得方便。[RecyclerView ins and outs - Google I/O 2016](https://www.youtube.com/watch?v=LqBlYJTfLP4)

这里只就`header`展开分析，`footer`情况类似。直接上代码。

```java
public void addHeaderView(View view) {
    if (pullRefreshEnabled && !(mHeaderViews.get(0) instanceof ArrowRefreshHeader)) {
        ArrowRefreshHeader refreshHeader = new ArrowRefreshHeader(mContext);
        mHeaderViews.add(0, refreshHeader);
        mRefreshHeader = refreshHeader;
        mRefreshHeader.setProgressStyle(mRefreshProgressStyle);
    }
    mHeaderViews.add(view);
    sHeaderTypes.add(HEADER_INIT_INDEX + mHeaderViews.size());
}

@Override
public void setAdapter(Adapter adapter) {
    mAdapter = adapter;
    mWrapAdapter = new WrapAdapter(mHeaderViews, mFootViews, adapter);
    super.setAdapter(mWrapAdapter);
    mAdapter.registerAdapterDataObserver(mDataObserver);
    mDataObserver.onChanged();
}

private class WrapAdapter extends RecyclerView.Adapter<ViewHolder> {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_REFRESH_HEADER) {
            mCurrentPosition++;
            return new SimpleViewHolder(mHeaderViews.get(0));
        } else if (isContentHeader(mCurrentPosition)) {
            if (viewType == sHeaderTypes.get(mCurrentPosition - 1)) {
                mCurrentPosition++;
                View view = mHeaderViews.get(headerPosition++);
                return new SimpleViewHolder(view);
            }
        } else if (viewType == TYPE_FOOTER) {
            return new SimpleViewHolder(mFootViews.get(0));
        }
        return adapter.onCreateViewHolder(parent, viewType);
    }
}
```

+ `XRecyclerView.addHeaderView()`方法用于添加头部，`XRecyclerView`会持有作为header的View
+ `XRecyclerView.setAdapter()`是关键所在，它会将原始的`Adapter`包装成`WrapAdapter`
+ `WrapAdapter`的特别之处在于它为`header`创建`SimpleViewHolder`，非`header`则代理给原始的`Adapter`

上面最后一点，可以解释我们遇到的问题。可能因为某种原因，有个`header`没被正确地识别出来，结果走到了`return adapter.onCreateViewHolder(parent, viewType)`这个分支。于是我们的自己的`Adapter`中看到了10002这个预期外的`viewType`。

为什么`XRecyclerView`的`header`没被正确地识别出来？分析`WrapAdapter.onCreateViewHolder()`，可以看出其实它对`mCurrentPosition`的计算很有问题。如果`onCreateViewHolder()`重复调用，`mCurrentPosition`肯定会算错。原本以为这是个bug，但另一方面考虑到`header`数量的不确定性，这里的`mCurrentPosition`好像的确没有更好的计算方式。

联系起来思考：会不会是`onCreateViewHolder()`调用两次，导致`mCurrentPosition`不对，进一步导致`header`没有识别出来。最终引起后面的一系列错误。

## 为什么`onCreateViewHolder`调用两次
正常来说`onCreateViewHolder()`肯定只为每个位置调用一次，调用后创建出来的`ViewHolder`是可以回收复用的。有没有异常情况？搜到了`RecyclerView.Adapter.onFailedToRecycleView(ViewHolder holder)`这个方法。

## onFailedToRecycleView
> Called by the RecyclerView if a ViewHolder created by this Adapter cannot be recycled due to its transient state. Upon receiving this callback, Adapter can clear the animation(s) that effect the View's transient state and return true so that the View can be recycled. Keep in mind that the View in question is already removed from the RecyclerView.
> In some cases, it is acceptable to recycle a View although it has transient state. Most of the time, this is a case where the transient state will be cleared in onBindViewHolder(RecyclerView.ViewHolder, int) call when View is rebound to a new position. For this reason, RecyclerView leaves the decision to the Adapter and uses the return value of this method to decide whether the View should be recycled or not.
> Note that when all animations are created by RecyclerView.ItemAnimator, you should never receive this callback because RecyclerView keeps those Views as children until their animations are complete. This callback is useful when children of the item views create animations which may not be easy to implement using an RecyclerView.ItemAnimator.
> You should never fix this issue by calling holder.itemView.setHasTransientState(false); unless you've previously called holder.itemView.setHasTransientState(true);. Each View.setHasTransientState(true) call must be matched by a View.setHasTransientState(false) call, otherwise, the state of the View may become inconsistent. You should always prefer to end or cancel animations that are triggering the transient state instead of handling it manually.

> 如果当前`Adapter`创建的`ViewHolder`由于其临时状态不能被回收复用，则`RecyclerView`会调用`onFailedToRecycleView`方法。`Adapter`可以在这个回调方法中清理影响/导致`ViewHolder`临时状态的动画并返回`true`。返回`true`可以让`View`被回收复用。但要注意，这个有问题的`View`此时已经从`RecyclerView`中移除。
> 某些情况下，一些`View`虽然有临时状态但仍然可被回收复用。大多数时候，这些临时状态会在当前`View`绑定到新的位置 时由`onBindViewHolder(RecyclerView.ViewHolder, int)`给清理掉。所以`RecyclerView`把决定权留给`Adapter`，并使用`onFailedToRecycleView()`方法的返回值作为`View`是否被回收的依据。
> 注意，所有由`RecyclerView.ItemAnimator`创建的动画，都不会导致`onFailedToRecycleView()`调用，因为`RecyclerView`会一直保留带这种动画的`View`直到动画完成。`onFailedToRecycleView()`回调对于处理那些创建复杂动画的`View`非常有效。这里的复杂动画指的是那些无法用`RecyclerView.ItemAnimator`实现的动画
> 注意，你不能简单地调用`holder.itemView.setHasTransientState(false);`来避免这个问题，除非你之前调用过`holder.itemView.setHasTransientState(true);`。每个`View.setHasTransientState(true)`调用必须跟`View.setHasTransientState(false)`调用配对。否则`View`的状态不一致。你通常应当结束或取消引起`View`临时状态的动画，而不是手动调用`View.setHasTransientState(false)`来应对这种情况

文档说得很明白，`onFailedToRecycleView(ViewHolder holder)是`View`不能被回收复用时的回调方法。我们可以在这个方法清理`View`的临时状态。

1. `ContentHeader`是不是不能被回收？
2. 是不这些动画导致`ContentHeader`是不是不能被回收

在`WrapAdapter.onFailedToRecycleView()`中打印日志，发现这个方法果然被调用了，证明`ContentHeader`的确回收失败，所以下次使用`ContentHeader`会重新创建。这解释了为什么`onCreateViewHolder`调用两次。

弹幕条数多于4条时会循环播放，少于3条时播放完成后逐渐隐藏。前者是使用`RecyclerView.ItemAnimator`实现的，而后者是自定义动画。根据文档，正是这里的自定义动画导致`ContentHeader`不能被回收。


```java
@Override
public boolean onFailedToRecycleView(ViewHolder holder) {
    System.out.println("create 10002: onFailedToRecycleView");
    return super.onFailedToRecycleView(holder);
}

@Override
public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == 10002) {
        System.out.println("create 10002");
}
```

```
02-21 11:36:13.462 15618-15618/com.tencent.PmdCampus I/System.out: create 10002
02-21 11:36:20.022 15618-15618/com.tencent.PmdCampus I/System.out: create 10002
02-21 11:36:23.802 15618-15618/com.tencent.PmdCampus I/System.out: create 10002: onFailedToRecycleView
02-21 11:36:23.812 15618-15618/com.tencent.PmdCampus I/System.out: create 10002
```

# 解决办法
找到原因后问题其实就解决了一半。重写`WrapAdapter.onFailedToRecycleView()`方法，代码如下：

```java
@Override
public boolean onFailedToRecycleView(ViewHolder holder) {
    if (holder instanceof WrapAdapter.SimpleViewHolder) {
        WrapAdapter.SimpleViewHolder h = (SimpleViewHolder) holder;
        BBViewImpl v = (BBViewImpl) h.itemView.findViewById(R.id.bb);
        if (v != null) {
            // 清理动画
            v.stop();
        }
        return true;
    }
    return super.onFailedToRecycleView(holder);
}
```

界面错乱问题解决！

总结：

1. 注意`View`的动画可能导致出现临时状态，对于有临时状态的`View`，`RecyclerView`默认是不回收的
2. 我们在使用`RecyclerView`时，通常只关注`onCreateViewHolder()`和`onBindViewHolder()`。但必要时也应关注`onFailedToRecycleView()`等方法。

<!--
# 补充
还出现一个现象， 某些情况下， 弹幕会卡住。 -->

