package com.keytop.linephonetest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.keytop.linephonetest.utils.SipManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    LinphoneCore mLinphoneCore;
    private static final String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.sip_account) public EditText sip_account;
    @BindView(R.id.sip_domain) public EditText sip_domain;
    @BindView(R.id.sip_tocallAccount) public EditText sip_tocallAccount;
    @BindView(R.id.sip_register) public Button sip_register;
    @BindView(R.id.sip_accept) public Button sip_accept;
    @BindView(R.id.sip_tocall) public Button sip_tocall;
    @BindView(R.id.sip_log) public TextView sip_log;
    private SipManager mSipManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        mSipManager = new SipManager(this);
        ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSipManager.onRelease();//如果不释放，下次打电话会失败
        EventBus.getDefault().unregister(this);
    }

    public enum  UIEvent{
        MSG_LOG_TEXT(0,""),
        MSG_ACCEPT_CALL(1,""),
        MSG_BACKGROUND_CALL(2,"");

        UIEvent(int code,String message){
            this.code =code;
            this.message = message;
        }

        private int code;
        private String message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.sip_register)
    public void register(){
        try {
            mSipManager.register(sip_account.getText().toString(),"71234568",sip_domain.getText().toString());
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }



    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogEvent(UIEvent event){
        switch (event){
            case MSG_LOG_TEXT:
                if (!TextUtils.isEmpty(event.getMessage()))
                sip_log.setText(event.getMessage());
                break;
            case MSG_ACCEPT_CALL:
                acceptCall();
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onBackGroundEvent(UIEvent event){
        switch (event){
            case MSG_BACKGROUND_CALL:
                try {
                    mSipManager.toCall(sip_tocallAccount.getText().toString());
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @OnClick(R.id.sip_tocall)
    public void invate(){
//        mSipManager.invate(sip_tocallAccount.getText().toString(),sip_domain.getText().toString());
        EventBus.getDefault().post(UIEvent.MSG_BACKGROUND_CALL);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.sip_accept)
    public void acceptCall(){
        try {
            mSipManager.acceptCall();
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

}
