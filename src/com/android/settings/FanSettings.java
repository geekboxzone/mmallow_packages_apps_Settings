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
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.app.Activity;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.widget.GridView;
import android.view.Window;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.util.Log;
import java.util.List;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FanSettings extends SettingsPreferenceFragment {
    private static final String TAG = "FanSettings";
    private static final boolean DEBUG=false;
    private static final String FAN_MODE_PROPERTY = "persist.sys.fan_mode";
    private static final String FAN_MODE_SYSFILE = "sys/class/fan/ctrl/mode";
    private static final String FAN_TEMP_SYSFILE = "sys/class/fan/ctrl/temp";
    private static final String FAN_TEMP_PROPERTY = "persist.sys.fan_temp";

    private static final String ALWAYS_OFF = "0";
    private static final String ALWAYS_ON  = "1";
    private static final String SMARTMODE  = "2";
    private static final String DEFAULT_TEMP = "60";

    private Context mContext;
    private FanInfoAdapter mAdapter;
    private Dialog mTempDialog;
    private GridView gridView;
    private GridViewInfoAdapter gridViewAdapter;

    public class ModeInfo{
	public String titleStr;
        public int  temp;
	public boolean  isChecked;
	public ModeInfo(String title,boolean isChecked){
		titleStr = title;
		this.isChecked=isChecked;
        }
    }
    public class TempInfo{
        public int temp;
	public boolean isChecked;
	public TempInfo(int temp,boolean isChecked){
		this.temp=temp;
		this.isChecked=isChecked;
	}

    }

    public static class InitFanReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DEBUG) Log.d(TAG,"BOOT UPDATE");
            String temp=SystemProperties.get(FAN_TEMP_PROPERTY, DEFAULT_TEMP);
            updateFanTempSettings(temp);
	    String mode=SystemProperties.get(FAN_MODE_PROPERTY, ALWAYS_OFF);
            updateFanModeSettings(mode);
         }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
	ListView listView = getListView();
        listView.setItemsCanFocus(true);
        mAdapter = new FanInfoAdapter(mContext);
        listView.setAdapter(mAdapter);
    }

    private static  void updateFanModeSettings(String mode){
        if(DEBUG) Log.d(TAG,"updateFanModeSettings mode="+mode);
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


     private static  void updateFanTempSettings(String temp){
        if(DEBUG) Log.d(TAG,"updateFanTempSettings temp="+temp);
        File file = new File(FAN_TEMP_SYSFILE);
        if((file==null) || !file.exists()){
            return ;
        }

        try {
            FileOutputStream fout = new FileOutputStream(file);
            PrintWriter pWriter = new PrintWriter(fout);
                pWriter.println(temp);
                pWriter.flush();
                pWriter.close();
                fout.close();
                SystemProperties.set(FAN_TEMP_PROPERTY,temp);
            } catch (IOException e) {
                Log.d("FAN", ": error = " + e);
            }
         String mode=SystemProperties.get(FAN_MODE_PROPERTY, ALWAYS_OFF);
	 if(mode.equals(SMARTMODE)){
		updateFanModeSettings(mode);
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
            super.onDestroy();
        }

        private class FanModeTask extends AsyncTask<String, Void, String>{
            @Override
            protected String doInBackground(String... params) {
            updateFanModeSettings(params[0]);
                return params[0];
            }
            @Override
            protected void onPostExecute(String result) {
            if(isCancelled()) return;
                mAdapter.updateFanModeList(null,Integer.parseInt(result),true);
            }
      }


        private class FanTempTask extends AsyncTask<String, Void, String>{
            @Override
            protected String doInBackground(String... params) {
             updateFanTempSettings(params[0]);
                return params[0];
            }

            @Override
            protected void onPostExecute(String result) {
            if(isCancelled()) return;
                gridViewAdapter.updateTempList(Integer.parseInt(result),true);
            }
      }

    private class FanInfoAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private List<ModeInfo> mFanModeList;
        public FanInfoAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String mode =SystemProperties.get(FAN_MODE_PROPERTY, ALWAYS_OFF);
            updateFanModeList(mode,-1,false);
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mFanModeList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mFanModeList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ModeInfo modeInfo =(ModeInfo) getItem(position);
	    final int mPosition=position;
            final View row = convertView != null ? convertView : createFanInfoRow(parent);
            row.setTag(modeInfo);
            ((TextView) row.findViewById(android.R.id.title)).setText(mFanModeList.get(position).titleStr);
            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(mFanModeList.get(position).isChecked);
            radioButton.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    FanModeTask mFanModeTask=new FanModeTask();
                    mFanModeTask.execute(String.valueOf(mPosition));
		    return false;
                }});
            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
            View settingsDivider = row.findViewById(R.id.divider);
            if(position==2){
		settingsButton.setVisibility(View.VISIBLE);
                settingsDivider.setVisibility(View.VISIBLE);
		if(mFanModeList.get(position).isChecked){
		 settingsButton.setAlpha(1f);
		 settingsButton.setEnabled(true);
		 settingsButton.setFocusable(true);
		}else{
                 settingsButton.setAlpha(Utils.DISABLED_ALPHA);
                 settingsButton.setEnabled(false);
                 settingsButton.setFocusable(false);
		}
	    }else{
	     settingsButton.setVisibility(View.INVISIBLE);
	     settingsDivider.setVisibility(View.INVISIBLE);
            }
            settingsButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                   showTempDialog();
                }});
            return row;
        }

         public void updateFanModeList(String mode,int position,boolean force){
          if(mFanModeList!=null){
           mFanModeList.clear();
           mFanModeList=null;
          }
          String[] modelist=mContext.getResources().getStringArray(R.array.fan_modelist);
		  String[] values=mContext.getResources().getStringArray(R.array.fan_modelist_values);
          mFanModeList=new ArrayList<ModeInfo>();
          for(int i=0;i<modelist.length;i++){
	      ModeInfo info=null;
              if(mode==null){
                 info=new ModeInfo(modelist[i],position==i?true:false);
              }else{
                 info=new ModeInfo(modelist[i],mode.equals(values[i])?true:false);
              }
              mFanModeList.add(info);
          }
          if(force) notifyDataSetChanged();
        }
        private View createFanInfoRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.fan_info_row, parent, false);
            final View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
		    ModeInfo mode=(ModeInfo)row.getTag();
		    int position=0;
		    String[] modeList=mContext.getResources().getStringArray(R.array.fan_modelist);
		    for(int i=0;i<modeList.length;i++){
			if(mode.titleStr.equals(modeList[i])){
			   position=i;
			   break;
			 }

		    }
		    FanModeTask mFanModeTask=new FanModeTask();
		    mFanModeTask.execute(String.valueOf(position));
                }});
            return row;
        }
    }

        private void showTempDialog()
	{
		mTempDialog = new Dialog(mContext,R.style.FanDialogTheme);
		mTempDialog.show();
		final Window window = mTempDialog.getWindow();
		window.setContentView(R.layout.fan_temp_settings);
		gridView=(GridView) window.findViewById(R.id.gridview);
                gridViewAdapter = new GridViewInfoAdapter(
							mContext);
	        gridView.setAdapter(gridViewAdapter);
                gridView.setOnItemClickListener(new ItemClickListener());
	}

        public class ItemClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View view, int position,				long arg3) {
	            FanTempTask mFanTempTask=new FanTempTask();
                    mFanTempTask.execute(String.valueOf(gridViewAdapter.mTempListInfo.get(position).temp));
		    mTempDialog.dismiss();
		}
        }

       public class GridViewInfoAdapter extends BaseAdapter {
             public List<TempInfo> mTempListInfo = null;
             LayoutInflater infater = null;
             public GridViewInfoAdapter(Context context) {
                 infater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	         String temp =SystemProperties.get(FAN_TEMP_PROPERTY,DEFAULT_TEMP);
	         updateTempList(Integer.parseInt(temp),false);
            }
            @Override
            public int getCount() {
            // TODO Auto-generated method stub
                return mTempListInfo.size();
            }
            @Override
            public Object getItem(int position) {
            // TODO Auto-generated method stub
                return mTempListInfo.get(position);
            }
            @Override
            public long getItemId(int position) {
            // TODO Auto-generated method stub
               return position;
            }
            @Override
            public View getView(int position, View convertview, ViewGroup arg2){
               View view = null;
               ViewHolder holder = null;
	       final int  mPosition=position;
               if (convertview == null || convertview.getTag() == null) {
                  view = infater.inflate(R.layout.fan_temp_gridview, null);
                  holder = new ViewHolder(view);
                  view.setTag(holder);
               } else {
                  view = convertview;
                  holder = (ViewHolder) convertview.getTag();
               }
                  TempInfo info = (TempInfo) getItem(position);
                  holder.tempText.setText(""+info.temp);
	          holder.radioImage.setImageResource(info.isChecked? R.drawable.fan_temp_radio_on:R.drawable.fan_temp_radio_off);
	          return view;

              }

	   class ViewHolder {
              TextView tempText;
	      ImageView radioImage;
              public ViewHolder(View view){
                  this.tempText= (TextView) view.findViewById(android.R.id.title);
                  this.radioImage=(ImageView)view.findViewById(android.R.id.button1);
	     }
          }

          public void updateTempList(int temp, boolean force){
             if(mTempListInfo!=null){
                  mTempListInfo.clear();
                  mTempListInfo=null;
              }
              mTempListInfo=new ArrayList<TempInfo>();
              int[] tempList=mContext.getResources().getIntArray(R.array.fan_templist);
              for(int i=0;i<tempList.length;i++){
                 TempInfo info=new TempInfo(tempList[i],temp==tempList[i]?true:false);
                 mTempListInfo.add(info);
               }
	     if(force) notifyDataSetChanged();
        }
    }
}
