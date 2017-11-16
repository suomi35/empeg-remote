package com.chasinglemons.empeg;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends FragmentActivity implements MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks {

    public static final String WEAR_MESSAGE_PATH = "/message";
    public static final String SHARED_PREF_PAGE = "displayedPage";
    private GoogleApiClient mApiClient;
    private boolean allowVFDUpdates = false;
    private SharedPreferences config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        config = PreferenceManager.getDefaultSharedPreferences(this);

        initGoogleApiClient();

        // Instantiate a ViewPager and a PagerAdapter.
        final GridViewPager mPager = (GridViewPager) findViewById(R.id.pager);
        final DotsPageIndicator mPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        GridPagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mPager.setCurrentItem(0, config.getInt(SHARED_PREF_PAGE, 0), true);
                mPager.getAdapter().notifyDataSetChanged();
                mPager.removeOnLayoutChangeListener(this);

            }
        });

        mPageIndicator.setPager(mPager);
        mPageIndicator.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int row, int column, float rowOffset, float columnOffset,
                                       int rowOffsetPixels, int columnOffsetPixels) {
            }

            @Override
            public void onPageSelected(int row, int column) {
                // if it's VFD then allow the update requests
                if (column == 1) {
                    allowVFDUpdates = true;
                } else {
                    allowVFDUpdates = false;
                }

                SharedPreferences.Editor editor = config.edit();
                editor.putInt(SHARED_PREF_PAGE, column);
                editor.apply();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initGoogleApiClient() {
        Log.d("WATCHMAIN", "initGoogleApiClient");
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        if (!(mApiClient.isConnected() || mApiClient.isConnecting())) {
            Log.d("WATCHMAIN", "connecting to ApiClient...");
            mApiClient.connect();
        } else {
            Log.d("WATCHMAIN", "something is fucked");
            Log.d("WATCHMAIN", "mApiClient != null: " + (mApiClient != null));
            Log.d("WATCHMAIN", "!(mApiClient.isConnected(): " + (!mApiClient.isConnected()));
            Log.d("WATCHMAIN", "mApiClient.isConnecting(): " + (mApiClient.isConnecting()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())) {
            mApiClient.connect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.d("MAIN", "onMessageReceived(" + messageEvent.getPath() + ")");

        Intent intent = new Intent("screen-update");
        intent.putExtra("updatedScreen", new String(messageEvent.getData()));
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("WATCHMAIN", "onConnected()!");
        Wearable.MessageApi.addListener(mApiClient, this);
    }

    @Override
    protected void onStop() {
        if (mApiClient != null) {
            Wearable.MessageApi.removeListener(mApiClient, this);
            if (mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mApiClient != null)
            mApiClient.unregisterConnectionCallbacks(this);
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("WATCHMAIN", "onConnectionSuspended()");
    }

    public void getVFDUpdate(final String path, final String text) {
        if (allowVFDUpdates) {
            sendMessage(path, text);
        }
    }

    public void sendMessage(final String path, final String text) {
        if (mApiClient.isConnected()) {
            Log.d("WATCHMAIN", "sendMessage()");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mApiClient, node.getId(), path, text.getBytes()).await();
                        Log.d("WATCHMAIN", "result: " + result.toString());
                    }
                }
            }).start();
        } else {
            Log.d("WATCHMAIN", "APIClient is NOT connected, bailing");
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentGridPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int column) {
            Log.d("WATCHMAIN", "getFragment - row: " + row + ", column: " + column);
            if (column == 0) {
                Log.d("WATCHMAIN", "Adding DPAD fragment");
                return new DpadFrament();
            } else if (column == 1) {
                Log.d("WATCHMAIN", "Adding VFD fragment");
                return new VfdFrament();
            } else {
                Log.d("WATCHMAIN", "Adding KNOB fragment");
                return new KnobFragment();
            }
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int colCount) {
            return 3;
        }
    }
}