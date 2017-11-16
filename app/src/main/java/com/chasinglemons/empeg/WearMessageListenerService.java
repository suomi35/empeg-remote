package com.chasinglemons.empeg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by suomi35 on 8/1/2016.
 */
public class WearMessageListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener {
    private static final String START_ACTIVITY = "/start_activity";
    private static final String NONE = "none";
    private static final String HTTP_PREFIX = "http://";
    private static final String PREF_EMPEG_IP = "activeEmpegIP";

    public static String BUTTON_UP = "up";
    public static String BUTTON_LEFT = "left";
    public static String BUTTON_RIGHT = "right";
    public static String BUTTON_DOWN = "down";
    public static String BUTTON_UP_LONG = "up_long";
    public static String BUTTON_LEFT_LONG = "left_long";
    public static String BUTTON_RIGHT_LONG = "right_long";
    public static String BUTTON_DOWN_LONG = "down_long";

    public static GoogleApiClient mApiClient;

    @Override
    public void onCreate() {
        Log.d("WMLS", "onCreate()");
        super.onCreate();
        initGoogleApiClient();
    }

    @Override
    public void onDestroy() {
        Log.d("WMLS", "onDestroy()");
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (!mApiClient.isConnected()) {
            Log.d("WMLS", "mApiClient was NOT connected");
            initGoogleApiClient();
        }

        Log.d("WMLS", "messageEvent.getPath(): " + messageEvent.getPath());

        SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String playerIP = config.getString(PREF_EMPEG_IP, NONE);
        Log.d("WMLS", "playerIP: " + playerIP);

        if (!playerIP.equals(NONE)) {
            if (mApiClient.isConnected()) {
                String message = new String(messageEvent.getData());
                Log.d("WMLS", "message: " + message);
                if (message.equals("get_vfd")) {
                    new getVfd().execute(HTTP_PREFIX + playerIP + "/proc/empeg_notify");
                } else {
                    Log.d("WMLS", "sending: " + message);
                    new sendButton().execute(HTTP_PREFIX + playerIP + message);
                }
            } else {
                Log.d("WMLS", "mApiClient is NOT connected");
            }
        }
 //           super.onMessageReceived(messageEvent);
        }

    public class sendButton extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... url) {
            Log.d("WMLS", "sendButton...");
            String responseBody = "";
            try {
                HttpClient httpclient = new DefaultHttpClient();
                //				//Log.i("EMPEG","FETCHING: "+url[0]);
                HttpGet httpget = new HttpGet(url[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                responseBody = httpclient.execute(httpget, responseHandler);

                httpclient.getConnectionManager().shutdown();
            } catch (MalformedURLException e) {
                //Log.i("EMPEG","MalformedURLException");
            } catch (IOException e) {
                //Log.i("EMPEG","IOException");
            }
            return responseBody;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(String result) {
            //			//Log.i("EMPEG","onPostExecute: "+result);
        }
    }

    public class getVfd extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... url) {
            Log.d("WMLS", "sendButton...");
            String responseBody = "";
            try {
                HttpClient httpclient = new DefaultHttpClient();
                //				//Log.i("EMPEG","FETCHING: "+url[0]);
                HttpGet httpget = new HttpGet(url[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                responseBody = httpclient.execute(httpget, responseHandler);

                httpclient.getConnectionManager().shutdown();
            } catch (MalformedURLException e) {
                //Log.i("EMPEG","MalformedURLException");
            } catch (IOException e) {
                //Log.i("EMPEG","IOException");
            }
            return responseBody;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(String result) {
            String parsedResult = ":#::#:comm error:#:";
            if (result != null) {
                Log.i("WMLS", "onPostExecute: " + result);
                // I think text parsing should occur here
                String[] infos = result.split(System.getProperty("line.separator"));
                // nicen time
                parsedResult = infos[0].split(" = \"")[1].split("  ")[1].substring(0, infos[0].split(" = \"")[1].split("  ")[1].length() - 2) + ":#:";
                parsedResult += infos[1].split(" = \"")[1].substring(0, infos[1].split(" = \"")[1].length() - 2) + ":#:";
                parsedResult += infos[6].split(" = \"")[1].substring(0, infos[6].split(" = \"")[1].length() - 2) + ":#:";
                parsedResult += infos[7].split(" = \"")[1].substring(0, infos[7].split(" = \"")[1].length() - 2);
            }

            final String finalVFDString = parsedResult;

            sendMessage("/message", finalVFDString);
        }
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();

        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //sendMessage(START_ACTIVITY, "");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void sendMessage(final String path, final String text) {
        Log.d("WMLS", "path: " + path + ", text: " + text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes()).await();
                    Log.d("WMLS", "result: " + result.toString());
                }
            }
        }).start();
    }
}