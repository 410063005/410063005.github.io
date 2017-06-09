---
title: ArgbEvaluator的用法
tags:
  - android
date: 2017-05-27 17:02:37
---

介绍了ArgbEvaluator用法。
<!-- more -->

[这篇文章](https://github.com/jiang111/awesome-android-tips)提到了ArgbEvaluator 

> ArgbEvaluator ArgbEvaluator.evaluate(float fraction, Object startValue, Object endValue);根据一个起始颜色值和一个结束颜色值以及一个偏移量生成一个新的颜色，分分钟实现类似于微信底部栏滑动颜色渐变。


ArgbEvaluator继承自TypeEvaluator，TypeEvaluator的定义相当简单：

```java
/**
 * 本接口用于ValueAnimator.setEvaluator(TypeEvaluator)方法。Evaluator可用于给任意属性类型创建动画，它允许给不能自动被动画系统理解和使用的类型创建自定义evaluator
 */
public interface TypeEvaluator<T> {

    /**
     * 这个方法返回起始值和结束值的一个线性插值，fraction参数代表起始值和结束值之间的一个比例。这个方法是一个简单的参数计算：result = x0 + t * (x1 - x0)
     * 这里的x0是起始值，x1是结束值，t是fraction
     */
    public T evaluate(float fraction, T startValue, T endValue);
}


/**
 * 这个evaluator用于ARGB色值插值计算
 */
public class ArgbEvaluator implements TypeEvaluator {

    /**
     * 这个方法对ARGB色值的每一个通道进行插值计算，返回合并后的色值
     */
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }
}
```

这里写了一个demo用于演示ArgbEvaluator的用法，完整代码见[这里](https://github.com/410063005/demos.git)。

![](screen.gif)

```java
public class ArgbEvaluatorDemo extends AppCompatActivity {
    private static final String TAG = "ArgbEvaluatorDemo";

    TabLayout mTabLayout;
    ViewPager mViewPager;
    ArgbEvaluator mEvaluator;
    int mStartColor;
    int mEndColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_argb_evaluator_demo);

        mStartColor = ContextCompat.getColor(ArgbEvaluatorDemo.this, R.color.colorAccent);
        mEndColor = ContextCompat.getColor(ArgbEvaluatorDemo.this, R.color.colorPrimary);

        mTabLayout = (TabLayout) findViewById(R.id.pager_titles);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new Fragment1();

                    default:
                        return new Fragment2();
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

        });

        // mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);


        mEvaluator = new ArgbEvaluator();

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout) {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);


                if (positionOffset > 0) {
                    Log.i(TAG, "onPageScrolled: " + position + " " + (position + 1));

                    TabLayout.Tab t = mTabLayout.getTabAt(position);
                    if (t != null && t.getCustomView() != null) {
                        TextView tv = (TextView) t.getCustomView().findViewById(android.R.id.text1);
                        tv.setTextColor((int) mEvaluator.evaluate(positionOffset, mStartColor, mEndColor));
                    }

                    TabLayout.Tab t2 = mTabLayout.getTabAt(position + 1);
                    if (t2 != null && t2.getCustomView() != null) {
                        TextView tv2 = (TextView) t2.getCustomView().findViewById(android.R.id.text1);
                        tv2.setTextColor((int) mEvaluator.evaluate(positionOffset, mEndColor, mStartColor));
                    }
                } else {
                    // Log.i(TAG, "onPageScrolled: " + position + " " + (position + 1));
                }


            }
        });
        mTabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
    }

    public static class Fragment1 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView tv = new TextView(getContext());
            tv.setText("#1");
            return tv;
        }
    }

    public static class Fragment2 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView tv = new TextView(getContext());
            tv.setText("#2");
            return tv;
        }
    }
}
```