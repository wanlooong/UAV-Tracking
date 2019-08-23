package com.dji.videostreamdecodingsample;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;
import com.dji.videostreamdecodingsample.tools.DashedLine;
import com.dji.videostreamdecodingsample.tools.RectView;
import com.dji.videostreamdecodingsample.tools.RectView2;
import com.dji.videostreamdecodingsample.tools.Tracking;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.thirdparty.afinal.core.AsyncTask;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements DJICodecManager.YuvDataCallback, View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MSG_WHAT_SHOW_TOAST = 0;
    private static final int MSG_WHAT_UPDATE_TITLE = 1;
    private SurfaceHolder.Callback surfaceCallback;

    private enum DemoType {USE_TEXTURE_VIEW, USE_SURFACE_VIEW, USE_SURFACE_VIEW_DEMO_DECODER}

    private static DemoType demoType = DemoType.USE_SURFACE_VIEW_DEMO_DECODER;
    private VideoFeeder.VideoFeed standardVideoFeeder;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    //    private TextView titleTv;
    public Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_SHOW_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_UPDATE_TITLE:
//                    if (titleTv != null) {
//                        titleTv.setText((String) msg.obj);
//                    }
                    break;
                default:
                    break;
            }
        }
    };
    private TextureView videostreamPreviewTtView;
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;
    private Camera mCamera;
    private DJICodecManager mCodecManager;
    private Button screenShot, model2Button, model3Button;
    private StringBuilder stringBuilder;
    private int videoViewWidth;
    private int videoViewHeight;
    private int count;
    private Tracking tracking;
    private RectView rectView;
    static private Aircraft aircraft = (Aircraft) VideoDecodingApplication.getProductInstance();
    static private FlightController flightController;

    @Override
    protected void onResume() {
        super.onResume();
        initSurfaceOrTextureView();
        notifyStatusChange();
    }

    private void initSurfaceOrTextureView() {
        switch (demoType) {
            case USE_SURFACE_VIEW:
                initPreviewerSurfaceView();
                break;
            case USE_SURFACE_VIEW_DEMO_DECODER:
                /**
                 * we also need init the textureView because the pre-transcoded video steam will display in the textureView
                 */
                initPreviewerTextureView();

                /**
                 * we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                 * on surfaceView
                 */
                initPreviewerSurfaceView();
                break;
            case USE_TEXTURE_VIEW:
                initPreviewerTextureView();
                break;
        }
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
            }
            if (standardVideoFeeder != null) {
                standardVideoFeeder.removeVideoDataListener(mReceivedVideoDataListener);
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        if (!OpenCVLoader.initDebug()) {
//            showToast("Init OpenCV success.");
        } else {
//            showToast("Init OpenCV failure.");
        }
//        init_Drone();
        initView();
        setDefaultValues();
        bindEvents();
    }

    private void showToast(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_SHOW_TOAST, s)
        );
    }

    private void updateTitle(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_UPDATE_TITLE, s)
        );
    }

    private void initUi() {
//        screenShot = (Button) findViewById(R.id.activity_main_screen_shot);
//        screenShot.setSelected(false);
//        model2Button = (Button) findViewById(R.id.model2);
//        model2Button.setSelected(false);
//        model3Button = (Button) findViewById(R.id.model3);
//        model3Button.setSelected(false);

//        titleTv = (TextView) findViewById(R.id.title_tv);
        videostreamPreviewTtView = (TextureView) findViewById(R.id.livestream_preview_ttv);
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
        videostreamPreviewSf.setClickable(true);
        videostreamPreviewSf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float rate = VideoFeeder.getInstance().getTranscodingDataRate();
                showToast("current rate:" + rate + "Mbps");
                if (rate < 10) {
                    VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
                    showToast("set rate to 10Mbps");
                } else {
                    VideoFeeder.getInstance().setTranscodingDataRate(3.0f);
                    showToast("set rate to 3Mbps");
                }
            }
        });
        float rate = VideoFeeder.getInstance().getTranscodingDataRate();
        if (rate < 10) {
            VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
        }
        updateUIVisibility();
    }

    private void updateUIVisibility() {
        switch (demoType) {
            case USE_SURFACE_VIEW:
                videostreamPreviewSf.setVisibility(View.VISIBLE);
                videostreamPreviewTtView.setVisibility(View.GONE);
                break;
            case USE_SURFACE_VIEW_DEMO_DECODER:
                /**
                 * we need display two video stream at the same time, so we need let them to be visible.
                 */
                videostreamPreviewSf.setVisibility(View.VISIBLE);
                videostreamPreviewTtView.setVisibility(View.VISIBLE);
                break;

            case USE_TEXTURE_VIEW:
                videostreamPreviewSf.setVisibility(View.GONE);
                videostreamPreviewTtView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private long lastupdate;

    private void notifyStatusChange() {

        final BaseProduct product = VideoDecodingApplication.getProductInstance();

        Log.d(TAG, "notifyStatusChange: " + (product == null ? "Disconnect" : (product.getModel() == null ? "null model" : product.getModel().name())));
        if (product != null && product.isConnected() && product.getModel() != null) {
            updateTitle(product.getModel().name() + " Connected " + demoType.name());
        } else {
            updateTitle("Disconnected");
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (System.currentTimeMillis() - lastupdate > 1000) {
                    Log.d(TAG, "camera recv video data size: " + size);
                    lastupdate = System.currentTimeMillis();
                }
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        /**
                         we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                         * on surfaceView
                         */
                        DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
                        break;

                    case USE_TEXTURE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                }

            }
        };

        if (null == product || !product.isConnected()) {
            mCamera = null;
            showToast("Disconnected");
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast("can't change mode of camera, error:" + djiError.getDescription());
                        }
                    }
                });

                if (demoType == DemoType.USE_SURFACE_VIEW_DEMO_DECODER) {
                    if (VideoFeeder.getInstance() != null) {
                        standardVideoFeeder = VideoFeeder.getInstance().provideTranscodedVideoFeed();
                        standardVideoFeeder.addVideoDataListener(mReceivedVideoDataListener);
                    }
                } else {
                    if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
                    }
                }
            }
        }
    }

    /**
     * Init a fake texture view to for the codec manager, so that the video raw data can be received
     * by the camera
     */
    private void initPreviewerTextureView() {
        videostreamPreviewTtView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(getApplicationContext(), surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * Init a surface view for the DJIVideoStreamDecoder
     */
    private void initPreviewerSurfaceView() {
        videostreamPreviewSh = videostreamPreviewSf.getHolder();
        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = videostreamPreviewSf.getWidth();
                videoViewHeight = videostreamPreviewSf.getHeight();
                Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager == null) {
                            mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
                                    videoViewHeight);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        // This demo might not work well on P3C and OSMO.
                        NativeHelper.getInstance().init();
                        DJIVideoStreamDecoder.getInstance().init(getApplicationContext(), holder.getSurface());
                        DJIVideoStreamDecoder.getInstance().resume();
//                        DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
                        break;
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        //mCodecManager.onSurfaceSizeChanged(videoViewWidth, videoViewHeight, 0);
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().changeSurface(holder.getSurface());
                        break;
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.cleanSurface();
                            mCodecManager.destroyCodec();
                            mCodecManager = null;
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().stop();
                        NativeHelper.getInstance().release();
                        break;
                }

            }
        };

        videostreamPreviewSh.addCallback(surfaceCallback);
    }

    private boolean TAG_ready = false, TAG_show = true;
    //    private Mat mat_init;
    private int HH, WW;
    private Rect rect_update;
    private Mat current_frame;
    private Queue<Mat> queue;

    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        if (yuvFrame != null) {
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Mat HSV = new Mat((int) (height * 1.5), width, CvType.CV_8UC1);
                    HSV.put(0, 0, bytes.clone());
                    Mat BGR = new Mat(HH, WW, CvType.CV_8UC4);
                    Imgproc.cvtColor(HSV, BGR, Imgproc.COLOR_YUV420sp2RGB);
                    current_frame = BGR.clone();
                    if (TAG_ready) {
                        queue.offer(BGR.clone());
                    }
                    HSV.release();
                    BGR.release();
                    if (rect_update != null && count++ % 3 == 0 && TAG_show) {
                        rectView.draw(rect_update);
                    }
                }

            });


        }

    }

    private double intersection(Rect rect) {
        if (rect_init == null)
            return 1;
        int p1_x = rect.left, p1_y = rect.top;
        int p2_x = rect.right, p2_y = rect.bottom;
        int p3_x = rect_init.left, p3_y = rect_init.top;
        int p4_x = rect_init.right, p4_y = rect_init.bottom;
        if (p1_x > p4_x || p2_x < p3_x || p1_y > p4_y || p2_y < p3_y) {
            return -1;
        }
        int Len = Math.min(p2_x, p4_x) - Math.max(p1_x, p3_x);
        int Wid = Math.min(p2_y, p4_y) - Math.max(p1_y, p3_y);
//        showToast(""+((Len * Wid )/ (float)(rect_init.height() * rect_init.width())));

        return (Len * Wid / (float) (rect_init.height() * rect_init.width()));
    }

    private void init_Drone() {
        flightController = aircraft.getFlightController();
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    flightController.setVirtualStickAdvancedModeEnabled(true);
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                    flightController.setVerticalControlMode(VerticalControlMode.POSITION);
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                } else {
                    showToast("Error in configuring the drone. Try restarting the drone and try again.");
                }
            }
        });
    }

    private RectView2 rectView2;
    private float V = 0.3f;
    private float max_V = 1.5f;

    private void handleYUVClick() {
        if (screenShot.isSelected()) {
            TAG_ready = false;
            screenShot.setText("INIT");
            screenShot.setSelected(false);
            ViewGroup vg = (ViewGroup) rectView.getParent();
            vg.removeView(rectView);
            queue.clear();
            queue = null;

            if (trackingTimer != null) {
                trackingTimer.cancel();
                trackingTimer.purge();
                trackingTimer = null;
//                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        showToast("Start turn down!");
////                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
//                    }
//                });
                flightController.setVirtualStickModeEnabled(false, null);
            }
            rect_init = null;

            switch (demoType) {
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(videostreamPreviewSh.getSurface());
                    DJIVideoStreamDecoder.getInstance().setYuvDataListener(null);
                    break;
            }
            stringBuilder = null;
        } else {
            screenShot.setText("CLOSE");
            screenShot.setSelected(true);
            File file = new File(ALBUM_PATH);
            if (!file.exists()) {
                file.mkdir();
            }
            HH = DJIVideoStreamDecoder.getInstance().height;
            WW = DJIVideoStreamDecoder.getInstance().width;
            queue = new LinkedList<>();

            switch (demoType) {
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(null);
                    DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
                    break;
            }
            rectView = new RectView(this);
            addContentView(rectView, params);
//            rectView2=new RectView2(this);
//            addContentView(rectView2,params);

            altitude = flightController.getState().getAircraftLocation().getAltitude();
            trackingTimer = new Timer();
//            mSendVirtualStickDataTimer = new Timer();
            flightControlData = new FlightControlData(0, 0, 0, altitude);
//            trackingTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
////                    if (System.currentTimeMillis() % 1000 == 0) {
//////                        showToast("" + lineRoll + " " + linePitch);
////                        flightController = aircraft.getFlightController();
////                    }
//
//                    flightController.sendVirtualStickFlightControlData(
//                            new FlightControlData(
//                                    lineRoll, linePitch, 0, altitude
//                            ), new CommonCallbacks.CompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        showToast("SendControlData error !");
//                                    }
//                                }
//                            }
//                    );
//                }
//            }, 0, 33);
            trackingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (TAG_ready) {
                        while (!queue.isEmpty()) {
//                            showToast(queue.size() + "");
                            Mat mat = queue.poll();
                            if (mat == null || (Arrays.equals(mat.get(0, 0), mat.get(100, 100)) && Arrays.equals(mat.get(100, 100), mat.get(200, 200))))
                                continue;
                            Rect update = tracking.run(mat);
                            double temp = 1;
                            if (update != null) {
                                rect_update = update;
                                temp = intersection(rect_update);
                            }
                            if (update == null) {
                                stop(true);
                                tracking.init(mat, rect_update, HEIGHT, WIDTH);
//                                showToast("丢失！");
                            } else if (temp < 0.6 && !TAG_Gimbal) {
                                stop(false);
                                if (temp <= 0) {
                                    if (V < 1.5f)
                                        V += 0.1f;
                                } else
                                    V = 0.3f;
                                int t1 = rect_update.top - rect_init.top, t2 = rect_update.left - rect_init.left;
                                if (Math.abs(t1) > Math.abs(t2)) {
                                    if (t1 < 0) {
                                        quadLine("N", V);
                                    } else {
                                        quadLine("S", V);
                                    }
                                } else {
                                    if (t2 < 0) {
                                        quadLine("W", V);
                                    } else {
                                        quadLine("E", V);
                                    }
                                }
                            } else {
                                stop(true);
                            }
                            mat.release();
                        }
//                        showToast("" + (System.currentTimeMillis() - startTime));
//                        AsyncTask.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                long startTime = System.currentTimeMillis();
//                                showToast("" + (System.currentTimeMillis() - startTime));
//                            }
//                        });
                    }
                }
            }, 1, 1);
            HEIGHT = this.getResources().getDisplayMetrics().heightPixels;
            WIDTH = this.getResources().getDisplayMetrics().widthPixels;
            trackingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!rectView.TAG && current_frame != null) {
                        if (trackingTimer != null) {
                            rect_init = rectView.getRect();
                            tracking = new Tracking();
                            tracking.init_KalmanFilter((double) rect_init.left, (double) rect_init.top);
                            tracking.init(current_frame.clone(), rect_init, HEIGHT, WIDTH);
//                            showToast("Action!");
                            TAG_ready = true;
                            this.cancel();
                        } else {
                            showToast("未初始化！");
                        }
                    }
                }
            }, 500, 100);

        }
    }

    private Rect rect_init;
    private Timer trackingTimer;
    private int HEIGHT, WIDTH;
    private boolean TAG_Gimbal = false;
    private static String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/DJI_Photo/";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    public void model() {
        if (TAG_model1) {
            max_V = 1.5f;
            TAG_Gimbal = true;
            showToast("Action!");
            trackingTimer.schedule(design(TAG_design, 1), 0, 100);
        } else if (TAG_model2) {
            max_V = 1.5f;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("Action!");
                }
            });
            if (TAG_design != -1) {
                TAG_Gimbal = true;
                trackingTimer.schedule(design(TAG_design, 2), 0, 100);
            }
            trackingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (TAG_model2) {
                        Date date = new Date(System.currentTimeMillis());
                        Imgcodecs.imwrite(ALBUM_PATH + "Model_2_" + TAG_design + "_" + simpleDateFormat.format(date) + ".jpg", current_frame);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Taking a photo !");
                            }
                        });
                    } else {
                        this.cancel();
                    }
                }
            }, 1000, 1000);
        } else if (TAG_model3) {
            max_V = 2.5f;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("Action!");
                }
            });
            if (TAG_design != -1) {
                TAG_Gimbal = true;
                trackingTimer.schedule(design(TAG_design, 2), 0, 100);
            }
            trackingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (TAG_model3) {
                        Date date = new Date(System.currentTimeMillis());
                        Imgcodecs.imwrite(ALBUM_PATH + "Model_3_" + TAG_design + "_" + simpleDateFormat.format(date) + ".jpg", current_frame);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Taking a photo !");
                            }
                        });
                    } else {
                        this.cancel();
                    }
                }
            }, 1000, 1000);
        } else if (TAG_model4) {
            max_V = 1.5f;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("Action!");
                }
            });
            TAG_Gimbal = true;
            trackingTimer.schedule(design(3, 4), 0, 100);
        }
    }

    private FrameLayout.LayoutParams params = new FrameLayout.LayoutParams
            (FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    private float linePitch = 0;
    private float lineRoll = 0;
    private float altitude = 2;
    private boolean TAG_model1, TAG_model2, TAG_model3, TAG_model4;
    private FlightControlData flightControlData;
    private CommonCallbacks.CompletionCallback completionCallback = new CommonCallbacks.CompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            if (djiError != null) {
                showToast("SendControlData error !");
            }
        }
    };

    // This function is what acutally sends the flight commands to the drone
    public void quadLine(String dir, float velocity) {
        dir = dir.toUpperCase();
        switch (dir) {
            case "E":
                linePitch = (1 * velocity);
                break;
            case "W":
                linePitch = (-1 * velocity);
                break;
            case "S":
                lineRoll = (-1 * velocity);
                break;
            case "N":
                lineRoll = (1 * velocity);
                break;
        }
        flightControlData.setRoll(lineRoll);
        flightControlData.setPitch(linePitch);
        flightController.sendVirtualStickFlightControlData(flightControlData, completionCallback);
    }

    // This function is sent to the drone to stop it
    public void stop(boolean tag) {
        linePitch = 0;
        lineRoll = 0;
        if (tag) {
            flightControlData.setRoll(lineRoll);
            flightControlData.setPitch(linePitch);
            flightController.sendVirtualStickFlightControlData(flightControlData, completionCallback);
        }
    }


    private FloatingActionButton fab01Add, fab01Add1;
    private boolean isAdd = false;
    private RelativeLayout rlAddBill, rlAddBill1;
    private int[] llId = new int[]{R.id.ll01, R.id.ll02, R.id.ll03, R.id.ll04, R.id.ll05, R.id.ll06, R.id.ll07};
    private LinearLayout[] ll = new LinearLayout[llId.length];
    private int[] fabId = new int[]{R.id.miniFab01, R.id.miniFab02, R.id.miniFab03, R.id.miniFab04, R.id.miniFab05, R.id.miniFab06, R.id.miniFab07};
    private FloatingActionButton[] fab = new FloatingActionButton[fabId.length];
    private AnimatorSet addBillTranslate1;
    private AnimatorSet addBillTranslate2;
    private AnimatorSet addBillTranslate3;
    private AnimatorSet addBillTranslate4;
    private AnimatorSet addBillTranslate5;
    private AnimatorSet addBillTranslate6;
    private AnimatorSet addBillTranslate7;
    private DashedLine dashedLine;
    private boolean TAG_MM4 = false;

    private void initView() {
        fab01Add = (FloatingActionButton) findViewById(R.id.fab);
        rlAddBill = (RelativeLayout) findViewById(R.id.rlAddBill);
        for (int i = 0; i < llId.length; i++) {
            ll[i] = (LinearLayout) findViewById(llId[i]);
        }
        for (int i = 0; i < fabId.length; i++) {
            fab[i] = (FloatingActionButton) findViewById(fabId[i]);
        }
        fab01Add1 = (FloatingActionButton) findViewById(R.id.fab1);
        rlAddBill1 = (RelativeLayout) findViewById(R.id.rlAddBill1);
    }

    private void setDefaultValues() {
        addBillTranslate1 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate2 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate3 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate4 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate5 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate6 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
        addBillTranslate7 = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.add_bill_anim);
    }

    private void bindEvents() {
        fab01Add.setOnClickListener(this);
        for (int i = 0; i < fabId.length; i++) {
            fab[i].setOnClickListener(this);
        }
        fab01Add1.setOnClickListener(this);
    }

    private void close_tracking() {
        TAG_ready = false;

        if (trackingTimer != null) {
            trackingTimer.cancel();
            trackingTimer.purge();
            trackingTimer = null;
            flightController.setVirtualStickModeEnabled(false, null);
        }
        rect_init = null;
        stringBuilder = null;
        TAG_show = false;
        rectView.empty();
        ViewGroup vg = (ViewGroup) rectView.getParent();
        vg.removeView(rectView);
        queue.clear();
        queue = null;
        rect_update = null;
    }

    private void init_tracking() {
        File file = new File(ALBUM_PATH);
        if (!file.exists()) {
            file.mkdir();
        }
        init_Drone();
        HH = DJIVideoStreamDecoder.getInstance().height;
        WW = DJIVideoStreamDecoder.getInstance().width;
        queue = new LinkedList<>();

        switch (demoType) {
            case USE_SURFACE_VIEW_DEMO_DECODER:
                DJIVideoStreamDecoder.getInstance().changeSurface(null);
                DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
                break;
        }
        rectView = new RectView(this);
        addContentView(rectView, params);

        altitude = flightController.getState().getAircraftLocation().getAltitude();
        trackingTimer = new Timer();
        flightControlData = new FlightControlData(0, 0, 0, altitude);
        trackingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TAG_ready) {
                    while (queue != null && !queue.isEmpty()) {
//                            showToast(queue.size() + "");
                        Mat mat = queue.poll();
                        if (mat == null || (Arrays.equals(mat.get(0, 0), mat.get(100, 100)) && Arrays.equals(mat.get(100, 100), mat.get(200, 200))))
                            continue;
                        Rect update = tracking.run(mat);
                        double temp = 1;
                        if (update != null) {
                            rect_update = update;
                            temp = intersection(rect_update);
                        }
                        if (update == null) {
                            stop(true);
                            tracking.init(mat, rect_update, HEIGHT, WIDTH);
//                                showToast("丢失！");
                        } else if (temp < 0.8 && !TAG_Gimbal) {
                            stop(false);
                            if (temp <= 0.3) {
                                if (V < max_V)
                                    V += 0.1f;
                            } else
                                V = 0.3f;
                            int t1 = rect_update.top - rect_init.top, t2 = rect_update.left - rect_init.left;
                            if (Math.abs(t1) > Math.abs(t2)) {
                                if (t1 < 0) {
                                    quadLine("N", V);
                                } else {
                                    quadLine("S", V);
                                }
                            } else {
                                if (t2 < 0) {
                                    quadLine("W", V);
                                } else {
                                    quadLine("E", V);
                                }
                            }
                        } else {
                            stop(true);
                        }
                        mat.release();
                    }
                }
            }
        }, 1, 1);
        HEIGHT = this.getResources().getDisplayMetrics().heightPixels;
        WIDTH = this.getResources().getDisplayMetrics().widthPixels;
        trackingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!rectView.TAG && current_frame != null) {
                    if (trackingTimer != null) {
                        rect_init = rectView.getRect();
                        tracking = new Tracking();
                        tracking.init_KalmanFilter((double) rect_init.left, (double) rect_init.top);
                        tracking.init(current_frame.clone(), rect_init, HEIGHT, WIDTH);
//                            showToast("Action!");
                        TAG_ready = true;
                        TAG_show = true;
                        this.cancel();
                    } else {
                        showToast("未初始化！");
                    }
                }
            }
        }, 500, 100);
    }

    private TimerTask design(final int v, final int model) {
        final int t1 = rect_init.width() / 2, t2 = rect_init.height() / 2;
        switch (v) {
            case 1:
                if (rect_init.width() < WIDTH * 2 / 3) {
                    if (rect_init.centerX() < WIDTH / 2) {
                        rect_init.left = WIDTH / 3 - t1;
                        rect_init.right = WIDTH / 3 + t1;
                    } else {
                        rect_init.left = WIDTH * 2 / 3 - t1;
                        rect_init.right = WIDTH * 2 / 3 + t1;
                    }
                }
                if (rect_init.height() < HEIGHT * 2 / 3) {
                    if (rect_init.centerY() < HEIGHT / 2) {
                        rect_init.top = HEIGHT / 3 - t2;
                        rect_init.bottom = HEIGHT / 3 + t2;
                    } else {
                        rect_init.top = HEIGHT * 2 / 3 - t2;
                        rect_init.bottom = HEIGHT * 2 / 3 + t2;
                    }
                }
                break;
            case 2:
                break;
            case 3:
                rect_init.left = WIDTH / 2 - t1;
                rect_init.right = WIDTH / 2 + t1;
                rect_init.top = HEIGHT / 2 - t2;
                rect_init.bottom = HEIGHT / 2 + t2;
                break;
        }
        if (v == 1 || v == 3) {
            return new TimerTask() {
                @Override
                public void run() {
                    float pitchValue = 1;
                    if (Math.abs(rect_update.centerY() - rect_init.centerY()) < t2 * 0.4) {
                        TAG_Gimbal = false;
                        if (model == 1) {
                            trackingTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (intersection(rect_update) > 0.6 && linePitch == 0 && lineRoll == 0) {
//                                    TAG_Gimbal = true;
                                        Date date = new Date(System.currentTimeMillis());
                                        Imgcodecs.imwrite(ALBUM_PATH + "Model_" + model + "_" + v + "_" + simpleDateFormat.format(date) + ".jpg", current_frame);
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                showToast("Take photo finished!");
                                            }
                                        });
                                        this.cancel();
                                    }
                                }
                            }, 10, 200);
                        } else if (model == 4) {
                            trackingTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (intersection(rect_update) > 0.6 && linePitch == 0 && lineRoll == 0) {
                                        TAG_show = false;
                                        rectView.empty();
                                        final long current_time = System.currentTimeMillis();
                                        TAG_ready = false;
                                        trackingTimer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                if (System.currentTimeMillis() - current_time > 6000) {
//                                                    TAG_Gimbal = false;
                                                    Date date = new Date(System.currentTimeMillis());
                                                    Imgcodecs.imwrite(ALBUM_PATH + "Model_" + model + "_" + v + "_" + simpleDateFormat.format(date) + ".jpg", current_frame);
                                                    showToast("Take photo finished!");
                                                    this.cancel();
                                                } else {
//                                                    showToast(""+(System.currentTimeMillis() - current_time));
                                                    quadLine("N", 1f);
                                                }
                                            }
                                        }, 0, 100);
                                        this.cancel();
                                    }
                                }
                            }, 10, 200);
                        }
                        this.cancel();
                    } else if (rect_update.centerY() < rect_init.centerY()) {
                        pitchValue *= 1;
                    } else {
                        pitchValue *= -1;
                    }

                    aircraft.getGimbal().rotate(new Rotation.Builder().pitch(pitchValue)
                            .mode(RotationMode.SPEED)
                            .yaw(Rotation.NO_ROTATION)
                            .roll(Rotation.NO_ROTATION)
                            .time(0)
                            .build(), new CommonCallbacks.CompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {

                        }
                    });
                }
            };
        } else {
            return null;
        }
    }

    private int TAG_design = -1;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                fab01Add.setImageResource(isAdd ? R.mipmap.addteam : R.mipmap.ic_close_white_24dp);
                isAdd = !isAdd;
                rlAddBill.setVisibility(isAdd ? View.VISIBLE : View.GONE);
                if (isAdd) {
                    addBillTranslate1.setTarget(ll[3]);
                    addBillTranslate1.start();
                    addBillTranslate2.setTarget(ll[4]);
                    addBillTranslate2.setStartDelay(50);
                    addBillTranslate2.start();
                    addBillTranslate3.setTarget(ll[5]);
                    addBillTranslate3.setStartDelay(100);
                    addBillTranslate3.start();
                    addBillTranslate4.setTarget(ll[6]);
                    addBillTranslate4.setStartDelay(150);
                    addBillTranslate4.start();
                }
                break;
            case R.id.fab1:
                fab01Add1.setImageResource(isAdd ? R.mipmap.pushpin : R.mipmap.ic_close_white_24dp);
                isAdd = !isAdd;
                rlAddBill1.setVisibility(isAdd ? View.VISIBLE : View.GONE);
                if (isAdd) {
                    addBillTranslate5.setTarget(ll[0]);
                    addBillTranslate5.start();
                    addBillTranslate6.setTarget(ll[1]);
                    addBillTranslate6.setStartDelay(50);
                    addBillTranslate6.start();
                    addBillTranslate7.setTarget(ll[2]);
                    addBillTranslate7.setStartDelay(100);
                    addBillTranslate7.start();
                }
                break;
            case R.id.miniFab01:
                TAG_design = 3;
                hideFABMenu(1);
                break;
            case R.id.miniFab02:
                TAG_design = 2;
                hideFABMenu(1);
                break;
            case R.id.miniFab03:
                TAG_design = 1;
                dashedLine = new DashedLine(this);
                addContentView(dashedLine, params);
                hideFABMenu(1);
                break;
            case R.id.miniFab04:
                TAG_model4 = true;
                init_tracking();
                hideFABMenu(0);
                break;
            case R.id.miniFab05:
                TAG_model3 = true;
                init_tracking();
                hideFABMenu(0);
                break;
            case R.id.miniFab06:
                TAG_model2 = true;
                init_tracking();
                hideFABMenu(0);
                break;
            case R.id.miniFab07:
                TAG_model1 = true;
                TAG_design = 3;
                init_tracking();
                hideFABMenu(0);
                break;
            default:
                break;
        }
    }

    private void hideFABMenu(int v) {
        if (v == 0) {
            rlAddBill.setVisibility(View.GONE);
            fab01Add.setImageResource(R.mipmap.addteam);
            isAdd = false;
        } else if (v == 1) {
            rlAddBill1.setVisibility(View.GONE);
            fab01Add1.setImageResource(R.mipmap.pushpin);
            isAdd = false;
        }
    }

    private boolean TAG_photo = true;

    public void action(View v) {
        if (TAG_model1 || TAG_model2 || TAG_model3 || TAG_model4) {
            TAG_photo = !TAG_photo;
            if (TAG_photo) {
                TAG_model1 = false;
                TAG_model2 = false;
                TAG_model3 = false;
                TAG_model4 = false;
                showToast("Taking photo finished!");
                stopRecord();
                close_tracking();
                TAG_design = -1;
            } else {
                if (TAG_design == 1) {
                    ViewGroup vg = (ViewGroup) dashedLine.getParent();
                    vg.removeView(dashedLine);
                }
                startRecord();
                model();
            }
        } else {
            showToast("未初始化！");
        }
    }

    /**
     * 开始录像的函数
     */
    private void startRecord() {

        final Camera camera = VideoDecodingApplication.getCameraInstance();//获取相机对象
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            }); // Execute the startRecordVideo API
        }
    }

    /**
     * 停止摄影方法的函数
     */
    private void stopRecord() {

        Camera camera = VideoDecodingApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {

                @Override
                public void onResult(DJIError djiError) {
                }
            }); // Execute the stopRecordVideo API
        }
    }
}
