package com.keytop.linephonetest.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.keytop.linephonetest.MainActivity;
import com.keytop.linephonetest.R;

import org.greenrobot.eventbus.EventBus;
import org.linphone.LinphoneUtils;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by fengwenhua on 2016/11/23.
 */

public class SipManager implements LinphoneCoreListener {
    private static final String TAG = SipManager.class.getSimpleName();

    private static SipManager mInstance;
    private Context mContext;
    private LinphoneCore mLinphoneCore;
    private Timer mTimer;

    private LinphoneCall mCall;

    public SipManager(Context c) {
        mContext = c;
        LinphoneCoreFactory.instance().setDebugMode(true, "KEYTOP_SIP");

        try {
            String basePath = mContext.getFilesDir().getAbsolutePath();
            copyAssetsFromPackage(basePath);

            mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, basePath + "/.linphonerc", basePath + "/linphonerc", null, mContext);
            initLinphoneCoreValues(basePath);

            setUserAgent();
            setFrontCamAsDefault();
            startIterate();
//            initLinPhone();
            mInstance = this;
            mLinphoneCore.setMaxCalls(3);
            mLinphoneCore.setNetworkReachable(true);
            mLinphoneCore.enableVideo(false, false);
            mLinphoneCore.setNetworkReachable(true); // 假设网络已经通了
        } catch (LinphoneCoreException e) {
        } catch (IOException e) {
        }
    }

    public void onRelease(){
        try {
            mLinphoneCore.destroy();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            mLinphoneCore = null;
        }
    }

    public void initLinPhone() throws LinphoneCoreException {
        mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, mContext);
        //optional setting based on your needs
        mLinphoneCore.setMaxCalls(1);
        mLinphoneCore.setNetworkReachable(true);
        mLinphoneCore.enableVideo(false, false);
    }

    public void toCall(String account) throws LinphoneCoreException {
        LinphoneCall call = mLinphoneCore.invite(account);
        boolean mIsCalling = true;

        boolean isConnected = false;
        long iterateIntervalMs = 50L;

        if (call == null) {
            Log.i(TAG, "Could not place call to");
        } else {
            Log.i(TAG, "Call to: " + account);

            while (mIsCalling) {
                mLinphoneCore.iterate();

                try {
                    Thread.sleep(iterateIntervalMs);

                    if (call.getState().equals(LinphoneCall.State.CallEnd)
                            || call.getState().equals(LinphoneCall.State.CallReleased)) {
                        mIsCalling = false;
                        Log.i(TAG, "LinphoneCall.State.CallEnd|LinphoneCall.State.CallReleased");
                    }

                    if (call.getState().equals(LinphoneCall.State.StreamsRunning)) {
                        isConnected = true;
                        // do your stuff
                        Log.i(TAG, "LinphoneCall.State.StreamsRunning");
                    }

                    if (call.getState().equals(LinphoneCall.State.OutgoingRinging)) {
                        // do your stuff
                        Log.i(TAG, "LinphoneCall.State.OutgoingRinging");
                    }
                } catch (InterruptedException var8) {
                    Log.i(TAG, "Interrupted! Aborting");
                }
            }
            if (!LinphoneCall.State.CallEnd.equals(call.getState())) {
                Log.i(TAG, "Terminating the call");
                mLinphoneCore.terminateCall(call);
            }
        }
    }

    public void register(String account, String password, String domain) throws LinphoneCoreException {
        String identity = "sip:" + account + "@" + domain;
        LinphoneProxyConfig proxyConfig = mLinphoneCore.createProxyConfig(identity, domain, null, true);
        proxyConfig.setExpires(300);

        mLinphoneCore.addProxyConfig(proxyConfig);

        LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(
                account, password, null, domain);
        mLinphoneCore.addAuthInfo(authInfo);
        mLinphoneCore.setDefaultProxyConfig(proxyConfig);
    }

    /**com.keytop.linephonetest E/myphone: No To: address, cannot build request*/
    public void invate(String useName, String domain) {
        LinphoneAddress la = LinphoneCoreFactory.instance().createLinphoneAddress(useName, domain, useName + "@" + domain);
        la.setTransport(LinphoneAddress.TransportType.LinphoneTransportTcp);
        try {
            mLinphoneCore.invite(la);
        } catch (LinphoneCoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void copyAssetsFromPackage(String basePath) throws IOException {
        SipUtils.copyIfNotExist(mContext, R.raw.oldphone_mono, basePath + "/oldphone_mono.wav");
        SipUtils.copyIfNotExist(mContext, R.raw.ringback, basePath + "/ringback.wav");
        SipUtils.copyIfNotExist(mContext, R.raw.toy_mono, basePath + "/toy_mono.wav");
        SipUtils.copyIfNotExist(mContext, R.raw.linphonerc_default, basePath + "/.linphonerc");
        SipUtils.copyFromPackage(mContext, R.raw.linphonerc_factory, new File(basePath + "/linphonerc").getName());
        SipUtils.copyIfNotExist(mContext, R.raw.lpconfig, basePath + "/lpconfig.xsd");
        SipUtils.copyIfNotExist(mContext, R.raw.rootca, basePath + "/rootca.pem");
    }

    private void initLinphoneCoreValues(String basePath) {
        mLinphoneCore.setContext(mContext);
        mLinphoneCore.setRing(basePath + "/ringback.wav");
        mLinphoneCore.setRootCA(basePath + "/rootca.pem");
        mLinphoneCore.setPlayFile(basePath + "/toy_mono.wav");
        mLinphoneCore.setChatDatabasePath(basePath + "/linphone-history.db");

        int availableCores = Runtime.getRuntime().availableProcessors();
        mLinphoneCore.setCpuCount(availableCores);
    }

    private void setUserAgent() {
        try {
            String versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            if (versionName == null) {
                versionName = String.valueOf(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode);
            }
            mLinphoneCore.setUserAgent("Myphone", versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void setFrontCamAsDefault() {
        int camId = 0;
        AndroidCameraConfiguration.AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
        for (AndroidCameraConfiguration.AndroidCamera androidCamera : cameras) {
            if (androidCamera.frontFacing)
                camId = androidCamera.id;
        }
        mLinphoneCore.setVideoDevice(camId);
    }

    public void acceptCall() throws LinphoneCoreException {

        LinphoneCallParams params = mLinphoneCore.createDefaultCallParameters();

        boolean isLowBandwidthConnection = !LinphoneUtils
                .isHighBandwidthConnection(mContext);
        if (isLowBandwidthConnection) {
            params.enableLowBandwidth(true);
            org.linphone.mediastream.Log.i("Low bandwidth enabled in call params");
        }

        mCall = null;
        List<LinphoneCall> calls = LinphoneUtils
                .getLinphoneCalls(mLinphoneCore);
        for (LinphoneCall call : calls) {
            if (LinphoneCall.State.IncomingReceived == call.getState()) {
                mCall = call;
                break;
            }
        }
        if (mCall != null) {
            mLinphoneCore.acceptCallWithParams(mCall, params);
        }
    }

    private void startIterate() {
        TimerTask lTask = new TimerTask() {
            @Override
            public void run() {
                mLinphoneCore.iterate();
            }
        };

        mTimer = new Timer("my scheduler");
        mTimer.schedule(lTask, 0, 20);
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {
        Log.i(TAG, "authInfoRequested");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("authInfoRequested");
        EventBus.getDefault().post(event);
    }

    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {
        Log.i(TAG, "callStatsUpdated>>>" + linphoneCallStats.getIceState().toString());
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("callStatsUpdated>>>"+linphoneCallStats.getIceState().toString());
        EventBus.getDefault().post(event);
    }

    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {
        Log.i(TAG, "newSubscriptionRequest");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("newSubscriptionRequest");
        EventBus.getDefault().post(event);
    }

    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {
        Log.i(TAG, "notifyPresenceReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("notifyPresenceReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {
        Log.i(TAG, "dtmfReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("dtmfReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {
        Log.i(TAG, "notifyReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("notifyReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {
        Log.i(TAG, "transferState");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("transferState");
        EventBus.getDefault().post(event);
    }

    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {
        Log.i(TAG, "infoReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("infoReceived");
        EventBus.getDefault().post(event);

    }

    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {
        Log.i(TAG, "subscriptionStateChanged");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("subscriptionStateChanged");
        EventBus.getDefault().post(event);
    }

    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {
        Log.i(TAG, "publishStateChanged");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("publishStateChanged");
        EventBus.getDefault().post(event);
    }

    @Override
    public void show(LinphoneCore linphoneCore) {
        Log.i(TAG, "show");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("show");
        EventBus.getDefault().post(event);
    }

    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {
        Log.i(TAG, "displayStatus");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("displayStatus");
        EventBus.getDefault().post(event);
    }

    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {
        Log.i(TAG, "displayMessage");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("displayMessage");
        EventBus.getDefault().post(event);
    }

    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {
        Log.i(TAG, "displayWarning");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("displayWarning");
        EventBus.getDefault().post(event);
    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {
        Log.i(TAG, "fileTransferProgressIndication");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("fileTransferProgressIndication");
        EventBus.getDefault().post(event);
    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {
        Log.i(TAG, "fileTransferRecv");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("fileTransferRecv");
        EventBus.getDefault().post(event);
    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        return 0;
    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
        Log.i(TAG, "globalState");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("globalState");
        EventBus.getDefault().post(event);
    }

    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
        Log.i(TAG, "registrationState");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("registrationState");
        EventBus.getDefault().post(event);
    }

    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {
        Log.i(TAG, "configuringStatus");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("configuringStatus");
        EventBus.getDefault().post(event);
    }

    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {
        Log.i(TAG, "messageReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("messageReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        Log.i(TAG, "callState>>>>"+linphoneCall.getDirection().toString());
//        if (state.equals(LinphoneCall.State.IncomingReceived)&&linphoneCall.getDirection().toString().equals(CallDirection.Incoming.toString())) {
//            EventBus.getDefault().post(MainActivity.UIEvent.MSG_ACCEPT_CALL);
//        }
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("callState>>>"+linphoneCall.getDirection().toString());
        EventBus.getDefault().post(event);
    }

    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {
        Log.i(TAG, "callEncryptionChanged");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("callEncryptionChanged");
        EventBus.getDefault().post(event);
    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {
        Log.i(TAG, "notifyReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("notifyReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {
        Log.i(TAG, "isComposingReceived");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("isComposingReceived");
        EventBus.getDefault().post(event);
    }

    @Override
    public void ecCalibrationStatus(LinphoneCore linphoneCore, LinphoneCore.EcCalibratorStatus ecCalibratorStatus, int i, Object o) {
        Log.i(TAG, "ecCalibrationStatus");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("ecCalibrationStatus");
        EventBus.getDefault().post(event);
    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {
        Log.i(TAG, "uploadProgressIndication");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("uploadProgressIndication");
        EventBus.getDefault().post(event);
    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {
        Log.i(TAG, "uploadStateChanged");
        MainActivity.UIEvent event = MainActivity.UIEvent.MSG_LOG_TEXT;
        event.setMessage("uploadStateChanged");
        EventBus.getDefault().post(event);
    }
}
