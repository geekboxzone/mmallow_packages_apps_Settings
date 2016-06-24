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
import android.os.Bundle;
import android.os.SystemProperties;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;


public class LedSettings extends SettingsPreferenceFragment {

    private static final String TAG = "LedSettings";
    private static final boolean DEBUG=true;
    private static final String RED_LED = "red";
    private static final String BLUE_LED = "blue";
    private static final int RED_HINT= 0;
    private static final int BLUE_HINT=1;
    private static final int LED_NUM = 2 ;
    private static final String RED_TRIGGER_PROPERTY= "persist.sys.red.trigger";
	private static final String BLUE_TRIGGER_PROPERTY= "persist.sys.blue.trigger";
    private static final String RED_TRIGGER_SYSFILE= "sys/class/leds/red/trigger";
	private static final String BLUE_TRIGGER_SYSFILE= "sys/class/leds/blue/trigger";

    private static final String DEFAULT_RED_TRIGGER="off";
	private static final String DEFAULT_BLUE_TRIGGER="default-on";

    private Context mContext;
    private LedInfoAdapter mLedInfoAdapter;
    private TriggerListViewAdapter mTriggerListViewAdapter;
    private Dialog mLedTriggerDialog;
    private ListView mLedTriggerList;
    private List<String> mTriggerSupportList = new ArrayList<String>();
	private List<String> mTriggerSupportListDes = new ArrayList<String>();

    String[] userList=new String[]{"temp","heartbeat","default-on","off"};
    String[] userListTitle=new String[]{"High Temprerature","Heartbeat","Always On","Always Off"}; 
	HashMap<String,String> mHashMap=new HashMap<String,String>();

    public class LedInfo {
        public String ledColor;
	public String triggerStr;

	public LedInfo(String ledColor, String triggerStr) {
	  this.ledColor = ledColor;
	  this.triggerStr = triggerStr;
	}

    }
    public class TriggerInfo {
		public String ledColor;
        public String triggerStr;
		public String describeStr;
        public boolean isChecked;
        public TriggerInfo(String ledColor,String triggerStr,String describeStr, boolean isChecked) {
          this.ledColor = ledColor;
		  this.triggerStr = triggerStr;
          this.isChecked = isChecked;
		  this.describeStr=describeStr;
        }

    }

   public static class InitLedReceiver extends BroadcastReceiver{
	   @Override
	  public void onReceive(Context context, Intent intent) {
      if(DEBUG) Log.d(TAG,"BOOT UPDATE");
	      BootUpdateLedSettings();
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
		InitCurrentSupportList(getSupportList());
        mLedInfoAdapter = new LedInfoAdapter(mContext);
        listView.setAdapter(mLedInfoAdapter);
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

    private class LedInfoAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private List<LedInfo> mLedInfoList;
        public LedInfoAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            updateLedInfoList(-1,false);
        }
        @Override
        public int getCount() {
                // TODO Auto-generated method stub
                return mLedInfoList.size();
        }

        @Override
        public Object getItem(int position) {
                // TODO Auto-generated method stub
                return mLedInfoList.get(position);
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
	      LedInfo ledInfo=(LedInfo) getItem(position);
	      view = mInflater.inflate(R.layout.led_info_row, null);
          holder = new ViewHolder(view);
          view.setTag(ledInfo);
          LedInfo info = (LedInfo) getItem(position);
	      holder.ledImage.setImageResource(info.ledColor.equals(RED_LED)? R.drawable.led_red_img:R.drawable.led_blue_img);
	      holder.triggerText.setText(info.triggerStr);
	      return view;

        }

       class ViewHolder {
	      ImageView ledImage;
          TextView  triggerText;  
	      public ViewHolder(View view) {
		       this.ledImage = (ImageView) view.findViewById(R.id.ledImage);
		       this.triggerText = (TextView) view.findViewById(R.id.triggerText);
               final View header = view.findViewById(R.id.header);
		       final View mView=view;
		       header.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    LedInfo info=(LedInfo)mView.getTag();
			        showLedTriggerList(info);
                }});

          }
        }

       public void updateLedInfoList(int position, boolean force){

	      if(mLedInfoList!=null){
                mLedInfoList.clear();
                mLedInfoList=null;
           }
          mLedInfoList=new ArrayList<LedInfo>();
          String[] list=mContext.getResources().getStringArray(R.array.led_list);
		  String triggerStr=null;
	      for(int i=0;i<list.length;i++){
              LedInfo info=null;
			  if(list[i].equals(RED_LED)){
			   triggerStr=SystemProperties.get(RED_TRIGGER_PROPERTY,DEFAULT_RED_TRIGGER);
		      }else{
               triggerStr =SystemProperties.get(BLUE_TRIGGER_PROPERTY,DEFAULT_BLUE_TRIGGER);
		      }
			  if(DEBUG)Log.d(TAG,"updateLedInfoList triggerStr "+triggerStr);
              info=new LedInfo(list[i],mHashMap.get(triggerStr));
              mLedInfoList.add(info);
          }

          if(force) notifyDataSetChanged();

           }

      }

        private void showLedTriggerList(LedInfo info) {
                mLedTriggerDialog = new Dialog(mContext,R.style.LedDialogTheme);
                mLedTriggerDialog.show();
                final Window window = mLedTriggerDialog.getWindow();
                window.setContentView(R.layout.led_trigger_list);
                mLedTriggerList=(ListView) window.findViewById(R.id.listview);
                TextView titleView=(TextView) window.findViewById(R.id.titleview);
		        String[] titles=mContext.getResources().getStringArray(R.array.led_title);
                titleView.setText(info.ledColor.equals(RED_LED)?titles[RED_HINT]:titles[BLUE_HINT]);
                mTriggerListViewAdapter = new TriggerListViewAdapter(mContext,info);
                mLedTriggerList.setAdapter(mTriggerListViewAdapter);
                mLedTriggerList.setOnItemClickListener(new ItemClickListener());
        }
        public class ItemClickListener implements OnItemClickListener {
                public void onItemClick(AdapterView<?> arg0, View view, int position,  long arg3) {

					Log.d(TAG,"trigger item click");
					updateLedTriggerSettings(mTriggerListViewAdapter.mLedTriggerInfoList.get(position));
					mLedTriggerDialog.dismiss();
					mLedInfoAdapter.updateLedInfoList(-1,true);
                }
        }



    private class TriggerListViewAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        public List<TriggerInfo> mLedTriggerInfoList;
	    public LedInfo mLedInfo;
        public TriggerListViewAdapter(Context context, LedInfo info) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        mLedInfo=info;
			String temp=null;
			if(mLedInfo.ledColor.equals(RED_LED)){
			temp =SystemProperties.get(RED_TRIGGER_PROPERTY,DEFAULT_RED_TRIGGER);
			}else{
			temp =SystemProperties.get(BLUE_TRIGGER_PROPERTY,DEFAULT_BLUE_TRIGGER);
			}
            updateLedTriggerInfoList(temp,false);
        }
        @Override
        public int getCount() {
                // TODO Auto-generated method stub
                return mLedTriggerInfoList.size();
        }

        @Override
        public Object getItem(int position) {
                // TODO Auto-generated method stub
                return mLedTriggerInfoList.get(position);
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
           TriggerInfo triggerInfo=(TriggerInfo) getItem(position);
           view = mInflater.inflate(R.layout.led_trigger_list_info, null);
           holder = new ViewHolder(view);
           view.setTag(triggerInfo);
           holder.radioImage.setImageResource(triggerInfo.isChecked? R.drawable.fan_temp_radio_on:R.drawable.fan_temp_radio_off);
           holder.triggerText.setText(triggerInfo.describeStr);
	       return view;

       }

       class ViewHolder {
          ImageView radioImage;
          TextView  triggerText;

          public ViewHolder(View view) {
                this.radioImage = (ImageView) view.findViewById(R.id.radioImage);
                this.triggerText = (TextView) view.findViewById(R.id.triggerText);
                final View header = view.findViewById(R.id.header);
                final View mView=view;
                header.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    TriggerInfo info=(TriggerInfo)mView.getTag();
                    Log.d(TAG,"click ");
			        updateLedTriggerSettings(info);
	                mLedTriggerDialog.dismiss();
		            mLedInfoAdapter.updateLedInfoList(-1,true);

                }});

          }
        }

       public void updateLedTriggerInfoList(String triggerStr, boolean force){
        Log.d(TAG," updateLedTriggerInfoList triggerStr= "+triggerStr);
          if(mLedTriggerInfoList!=null){
                mLedTriggerInfoList.clear();
                mLedTriggerInfoList=null;
           }
          mLedTriggerInfoList=new ArrayList<TriggerInfo>();
          for(int i=0;i<mTriggerSupportList.size();i++){
              TriggerInfo info=null;
              info=new TriggerInfo(mLedInfo.ledColor,mTriggerSupportList.get(i),mTriggerSupportListDes.get(i),triggerStr.equals(mTriggerSupportList.get(i))?true:false);
              mLedTriggerInfoList.add(info);
			  Log.d(TAG," updateLedTriggerInfoList triggerStr info  "+info.isChecked);
          }

          if(force) notifyDataSetChanged();

      }

   }

   public void InitCurrentSupportList(List<String> list){

    for(int i=0;i<userList.length;i++){
	   for(int j=0;j<list.size();j++){
	      if(userList[i].equals(list.get(j))){

                mHashMap.put(userList[i],userListTitle[i]);
				mTriggerSupportList.add(userList[i]);
				mTriggerSupportListDes.add(userListTitle[i]);
            }
	  }
    }


   }
   public List getSupportList() {

       Pattern p = Pattern.compile("[\\[\\]]+");
       ArrayList<String> list = new ArrayList<String>();
       try {
            FileReader fread = new FileReader("/sys/class/leds/red/trigger");
            BufferedReader buffer = new BufferedReader(fread);
            String str = null;

            while ((str = buffer.readLine()) != null) {
		String[] result = p.split(str);
                StringBuffer sb = new StringBuffer();
		for(int j = 0; j < result.length; j++){
                      sb. append(result[j]);
                   }

		String[] s = sb.toString().trim().split("\\ ");
		  for (int i = 0; i<s.length; i++){
	          if(DEBUG)Log.d(TAG,"list "+s[i]);
			  list.add(s[i]);
		   }  
         }

            fread.close();
            buffer.close();
          } catch (IOException e) {
               Log.e(TAG, "IO Exception");
          }
     return list;
   }

  private static void BootUpdateLedSettings(){
     String triggerRed=SystemProperties.get(RED_TRIGGER_PROPERTY, DEFAULT_RED_TRIGGER);
	 String triggerBlue=SystemProperties.get(BLUE_TRIGGER_PROPERTY, DEFAULT_BLUE_TRIGGER);
	 File fileRed = new File(RED_TRIGGER_SYSFILE);
     File fileBlue= new File(BLUE_TRIGGER_SYSFILE);;
     if((fileRed==null) || !fileRed.exists()){
		return;
	 }
	for(int i=0;i<2;i++)
    try{

       FileOutputStream fout = new FileOutputStream(i==0?fileRed:fileBlue);
	   PrintWriter pWriter = new PrintWriter(fout);
	   pWriter.println(i==0?triggerRed:triggerBlue);
	   pWriter.flush();
	   pWriter.close();
       fout.close();
       SystemProperties.set(i==0?RED_TRIGGER_PROPERTY:BLUE_TRIGGER_PROPERTY,i==0?triggerRed:triggerBlue);
	    } catch (IOException e) {
	     Log.d("Led", ": error = " + e);
		}

  }

  private void updateLedTriggerSettings(TriggerInfo info){

      if(DEBUG) Log.d(TAG,"updateLedTriggerSettings ledColor="+info.ledColor+" triggerStr= "+info.triggerStr);
	  File file=null;
	   if(info.ledColor.equals(RED_LED)){
          file = new File(RED_TRIGGER_SYSFILE);
	   }else{
	     file = new File(BLUE_TRIGGER_SYSFILE);
	   }
      if((file==null) || !file.exists()){
              return ;
	  }

	try {
         FileOutputStream fout = new FileOutputStream(file);
         PrintWriter pWriter = new PrintWriter(fout);
		 pWriter.println(info.triggerStr);
	     pWriter.flush();
		 pWriter.close();
		 fout.close();
		 if(info.ledColor.equals(RED_LED)){
			SystemProperties.set(RED_TRIGGER_PROPERTY,info.triggerStr);
		  }else{
		    SystemProperties.set(BLUE_TRIGGER_PROPERTY,info.triggerStr);
		  }
	     } catch (IOException e) {
	      Log.d("Led", ": error = " + e);
       }
      mTriggerListViewAdapter.updateLedTriggerInfoList(info.triggerStr, true);
    }
 }
