package com.jm.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import java.lang.reflect.Field;

public class TestOnlyUtil {
    public static void removeTestOnlyFlag(Context context, String packageName, String className) {
        try {
            PackageManager pm = context.getPackageManager();
            ActivityInfo activityInfo = pm.getActivityInfo(new ComponentName(packageName, className), 0);

            Field testOnlyField = ActivityInfo.class.getDeclaredField("testOnly");
            testOnlyField.setAccessible(true);
            testOnlyField.setBoolean(activityInfo, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}