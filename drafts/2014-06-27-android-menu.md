http://developer.android.com/guide/topics/ui/menus.html

## options menu和action bar
应该将搜索，写邮件，设置等会对app全局产生影响的action放到option menu中。从3.0开始，option menu中的item
将出现在action bar中。

而一些3.0以上的设备，已经去掉了menu键。

## context menu和contextual action mode
context menu是指用户长按某个元素后出现的漂浮menu。它包含影响当前选中内容和当前context的action。

在3.0以上的设备上，应当使用contextual action mode。这个mode会在当前屏幕的顶部显示可以操作的action，这些
action会对当前选中的内容产生影响，且允许用户选中多个项。

## popup menu
popup up在垂直list中显示跟当前view相关的一系列action。popup menu中的action不应直接影响相应的内容，因为这是contextual 
actions的职责。

# 在xml中定义menu
应当使用xml menu resource来定义menu，而不应使用java代码完成这些工作(虽然也可行)。

<group>标签是一个可选标签，可以理解为一个不可见的container(invisible container)。它允许将menu item分类，同一类的menu
可以共享相同的属性，比如active state和可见性。

<item>标签主要包含以下属性：
+ id
+ icon
+ title
+ showAsAction，指定了这个item何时以及怎样显示在action bar中。

<menu>还可以包含子菜单。

使用 MenuInflater.inflate() 将一个xml menu resource生成menu

# creating an option menu

>Even if you're not developing for Android 3.0 or higher, you can build your own action bar layout 
>for a similar effect. For an example of how you can support older versions of Android with an action bar, 
>see the Action Bar Compatibility sample.

? 即使不是为android 3.0开发，也可自己实现类似于action bar的效果。

既可以从activity中定义option menu也可以从fragment中定义option menu。如果两者中都有，则它们会组合起来显示在ui中。
activity的option menu先显示，然后是fragment的。每个fragment的option menu按fragment被添加到activity中的顺序显示。
有必要的话，可以通过 item 的 orderInCategory 属性来调整 <item> 的顺序。

注意，2.3及以下版本中，系统会在用户第一次打开option menu时调用 onCreateOptionsMenu()，而3.0及以上版本中，系统会在
activity 启动时调用 onCreateOptionsMenu()，以将各个 item 显示到 action bar。

# Handling click events

系统会先调用 activity 的 onOptionsItemSelected() ， 然后调用 fragment 的 onOptionsItemSelected()，直到某个 onOptionsItemSelected()
返回 true，或者所有的 onOptionsItemSelected() 被调用。

小技巧：3.0中可以在 menu item xml中增加了 andrid:onClick 属性来定义 on-click 行为。该属性的值必须是activity中定义的某个方法名，
该方法为public，且接受一个 MenuItem 参数。跟 activity 的布局中的 onClick 很像。

小技巧：如果你的应用中有多个 activity，且它们中的某些有相同的 option menu, 可以创建一个 activity， 这个activity什么也不做只是实现 
onCreateOptionsMenu() 和 onOptionsItemSelected() 方法。需要相同 option menu 的 activity 只需要继承这个类。
这样，你可以为多个类管理一份代码，且所有的子类都有相同的 menu 行为。如果某个子类中需要新的 menu item，重写 onCreateOptionsMenu()，并
调用 super.onCreateOptionsMenu()，之后调用 menu.add() 来添加新的 item。

# 运行时修改 menu item
系统调用 onCreateOptionsMenu() 后，它将持有 Menu 的实例，除非因为某些原因导致这个无效， onCreateOptionsMenu()方法不会再次被调用。当然，
在 onCreateOptionsMenu() 方法中你应该只是创建了　menu 的初始状态。

可以在 onPrepareOptionMenu() 方法中修改 menu，包括增加、移除或禁用 item。

2.3及以下版本中，系统会在用户每次打开 option menu 时调用 onPrepareOptionMenu()。

3.0及以上版本中，如果 option menu 的 item 出现在 action bar 中， option menu会被认为一直是打开的。这时如果想更新 menu，则需要调用 
invalidateOptionMenu() 来要求系统重新调用 onPrepareOptionMenu() 方法。需要相同

# Creating Contextual Menus
可能为任何 view 提供 contextual menu，但它们通常用于 ListView，GridView或其他 view collections，用户可以对每个 item 进行直接操作。

注意：在 3.0以上的设备上，应当优先使用 contextual action mode。只有在 3.0 以下设备上，才应使用 floating context menu。

## Creating a floating context menu

View.registerForContextMenu()

## Using the contextual action mode

通常在以下情况进行 context action mode：
+ 用户在 view 上长按
+ 用户选择了 checkbox 或类似的 UI控件

// TODO 实践一下

可以使用 ActionMode api 来修改 contextual action bar，比如 title 或 subtitle (指示当前多少 item 被选中时非常有效)

    ListView listView = getListView();
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            return false;
        }
    });


# Creating a Popup Menu
PopupMenu 是跟 View 关联的模块化的 menu。 

注意： popup menu 跟 context menu 存在一些差别，后者通常是指一些会影响选中内容的 action。 
popup menu 通常是提供命令语句中的另一个部分，比如，回复邮件时可以回复或回复全部，一个用于添加的 button 不仅可以添加A，
还可能添加B。 popup menu 跟 spinner 有些像，但是不会一直显示同样的可选项。

// 实践一下

显示 popup menu 

    <ImageButton
        android:layout_width="wrap_content" 
        android:layout_height="wrap_content" 
        android:src="@drawable/ic_overflow_holo_dark"
        android:contentDescription="@string/descr_overflow_button"
        android:onClick="showPopup" />
    

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }
处理 popup menu 事件

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.actions);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.archive:
                archive(item);
                return true;
            case R.id.delete:
                delete(item);
                return true;
            default:
                return false;
        }
    }

# creating menu groups
menu group 是一组 menu item , 它们共享相同的某些属性。使用 menu group 可以实现以下功能：
+ setGroupVisible(): 显示或隐藏所有 items
+ setGroupEnabled(): 启用或禁用所有 items
+ setGroupCheckable(): 是否可以 checkbox


 