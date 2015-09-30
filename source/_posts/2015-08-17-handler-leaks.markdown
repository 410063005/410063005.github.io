---
layout: post
title: "Handler泄露问题分析"
date: 2015-08-17 17:13:08 +0800
comments: true
categories: android
---
本文分析了Handler泄露是如何产生的。简单探讨Handler如何检测内存泄露，并提供了一个安全的Handler实现。

<!--more-->

# Handler泄露
Android Studio中如下代码会提示`This Handler class should be static or leaks might occur`。

```
public class DemoActivity extends Activity {    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
}
```

# 问题分析
警告的原因在[This Handler class should be static or leaks might occur: IncomingHandler](http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler)这个帖子中有讲到。这篇帖子中的示例代码如下：

```
public class UDPListenerService extends Service
{
    private static final String TAG = "UDPListenerService";
    //private ThreadGroup myThreads = new ThreadGroup("UDPListenerServiceWorker");
    private UDPListenerThread myThread;
    /**
     * Handler to communicate from WorkerThread to service.
     */
    private Handler mServiceHandler;

    // Used to receive messages from the Activity
    final Messenger inMessenger = new Messenger(new IncomingHandler());
    // Use to send message to the Activity
    private Messenger outMessenger;

    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
        }
    }

    /**
     * Target we publish for clients to send messages to Incoming Handler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    [ ... ]
}
```

> 如果`IncomingHandler`类是非静态的，它将持有一个对`Service`对象的引用。同一个线程中的所有`Handler`对象共享同一个`Looper`对象，这些`Handler`都从同一个`Looper`中读取和发送`Message`。

> 所有的`Message`都包含一个目标`Handler`成员，只要消息队列中有消息引用这个目标`Handler`，那它就不能被垃圾回收。如果`Handler`不是静态的，`Service`或`Activity`就不能被垃圾回收，即使`Service`或`Activity`被销毁。

> 这可能引起内存泄露，至少是某些时候会出现内存泄露——只要消息还在队列中。这通常不是什么大问题，除非你发送长时间的延时消息。

可以将`IncomingHandler`变成静态的，并且持有对`Service`的`WeakReference`，修改后的代码如下：

```
static class IncomingHandler extends Handler {
    private final WeakReference<UDPListenerService> mService; 

    IncomingHandler(UDPListenerService service) {
        mService = new WeakReference<UDPListenerService>(service);
    }
    @Override
    public void handleMessage(Message msg)
    {
         UDPListenerService service = mService.get();
         if (service != null) {
              service.handleMessage(msg);
         }
    }
}
```

注意：<font color="red">如果想使用嵌套类，它必须是静态的。否则，`WeakReference`不起任何作用。内部类(嵌套但是非静态的)永远持有对外部对象的引用。</font>当被引用的对象被回收后，`WeakReference.get()`将返回`null`。这是正确的，说明此时`Service`或`Activity`确实已死亡。

# Handler如何检测内存泄露
出现Lint警告的原因是因为检查到潜在的内存泄露。构造`Handler`对象时传递`Handler.Callback`可以避免出现警告，因为这时没有继承`Handler`所以并没有非静态的`Handler`内部类。

```
Handler mIncomingHandler = new Handler(new Handler.Callback() {
    @Override
    public boolean handleMessage(Message msg) {
    }
});
```

<font color="red">当然，这么写并不能真正地避免潜在的内存泄露</font>。匿名的`Callback`会持有外部对象，而`mIncomingHandler`又持有`Handler.Callback`对象。只要`Message`仍然在`Looper`的消息队列中，`Service`就不能被GC。如上，只要队列中没有长延时的消息，这通常不是严重问题。

问题来了，为什么能绕开Lint检查呢？看看`Handler`是如何检查内存泄露的：

```
public class Handler {
    /*
     * Set this flag to true to detect anonymous, local or member classes
     * that extend this Handler class and that are not static. These kind
     * of classes can potentially create leaks.
     */
    private static final boolean FIND_POTENTIAL_LEAKS = true;
	...
	
    public Handler(Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }
		...
	}

	...
}
```

后一个条件容易理解，检查相应的`Handler`类是否有`static`修饰符；前一个条件，检查当前类是否匿名类、成员类或本地类。

其实不过是判断当前类是否嵌套类。这里的检查并不严格。比如，并不针对`Callback`进行检查，所以通过`Handler(Callback)`构造方法创建Handler可以绕开`FIND_POTENTIAL_LEAKS`检查。

关于匿名类、[成员类][成员类]和[本地类][本地类]。

+ 成员类 - Member Classes. A member class is a class that is declared as a non-static member of a containing class. 
+ 本地类 - Local classes are classes that are defined in a block, which is a group of zero or more statements between balanced braces. 

看下面代码应该更容易理解匿名类、[成员类][成员类]和[本地类][本地类]：

```
public class MyClass {

    private Handler mHandler = new Handler(); // OK
    private Handler mHandler2 = new Handler() {}; // Leaks might occur, 匿名内部类, 会引用外部类
    private InnerHandler mHandler3 = new InnerHandler(); // Leaks might occur, 内部类, 会引用外部类
    private NestedHandler mHandler4 = new NestedHandler(); // OK, 嵌套类, 不会引用外部类

    private class InnerHandler extends Handler {}

    private static class NestedHandler extends Handler {}

    public static void main(String[] args) {
        MyClass obj = new MyClass();


        new Handler();          // OK
        new Handler() {};       // OK, 匿名嵌套类, 不会引用外部类

        System.out.println(obj.mHandler2.getClass().isAnonymousClass()); // Anonymous Class
        System.out.println(obj.mHandler3.getClass().isMemberClass());    // Member Class
        System.out.println(new HashMap<String, String>() {}.getClass().isAnonymousClass());

        {
            class LocalClass {}
            System.out.println(LocalClass.class.isLocalClass()); // Local Class
        }
    }
}

public class Handler {
    private static final boolean FIND_POTENTIAL_LEAKS = true;

    public static class Message {

    }

    public interface Callback {
        public boolean handleMessage(Message msg);
    }

    public Handler() {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                System.err.println("The following Handler class should be static or leaks might occur: " +
                        klass.getCanonicalName());
            } else {
                System.out.println("OK: " + klass.getCanonicalName());
            }
        }
    }
}
```

注：后四个`System.out.println`输出结果均为`true`。

# 安全的Handler
安全的Handler写起来大同小异，<font color="red">基本原理是持有Activity或Service的弱引用而不是强引用，避免长延时Message未被处理引起销毁后的Activity等组件不能及时被GC</font>。但有些实现用起来不太符合习惯，比如[这个](http://code.oa.com/v2/weima/detail/19193)，只能通过继承的方式使用。

安全的Handler写起来并不困难，但每次重头写还是比较繁琐的。我的实现如下，附带典型用法：

```
public class SafeHandler<T extends HandlerContainer> extends Handler {
        protected WeakReference<T> mRef;

        public SafeHandler(WeakReference<T> ref) {
            mRef = ref;
        }
		
        public SafeHandler(T obj) {
            mRef = new WeakReference<>(obj);
        }

        public T getContainer() {
            return mRef.get();
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            HandlerContainer container = getContainer();
            if (container != null) {
                container.handleMessage(msg);
            }
        }
}

/**
 * 表示Handler所在的容器(类), 通常是Activity或Fragment.
 */
public interface HandlerContainer {
	void handleMessage(Message message);
}
```

示例:

```
public class DemoActivity extends Activity implements HandlerContainer {
    // 实现接口是不是比继承父类清爽些!
	private SafeHandler<DemoActivity> mSafeHandler = new SafeHandler<>(this);

	// 也不阻止你使用继承
	private class MyContainer implements HandlerContainer {
		@Override
		public void handleMessage(Message message) {
			// handle message
		}
	}

    private SafeHandler<MyContainer> mSafeHandler2 = new SafeHandler<>(new MyContainer());
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// 发条消息
        mSafeHandler.sendEmptyMessageDelayed(0, 5 * 60 * 1000);
    }

    @Override
    public void handleMessage(Message message) {
        // handle message
    }
}
```

Android SDK提供[`Callback`][callback]接口，它有一个`boolean handleMessage(Message message)`方法，`HandlerContainer`其实是不必要的。简化后的代码：

```
public class SafeHandler<T extends Handler.Callback> extends Handler {
        protected WeakReference<T> mRef;

        public SafeHandler(WeakReference<T> ref) {
            mRef = ref;
        }

        public SafeHandler(T obj) {
            mRef = new WeakReference<>(obj);
        }

        public T getContainer() {
            return mRef.get();
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            Callback container = getContainer();
            if (container != null) {
                container.handleMessage(msg);
            }
        }
}

public class DemoActivity extends Activity implements Handler.Callback {
    private Sugar.SafeHandler<DemoActivity> mSafeHandler = new Sugar.SafeHandler<>(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSafeHandler.sendEmptyMessageDelayed(0, 5 * 60 * 1000);
    }

    @Override
    public boolean handleMessage(Message message) {
        // handle message
        return false;
    }
}
```


[成员类]: http://docstore.mik.ua/orelly/java-ent/jnut/ch03_10.htm
[本地类]: https://docs.oracle.com/javase/tutorial/java/javaOO/localclasses.html
[callback]: https://developer.android.com/reference/android/os/Handler.Callback.html
