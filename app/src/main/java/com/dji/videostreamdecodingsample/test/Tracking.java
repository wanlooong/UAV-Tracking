package com.dji.videostreamdecodingsample.test;

import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.dji.videostreamdecodingsample.tools.Yolo;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerKCF;

import java.util.List;

public class Tracking {
    private Tracker tracker;
    private static int SIZE = 500;

    public Tracking() {
        tracker = TrackerKCF.create();
    }

    public void init(Mat image) {
//        Mat resized = new Mat();
//        Size sz = new Size((int) (SIZE * image.width() / image.height()), SIZE);
//        Imgproc.resize(image, resized, sz);
        Yolo yolov3=new Yolo();
        List<float[]> result = yolov3.run(image);
        float[] one=result.get(0);
        Rect2d roi = new Rect2d(new Point(one[0], one[1]), new Point(one[2], one[3]));
//        Rect2d roi = new Rect2d(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
        Log.e(this.getClass().getSimpleName(), roi.toString());
        tracker.init(image, roi);
//        resized.release();
    }

    public Mat run(Mat image) {
//        Mat resized = new Mat();
//        Size sz = new Size((int) (SIZE * image.width() / image.height()), SIZE);
//        Imgproc.resize(image, resized, sz);

        Rect2d rect2d = new Rect2d();
        boolean ok = tracker.update(image, rect2d);
        Log.e(this.getClass().getSimpleName(), ok+"");
        Imgproc.rectangle(image, rect2d.tl(), rect2d.br(), new Scalar(0, 255, 0), 3);
//        if (ok) {
//            Imgproc.rectangle(resized, rect2d.tl(), rect2d.br(), new Scalar(0, 255, 0), 3);
//            return resized;
//        } else {
//            return null;
//        }
        return image;
    }

    static public String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/yolov3/";
    public int HEIGHT=360;

    public void test() {
        Mat frame = Imgcodecs.imread(ALBUM_PATH + "test/0124.jpg", Imgcodecs.IMREAD_COLOR);
        Mat resized = new Mat();
        Size sz = new Size((int) (HEIGHT * frame.width() / frame.height()), HEIGHT);
        Imgproc.resize(frame, resized, sz);
        init(resized);

        for (int i = 125; i < 200; i++) {
            long tt1 = System.currentTimeMillis();
            frame = Imgcodecs.imread(ALBUM_PATH + String.format("test/%04d.jpg", i), Imgcodecs.IMREAD_COLOR);

            resized = new Mat();
            Imgproc.resize(frame, resized, sz);

            Mat tt=run(resized);
            Imgcodecs.imwrite(ALBUM_PATH + String.format("out/%04d.jpg", i), tt);

            frame.release();
            long tt2 = System.currentTimeMillis() - tt1;
            Log.e(this.getClass().getSimpleName(), i + " run...tt2:" + tt2);
        }
    }
}
