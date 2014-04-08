package com.shyamu.translocwidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shyamu.translocwidget.TransLocJSON.TransLocAgencies;
import com.shyamu.translocwidget.TransLocJSON.TransLocAgency;
import com.shyamu.translocwidget.TransLocJSON.TransLocArrival;
import com.shyamu.translocwidget.TransLocJSON.TransLocArrivalEstimate;
import com.shyamu.translocwidget.TransLocJSON.TransLocArrivalEstimates;
import com.shyamu.translocwidget.TransLocJSON.TransLocRoute;
import com.shyamu.translocwidget.TransLocJSON.TransLocStop;
import com.shyamu.translocwidget.TransLocJSON.TransLocStops;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.widget.AdapterView.OnItemSelectedListener;

public class WidgetConfigurationActivity extends Activity {

    private static final String AGENCIES_URL = "http://api.transloc.com/1.1/agencies.json";
    private static final String ROUTES_URL = "http://api.transloc.com/1.1/routes.json?agencies=";
    private static final String STOPS_URL = "http://api.transloc.com/1.1/stops.json?agencies=";
    private static final String ARRIVALS_URL = "http://api.transloc.com/1.1/arrival-estimates.json?agencies=";

    private static final String TAG = "ConfigActivity";

    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    private ArrayList<TransLocStop> fullStopList = new ArrayList<TransLocStop>();

    private int currentAgencyId;
    private int currentRouteId;
    private int currentStopId;

    private String routeShortName;
    private String routeLongName;
    private String stopName;

    private int mAppWidgetId = 0;
    ProgressDialog dialog = null;

    RelativeLayout rlPreview;
    Spinner sSelectAgency, sSelectRoute, sSelectStop;
    Button bReset, bMakeWidget;
    TextView tvHelpMessage;
    TextView tvRoutePreview, tvRemainingTimePreview, tvStopPreview, tvMinsPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "in onCreate");
        setContentView(R.layout.activity_configuration);

        // references to Spinners and Buttons
        sSelectAgency = (Spinner) findViewById(R.id.sSelectAgency);
        sSelectRoute = (Spinner) findViewById(R.id.sSelectRoute);
        sSelectStop = (Spinner) findViewById(R.id.sSelectStop);
        bReset = (Button) findViewById(R.id.bReset);
        bMakeWidget = (Button) findViewById(R.id.bMakeWidget);
        tvHelpMessage = (TextView) findViewById(R.id.tvHelp);

        // preview elements
        rlPreview = (RelativeLayout) findViewById(R.id.rlPreview);
        tvMinsPreview = (TextView) findViewById(R.id.tvMins_preview);
        tvRemainingTimePreview = (TextView) findViewById(R.id.tvRemainingTime_preview);
        tvRoutePreview = (TextView) findViewById(R.id.tvRoute_preview);
        tvStopPreview = (TextView) findViewById(R.id.tvStop_preview);

        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        editor = settings.edit();

        // Set result initially as cancelled in case user cancels configuration
        setResult(RESULT_CANCELED);

        // show warning dialog if weekend or outside business hours
        DateTime currentTime = new DateTime(System.currentTimeMillis());
        // day of of week 6 and 7 = Saturday and Sunday
        Log.v(TAG,"day= " + currentTime.getDayOfWeek());
        Log.v(TAG,"hour= " + currentTime.getHourOfDay());
        if(currentTime.getDayOfWeek() > 5 || currentTime.getHourOfDay() > 18 || currentTime.getHourOfDay() < 6) {
            Log.v(TAG,"true");
            // show warning dialog
            Utils.showAlertDialog(WidgetConfigurationActivity.this,"Warning", "Based on the current time and day of week, many routes may not be running at this time. You can continue to try and make a widget but be advised you may get better results during normal business hours.");
        }

        // Populate agency spinner
        new PopulateAgenciesTask().execute();

        // Defining a click event listener for the button "Reset"
        OnClickListener setResetClickedListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doReset();
            }
        };

        // Defining a click event listener for the button "Make Widget"
        OnClickListener setMakeWidgetClickedListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doMakeWidget();
            }
        };

        // On click listener for help text
        OnClickListener setHelpClickedListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Why can't I find my agency?", getString(R.string.help_dialog));
            }
        };

        // On click listener for preview layout
        OnClickListener setPreviewClickedListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch customization activity
                Intent intent = new Intent(WidgetConfigurationActivity.this, CustomizeWidgetActivity.class);
                intent.putExtra("widgetId",mAppWidgetId);
                startActivity(intent);
            }
        };

        // Get widgetId from appwidgetmanager
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {

            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the click listeners on the buttons
        bReset.setOnClickListener(setResetClickedListener);
        bMakeWidget.setOnClickListener(setMakeWidgetClickedListener);
        tvHelpMessage.setOnClickListener(setHelpClickedListener);
        rlPreview.setOnClickListener(setPreviewClickedListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // prevents force close when device rotates or app is paused while inside asynctask
        if(dialog != null) dialog.dismiss();
        dialog = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        int textColor = settings.getInt("textColor", -1);
        int backgroundColor = settings.getInt("backgroundColor",1996554497);

        editor.putInt("textColor-" + mAppWidgetId, textColor).commit();
        editor.putInt("backgroundColor-" + mAppWidgetId, backgroundColor).commit();

        Log.v(TAG,Integer.toString(settings.getInt("textColor-" + mAppWidgetId, -2)));
        Log.v(TAG,Integer.toString(settings.getInt("backgroundColor-" + mAppWidgetId, -2)));

        // change colors in preview
        rlPreview.setBackgroundColor(backgroundColor);
        tvStopPreview.setTextColor(textColor);
        tvRoutePreview.setTextColor(textColor);
        tvRemainingTimePreview.setTextColor(textColor);
        tvMinsPreview.setTextColor(textColor);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                // start about activity
                Log.v(TAG,"menu option selected");
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return true;
    }

    private void doReset() {
        // start over
        new PopulateAgenciesTask().execute();
    }

    private void doMakeWidget() {
        // Get arrival time and make the widget
        new PopulateArrivalTask().execute();
    }

    private void doErrorMiscHandling() {
        bMakeWidget.setEnabled(false);
        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private class PopulateAgenciesTask extends AsyncTask<Void, Void, TransLocAgencies> {


        @Override
        protected void onPreExecute() {
            // show dialog
            if(dialog == null) {
                dialog = ProgressDialog.show(WidgetConfigurationActivity.this, "Loading", "Please Wait...");
            }
        }

        protected TransLocAgencies doInBackground(Void... voids) {
            try {
                return new ObjectMapper().readValue(Utils.getJsonResponse(AGENCIES_URL), TransLocAgencies.class);
            } catch (Exception e) {
                Log.e(TAG, "ERROR in getting JSON data for agencies");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final TransLocAgencies agencyList) {
            if (agencyList == null) {
                Log.e(TAG, "error in getting list of agencies");
                doErrorMiscHandling();
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Data", getString(R.string.error_no_data));
                bMakeWidget.setEnabled(false);
            }
            else {
                sSelectAgency.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                        currentAgencyId = agencyList.getData().get(pos).agencyId;
                        new PopulateRoutesTask().execute();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });

                // sort agency list first
                ArrayList<TransLocAgency> sortedList = (ArrayList<TransLocAgency>) agencyList.getData();

                // remove unwanted agencies (those that don't have arrival times)
                // 72 = NYU, 104 = CTA
                for(int i = 0; i < sortedList.size(); i++) {
                    int agencyId = sortedList.get(i).agencyId;
                    if(agencyId == 72 || agencyId == 104) {
                        sortedList.remove(i);
                    }
                }

                Collections.sort(sortedList, sortTransLocAgency());
                ArrayAdapter<TransLocAgency> agencyArrayAdapter = new ArrayAdapter<TransLocAgency>(getBaseContext(), android.R.layout.simple_list_item_1, agencyList.getData());

                sSelectAgency.setAdapter(agencyArrayAdapter);
            }


        }

    }

    // comparator for sorting agencies
    private Comparator<TransLocAgency> sortTransLocAgency() {
        return new Comparator<TransLocAgency>() {
            @Override
            public int compare(TransLocAgency transLocAgency, TransLocAgency transLocAgency2) {
                return transLocAgency.longName.compareTo(transLocAgency2.longName);
            }
        };
    }


    private Comparator<TransLocRoute> sortTransLocRoute() {
        return new Comparator<TransLocRoute>() {
            @Override
            public int compare(TransLocRoute transLocRoute, TransLocRoute transLocRoute2) {
                if(Character.isDigit(transLocRoute.shortName.charAt(0)) || Character.isDigit(transLocRoute2.shortName.charAt(0))) {
                    if(Character.isLetter(transLocRoute.shortName.charAt(0)) || Character.isLetter(transLocRoute.shortName.charAt(transLocRoute.shortName.length()-1))) return -1;
                    else if(Character.isLetter(transLocRoute2.shortName.charAt(0)) || Character.isLetter(transLocRoute2.shortName.charAt(transLocRoute2.shortName.length()-1))) return 1;
                    try {
                        int route1 = Integer.valueOf(transLocRoute.shortName);
                        int route2 = Integer.valueOf(transLocRoute2.shortName);
                        if(route1 < route2) return -1;
                        else if(route1 > route2) return 1;
                        else return 0;
                    } catch (NumberFormatException e) {
                        return transLocRoute.shortName.compareTo(transLocRoute2.shortName);
                    }

                } else {
                    return transLocRoute.longName.compareTo(transLocRoute2.longName);
                }
            }
        };
    }




    private class PopulateRoutesTask extends AsyncTask<Void, Void, ArrayList<TransLocRoute>> {

        @Override
        protected void onPreExecute() {
            if(dialog == null) {
                dialog = ProgressDialog.show(WidgetConfigurationActivity.this, "Loading", "Please Wait...");
            }
        }
        @SuppressWarnings("unchecked")
        protected ArrayList<TransLocRoute> doInBackground(Void... voids) {
            try {
                Map<String, Object> routeMap = new ObjectMapper().readValue(Utils.getJsonResponse(ROUTES_URL + currentAgencyId), Map.class);
                Map<String, Object> agencyMap = (Map) routeMap.get("data");
                List<Map<String, Object>> routeList = (List) agencyMap.get(Integer.toString(currentAgencyId));
                final ArrayList<TransLocRoute> routesArrayList = new ArrayList<TransLocRoute>();

                if (routeList == null) {
                    // returns empty list
                    return routesArrayList;
                } else {

                    for (Map<String, Object> route : routeList) {
                        routesArrayList.add(new TransLocRoute(Integer.parseInt((String) route.get("route_id")), (String) route.get("short_name"), (String) route.get("long_name")));
                    }
                    return routesArrayList;
                }


            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }


        }

        @Override
        protected void onPostExecute(final ArrayList<TransLocRoute> routesArrayList) {
            ArrayList<String> arr = new ArrayList<String>();

            if(routesArrayList == null) {
                // no connection
                doErrorMiscHandling();
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Data", getString(R.string.error_no_data));
                // empty routes and stops spinner (set to empty array)
                sSelectRoute.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));
                sSelectStop.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));
            }
            else if (routesArrayList.isEmpty()) {
                Log.e(TAG, "error in getting list of routes - empty list");
                doErrorMiscHandling();
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Routes Available", "No routes are currently available for the agency you have selected. Please try again later when buses are running.");
                // empty routes and stops spinner (set to empty array)
                sSelectRoute.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));
                sSelectStop.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));

            } else {
                sSelectRoute.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                        currentRouteId = routesArrayList.get(pos).id;
                        routeLongName = routesArrayList.get(pos).longName;
                        routeShortName = routesArrayList.get(pos).shortName;
                        new PopulateStopsTask().execute();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });
                // sort only if agency is 116 (UF)
                if(currentAgencyId == 116) Collections.sort(routesArrayList, sortTransLocRoute());
                ArrayAdapter<TransLocRoute> routeArrayAdapter = new ArrayAdapter<TransLocRoute>(getBaseContext(), android.R.layout.simple_list_item_1, routesArrayList);
                sSelectRoute.setAdapter(routeArrayAdapter);

            }
        }

    }

    private class PopulateStopsTask extends AsyncTask<Void, Void, TransLocStops> {
        @Override
        protected void onPreExecute() {
            if(dialog == null) {
                dialog = ProgressDialog.show(WidgetConfigurationActivity.this, "Loading", "Please Wait...");
            }
        }

        protected TransLocStops doInBackground(Void... voids) {
            try {
                return new ObjectMapper().readValue(Utils.getJsonResponse(STOPS_URL + currentAgencyId), TransLocStops.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(TransLocStops stopList) {

            fullStopList.clear();
            if (stopList != null) {
                fullStopList.addAll(stopList.data);
            } else {
                Log.e(TAG, "error in getting stops list");
                doErrorMiscHandling();

                // no connection
                //Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Data", getString(R.string.error_no_data));
                //Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Stops Available", "No stops are currently available for the route you have selected. Please try again later when buses are running.");
            }

            new FilterStopListTask().execute(fullStopList);

        }
    }

    private class FilterStopListTask extends AsyncTask<ArrayList<TransLocStop>, String, ArrayList<TransLocStop>> {
        @Override
        protected ArrayList<TransLocStop> doInBackground(ArrayList<TransLocStop>... fullStopList) {
            if (fullStopList.length == 1) {
                if (fullStopList[0] == null) {
                    return null;
                }
                ArrayList<TransLocStop> currentRouteStopList = new ArrayList<TransLocStop>();
                for (int i = fullStopList[0].size() - 1; i >= 0; i--) {
                    if (fullStopList[0].get(i).routes.contains(currentRouteId)) {
                        currentRouteStopList.add(fullStopList[0].get(i));
                    }
                }
                return currentRouteStopList;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final ArrayList<TransLocStop> currentRouteStopList) {
            ArrayList<String> arr = new ArrayList<String>();
            if(currentRouteStopList == null) {
                doErrorMiscHandling();
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Data", getString(R.string.error_no_data));
                sSelectStop.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));

            }
            else if(currentRouteStopList.isEmpty()) {
                Log.e(TAG, "error in getting stops list");
                doErrorMiscHandling();
                // empty stops spinner
                sSelectStop.setAdapter(new ArrayAdapter<String>(WidgetConfigurationActivity.this, android.R.layout.simple_dropdown_item_1line, arr));
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Stops Available", "No stops are currently available for the route you have selected. Please try again later when buses are running.");
            } else {
                ArrayAdapter<TransLocStop> stopArrayAdapter = new ArrayAdapter<TransLocStop>(getBaseContext(), android.R.layout.simple_list_item_1, currentRouteStopList);
                sSelectStop.setAdapter(stopArrayAdapter);
                sSelectStop.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                        currentStopId = currentRouteStopList.get(pos).stopId;
                        stopName = currentRouteStopList.get(pos).name;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });
                bMakeWidget.setEnabled(true);

            }

            if(dialog != null) {
                dialog.dismiss();
                dialog = null;
            }

        }
    }


    private class PopulateArrivalTask extends AsyncTask<Void, Void, TransLocArrivalEstimates> {
        ProgressDialog makeWidgetDialog;

        private int minutes = -1;

        @Override
        protected void onPreExecute() {
            makeWidgetDialog = ProgressDialog.show(WidgetConfigurationActivity.this, "Making Widget", "Please Wait...");
        }

        protected TransLocArrivalEstimates doInBackground(Void... voids) {

            String url = ARRIVALS_URL + currentAgencyId + "&routes=" + currentRouteId + "&stops=" + currentStopId;
            // URL is stored as urlXX with XX being the appwidget ID
            editor.putString("url" + mAppWidgetId, url).commit();

            Log.v(TAG, "arrival estimates URL: " + url);

            try {
                return new ObjectMapper().readValue(Utils.getJsonResponse(url), TransLocArrivalEstimates.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(TransLocArrivalEstimates arrivalEstimatesList) {
            Date currentTimeUTC;
            Date arrivalTimeUTC;
            if(arrivalEstimatesList == null) {
                // no connection
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Data", getString(R.string.error_no_data));
            } else if (arrivalEstimatesList.data.isEmpty()) {
                Utils.showAlertDialog(WidgetConfigurationActivity.this, "Error - No Arrival Times", "No arrival times are currently available for the route and stop you have selected. Please try again later when buses are running.");
            } else {
                TransLocArrivalEstimate arrivalEstimate = arrivalEstimatesList.data.get(0);
                TransLocArrival arrival = arrivalEstimate.arrivals.get(0);
                currentTimeUTC = arrivalEstimatesList.generatedOn;
                arrivalTimeUTC = arrival.arrivalAt;
                Log.v(TAG, "current time: " + currentTimeUTC + " ... " + "arrival time: " + arrivalTimeUTC);
                minutes = Utils.getMinutesBetweenTimes(currentTimeUTC, arrivalTimeUTC);

                // Getting an instance of WidgetManager
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getBaseContext());

                // Instantiating the class RemoteViews with widget_layout
                RemoteViews views = new RemoteViews(getBaseContext().getPackageName(), R.layout.widget_layout);

                //Set the time remaining of the widget
                views.setTextViewText(R.id.tvRemainingTime, Integer.toString(minutes));
                if (minutes < 1) views.setTextViewText(R.id.tvRemainingTime, "<1");
                if (minutes < 2) views.setTextViewText(R.id.tvMins, "min away");
                else views.setTextViewText(R.id.tvMins, "mins away");

                // commit widget info to preferences and set text on remoteview
                // if short name is less than 5 characters, use short name + long name
                if (routeShortName.length() < 5 && routeShortName.length() > 0) {
                    String widgetRouteName = routeShortName + " - " + routeLongName;
                    editor.putString("routeName" + mAppWidgetId, widgetRouteName).commit();
                    views.setTextViewText(R.id.tvRoute, widgetRouteName);
                } else if(routeShortName.length() == 0) {
                    editor.putString("routeName" + mAppWidgetId, routeLongName).commit();
                    views.setTextViewText(R.id.tvRoute, routeLongName);
                } else {
                    editor.putString("routeName" + mAppWidgetId, routeShortName).commit();
                    views.setTextViewText(R.id.tvRoute, routeShortName);
                }

                editor.putString("stopName" + mAppWidgetId, stopName).commit();
                views.setTextViewText(R.id.tvStop, stopName);

                // set colors for widget
                int backgroundColor = settings.getInt("backgroundColor",1996554497);
                int textColor = settings.getInt("textColor",-1);
                views.setInt(R.id.rlWidgetLayout,"setBackgroundColor",backgroundColor);
                views.setTextColor(R.id.tvRemainingTime,textColor);
                views.setTextColor(R.id.tvMins,textColor);
                views.setTextColor(R.id.tvRoute,textColor);
                views.setTextColor(R.id.tvStop,textColor);

                // setup intent for tap on widget
                Intent clickIntent = new Intent(getBaseContext(), TransLocWidgetProvider.class);
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), mAppWidgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                // add pending intent to whole widget
                views.setOnClickPendingIntent(R.id.rlWidgetLayout, pendingIntent);

                // Tell the AppWidgetManager to perform an update on the app widget

                appWidgetManager.updateAppWidget(mAppWidgetId, views);
                Log.v(TAG, "mappwidgetid: " + mAppWidgetId);

                // Return RESULT_OK from this activity
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                editor.putBoolean("configComplete", true);
                Toast.makeText(getApplicationContext(), "Tap on the widget to update!", Toast.LENGTH_LONG ).show();
                finish();

            }
            makeWidgetDialog.dismiss();
        }
    }


}

