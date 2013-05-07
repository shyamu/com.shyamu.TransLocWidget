package com.shyamu.translocwidget;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
 
public class WidgetConfigurationActivity extends Activity{
    private int mAppWidgetId = 0 ;
    
    Spinner sSelectAgency, sSelectRoute, sSelectStop;
    Button bReset, bMakeWidget;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_activity); 
 
        // Getting references to Spinners and Buttons
        sSelectAgency = (Spinner) findViewById(R.id.sSelectAgency);
        sSelectRoute = (Spinner) findViewById(R.id.sSelectRoute);
        sSelectStop = (Spinner) findViewById(R.id.sSelectStop);
        bReset = (Button) findViewById(R.id.bReset);
        bMakeWidget = (Button) findViewById(R.id.bMakeWidget);

        // Defining a click event listener for the button "Set Color"
        OnClickListener setColorClickedListener  = new OnClickListener() {
 
            @Override
            public void onClick(View v) {
               // colorPicker();
            }
        };
 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
 
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
 
        // Setting the click listener on the "Set Color" button
       // btnSetColor.setOnClickListener(setColorClickedListener);
    }
    
    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
          String response = "";
          for (String url : urls) {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            try {
              HttpResponse execute = client.execute(httpGet);
              InputStream content = execute.getEntity().getContent();

              BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
              String s = "";
              while ((s = buffer.readLine()) != null) {
                response += s;
              }

            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          return response;
        }

        @Override
        protected void onPostExecute(String result) {
          returnJsonString(result);
        }
        
        public String returnJsonString(String result) {
    		return result;
    	}
        
      }

	
    
    
 /*
    public void colorPicker() {
 
        //      initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
        //      for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, 0xff0000ff, new OnAmbilWarnaListener() {
 
            // Executes, when user click Cancel button
            @Override
            public void onCancel(AmbilWarnaDialog dialog){
            }
 
            // Executes, when user click OK button
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // Create an Intent to launch WidgetConfigurationActivity screen
                Intent intent = new Intent(getBaseContext(), WidgetConfigurationActivity.class);
 
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
 
                // This is needed to make this intent different from its previous intents
                intent.setData(Uri.parse("tel:/"+ (int)System.currentTimeMillis()));
 
                // Creating a pending intent, which will be invoked when the user
                // clicks on the widget
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0,
                                                intent, PendingIntent.FLAG_UPDATE_CURRENT);
 
                // Getting an instance of WidgetManager
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getBaseContext());
 
                // Instantiating the class RemoteViews with widget_layout
                RemoteViews views = new RemoteViews(getBaseContext().getPackageName(), R.layout.widget_layout);
 
                // Setting the background color of the widget
                views.setInt(R.id.widget_aclock, "setBackgroundColor", color);
 
                //  Attach an on-click listener to the clock
                views.setOnClickPendingIntent(R.id.widget_aclock, pendingIntent);
 
                // Tell the AppWidgetManager to perform an update on the app widget
                appWidgetManager.updateAppWidget(mAppWidgetId, views);
 
                // Return RESULT_OK from this activity
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        dialog.show();
        
    } */
}
