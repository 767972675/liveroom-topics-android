package com.zego.videoexternalrender.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zego.common.ZGHelper;
import com.zego.common.ZGManager;
import com.zego.videoexternalrender.R;
import com.zego.videoexternalrender.videorender.VideoRenderer;
import com.zego.zegoavkit2.enums.VideoExternalRenderType;
import com.zego.zegoavkit2.videorender.ZegoExternalVideoRender;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

public class ZGVideoRenderUI extends AppCompatActivity implements IZegoLivePublisherCallback, IZegoLivePlayerCallback {
    private TextureView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;

    private String mRoomID = "zgver_";
    private String mRoomName = "VideoExternalRenderDemo";
    private String mPlayStreamID = "";

    private ZegoExternalVideoRender zegoExternalVideoRender = null;

    private VideoRenderer videoRenderer;
    private int chooseRenderType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_render);

        mPreView = (TextureView)findViewById(R.id.pre_view);
        mPlayView = (TextureView)findViewById(R.id.play_view);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);
        mDealBtn = (Button)findViewById(R.id.publish_btn);
        mDealPlayBtn = (Button)findViewById(R.id.play_btn);

        chooseRenderType = getIntent().getIntExtra("RenderType", 0);

        String deviceID = ZGHelper.generateDeviceId(this);
        mRoomID += deviceID;
        ZGManager.setLoginUser(deviceID, deviceID);


        videoRenderer = new VideoRenderer();
        videoRenderer.init();
        // 添加外部渲染视图
        videoRenderer.addView (com.zego.zegoavkit2.ZegoConstants.ZegoVideoDataMainPublishingStream, mPreView);

        zegoExternalVideoRender = new ZegoExternalVideoRender();
        // 设置外部渲染回调监听
        zegoExternalVideoRender.setZegoExternalRenderCallback2(videoRenderer);

        //设置推流回调监听
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        //设置拉流回调监听
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, mRoomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {
                if (errorcode == 0){

                    ZGManager.sharedInstance().api().setPreviewView(mPreView);
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
                    ZGManager.sharedInstance().api().enableCamera(true);
                    // 设置推流分辨率
                    ZGManager.sharedInstance().api().setAVConfig (new ZegoAvConfig(ZegoAvConfig.Level.High));
                    ZGManager.sharedInstance().api().startPreview();
                    ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

                } else {
                    mErrorTxt.setText("login room fail, err:"+errorcode);
                }
            }
        });

        if (!ret){
            mErrorTxt.setText("login room fail(sync)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        videoRenderer.uninit();
        ZGManager.sharedInstance().api().logoutRoom();
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().unInitSDK();
    }

    public void DealPublishing(View view){
        if (mDealBtn.getText().toString().equals("StopPublish")){
            //停止推流
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();

            runOnUiThread(()->{
                mDealBtn.setText("StartPublish");
            });

        } else {
            //开始推流
            ZGManager.sharedInstance().api().setPreviewView(mPreView);
            ZGManager.sharedInstance().api().startPreview();
            ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);
        }
    }

    public void dealPlay(View view){

        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")){
            // 拉流视图
            if (chooseRenderType == VideoExternalRenderType.NOT_DECODE.value()) {
                videoRenderer.addDecodView(mPlayView);
            } else {
                videoRenderer.addView (mPlayStreamID, mPlayView);
            }

            // 开始拉流
            boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mPlayStreamID, null);

            mErrorTxt.setText("");
            if (!ret){
                runOnUiThread(()->{
                    mErrorTxt.setText("拉流失败");
                });
            }

        } else {
            if (!mPlayStreamID.equals("")){
                //停止拉流
                ZGManager.sharedInstance().api().stopPlayingStream(mPlayStreamID);
                runOnUiThread(()->{
                    mDealPlayBtn.setText("StartPlay");
                });
            }
        }
    }

    //推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode != 0) {
            runOnUiThread(()->{
                mErrorTxt.setText("publish fail, err:"+stateCode);
                mDealBtn.setText("StartPublish");
            });
        } else {
            mDealBtn.setText("StopPublish");
            mPlayStreamID = streamID;
        }
    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

    }

    @Override
    public AuxData onAuxCallback(int i) {
        return null;
    }

    @Override
    public void onCaptureVideoSizeChangedTo(int i, int i1) {

    }

    @Override
    public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {

    }

    @Override
    public void onCaptureVideoFirstFrame() {

    }

    // 拉流回调
    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {

        if (stateCode != 0){
            runOnUiThread(()->{
                mErrorTxt.setText("拉流失败，err："+stateCode);
            });
        } else {
            runOnUiThread(() -> {
                mDealPlayBtn.setText("StopPlay");
            });
        }
    }

    @Override
    public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {

    }

    @Override
    public void onInviteJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onRecvEndJoinLiveCommand(String s, String s1, String s2) {

    }

    @Override
    public void onVideoSizeChangedTo(String streamID, int width, int height) {
        Log.d("Zego","onVideoSizeChangedTo callback, streamID: "+streamID+", width:"+width+",height:"+height);

    }
}
