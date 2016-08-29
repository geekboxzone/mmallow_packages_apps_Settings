/*  
 *  Copyright(C), 2015-2016, Fuzhou Rockchip Co. ,Ltd.  All Rights Reserved.
 *
 *  File:   CABCReceiver.java
 *
 *  Author: dzy
 *
 */
package com.android.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;

public class CABCReceiver extends BroadcastReceiver{

	
	private final String BOOT_ACTION="android.intent.action.BOOT_COMPLETED";
	private final String CABC_CHANGE="android.intent.action.CABC_CHANGE";
	private final String TAG="CABC";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action=intent.getAction();
		int curBrightness=Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 200,
                UserHandle.USER_CURRENT);
        int lowBrightness=SystemProperties.getInt("ro.config.low_brightness",120);
		if(action.equals(BOOT_ACTION)){
			//SharedPreferences CABCPreference=context.getSharedPreferences("CABC", context.MODE_PRIVATE);
	        boolean isOpen=SystemProperties.get("persist.sys.cabc_status","0").equals("1");
	        Log.d(TAG,"isOpen="+isOpen);
	        setCABC(curBrightness<=lowBrightness?false:isOpen);
		}else if(action.equals(CABC_CHANGE)){
			boolean isOpen=intent.getBooleanExtra("cabc_change", false);
			Log.d(TAG,"CABC change isOpen="+isOpen);
			setCABC(curBrightness<=lowBrightness?false:isOpen);
		}
	}

	
	private void setCABC(boolean isOpen){
        File f = new File("/sys/class/graphics/fb0/cabc");
        OutputStream output = null;     
        OutputStreamWriter outputWrite = null;
        PrintWriter  print = null;
        String strbuf=isOpen?"1 5 257 255 192":"0 5 257 255 192";
        try{ 
                output = new FileOutputStream(f);
                outputWrite = new OutputStreamWriter(output);
                print = new PrintWriter(outputWrite);
                print.print(strbuf.toString());
                print.flush();
                output.close();
        } catch (Exception e) { 
                e.printStackTrace();
        }    
        
    }    
}
