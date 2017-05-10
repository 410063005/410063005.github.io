---
title: how-to-set-actionbar-text
tags:
---


```java
public void setHomeButtonText(CharSequence charSequence) {
    TextView upTextView = new TextView(this);
    
    upTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    upTextView.setPadding((int) (4 * SystemUtils.getDensity(this)), 0, 0, 0);
    upTextView.setTextColor(ContextCompat.getColor(this, R.color.n_H1));
    upTextView.setText(charSequence);

    upTextView.measure(0, 0);
    upTextView.layout(0, 0, upTextView.getMeasuredWidth(),
            upTextView.getMeasuredHeight());
    Bitmap bitmap = Bitmap.createBitmap(upTextView.getMeasuredWidth(),
            upTextView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    upTextView.draw(canvas);
    BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setHomeAsUpIndicator(bitmapDrawable);
    }
}
```

http://stackoverflow.com/questions/14172654/how-to-set-actionbar-logo-to-text-textview