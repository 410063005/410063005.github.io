---
title: glide-cache
tags: android
---
Glide的缓存有多大，其缓存策略是怎样的？
<!--more-->

# 缓存大小
缓存是一个复杂的话题。Glide缓存有几个主要考虑因素：

+ 一是机器的内存有多大。缓存过大的话，导致内存压力大。缓存过小的话，无法充分利用内存提升性能。所以应当根据内存大小来分配适当大小的缓存。另外，高端机上可以分多分配一些，低端机上则应少分配一些。
+ 二是屏幕大小。Glide分配多少缓存会基于图片数量来考虑。假设一张图片占满足一屏，那应当分配几屏的缓存？

如何判断是否低端机(内存较小的机型)？API level 19以上根据`ActivityManager.isLowMemoryDevice()`判断。

> 如果是低内存机器返回true。一台设备是否低内存最终由设备配置决定，目前而言通常指512MB内存、屏幕分辨率为800*480或更少。

另一个方法是`ActivityManager.getMemoryClass()`。它也很有用。

> 返回当前设备上每个应用大概的内存值。这个值让你了解为了让系统运行良好自己的应用内存占用限值是大概多少。返回值的单位是MB。最少的是16MB(跟这些设备上Java Heap大小相同)。内存较大的设备返回24或更大。

`MemorySizeCalculator`用于计算缓存大小。

```java
  public static final class Builder {
    static final int MEMORY_CACHE_TARGET_SCREENS = 2;
    static final int BITMAP_POOL_TARGET_SCREENS = 4;
    static final float MAX_SIZE_MULTIPLIER = 0.4f;
    static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f;
    // 4MB.
    static final int ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024;

	// 缺省时memoryCacheScreens为2, 即缓存两屏图片
    private float memoryCacheScreens = MEMORY_CACHE_TARGET_SCREENS;
	// 缺省时bitmapPoolScreens为4，即对象池大小为4屏图片
    private float bitmapPoolScreens = BITMAP_POOL_TARGET_SCREENS;
    private float maxSizeMultiplier = MAX_SIZE_MULTIPLIER;
    private float lowMemoryMaxSizeMultiplier = LOW_MEMORY_MAX_SIZE_MULTIPLIER;
    private int arrayPoolSizeBytes = ARRAY_POOL_SIZE_BYTES;
  }

  MemorySizeCalculator(Context context, ActivityManager activityManager,
      ScreenDimensions screenDimensions, float memoryCacheScreens, float bitmapPoolScreens,
      int targetArrayPoolSize, float maxSizeMultiplier, float lowMemoryMaxSizeMultiplier) {	
    this.context = context;
	// 缺省时arrayPoolSizeBytes为4MB, 但低内存机器上为2MB
    arrayPoolSize =
        isLowMemoryDevice(activityManager)
            ? targetArrayPoolSize / LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR
            : targetArrayPoolSize;
    // 当前应用的内存限值(内存最大值, 注意这只是一个建议)
    final int maxSize = getMaxSize(activityManager, maxSizeMultiplier, lowMemoryMaxSizeMultiplier);
    // 一屏数据占用的内存大小
    final int screenSize = screenDimensions.getWidthPixels() * screenDimensions.getHeightPixels()
        * BYTES_PER_ARGB_8888_PIXEL;
    // Bitmap对象池大小
    int targetPoolSize = Math.round(screenSize * bitmapPoolScreens);
    // Bitmap缓存大小
    int targetMemoryCacheSize = Math.round(screenSize * memoryCacheScreens);
    // 剩余可用内存
    int availableSize = maxSize - arrayPoolSize;

    // (Bitmap对象池大小 + Bitmap缓存大小)超过剩余可用内存时进行适当调整
    if (targetMemoryCacheSize + targetPoolSize <= availableSize) {
      memoryCacheSize = targetMemoryCacheSize;
      bitmapPoolSize = targetPoolSize;
    } else {
      float part = availableSize / (bitmapPoolScreens + memoryCacheScreens);
      memoryCacheSize = Math.round(part * memoryCacheScreens);
      bitmapPoolSize = Math.round(part * bitmapPoolScreens);
    }

  }
```

MemorySizeCalculator会输出最终的缓存参数。如果看不到MemorySizeCalculator日志，adb shell中执行`setprop log.tag.MemorySizeCalculator VERBOSE`。我的机器输出如下(为便于查看稍微调整了下格式)：

```
07-26 03:06:09.062 5193-5193/com.example.demo_glide D/MemorySizeCalculator: Calculation complete, Calculated 
memory cache size: 11.47 MB, 
pool size: 22.93 MB, 
byte array size: 4.00 MB, 
memory class limited? true, 
max size: 38.40 MB, 
memoryClass: 96, 
isLowMemoryDevice: false
```

手动校验一遍。

targetMemoryCacheSize: 15500160 = 1080 (width) * 1794 (height) * 4 (每像素占用内存) * 2 (缓存屏数)
targetPoolSize: 31000320 = 1080 (width) * 1794 (height) * 4 (每像素占用内存) * 2 (对象池屏数)
maxSize: 40265320 = 96 (memory class) * 1024 * 1024 * 0.4 (maxSizeMultiplier)
arrayPoolSize: 4194304 = 4 * 1024 * 1024 (缺省的arrayPoolSize)
availableSize: 36071016 = 40265320(maxSize) - 4194304 (arrayPoolSize)

由于targetMemoryCacheSize与targetPoolSize之和超过availableSize，所以实际的缓存大小和BitmapPool大小需要重新调整一下。调整原则很简单，按比例分就行。

part: 6011836 = 36071016(availableSize) / (2 + 4) 
memoryCacheSize: 12023672 = 6011836 * 2
bitmapPoolSize: 24047344 = 6011836 * 4

换算成MB。

memoryCacheSize 12023672B = 11.47MB
bitmapPoolSize 24047344B = 22.93MB
arrayPoolSize 4MB
maxSize 40265320 = 38.40MB

跟日志输出一致。

TODO Glide的缓存大小是否合理？

# 缓存策略
memoryCacheSize，bitmapPoolSize，arrayPoolSize三个参数值确定后，就该真正的主角出场了。

GlideBuilder允许自定义缓存策略。如果没有自定义缓存策略，使用内置的缓存策略。

```java
public final class GlideBuilder {

    public Glide build(Context context) {
        ...
    
        if (memorySizeCalculator == null) {
          memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
        }
    
        if (bitmapPool == null) {
          int size = memorySizeCalculator.getBitmapPoolSize();
          bitmapPool = new LruBitmapPool(size);
        }
    
        if (arrayPool == null) {
          arrayPool = new LruArrayPool(memorySizeCalculator.getArrayPoolSizeInBytes());
        }
    
        if (memoryCache == null) {
          memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
        }
    
        if (diskCacheFactory == null) {
          diskCacheFactory = new InternalCacheDiskCacheFactory(context);
        }
    
        ...
        engine = new Engine(memoryCache, diskCacheFactory, diskCacheExecutor, sourceExecutor,
              GlideExecutor.newUnlimitedSourceExecutor());
    
    
        RequestManagerRetriever requestManagerRetriever = new RequestManagerRetriever(
            requestManagerFactory);
    
        return new Glide(...);
    }
}
```

内置的缓存策略包括。

|类|功能|大小|备注
|----------------|--------------|-------------|----------------------------|
|LruResourceCache|resource内存缓存(为什么不是bitmap缓存?)|memoryCacheSize|[cache-bitmap](https://developer.android.com/topic/performance/graphics/cache-bitmap)      |
|LruBitmapPool   |bitmap池                            |bitmapPoolSize|[bitmap复用](https://www.youtube.com/watch?v=_ioFW3cyRV0)，[manage-memory](https://developer.android.com/topic/performance/graphics/manage-memory)|
|LruArrayPool    |byte数组池                          |arrayPoolSize| 防止图片操作导致内存抖动和频繁GC  |
|DiskCache.Factory|disk缓存                           | -           | [cache-bitmap](https://developer.android.com/topic/performance/graphics/cache-bitmap)  |

![](glide-cache.png)



# 缓存数据
解码？

未解码？

# BitmapPool
