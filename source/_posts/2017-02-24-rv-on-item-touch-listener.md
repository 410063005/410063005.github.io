---
title: RecyclerView.OnItemTouchListener的用法
tags:
  - Android
  - UI
date: 2017-02-24 19:37:58
toc: true
---
如何给`RecyclerView`中的Item添加点击事件呢？本文介绍了几种不同的方法。
<!--more-->

# 介绍
给RecyclverView中的item添加点击处理事件最简单的做法是给item中的view添加OnClickListener监听器。这是一个实际的例子：

```java
public void onBindViewHolder(final ViewHolder holder, int position) {
    holder.rlRoot.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            
        }
    });    
}
```

[这篇帖子][RecyclerView onClick]中提出了一个疑问，认为虽然这种做法并没有严重不妥，但每次绑定数据时就重新创建一个listener总觉得不是很自然。有没有更好的办法呢？比如说类似ListView的OnItemClickListener，用起来应该方便很多。

## 方法一
一种建议的做法是从Fragment或Activity中向Adapter传一个listener。示例如下：

```java
// Activity的代码
private View.OnClickListener mItemClick = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
        MainActivity.start();
        
    }
};

MainAdapter mainAdapter = new MainAdapter(this, mItemClick);

// Adapter的代码
 @Override
public MainAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
    View itemView = activity.getLayoutInflater().inflate(R.layout.main_adapter_item, viewGroup, false);
    ViewHolder holder = new ViewHolder(itemView);
    itemView.setOnClickListener(mItemClick);
    return holder;
}
```

## 方法二
[这里][better]给出了一种更好的做法。

```java
RecyclerView recyclerView = findViewById(R.id.recycler);
recyclerView.addOnItemTouchListener(
    new RecyclerItemClickListener(context, recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
      @Override public void onItemClick(View view, int position) {
        // do whatever
      }

      @Override public void onLongItemClick(View view, int position) {
        // do whatever
      }
    })
);
```

而`RecyclerItemClickListener`的实现如下：

```java
public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
  private OnItemClickListener mListener;

  public interface OnItemClickListener {
    public void onItemClick(View view, int position);

    public void onLongItemClick(View view, int position);
  }

  GestureDetector mGestureDetector;

  public RecyclerItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener) {
    mListener = listener;
    mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mChildView != null && mListener != null) {
                final int pos = mRecyclerView.getChildAdapterPosition(mChildView);

                if (pos != RecyclerView.NO_POSITION) {
                    mListener.onItemClick(mChildView, pos);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (child != null && mListener != null) {
                mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child));
            }
        }
    });
}

  @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
    View childView = view.findChildViewUnder(e.getX(), e.getY());
    if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
      mListener.onItemClick(childView, view.getChildAdapterPosition(childView));
      return true;
    }
    return false;
  }

  @Override public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

  @Override
  public void onRequestDisallowInterceptTouchEvent (boolean disallowIntercept){}
}
```

# 问题
最开始的需求如下面的截图，它只是一个好友列表。点击列表中的每一个item要求跳转到个人主页。可以看到，这里非常适合使用上述的RecyclerItemClickListener方案，可以避免每次绑定数据时创建listener的问题，简直跟ListView的OnItemClickListener一样好用。

![](rv_demo1.jpg)

```java
RecyclerView recyclerView = findViewById(R.id.recycler);
recyclerView.addOnItemTouchListener(
    new RecyclerItemClickListener(context, recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
      @Override public void onItemClick(View view, int position) {
        // 跳转到个人主页
      }

      @Override public void onLongItemClick(View view, int position) {
        // do whatever
      }
    })
);
```

一开始上面的做法的确非常不错。不幸的是，最近需求发生了变化。该页面除了是好友列表，还要求在某些条件下会出来以下两种情形。

![](rv_demo2.jpg)
![](rv_demo3.jpg)

注意：这里的绿色方框中的内容是由RecyclerView的特殊item来实现的。

1. 第一个截图中，需求是蓝色按钮点击跳转到加好友页面
2. 第二个截图中，需求是绿色方框中的用户区域点击跳转到个人主页，蓝色按钮点击跳转加好友页面

使用RecyclerItemClickListener不太好实现，看来它有其局限性，所以还是乖乖给需要点击事件的控件添加相应的OnClickListener吧。给"绑定手机"，"加好友"，以及用户区域添加OnClickListener后，问题来了：

1. 第一个截图中，点击"绑定手机"不起作用，双击倒是可以跳转到加好友页面
2. 第二个截图中，点击绿色方框中的用户区域不起作用，点击"加好友"也不起作用

怎么办？完全去掉`RecyclerItemClickListener`，然后回退到给需求点击事件的控件单独添加OnClickListener？又作了如下尝试：

```java
RecyclerView recyclerView = findViewById(R.id.recycler);
recyclerView.addOnItemTouchListener(
    new RecyclerItemClickListener(context, recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
      @Override public void onItemClick(View view, int position) {
        // 在这里进行处理
      }

      @Override public void onLongItemClick(View view, int position) {
        // do whatever
      }
    })
);
```

1. 第一个截图中，点击绿色方框部分任何一处，均可跳转到加好友页面
2. 第二个截图中，点击绿色方框中的用户区域任何一处，均可跳转到加好友页面，点击"加好友"不起作用

# 思考
为什么上述场景中点击事件不起任何作用？

先来看一下`RecyclerView.OnItemTouchListener`。OnItemTouchListener允许应用拦截RecyclerView的view hierarchy level处理过程中的触摸事件，以防这些事件被认为是RecyclerView的滚动操作。


再来看`RecyclerView.addOnItemTouchListener()`。该方法用于添加RecyclerView.OnItemTouchListener，在touch事件被分发到child view或被用作view的标准滚动行为之前拦截这些事件。客户端代码可以使用监听器来实现item的操作行为。一旦从`RecyclerView.OnItemTouchListener.onInterceptTouchEvent(RecyclerView, MotionEvent)`返回true，则对应的`RecyclerView.OnItemTouchListener.onTouchEvent(RecyclerView, MotionEvent)`方法会在收到每个MotionEvent事件时被调用，直到手势结束。

所以很明显，`viewHolder.button.setOnClickListener`这种方式设置的监听器不起作用是因为touch事件被OnItemTouchListener给拦截了。

分析RecyclerItemClickListener的实现，不难找到问题的关键在于：<font color="red">如果找到了mChildView在RecyclerView中的位置，会直接调用mListener.onItemClick()并返回true。而返回true意味着touch事件被消费掉了，再也没可能被分发到child view</font>。所以类似`viewHolder.button.setOnClickListener`这种方式设置的监听器必然不起作用。

```java
public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {

  public RecyclerItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener) {
    mListener = listener;
    mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mChildView != null && mListener != null) {
                final int pos = mRecyclerView.getChildAdapterPosition(mChildView);

                if (pos != RecyclerView.NO_POSITION) {
                    mListener.onItemClick(mChildView, pos);
                    return true;
                }
            }
            return false;
        }
        
        ...
    });
}
```

如何修复这里的问题？暂时想不到比较通用的解决方案，但对于我们这里的问题还是不难处理的。解决办法就是针对那些特殊viewType对应的item，保证一定不会消费其touch事件，而是允许分发出去让child view处理。修改后的代码如下：

```java
    private List<Integer> mSpecialViewTypes;
    
    public void setSpecialViewTypes(List<Integer> specialViewTypes) {
        mSpecialViewTypes = specialViewTypes;
    }
    
    private List<Integer> mSpecialViewTypes;

    public RecyclerItemClickListener(Context context, OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mChildView != null && mListener != null) {
                    final int pos = mRecyclerView.getChildAdapterPosition(mChildView);
                    
                    //========================================
                    // 对于特殊的viewType, onSingleTapUp()返回false
                    int viewType = mRecyclerView.getAdapter().getItemViewType(pos);
                    if (mSpecialViewTypes != null && mSpecialViewTypes.contains(viewType)) {
                        return false;
                    }
                    //========================================                    

                    if (pos != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(mChildView, pos);
                        return true;
                    }
                }
                return false;
            }
        }            
    }
```

具体到我们的例子，`view type 1`和`view type 2`就是所谓的特殊viewType，我们不应在`onSingleTapUp()`方法中拦截那些会分发到它们的touch事件。修改后问题完美解决。

![](rv_demo2.jpg)
![](rv_demo3.jpg)

[RecyclerView onClick]: http://stackoverflow.com/questions/24471109/recyclerview-onclick
[better]: http://stackoverflow.com/questions/24471109/recyclerview-onclick/26196831#26196831