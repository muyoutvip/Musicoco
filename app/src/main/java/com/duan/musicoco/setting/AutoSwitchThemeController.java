package com.duan.musicoco.setting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.duan.musicoco.app.App;
import com.duan.musicoco.app.manager.BroadcastManager;
import com.duan.musicoco.preference.AppPreference;
import com.duan.musicoco.preference.ThemeEnum;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by DuanJiaNing on 2017/8/19.
 */

public class AutoSwitchThemeController {

    private static volatile AutoSwitchThemeController mInstance;

    private final Context context;
    private final AppPreference appPreference;
    private Calendar current, nightThemeStart, nightThemeEnd;
    private PendingIntent piS, piE;
    private AlarmManager alarmManager;

    private boolean isSet = false;

    private AutoSwitchThemeController(Context context) {
        this.context = context;
        this.appPreference = new AppPreference(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
    }

    public static AutoSwitchThemeController getInstance(Context context) {
        if (mInstance == null) {
            synchronized (AutoSwitchThemeController.class) {
                if (mInstance == null) {
                    mInstance = new AutoSwitchThemeController(context);
                }
            }
        }
        return mInstance;
    }

    // 设置切换提醒
    public void setAlarm() {

        current = Calendar.getInstance();
        nightThemeStart = new GregorianCalendar(
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH),
                22, 30); // 22:30 切换到夜间模式
        nightThemeEnd = new GregorianCalendar(
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH),
                7, 0); // 07:00 切换到白天模式
        checkTheme();
        isSet = true;

        Intent intentS = new Intent();
        Intent intentE = new Intent();

        intentS.setAction(BroadcastManager.FILTER_APP_THEME_CHANGE_AUTOMATIC);
        intentS.putExtra(BroadcastManager.APP_THEME_CHANGE_AUTOMATIC_TOKEN,
                BroadcastManager.APP_THEME_CHANGE_AUTOMATIC_DARK);

        intentE.setAction(BroadcastManager.FILTER_APP_THEME_CHANGE_AUTOMATIC);
        intentE.putExtra(BroadcastManager.APP_THEME_CHANGE_AUTOMATIC_TOKEN,
                BroadcastManager.APP_THEME_CHANGE_AUTOMATIC_WHITE);

        piS = PendingIntent.getBroadcast(context, 0, intentS, 0);
        piE = PendingIntent.getBroadcast(context, 1, intentE, 0);

        alarmManager.set(AlarmManager.RTC_WAKEUP, nightThemeStart.getTimeInMillis(), piS);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nightThemeEnd.getTimeInMillis(), piE);

    }

    public void cancelAlarm() {
        if (piE != null) {
            alarmManager.cancel(piE);
        }
        if (piS != null) {
            alarmManager.cancel(piS);
        }
        isSet = false;
    }

    private void checkTheme() {

        int curH = current.get(Calendar.HOUR_OF_DAY);
        int curM = current.get(Calendar.MINUTE);
        int startH = nightThemeStart.get(Calendar.HOUR_OF_DAY);
        int startM = nightThemeStart.get(Calendar.MINUTE);
        int endH = nightThemeEnd.get(Calendar.HOUR_OF_DAY);
        int endM = nightThemeEnd.get(Calendar.MINUTE);

        if (curH >= startH && curM >= startM && curH < endH && curM < endM) {
            //  切换夜间
            appPreference.updateTheme(ThemeEnum.DARK);

        } else {
            //  切换白天
            appPreference.updateTheme(ThemeEnum.WHITE);
        }
        BroadcastManager.getInstance(context).sendBroadcast(BroadcastManager.FILTER_APP_THEME_CHANGE_AUTOMATIC, null);
    }

    public boolean isSet() {
        return isSet;
    }
}