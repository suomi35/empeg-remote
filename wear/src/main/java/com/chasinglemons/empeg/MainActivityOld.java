//package com.chasinglemons.empeg;
//
//import android.app.Activity;
//import android.os.Bundle;
//import android.support.wearable.view.DotsPageIndicator;
//import android.util.Log;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.ImageButton;
//import android.widget.ListView;
//
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.wearable.MessageApi;
//import com.google.android.gms.wearable.MessageEvent;
//import com.google.android.gms.wearable.Node;
//import com.google.android.gms.wearable.NodeApi;
//import com.google.android.gms.wearable.Wearable;
//
//public class MainActivityOld extends Activity implements MessageApi.MessageListener,
//        GoogleApiClient.ConnectionCallbacks, View.OnClickListener, View.OnLongClickListener {
//
//    public static String BUTTON_UP = "up";
//    public static String BUTTON_LEFT = "left";
//    public static String BUTTON_RIGHT = "right";
//    public static String BUTTON_DOWN = "down";
//    public static String BUTTON_UP_LONG = "up_long";
//    public static String BUTTON_LEFT_LONG = "left_long";
//    public static String BUTTON_RIGHT_LONG = "right_long";
//    public static String BUTTON_DOWN_LONG = "down_long";
//
//    private static final String WEAR_MESSAGE_PATH = "/message";
//    private GoogleApiClient mApiClient;
//
//    private ListView mListView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        ImageButton btnUp = (ImageButton) findViewById(R.id.button_up);
//        btnUp.setOnClickListener(this);
//        btnUp.setOnLongClickListener(this);
//        ImageButton btnLeft = (ImageButton) findViewById(R.id.button_left);
//        btnLeft.setOnClickListener(this);
//        btnLeft.setOnLongClickListener(this);
//        ImageButton btnRight = (ImageButton) findViewById(R.id.button_right);
//        btnRight.setOnClickListener(this);
//        btnRight.setOnLongClickListener(this);
//        ImageButton btnDown = (ImageButton) findViewById(R.id.button_down);
//        btnDown.setOnClickListener(this);
//        btnDown.setOnLongClickListener(this);
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        initGoogleApiClient();
//
//        DotsPageIndicator dotsIndicator = (DotsPageIndicator)findViewById(R.id.page_indicator);
//        dotsIndicator.setPager(mViewPager);
//    }
//
//    private void initGoogleApiClient() {
//        Log.d("WATCHMAIN", "initGoogleApiClient");
//        mApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Wearable.API)
//                .addConnectionCallbacks(this)
//                .build();
//
//        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())) {
//            Log.d("WATCHMAIN", "connecting to ApiClient...");
//            mApiClient.connect();
//        } else {
//            Log.d("WATCHMAIN", "something is fucked");
//            Log.d("WATCHMAIN", "mApiClient != null: " + (mApiClient != null));
//            Log.d("WATCHMAIN", "!(mApiClient.isConnected(): " + (!mApiClient.isConnected()));
//            Log.d("WATCHMAIN", "mApiClient.isConnecting(): " + (mApiClient.isConnecting()));
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())) {
//            mApiClient.connect();
//        }
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//    }
//
//    @Override
//    public void onMessageReceived(final MessageEvent messageEvent) {
//        Log.d("MAIN", "onMessageReceived(" + messageEvent.getPath() + ")");
////        runOnUiThread(new Runnable() {
////            @Override
////            public void run() {
////                TextView tv = (TextView) findViewById(R.id.test);
////                tv.setText(messageEvent.getPath());
////                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
////                }
////            }
////        });
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        Log.d("WATCHMAIN", "onConnected()!");
//        Wearable.MessageApi.addListener(mApiClient, this);
//    }
//
//    @Override
//    protected void onStop() {
//        if (mApiClient != null) {
//            Wearable.MessageApi.removeListener(mApiClient, this);
//            if (mApiClient.isConnected()) {
//                mApiClient.disconnect();
//            }
//        }
//        super.onStop();
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (mApiClient != null)
//            mApiClient.unregisterConnectionCallbacks(this);
//        super.onDestroy();
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.d("WATCHMAIN", "onConnectionSuspended()");
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.button_up:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Top");
//                break;
//            case R.id.button_left:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Left");
//                break;
//            case R.id.button_right:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Right");
//                break;
//            case R.id.button_down:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Bottom");
//                break;
//        }
//    }
//
//    @Override
//    public boolean onLongClick(View v) {
//        switch (v.getId()) {
//            case R.id.button_up:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Top.L");
//                break;
//            case R.id.button_left:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Left.L");
//                break;
//            case R.id.button_right:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Right.L");
//                break;
//            case R.id.button_down:
//                sendMessage(WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Bottom.L");
//                break;
//        }
//        return true;
//    }
//
//    private void sendMessage(final String path, final String text) {
//        if (mApiClient.isConnected()) {
//            Log.d("WATCHMAIN", "sendMessage()");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
//                    for (Node node : nodes.getNodes()) {
//                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
//                                mApiClient, node.getId(), path, text.getBytes()).await();
//                        Log.d("WATCHMAIN", "result: " + result.toString());
//                    }
//                }
//            }).start();
//        } else {
//            Log.d("WATCHMAIN", "APIClient is NOT connected, bailing");
//        }
//    }
//}