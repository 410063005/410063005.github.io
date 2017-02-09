---
title: 应用启动速度优化
tags: android
---
同学我来了的首页采用经典的多tab页结构(类似微信首页)。从UI上看，首页承载了"遇见"、"发现"、"消息"和"我的"四个tab页。实际实现中使用的`ViewPager`+`FragmentPagerAdapter`，每个tab页对应一个`fragment`。跟微信不一样的是，进入首页后我们的tab页会发请求拉取数据，而微信更多的直接显示已有的推送消息。我们的tab页中加载的图片远远多于微信，甚至还可能有视频。

首页相对其他页面较重，启动慢、性能差。用户进入应用后首先看到的就是首页，它的启动最为频繁。首页启动慢除了影响用户体验，更会让用记对app产生不好的感知：这个app做得不行。

# 问题描述
首页相对其他页面较重。所以直观上启动慢相对较慢是说得通的。但更深层次的原因是什么呢？有没有办法优化呢？

我们先测一下首页启动速度。使用logcat抓取`ActivityManager`打印的Activity启动时间应该是最简单有效的测试方法了，`adb logcat -v time | grep 'Displayed com.tencent.PmdCampus/.view.IndexActivity'`输出结果如下：

```
02-03 17:44:04.672 1116-1168/? I/ActivityManager: Displayed com.tencent.PmdCampus/.view.IndexActivity: +622ms
```

在我的OPPO R7sm上空页面的启动速度少于100ms，app中普通页面多数在200ms左右，而首页慢得可以，居然到了629.4ms！

|第一次|第二次|第三次|第四次|第五次|平均|
|------|------|--------|-----|-----|----|
|622ms |634ms |638ms   |614ms|639ms|629.4ms|

# 问题分析
通过分析代码和traceview日志，以下几个地方引起注意：

+ 为了避免切换tab过程中fragment频繁销毁和初始化，调用了`ViewAdapter.setOffscreenPageLimit(3)`，它会产生什么影响
+ traceview日志显示有些在主线执行时间过长的方法，这些方法会产生什么影响

下面逐步展开分析。

## fragment的初始化
关于ViewPager中fragment的初始化，我们先从`ViewAdapter.setOffscreenPageLimit()`方法说起。

> Set the number of pages that should be retained to either side of the current page in the view hierarchy in an idle state. Pages beyond this limit will be recreated from the adapter when needed.
> This is offered as an optimization. If you know in advance the number of pages you will need to support or have lazy-loading mechanisms in place on your pages, tweaking this setting can have benefits in perceived smoothness of paging animations and interaction. If you have a small number of pages (3-4) that you can keep active all at once, less time will be spent in layout for newly created view subtrees as the user pages back and forth.
> You should keep this limit low, especially if your pages have complex layouts. This setting defaults to 1

这个方法用于设置空闲状态时当前页面两边需要保留的页面数量。超过这个数量限制的页面将被销毁，而需要时又会重新创建。这个方法可以用于调优。比如你提前知道ViewPager中的页面数量或者页面有延迟加载机制，可以调整这里的参数以便页面动画及交互更流畅。如果页面数量很少的话(3到4个页面)，完全可以同时保留全部页面。这样，在用户切换页面时可以花更少的时间用于新创建的view的布局。应当保证这里的数量较小，尤其是在页面布局较复杂时。值为1。

容易理解`offscreenPageLimit`的含义。比如，我们有四个tab，假设`offscreenPageLimit`为1，

+ 启动时当前页为"遇见"，这时"遇见"右边的"发现"也会被初始化
+ 切换到"发现"时，左边的"遇见"页面仍被保留，右边的"消息"页面被初始化
+ 切换到"消息"页面时效果与上面的类似，但"遇见"页面则会被销毁，因为它相对当前的位置已超过`offscreenPageLimit`

// TODO 截图



## traceview分析

![](/images/index_activity.png)

![](/images/from_json.png)

traceview分析IndexActivity的生命周期，看不出任何异常。但`UserPref.getRemoteUserInfo()`方法引起我们的注意，它的耗时有些异常。而这个方法被"遇见"和"我的"等fragment调用了一共5次。

前面提到过IndexActivity由"遇见"，"发现"，"消息"和"我的"一共4个fragment组成。这里的fragment由ViewAdapter提供，而ViewAdapter的数据来自一个FragmentStatePagerAdapter。`viewAdapter.setOffscreenPageLimit(3)`被调用。


# 优化一
## 避免主线程中过多的JSON反序列化
UserInfo对象表示当前用户，这个对象比较大，结构复杂。该对象JSON序列化后持久化到SharedPreference当中。

初始化IndexActivity的过程，会从SharedPreference读取数据反序列化为UserInfo对象。设计不当导致会频繁在主线程中进行反序列化。

优化后，我们只需要一次反序列化，减轻主线程压力。

优化后OPPO R7sm上首页的启动速度如下

|第一次|第二次|第三次|第四次|第五次|平均|
|------|------|--------|-----|-----|----|
|389ms |375ms |429ms   |405ms|414ms|    |

## 优化fragment加载过程

```java
public class TaggedFragmentPagerAdapter extends FragmentStatePagerAdapter {

	@Override
	public Fragment getItem(int position) {
		Log.i("TaggedFragmentPagerAdapter", "getItem for position=" + position);
		switch (position): {
			case 0:
				return 遇见Fragment;

			case 1:
				return 发现Fragment;

			case 2:
				return 消息Fragment;

			case 3:
				return 我的Fragment;
		}
	}
}
```

IndexActivity中四个fragment的初始化过程如下。但发现IndexActivity启动时，打印的日志表明四个fragment被马上全部创建。而fragment创建时会立即请求后台数据，导致更多的开销。

```
TaggedFragmentPagerAdapter: getItem for position=0
TaggedFragmentPagerAdapter: getItem for position=1
TaggedFragmentPagerAdapter: getItem for position=2
TaggedFragmentPagerAdapter: getItem for position=3
```

去掉`viewAdapter.setOffscreenPageLimit(3)`调用后，打印的日志如表明启动IndexActivity时只会创建前两个fragment。

```
TaggedFragmentPagerAdapter: getItem for position=0
TaggedFragmentPagerAdapter: getItem for position=1
```

但是IndexActivity的启动时间并没有明显减少。说明性能瓶颈并不在于多启动后两个fragment。

|第一次|第二次|第三次|第四次|第五次|平均|
|------|------|--------|-----|-----|----|
|375ms |347ms |359ms   |426ms|361ms|    |

抓取[traceview文件](/images/ddms7813906848591170267.trace)进行分析。耗时较多的方法统计如下(统计依据是IndexActivity启动过程中在主线程中运行且"Incl Cpu Time"占比超过1%)：

![](/images/perf1.png)
![](/images/perf2.png)
![](/images/perf3.png)
![](/images/perf4.png)
![](/images/perf5.png)
![](/images/perf6.png)
![](/images/perf7.png)
![](/images/perf8.png)
![](/images/perf9.png)
![](/images/perf10.png)
![](/images/perf11.png)
![](/images/perf12.png)

这里的耗时方法可以分为两类，一类是消耗时间远超预期，比如`CampusApplication.getVideoUrl()`，看名字它应该不会耗时太多。一类是是这些方法不应该运行，或者说是可以避免运行的。下面详情分析。

+ `CampusApplication.getVideoUrl()`性能较低导致`MostMatchAdapter.onBindViewHolder()`性能低
+ 复杂布局导致`MostMatchAdapter.onCreateViewHolder()`性能低
+ `TweetResponsePref.getTweet()`在主线程中反序列化，导致性能低
+ `NetworkUtil.isNetWorkConnectedGood()`耗时多(?)
+ `MostMatchAdapter.onMeasure()`中调用`SystemUtils.getScreenWidth()`和`SystemUtils.getScreenHeight()`耗时多
+ `MostMatchAdapter.onCreateViewHolder()`性能低导致`RecyclerViewPagerAdatper.onCreateViewHolder()`耗时多
+ `MostMatchAdapter.onBindViewHolder()`性能低导致`RecyclerViewPagerAdatper.onBindViewHolder()`耗时多
+ `TweetResponsePref.getTweet()`性能低导致`FindPresenterImpl.getFriendTweets()`的回调耗时多
+ 布局复杂导致`RecommendPresenterImpl.getLikeRank()`的回调耗时多(根据在于RecommendFragment.onGetLikeRank()的inflate有问题)
+ 布局复杂导致RecommendFragment.onGetNewStudents()耗时较多
+ `TweetResponsePref.getTweet()`性能低导致`FindFragment.initHeaderView()`和`FindFragment.onCreateView()`耗时多
+ `TweetResponsePref.getTweet()`性能低导致`FindFragment.onGetFriendTweets()`耗时较多
+ 布局复杂导致`RecommendFragment.onCreateView()`耗时较多
+ `NetworkUtil.isNetWorkConnectedGood()`耗时多导致`autoPlayVideo()`耗时多

"遇见"页面又细分为"随遇"和"推荐"，启动IndexActivity时只初始化"随遇"即可。但目前是"随遇"和"推荐"都被初始化了。使用`FragmentPagerAdapter` + `ViewPager`实现，ViewPager的offscreenPageLimit最小为1，所以肯定会至少加载两个fragment，导致"随遇"和"推荐"都被初始化。

通过如下方式延迟"推荐"的加载过程。[来源](http://blog.csdn.net/yewei02538/article/details/50329119)

```java
public class RecommendFragment extends Fragment {
	private boolean mViewCreated;

  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		...
		view = ...
		mViewCreated = true;
		return view;
	}
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser && mViewCreated) {
			loadData();
		} else {
				mViewCreated = false;
		}
	}
```

`TweetResponsePref.getTweet()`中有JSON反序列化的操作，在主线程中调用时会严重拖慢UI。修改调用方式，将这些操作放到后台线程中。

|第一次|第二次|第三次|第四次|第五次|平均|
|------|------|--------|-----|-----|----|
|315ms |307ms |325ms   |312ms|305ms|    |

# 优化后
延迟初始化复杂UI

延迟加载数据

启动时间的变化

|第一次|第二次|第三次|第四次|第五次|平均|
|------|------|--------|-----|-----|----|
|256ms |274ms |262ms   |266ms|277ms|    |

额外收益

启动时请求数的变化

 -> 6个

启动时内存占用的变化

# 内存优化

## 内存泄露

splash
index

## 高效加载bitmap
