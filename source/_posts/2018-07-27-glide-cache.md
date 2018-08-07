---
title: Glide缓存分析
tags: android
date: 2018-07-27 18:02:56
---

Glide的缓存有多大，其缓存策略是怎样的？
<!--more-->

# 缓存和对象池的大小
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

# 缓存
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

## Memory缓存

从缓存中加载图片的流程如下。

![](load-from-cache.png)

```java
// Engine.java

  private EngineResource<?> loadFromCache(Key key, boolean isMemoryCacheable) {
    if (!isMemoryCacheable) {
      return null;
    }

    EngineResource<?> cached = getEngineResourceFromCache(key);
    if (cached != null) {
      cached.acquire();
      activeResources.put(key, new ResourceWeakReference(key, cached, getReferenceQueue()));
    }
    return cached;
  }

  @SuppressWarnings("unchecked")
  private EngineResource<?> getEngineResourceFromCache(Key key) {
    Resource<?> cached = cache.remove(key);

    final EngineResource<?> result;
    if (cached == null) {
      result = null;
    } else if (cached instanceof EngineResource) {
      // Save an object allocation if we've cached an EngineResource (the typical case).
      result = (EngineResource<?>) cached;
    } else {
      result = new EngineResource<>(cached, true /*isMemoryCacheable*/);
    }
    return result;
  }

```

还有以下几个相关方法。

```java
  @SuppressWarnings("unchecked")
  @Override
  public void onEngineJobComplete(Key key, EngineResource<?> resource) {
    Util.assertMainThread();
    // A null resource indicates that the load failed, usually due to an exception.
    if (resource != null) {
      resource.setResourceListener(key, this);

      if (resource.isCacheable()) {
        activeResources.put(key, new ResourceWeakReference(key, resource, getReferenceQueue()));
      }
    }
    // TODO: should this check that the engine job is still current?
    jobs.remove(key);
  }

  @Override
  public void onResourceReleased(Key cacheKey, EngineResource resource) {
    Util.assertMainThread();
    activeResources.remove(cacheKey);
    if (resource.isCacheable()) {
      cache.put(cacheKey, resource);
    } else {
      resourceRecycler.recycle(resource);
    }
  }

  @Override
  public void onResourceRemoved(final Resource<?> resource) {
    Util.assertMainThread();
    resourceRecycler.recycle(resource);
  }

```

向缓存添加图片的流程如下

![](glide-cache-arch.png)

有一个很重要的点：跟之前预想的不同，完成解码的图片resource并不是一开始就被添加到cache，而是先添加到active resource。当resource被释放时，如果可缓存则添加到cache，如果不可缓存则经由recycler回收至bitmapPool

## Disk缓存
disk缓存相对memory缓存不那么直观。对disk缓存的访问分散在几个不同的阶段。

一个是在DecodeJob快结束阶段

![](decode-job.png)

```java
  private static class DeferredEncodeManager<Z> {
    void encode(DiskCacheProvider diskCacheProvider, Options options) {
      TraceCompat.beginSection("DecodeJob.encode");
      try {
        diskCacheProvider.getDiskCache().put(key,
            new DataCacheWriter<>(encoder, toEncode, options));
      } finally {
        toEncode.unlock();
        TraceCompat.endSection();
      }
    }
  }
```

另一个是在DataFetcherGenerator.startNext()阶段

DataFetcherGenerator有三个子类

+ SourceGenerator
+ DataCacheGenerator
+ ResourceCacheGenerator

这三个子类在`DecodeJob.runGenerators()`中均有被用到。`runGenerators()`按下面的状态图运行(DecodeJob.Stage)。

```java
  /**
   * Where we're trying to decode data from.
   */
  private enum Stage {
    /** The initial stage. */
    INITIALIZE,
    /** Decode from a cached resource. */
    RESOURCE_CACHE,
    /** Decode from cached source data. */
    DATA_CACHE,
    /** Decode from retrieved source. */
    SOURCE,
    /** Encoding transformed resources after a successful load. */
    ENCODE,
    /** No more viable stages. */
    FINISHED,
  }
```

![](stage.png)

ResourceCacheGenerator和DataCacheGenerator均有`DiskCache.get()`操作，而SourceGenerator有`DiskCache.put()`操作。

```java
class ResourceCacheGenerator implements DataFetcherGenerator,

  @Override
  public boolean startNext() {
      ...
      currentKey = new ResourceCacheKey(sourceId, helper.getSignature(), helper.getWidth(),
          helper.getHeight(), transformation, resourceClass, helper.getOptions());
      cacheFile = helper.getDiskCache().get(currentKey);
      if (cacheFile != null) {
        this.sourceKey = sourceId;
        modelLoaders = helper.getModelLoaders(cacheFile);
        modelLoaderIndex = 0;
      }
    }
  }
}

class DataCacheGenerator implements DataFetcherGenerator,

  @Override
  public boolean startNext() {
    while (modelLoaders == null || !hasNextModelLoader()) {
      ...
      Key sourceId = cacheKeys.get(sourceIdIndex);
      Key originalKey = new DataCacheKey(sourceId, helper.getSignature());
      cacheFile = helper.getDiskCache().get(originalKey);
      if (cacheFile != null) {
        this.sourceKey = sourceId;
        modelLoaders = helper.getModelLoaders(cacheFile);
        modelLoaderIndex = 0;
      }
    }
}

class SourceGenerator implements DataFetcherGenerator {

  @Override
  public boolean startNext() {
    if (dataToCache != null) {
      Object data = dataToCache;
      dataToCache = null;
      cacheData(data);
    }
    ...
  }

  private void cacheData(Object dataToCache) {
    long startTime = LogTime.getLogTime();
    try {
      Encoder<Object> encoder = helper.getSourceEncoder(dataToCache);
      DataCacheWriter<Object> writer =
          new DataCacheWriter<>(encoder, dataToCache, helper.getOptions());
      originalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
      helper.getDiskCache().put(originalKey, writer);
      ...
    } finally {
      loadData.fetcher.cleanup();
    }

    sourceCacheGenerator =
        new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), helper, this);
  }
}


```

# 对象池
跟缓存相比，BitmapPool和ArrayPool使用简单明了很多。容易理解，BitmapPool出现在有Bitmap回收需求的地方，而ArrayPool则出现在有解码需求的地方。

比如，BitmapResource和BitmapDrawableResource都有一个BitmapPool成员。`Resource.recycle()`时相关的Bitmap不是被抛弃而是放回BitmapPool。`Downsampler`并不是真的生成新的Bitmap，有可能是从BitmapPool拿到的。

```java
public class BitmapDrawableResource extends DrawableResource<BitmapDrawable>
    implements Initializable {
  private final BitmapPool bitmapPool;

  public BitmapDrawableResource(BitmapDrawable drawable, BitmapPool bitmapPool) {
    super(drawable);
    this.bitmapPool = bitmapPool;
  }
}

public class BitmapResource implements Resource<Bitmap>,
    Initializable {
  private final Bitmap bitmap;
  private final BitmapPool bitmapPool;
}

public final class Downsampler {
  private final BitmapPool bitmapPool;

  private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int width,
      int height) {
    // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
    options.inBitmap = bitmapPool.getDirty(width, height, options.inPreferredConfig);
  }
}
```

而StreamGifDecoder和StreamBitmapDecoder都有一个ArrayPool成员。解码过程中需要用到byte[]，但不是直接new byte[]，而是调用`ArrayPool.get()`从对象池中拿，用完了归还。

```java
public class StreamGifDecoder implements ResourceDecoder<InputStream, GifDrawable> {

  private final List<ImageHeaderParser> parsers;
  private final ResourceDecoder<ByteBuffer, GifDrawable> byteBufferDecoder;
  private final ArrayPool byteArrayPool;

  public StreamGifDecoder(List<ImageHeaderParser> parsers, ResourceDecoder<ByteBuffer,
      GifDrawable> byteBufferDecoder, ArrayPool byteArrayPool) {
    this.parsers = parsers;
    this.byteBufferDecoder = byteBufferDecoder;
    this.byteArrayPool = byteArrayPool;
  }
}

public class StreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {

  private final Downsampler downsampler;
  private final ArrayPool byteArrayPool;

  public StreamBitmapDecoder(Downsampler downsampler, ArrayPool byteArrayPool) {
    this.downsampler = downsampler;
    this.byteArrayPool = byteArrayPool;
  }
}
```
