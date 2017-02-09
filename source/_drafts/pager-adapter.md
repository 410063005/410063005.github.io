---
title: pager_adapter
tags:
---

# PagerAdapter
实现`PagerAdapter`至少要覆盖以下方法：

+ instantiateItem(ViewGroup, int)
+ destroyItem(ViewGroup, int, Object)
+ getCount()
+ isViewFromObject(View, Object)

PagerAdapter比用于AdapterView的adapter更加通用。PagerAdapter使用回调方法来表示更新过程中的关键步骤而不是直接使用view重用机制。需要的话PagerAdapter也可以实现某种形式的view重用机制，或者使用更复杂的方法来管理view。

ViewPager使用key关联到每个page，而不是使用view。这个key用于独立于位置来识别adapter中指定的page。调用`PagerAdapter.startUpdate()`表示`ViewPager`中的内容将出现变化。接下来`instantiateItem()`或`destroyItem()`将被调用一次或多次。`finishUpdate()`表示更新过程结束了。`finishUpdate()`返回时`instantiateItem()`返回的、跟key对应的view将被添加到ViewParent，而跟key对应的view将被`destroyItem()`从ViewParent移除。`isViewFromObject()`方法用于判断key是否跟某个view对象关联。

简单实现的PagerAdapter中可以将pager view自身作为key。从`instantiateItem()`方法创建pager view后返回这个key，对应的`destroyItem()`会从ViewParent移除该pager view。而`isViewFromObject()`可以实现为`return view == object;`。

# Fragment
+ FragmentTransaction.detach() - Detach the given fragment from the UI. This is the same state as when it is put on the back stack: the fragment is removed from the UI, however its state is still being actively managed by the fragment manager. When going into this state its view hierarchy is destroyed.
+ FragmentTransaction.attach() - Re-attach a fragment after it had previously been detached from the UI with detach(Fragment). This causes its view hierarchy to be re-created, attached to the UI, and displayed.
+ FragmentTransaction.add() - Add a fragment to the activity state. This fragment may optionally also have its view (if Fragment.onCreateView returns non-null) into a container view of the activity.
+ FragmentTransaction.remove() - Remove an existing fragment. If it was added to a container, its view is also removed from that container.

注意：`detach()`仅仅将fragment从UI中移除，但fragment仍然存活并且被FragmentManager管理。而`remove()`会导致fragment销毁。`attach()`会导致fragment的UI重新创建。

# FragmentPagerAdapter和FragmentStatePagerAdapter
+ FragmentPagerAdapter - 适用于少量静态的fragment，比如几个tab页。用户访问的每个页面会保存在内存中，尽管其UI可能被移除。这种adapter可能占用大量内存。
+ FragmentStatePagerAdapter - 适用于页面数量很多的情形，有点类似于ListView。当页面不可见时整个fragment会被销毁，只保存fragment的状态。相比FragmentPagerAdapter能节省较多内存。

[FragmentPagerAdapter跟FragmentStatePagerAdapter的区别](http://stackoverflow.com/questions/18747975/difference-between-fragmentpageradapter-and-fragmentstatepageradapter)

# ViewPager的疑问
参考[这个链接](http://stackoverflow.com/questions/7951730/viewpager-and-fragments-whats-the-right-way-to-store-fragments-state/9646622#9646622)
