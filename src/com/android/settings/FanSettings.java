/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Images.Media;
import android.provider.Settings.SettingNotFoundException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import android.util.Log;
import android.hardware.usb.UsbManager;
import java.util.List;
import android.os.AsyncTask;
import android.os.SystemProperties;

public class FanSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener {
    private static final String TAG = "FanSettings";
    private static final boolean DEBUG=false;
    private CheckBoxPreference mAlwaysOnBox;
    private CheckBoxPreference mAlwaysOffBox;
    private CheckBoxPreference mSmartModeBox;
    private static final String KEY_ALWAYS_ON = "fan_always_on";
    private static final String KEY_ALWAYS_OFF = "fan_always_off";
    private static final String KEY_SMARTMODE = "fan_smartmode";
    private static final String FAN_MODE_PROPERTY = "persist.sys.fan_mode";
    private static final String FAN_MODE_SYSFILE = "sys/class/fan/ctrl/mode";

    private static final String ALWAYS_OFF = "0";
    private static final String ALWAYS_ON  = "1";
    private static final String SMARTMODE  = "2";

    public static class InitFanReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DEBUG) Log.d(TAG,"BOOT UPDATE");
            String mode=SystemProperties.get(FAN_MODE_PROPERTY, ALWAYS_OFF);
            updateFanSettings(mode);
         }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fan_mode);

        mAlwaysOnBox = (CheckBoxPreference) findPreference(KEY_ALWAYS_ON);
        mAlwaysOnBox.setOnPreferenceChangeListener(this);

        mSmartModeBox = (CheckBoxPreference) findPreference(KEY_SMARTMODE);
        mSmartModeBox.setOnPreferenceChangeListener(this);

        mAlwaysOffBox = (CheckBoxPreference) findPreference(KEY_ALWAYS_OFF);
        mAlwaysOffBox.setOnPreferenceChangeListener(this);
        String mode =SystemProperties.get(FAN_MODE_PROPERTY, ALWAYS_OFF);
        updateFanDisplay(mode);
    }

    private void updateFanDisplay(String mode){
        if(DEBUG) Log.d(TAG,"updateFanDisplay mode="+mode);
        if(mode.equals(ALWAYS_OFF)){
            mAlwaysOffBox.setChecked(true);
            mAlwaysOnBox.setChecked(false);
            mSmartModeBox.setChecked(false);
        }else if(mode.equals(ALWAYS_ON)){
            mAlwaysOffBox.setChecked(false);
            mAlwaysOnBox.setChecked(true);
            mSmartModeBox.setChecked(false);
        }else {
            mAlwaysOnBox.setChecked(false);
            mAlwaysOffBox.setChecked(false);
            mSmartModeBox.setChecked(true);
        }
    }

    private static  void updateFanSettings(String mode){
        if(DEBUG) Log.d(TAG,"updateFanSettings mode="+mode);
        File file = new File(FAN_MODE_SYSFILE);
        if((file==null) || !file.exists()){
            return ;
        }

        try {
            FileOutputStream fout = new FileOutputStream(file);
            PrintWriter pWriter = new PrintWriter(fout);
                pWriter.println(mode);
                pWriter.flush();
                pWriter.close();
                fout.close();
                SystemProperties.set(FAN_MODE_PROPERTY,mode);
            } catch (IOException e) {
                Log.d("FAN", ": error = " + e);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onDestroy() {
        // TODO Auto-generated method stub
            super.onDestroy();
        }

        public boolean onPreferenceChange(Preference preference, Object objValue) {
            String key=preference.getKey();
            if(DEBUG) Log.d(TAG,"change key= "+key);
                FanModeTask mFanModeTask=new FanModeTask();
            if(KEY_ALWAYS_OFF.equals(key)){
                mFanModeTask.execute(ALWAYS_OFF);
            }
            if(KEY_ALWAYS_ON.equals(key)){
                mFanModeTask.execute(ALWAYS_ON);
            }
            if(KEY_SMARTMODE.equals(key)){
                mFanModeTask.execute(SMARTMODE);
            }

            return true ;
        }

        private class FanModeTask extends AsyncTask<String, Void, String>{
            @Override
            protected String doInBackground(String... params) {
            // TODO Auto-generated method stub

            updateFanSettings(params[0]);
                return params[0];
            }

            @Override
            protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            if(isCancelled()) return;
                updateFanDisplay(result);
            }
        }
}
