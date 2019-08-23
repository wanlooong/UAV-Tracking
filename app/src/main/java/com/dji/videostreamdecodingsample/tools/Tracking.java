package com.dji.videostreamdecodingsample.tools;

import android.graphics.Rect;
import android.os.Environment;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerKCF;
import org.opencv.video.KalmanFilter;

public class Tracking {
    private Tracker tracker;
    private static int HEIGHT = 360;
    private static double A, B;
    private Size sz;
    private KalmanFilter kalmanFilter;
    private Mat prediction, measurement;
    private Rect last_rect;
    private int h, w;
    public int TAG;
    int count = 0;
    static String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/atest/";

    public Tracking() {
        tracker = TrackerKCF.create();
        measurement = new Mat(2, 1, CvType.CV_32F);
        TAG = 0;
    }

    public void init(Mat image, Rect rect, int h, int w) {
        last_rect = new Rect(rect);
        Mat resized = new Mat();
        sz = new Size((int) (HEIGHT * image.width() / image.height()), HEIGHT);
        A = h / (double) HEIGHT;
        this.h = h;
        B = w / (double) (HEIGHT * image.width() / image.height());
        this.w = w;
        Imgproc.resize(image, resized, sz);
        Rect2d roi = new Rect2d(rect.left / B, rect.top / A, rect.right / B - rect.left / B, rect.bottom / A - rect.top / A);
//        Imgproc.rectangle(resized, new Point(roi.x, roi.y),
//                new Point(roi.x + roi.width, roi.y + roi.height), new Scalar(0, 255, 0), 3);
//        Imgcodecs.imwrite(ALBUM_PATH + String.format("%04d.jpg", count++), resized);
        tracker.init(resized, roi);
//        resized.release();
    }

    public void init_KalmanFilter(double x, double y) {
        kalmanFilter = new KalmanFilter(4, 2, 0, CvType.CV_32F);
        Mat measurementMatrix = new Mat(2, 4, CvType.CV_32F);
        measurementMatrix.put(0, 0, new float[]{1, 0, 0, 0, 0, 1, 0, 0});
        kalmanFilter.set_measurementMatrix(measurementMatrix);
        Mat transitionMatrix = new Mat(4, 4, CvType.CV_32F);
        transitionMatrix.put(0, 0, new float[]{1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1});
        kalmanFilter.set_transitionMatrix(transitionMatrix);
        Mat processNoiseCov = new Mat(4, 4, CvType.CV_32F);
        processNoiseCov.put(0, 0, new float[]{0.03f, 0, 0, 0, 0, 0.03f, 0, 0, 0, 0, 0.03f, 0, 0, 0, 0, 0.03f});
        kalmanFilter.set_processNoiseCov(processNoiseCov);
        Mat statePre = new Mat(4, 1, CvType.CV_32F);
        statePre.put(0, 0, x);
        statePre.put(1, 0, y);
        statePre.put(2, 0, 0);
        statePre.put(3, 0, 0);
        kalmanFilter.set_statePre(statePre);
    }

    public Rect run(Mat image) {
        Mat resized = new Mat();
        Imgproc.resize(image, resized, sz);
//        image.release();
        Rect2d boundingBox = new Rect2d();
        boolean ok;
        ok = tracker.update(resized, boundingBox);
        resized.release();
//        Imgcodecs.imwrite(ALBUM_PATH + String.format("%04d-0.jpg", count++), image);

//        resized.release();
        if (ok && boundingBox.height != 0) {
            Rect rect = new Rect((int) (boundingBox.x * B), (int) (boundingBox.y * A), (int) (boundingBox.x * B + boundingBox.width * A), (int) (boundingBox.y * A + boundingBox.height * A));
//            Imgproc.rectangle(resized, new Point(boundingBox.x, boundingBox.y),
//                    new Point(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height), new Scalar(0, 255, 0), 3);
//            Imgcodecs.imwrite(ALBUM_PATH + String.format("%04d.jpg", count++), resized);
            kalmanFilter.predict();
            measurement.put(0, 0, rect.left);
            measurement.put(1, 0, rect.top);
            kalmanFilter.correct(measurement);
            last_rect = new Rect(rect);
            TAG = 0;
            return rect;
        } else if (TAG < 2) {
            prediction = kalmanFilter.predict();
            double x = prediction.get(0, 0)[0], y = prediction.get(1, 0)[0];
            Rect rect = new Rect((int) x, (int) y, (int) x + last_rect.width(), (int) y + last_rect.height());
            init(image, rect, this.h, this.w);
            last_rect = rect;
            measurement.put(0, 0, x);
            measurement.put(1, 0, y);
            kalmanFilter.correct(measurement);
            TAG += 1;
            count++;
            return rect;
        } else {
            count++;
            return null;
        }

    }
}
