package com.dji.videostreamdecodingsample;

import android.app.Application;
import android.content.Context;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

public class VideoDecodingApplication extends Application {

    private static BaseProduct mProduct;

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized void updateProduct(BaseProduct product) {
        mProduct = product;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        com.secneo.sdk.Helper.install(VideoDecodingApplication.this);
    }

    /**
     * 获取连接的产品相机对象
     *
     * @return
     */
    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        Camera camera = null;

        //判别是无人机还是手持的云台设备
        if (getProductInstance() instanceof Aircraft) {
            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }

}
