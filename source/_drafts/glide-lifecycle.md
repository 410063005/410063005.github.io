---
title: glide-lifecycle
tags: android
---
Glide中如何实现生命周期绑定


RequestManagerFragment 是何时被添加到Activity上去的

有几个RequestManagerFragment ，它们之间是什么关系

如何查看RequestManagerFragment 的日志


有几个RequestManager ?


RequestManagerFragment RequestManagerRetriever.getRequestManagerFragment()

SupportRequestManagerFragment RequestManagerRetriever.getSupportRequestManagerFragment()


RequestManager最终保存在tag为"com.bumptech.glide.manager"的Fragment当中。可能为RequestManagerFragment类型或SupportRequestManagerFragment类型。Fragment若不存在则创建。



```java
public class RequestManagerFragment extends Fragment {
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      registerFragmentWithRoot(activity);
    } catch (IllegalStateException e) {
      // OnAttach can be called after the activity is destroyed, see #497.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Unable to register fragment with root", e);
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    unregisterFragmentWithRoot();
  }

  private void registerFragmentWithRoot(Activity activity) {
    unregisterFragmentWithRoot();
    rootRequestManagerFragment = Glide.get(activity).getRequestManagerRetriever()
        .getRequestManagerFragment(activity.getFragmentManager(), null);
    if (rootRequestManagerFragment != this) {
      rootRequestManagerFragment.addChildRequestManagerFragment(this);
    }
  }

  private void unregisterFragmentWithRoot() {
    if (rootRequestManagerFragment != null) {
      rootRequestManagerFragment.removeChildRequestManagerFragment(this);
      rootRequestManagerFragment = null;
    }
  }
}
```

Glide.java

```java
  public static RequestManager with(Context context) {
    return getRetriever(context).get(context);
  }

  private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    // Context could be null for other reasons (ie the user passes in null), but in practice it will
    // only occur due to errors with the Fragment lifecycle.
    Preconditions.checkNotNull(
        context,
        "You cannot start a load on a not yet attached View or a  Fragment where getActivity() "
            + "returns null (which usually occurs when getActivity() is called before the Fragment "
            + "is attached or after the Fragment is destroyed).");
    return Glide.get(context).getRequestManagerRetriever();
  }
```



