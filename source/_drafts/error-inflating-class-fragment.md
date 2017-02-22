---
title: error-inflating-class-fragment
tags:
---

android.view.InflateException: Binary XML file line #2: Error inflating class fragment
       at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:769)
       at android.view.LayoutInflater.inflate(LayoutInflater.java:488)
       at android.view.LayoutInflater.inflate(LayoutInflater.java:420)
       at com.tencent.PmdCampus.comm.utils.DynamicAdapterHelper.createHolderForQuickSendTweet(DynamicAdapterHelper.java:63)
       at com.tencent.PmdCampus.adapter.DynamicAdapter.onCreateViewHolder(DynamicAdapter.java:111)
       at com.tencent.PmdCampus.comm.widget.XXRecyclerView$WrapAdapter.onCreateViewHolder(XXRecyclerView.java:519)
       at android.support.v7.widget.RecyclerView$Adapter.createViewHolder(RecyclerView.java:6078)
       at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:5248)
       at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:5158)
       at android.support.v7.widget.LinearLayoutManager$LayoutState.next(LinearLayoutManager.java:2061)
       at android.support.v7.widget.LinearLayoutManager.layoutChunk(LinearLayoutManager.java:1445)
       at android.support.v7.widget.LinearLayoutManager.fill(LinearLayoutManager.java:1408)
       at android.support.v7.widget.LinearLayoutManager.onLayoutChildren(LinearLayoutManager.java:590)
       at com.tencent.PmdCampus.comm.widget.RecylerViewPager.WrapContentLinearLayoutManager.onLayoutChildren(WrapContentLinearLayoutManager.java:31)
       at android.support.v7.widget.RecyclerView.dispatchLayoutStep2(RecyclerView.java:3379)
       at android.support.v7.widget.RecyclerView.dispatchLayout(RecyclerView.java:3188)
       at android.support.v7.widget.RecyclerView.consumePendingUpdateOperations(RecyclerView.java:1566)
       at android.support.v7.widget.RecyclerView$ViewFlinger.run(RecyclerView.java:4560)
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:800)
       at android.view.Choreographer.doCallbacks(Choreographer.java:603)
       at android.view.Choreographer.doFrame(Choreographer.java:571)
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:786)
       at android.os.Handler.handleCallback(Handler.java:815)
       at android.os.Handler.dispatchMessage(Handler.java:104)
       at android.os.Looper.loop(Looper.java:194)
       at android.app.ActivityThread.main(ActivityThread.java:5871)
       at java.lang.reflect.Method.invoke(Native Method)
       at java.lang.reflect.Method.invoke(Method.java:372)
       at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1119)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:885)
    Caused by: java.lang.IllegalArgumentException: Binary XML file line #2: Duplicate id 0x7f0e044d, tag null, or parent id 0x7f0e0311 with another fragment for com.tencent.PmdCampus.view.fragment.RecentPhotosFragment
       at android.support.v4.app.FragmentManagerImpl.onCreateView(FragmentManager.java:3420)
       at android.support.v4.app.FragmentController.onCreateView(FragmentController.java:120)
       at android.support.v4.app.FragmentActivity.dispatchFragmentsOnCreateView(FragmentActivity.java:378)
       at android.support.v4.app.BaseFragmentActivityHoneycomb.onCreateView(BaseFragmentActivityHoneycomb.java:33)
       at android.support.v4.app.FragmentActivity.onCreateView(FragmentActivity.java:79)
       at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:739)
       at android.view.LayoutInflater.inflate(LayoutInflater.java:488) 
       at android.view.LayoutInflater.inflate(LayoutInflater.java:420) 
       at com.tencent.PmdCampus.comm.utils.DynamicAdapterHelper.createHolderForQuickSendTweet(DynamicAdapterHelper.java:63) 
       at com.tencent.PmdCampus.adapter.DynamicAdapter.onCreateViewHolder(DynamicAdapter.java:111) 
       at com.tencent.PmdCampus.comm.widget.XXRecyclerView$WrapAdapter.onCreateViewHolder(XXRecyclerView.java:519) 
       at android.support.v7.widget.RecyclerView$Adapter.createViewHolder(RecyclerView.java:6078) 
       at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:5248) 
       at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:5158) 
       at android.support.v7.widget.LinearLayoutManager$LayoutState.next(LinearLayoutManager.java:2061) 
       at android.support.v7.widget.LinearLayoutManager.layoutChunk(LinearLayoutManager.java:1445) 
       at android.support.v7.widget.LinearLayoutManager.fill(LinearLayoutManager.java:1408) 
       at android.support.v7.widget.LinearLayoutManager.onLayoutChildren(LinearLayoutManager.java:590) 
       at com.tencent.PmdCampus.comm.widget.RecylerViewPager.WrapContentLinearLayoutManager.onLayoutChildren(WrapContentLinearLayoutManager.java:31) 
       at android.support.v7.widget.RecyclerView.dispatchLayoutStep2(RecyclerView.java:3379) 
       at android.support.v7.widget.RecyclerView.dispatchLayout(RecyclerView.java:3188) 
       at android.support.v7.widget.RecyclerView.consumePendingUpdateOperations(RecyclerView.java:1566) 
       at android.support.v7.widget.RecyclerView$ViewFlinger.run(RecyclerView.java:4560) 
       at android.view.Choreographer$CallbackRecord.run(Choreographer.java:800) 
       at android.view.Choreographer.doCallbacks(Choreographer.java:603) 
       at android.view.Choreographer.doFrame(Choreographer.java:571) 
       at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:786) 
       at android.os.Handler.handleCallback(Handler.java:815) 
       at android.os.Handler.dispatchMessage(Handler.java:104) 
       at android.os.Looper.loop(Looper.java:194) 
       at android.app.ActivityThread.main(ActivityThread.java:5871) 
       at java.lang.reflect.Method.invoke(Native Method) 
       at java.lang.reflect.Method.invoke(Method.java:372) 
       at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1119) 
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:885) 

       
# 案例一
Binary XML file line #2: Duplicate id 0x7f0e044d, tag null, or parent id 0x7f0e0311 with another fragment for com.tencent.PmdCampus.view.fragment.RecentPhotosFragment

# 案例二
权限问题越界引起的


android.view.InflateException: Binary XML file line #2: Binary XML file line #2: Error inflating class fragment
   at android.view.LayoutInflater.inflate(LayoutInflater.java:561)
   at android.view.LayoutInflater.inflate(LayoutInflater.java:437)
   at com.tencent.PmdCampus.comm.utils.DynamicAdapterHelper.createHolderForQuickSendTweet(DynamicAdapterHelper.java:63)
   at com.tencent.PmdCampus.adapter.DynamicAdapter.onCreateViewHolder(DynamicAdapter.java:111)
   at com.tencent.PmdCampus.comm.widget.XXRecyclerView$WrapAdapter.onCreateViewHolder(XXRecyclerView.java:519)
   at android.support.v7.widget.RecyclerView$Adapter.createViewHolder(RecyclerView.java:6078)
   
没有打印出详细日志，怎么办。 见[这里](http://stackoverflow.com/questions/19874882/android-view-inflateexception-binary-xml-file-error-inflating-class-fragment)

```
I have had similar problems on and off. The error message often provides very little detail, regardless of actual cause. But I found a way to get more useful info. It turns out that the internal android class 'LayoutInflater.java' (in android.view package) has an 'inflate' method that re-throws an exception, but does not pick up the details, so you lose info on the cause.

I used AndroidStudio, and set a breakpoint at LayoutInflator line 539 (in the version I'm working in), which is the first line of the catch block for a generic exception in that 'inflate' method:

        } catch (Exception e) {
            InflateException ex = new InflateException(
                    parser.getPositionDescription()
                            + ": " + e.getMessage());
            ex.initCause(e);
            throw ex;
If you look at 'e' in the debugger, you will see a 'cause' field. It can be very helpful in giving you a hint about what really occurred. This is how, for example, I found that the parent of an included fragment must have an id, even if not used in your code. Or that a TextView had an issue with a dimension.
```

RecentPhotosFragment