---
title: DiffUtil简介 
tags: android
toc: true
---
# 官方文档
## DiffUtil
[DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html)是用于计算两个数据集之间差异的工具类。DiffUtil将两个数据集之间的差异输出为一个更新操作列表，更新操作列表可以将前一个数据集转换成后一个数据庥。DiffUtil是在Support Library 24.2.0上加入的。

DiffUtil也可以用于计算RecyclerView Adapter的更新。具体见[ListAdapter](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/ListAdapter.html)及[AsyncListDiffer](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/AsyncListDiffer.html)，它们在后台线程中使用DiffUtil计算差异。

DiffUtil使用Eugene W. Myers算法计算将前一个数据集转换成后一个数据集的最少更新次数。Myers算法不处理被移动的数据项，所以DiffUtil会对结果进行二次处理以检查被移动的数据项。

如果数据集很大，Myers算法可能需要很长时间，所以建议在后台线程中计算数据集差异，然后在主线程中将[DiffUtil.DiffResult](https://developer.android.com/reference/android/support/v7/util/DiffUtil.DiffResult.html)应用于RecyclerView。

Myers算法在空间复杂度上优化过，它使用O(N)的空间来找到将前一个数据集转换为后一个数据集的最少添加和移除操作次数。算法时间复杂度为O(N + D^2)，D为待处理的数据集的大小。

如果开启移动检查模式，算法还需要额外的O(N^2)时间开销，N表示总的添加和移除次数。如果数据集已经是有序的(比如按时间升序排列)，可以关闭移动检查以提升性能。

算法实际运行时间完全跟数据集更新次数(the number of changes in the list)以及比较算法的性能(the cost of your comparison methods)相关。以下是几个可供参考的平均运行时间(待测试的列表为随机UUID，在Nexus 5X, Android M上进行测试的)

+ 100 items and 10 modifications: avg: 0.39 ms, median: 0.35 ms
+ 100 items and 100 modifications: 3.82 ms, median: 3.75 ms
+ 100 items and 100 modifications without moves: 2.09 ms, median: 2.06 ms
+ 1000 items and 50 modifications: avg: 4.67 ms, median: 4.59 ms
+ 1000 items and 50 modifications without moves: avg: 3.59 ms, median: 3.50 ms
+ 1000 items and 200 modifications: 27.07 ms, median: 26.92 ms
+ 1000 items and 200 modifications without moves: 13.54 ms, median: 13.36 ms

由于实现上的限制，列表的最大长度为2^26。

## AsyncListDiffer
注意，这个类是support library 27.1.0中添加的，具体包名为`com.android.support:recyclerview-v7`

[AsyncListDiffer](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/AsyncListDiffer.html)是在后台线程中使用[DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html)计算两个列表差异的辅助类。

它跟RecyclerView.Adapter关联，将列表的变更通知到Adapter。

简单来说，应该使用[ListAdapter](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/ListAdapter.html)继承自[RecyclerView.Adapter](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html)包装类而不应直接使用AsyncListDiffer。AsyncListDiffer可用于复杂场景，这些场景中覆盖adapter基类来支持异步列表差值并不方便。

AsyncListDiffer消费来自LiveData的值，可以简单地认为它代表adapter。接收到新列表时，AsyncListDiffer会在后台线程中使用DiffUtil计算列表内容的差异。

使用[getCurrentList()](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/AsyncListDiffer.html#getCurrentList())获取当前列表，它表示当前的数据对象。差异数据会在当前列表被更新前分发到`ListUpdateCallback`。如果直接将列表更新分发到adapter，adapter可以地使用[getCurrentList()](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/AsyncListDiffer.html#getCurrentList())地访问列表数据项以及列表长度。

一个简单的例子如下：

```java
@Dao
 interface UserDao {
     @Query("SELECT * FROM user ORDER BY lastName ASC")
     public abstract LiveData<List<User>> usersByLastName();
 }

 class MyViewModel extends ViewModel {
     public final LiveData<List<User>> usersList;
     public MyViewModel(UserDao userDao) {
         usersList = userDao.usersByLastName();
     }
 }

 class MyActivity extends AppCompatActivity {
     @Override
     public void onCreate(Bundle savedState) {
         super.onCreate(savedState);
         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
         RecyclerView recyclerView = findViewById(R.id.user_list);
         UserAdapter adapter = new UserAdapter();
         viewModel.usersList.observe(this, list -> adapter.submitList(list));
         recyclerView.setAdapter(adapter);
     }
 }

 class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
     private final AsyncListDiffer<User> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);
     @Override
     public int getItemCount() {
         return mDiffer.getCurrentList().size();
     }
     public void submitList(List<User> list) {
         mDiffer.submitList(list);
     }
     @Override
     public void onBindViewHolder(UserViewHolder holder, int position) {
         User user = mDiffer.getCurrentList().get(position);
         holder.bindTo(user);
     }
     public static final DiffUtil.ItemCallback<User> DIFF_CALLBACK
             = new DiffUtil.ItemCallback<User>() {
         @Override
         public boolean areItemsTheSame(
                 @NonNull User oldUser, @NonNull User newUser) {
             // User properties may have changed if reloaded from the DB, but ID is fixed
             return oldUser.getId() == newUser.getId();
         }
         @Override
         public boolean areContentsTheSame(
                 @NonNull User oldUser, @NonNull User newUser) {
             // NOTE: if you use equals, your object must properly override Object#equals()
             // Incorrectly returning false here will result in too many animations.
             return oldUser.equals(newUser);
         }
     }
 }
```

## ListAdapter
注意，这个类是support library 27.1.0中添加的，具体包名为`com.android.support:recyclerview-v7`

[ListAdapter](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/ListAdapter.html)继承自[RecyclerView.Adapter](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html)，它表示RecyclerView的数据列表，内部在后台线程中计算列表之间的差异。

ListAdapter是AsyncListDiffer的易于使用的包装。AsyncListDiffer实现了Adapter的常用的操作，包括数据项的访问和统计。

使用LiveData<List>是一种向adapter提供数据的简单方式，可以在有新列表时使用[submitList(List)](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/ListAdapter.html#submitList(java.util.List%3CT%3E))方法。当然，这种方式不是必须的。

ListAdapter源码如下：

```java
public abstract class ListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private final AsyncListDiffer<T> mHelper;

    @SuppressWarnings("unused")
    protected ListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mHelper = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                new AsyncDifferConfig.Builder<>(diffCallback).build());
    }

    @SuppressWarnings("unused")
    protected ListAdapter(@NonNull AsyncDifferConfig<T> config) {
        mHelper = new AsyncListDiffer<>(new AdapterListUpdateCallback(this), config);
    }

    /**
     * Submits a new list to be diffed, and displayed.
     * <p>
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    @SuppressWarnings("WeakerAccess")
    public void submitList(List<T> list) {
        mHelper.submitList(list);
    }

    @SuppressWarnings("unused")
    protected T getItem(int position) {
        return mHelper.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return mHelper.getCurrentList().size();
    }
}
```

# DiffUtil介绍

## 主要类

+ DiffUtil.Callback - 通常来说Callback表示回调，但这里的Callback更准确地说是表示我们提供的比较新老数据集的规则
+ DiffUtil.calculateDiff() - 计算数据集差异，差异为`DiffResult`
+ DiffResult.dispatchUpdatesTo() - 将差异分发到`ListUpdateCallback`或`Adapter`

DiffUtil.Callback

+ getOldListSize() - 旧数据集的大小
+ getNewListSize() - 新数据集的大小
+ areItemsTheSame() - 是否同一个数据项
+ areContentsTheSame() - 数据项内容是否相同
+ getChangePayload() - 同一个数据项的内容变了，这个方法返回值用于标识哪些内容变了。

`getChangePayload()`方法不太好理解。看以下代码，不能发现它跟`Adapter.notifyItemRangeChanged(position, count, payload)`相关。

```java
    public static class DiffResult {
        public void dispatchUpdatesTo(final Adapter adapter) {
            this.dispatchUpdatesTo(new ListUpdateCallback() {
				...
				
                public void onChanged(int position, int count, Object payload) {
                    adapter.notifyItemRangeChanged(position, count, payload);
                }
            });
        }
		
        public void dispatchUpdatesTo(ListUpdateCallback updateCallback) {
            BatchingListUpdateCallback batchingCallback;
            if(updateCallback instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback)updateCallback;
            } else {
                batchingCallback = new BatchingListUpdateCallback(updateCallback);
            }
			...
			batchingCallback.onChanged(snake.x + i, 1, this.mCallback.getChangePayload(snake.x + i, snake.y + i));
        }
    }
```

所以为了让`getChangePayload()`返回值生效，我们还必须正确的实现`Adapter.onBindViewHolder(RecyclerView.ViewHolder holder, int position, Listpayloads)`方法。

# DiffUtil应用

## IM会话界面

来了一条新消息(生成新的会话)，要插入数据项到顶部

来了一条新消息(已有会话)，要修改并移动数据项到顶部

+ 数据项整体刷新
+ 数据项局部刷新 - 红点, 时间, 消息内容

将一个会话置顶，要移动数据项

删除一个会话，要删除数据项

方案对比

1. 任何操作均重新加载所有会话并`notifyDataSetChanged()`：性能问题，界面抖动
2. 根据不同的操作实现不同的策略：编码复杂
3. 使用DiffUtil：在良好性能和复杂代码之间折衷

## 组队房间

RecyclerView 数据量很少



## 下拉刷新

方案对比

1. 清空数据集后重新添加：性能问题
2. 使用DiffUtil：良好性能

# 参考
[DiffUtil | Android Developers](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html)
[AsyncListDiffer | Android Developers](https://developer.android.com/reference/android/support/v7/recyclerview/extensions/AsyncListDiffer.html)
[Android 开发学习之路 - DiffUtil 使用教程 - Android - 掘金](https://juejin.im/entry/57bbb7f60a2b58006cbd9e0c)
[[Android] DiffUtil在RecyclerView中的使用详解 - 再读斋 - SegmentFault 思否](https://segmentfault.com/a/1190000007205469)
[【Android】详解7.0带来的新工具类：DiffUtil - CSDN博客](https://blog.csdn.net/zxt0601/article/details/52562770)