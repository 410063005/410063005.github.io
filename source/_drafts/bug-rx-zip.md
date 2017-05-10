---
title: bug-rx-zip
tags:
- android
- rx
---

从一个例子分析RxJava中`zipWith`操作符的使用方法。

<!--more-->

# 问题

![](demo.jpg)

点"通过"后，实际过程是这样的：

1. 第一步，先请求后台接口将用户加入小组
2. 第二步，再操作本地数据库修改本条记录(将`accepted`状态设置为true)
3. 第三步，最后更新当前UI

第一步如果出错了，是不能执行第二步的，否则会出现用户实际并没有成功加入小组，但应用中显示已通过申请的情况。

很不幸，我们的应用中出现了这样的bug。

# 分析
这里是很典型的一个嵌套请求的场景。如果用回调的方式来实现，大概是这样：

```java
joinTeam(new Callback() {
    
    public void onCompleted() {
        updateDb(new Callback() {
            public void onCompleted() {
            
            }
        
            public void onError(Throwable t) {
            
            }
        });
    }

    public void onError(Throwable t) {
    
    }
});
```

多层嵌套的回调代码可读性非常差，使用RxJava可以避免多层嵌套。就我们这个例子简化后的代码是这样：

```java
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {

        Observable<String> network = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(networkOperation());
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }
        })
        .subscribeOn(Schedulers.io());

        Observable<String> db = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(dbOperation());
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }
        })
        .subscribeOn(Schedulers.io());

        network
                .zipWith(db, new Func2<String, String, String>() {
                    @Override
                    public String call(String s, String s2) {
                        return s + " " + s2;
                    }
                })
                .subscribe(
                        new Action1<String>() {
                            @Override
                            public void call(String s) {
                                System.out.println(s);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                System.err.println(throwable);
                            }
                        });

        TimeUnit.SECONDS.sleep(3);
    }

    private static String networkOperation() {
        if (new Random().nextBoolean()) {
            throw new RuntimeException("network: something wrong");
        }
        System.out.println("networkOperation finished. thread=" + Thread.currentThread().getName());
        return "network result";
    }

    private static String dbOperation() {
        System.out.println("dbOperation finished. thread=" + Thread.currentThread().getName());
        return "db result";
    }
```

打出的日志如下。分两种情况，一种是网络操作成功，一种是网络操作失败。

```
networkOperation finished. thread=RxIoScheduler-2
dbOperation finished. thread=RxIoScheduler-3
network result db result
```

网络操作成功时打出的日志符合我们的预期。网络操作和数据库操作在不同的线程中。

```
dbOperation finished. thread=RxIoScheduler-3
java.lang.RuntimeException: network: something wrong
```

网络操作失败时打出的日志就很奇怪了：网络操作已经失败了，但仍然执行了数据库操作。这就是我们bug的原因所在。

# 改进

```java
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {

        Observable<String> network = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(networkOperation());
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }
        });
        //.subscribeOn(Schedulers.io());

        Observable<String> db = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(dbOperation());
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }
        });
        //.subscribeOn(Schedulers.io());

        network
                .zipWith(db, new Func2<String, String, String>() {
                    @Override
                    public String call(String s, String s2) {
                        return s + " " + s2;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        new Action1<String>() {
                            @Override
                            public void call(String s) {
                                System.out.println(s);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                System.err.println(throwable);
                            }
                        });

        TimeUnit.SECONDS.sleep(3);
    }

    private static String networkOperation() {
        if (new Random().nextBoolean()) {
            throw new RuntimeException("network: something wrong");
        }
        System.out.println("networkOperation finished. thread=" + Thread.currentThread().getName());
        return "network result";
    }

    private static String dbOperation() {
        System.out.println("dbOperation finished. thread=" + Thread.currentThread().getName());
        return "db result";
    }
```

修改后的代码在网络操作成功时打出的日志。跟前面不一样，网络操作和数据库操作在相同的线程中。

```
networkOperation finished. thread=RxIoScheduler-2
dbOperation finished. thread=RxIoScheduler-2
network result db result
```

修改后的代码在网络操作失败时打出的日志符合我们的预期。

```
java.lang.RuntimeException: network: something wrong
```

再来看我们的改动。修改前，`network`和`db`两个Observable均有`subscribeOn(Schedulers.io())`，而修改后是在`zipWith`操作后执行`subscribeOn(Schedulers.io())`的

# 理解zipWith和subscribeOn

简单来说，这个操作就是对源Observable和另一个Observable返回一个新的Observable，这个新的Observable发射出来的数据是对前两个发射出来的数据项应用指定的函数得到的。

如果一个源Observable发射的数据数量少于另一个，它会比另一个早结束。所以，另一个源Observable可能无法运行完成(所以也不会调用doOnCompleted())。即使两个源Observable发身的数据一样多也可能出现其中一个无法运行完成的情形。比如A已经结束了，而B即将结束，操作符检查到A再也不会发射新的数据了，会马上对B进行unsubscribe操作。


Observable<Integer> o = Observable.just(1, 2, 3);
Observable<String> o2 = Observable.just("A", "B");

输出
1 A
2 B

Observable<Integer> o = Observable.just(1, 2, 3);
Observable<String> o2 = Observable.just("A", "B", "C");

输出
1 A
2 B
3 C

```
public final <T2, R> Observable<R> zipWith(Observable<? extends T2> other,
                                           rx.functions.Func2<? super T, ? super T2, ? extends R> zipFunction)
Returns an Observable that emits items that are the result of applying a specified function to pairs of values, one each from the source Observable and another specified Observable.

The operator subscribes to its sources in order they are specified and completes eagerly if one of the sources is shorter than the rest while unsubscribing the other sources. Therefore, it is possible those other sources will never be able to run to completion (and thus not calling doOnCompleted()). This can also happen if the sources are exactly the same length; if source A completes and B has been consumed and is about to complete, the operator detects A won't be sending further values and it will unsubscribe B immediately. For example:
range(1, 5).doOnCompleted(action1).zipWith(range(6, 5).doOnCompleted(action2), (a, b) -> a + b)
action1 will be called but action2 won't. To work around this termination property, use doOnUnsubscribed() as well or use using() to do cleanup in case of completion or unsubscription.  
Backpressure:

The operator expects backpressure from the sources and honors backpressure from the downstream. (I.e., zipping with interval(long, TimeUnit) may result in MissingBackpressureException, use one of the onBackpressureX to handle similar, backpressure-ignoring sources.
Scheduler:
zipWith does not operate by default on a particular Scheduler.
Parameters:
other - the other Observable
zipFunction - a function that combines the pairs of items from the two Observables to generate the items to be emitted by the resulting Observable
Type parameters:
<T2> - the type of items emitted by the other Observable
<R> - the type of items emitted by the resulting Observable
Returns:
an Observable that pairs up values from the source Observable and the other Observable and emits the results of zipFunction applied to these pairs
See Also:
ReactiveX operators documentation: Zip
```

[zip图示](http://reactivex.io/documentation/operators/zip.html)

[zip]: https://juejin.im/entry/575691bb5bbb50006456a771