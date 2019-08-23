package com.dji.videostreamdecodingsample.tools;

import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgproc.*;
import org.opencv.imgcodecs.*;

import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Yolo {

    static String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/yolov3/";
    private Net net = null;
    static String[] names = new String[]{"person"};

    public Yolo() {
        net = Dnn.readNetFromDarknet(ALBUM_PATH + "yolov3-tiny.cfg", ALBUM_PATH + "yolov3-tiny.weights");
    }


    public double test(String image_file) {
        long startTime = System.currentTimeMillis();
        net = Dnn.readNetFromDarknet(ALBUM_PATH + "yolov3-tiny.cfg", ALBUM_PATH + "yolov3-tiny.weights");
        Mat im = Imgcodecs.imread(image_file, Imgcodecs.IMREAD_COLOR);

        Mat frame = new Mat();
        Size sz1 = new Size(im.cols(), im.rows());
        Imgproc.resize(im, frame, sz1);

        Mat resized = new Mat();
        Size sz = new Size(416, 416);
        Imgproc.resize(im, resized, sz);

        float scale = 1.0F / 255.0F;
        Mat inputBlob = Dnn.blobFromImage(im, scale, sz, new Scalar(0), false, false);
        net.setInput(inputBlob, "data");
        System.out.println(net.getLayerNames());
        Mat detectionMat = net.forward("yolo_16");

        for (int i = 0; i < detectionMat.rows(); i++) {
            int probability_index = 5;
            int size = detectionMat.cols() * detectionMat.channels();

            float[] data = new float[size];
            detectionMat.get(i, 0, data);
            float confidence = -1;
            int objectClass = -1;
            for (int j = 0; j < detectionMat.cols(); j++) {
                if (j >= probability_index && confidence < data[j]) {
                    confidence = data[j];
                    objectClass = j - probability_index;
                }
            }

            if (confidence > 0.3) {
                float x = data[0];
                float y = data[1];
                float width = data[2];
                float height = data[3];
                float xLeftBottom = (x - width / 2) * frame.cols();
                float yLeftBottom = (y - height / 2) * frame.rows();
                float xRightTop = (x + width / 2) * frame.cols();
                float yRightTop = (y + height / 2) * frame.rows();

                System.out.println("Class: " + names[objectClass]);
                System.out.println("Confidence: " + confidence);
                System.out.println("ROI: " + xLeftBottom + " " + yLeftBottom + " " + xRightTop + " " + yRightTop + "\n");
                Imgproc.rectangle(frame, new Point(xLeftBottom, yLeftBottom),
                        new Point(xRightTop, yRightTop), new Scalar(0, 255, 0), 3);
            }
        }

        Imgcodecs.imwrite(ALBUM_PATH + "out.png", frame);
        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        return runTime / 1000.0;
    }

    public List<float[]> run(Mat im) {
        long startTime = System.currentTimeMillis();

        Mat frame = new Mat();
        Size sz1 = new Size(im.cols(), im.rows());
        Imgproc.resize(im, frame, sz1);

        Mat resized = new Mat();
        Size sz = new Size(416, 416);
        Imgproc.resize(im, resized, sz);

        float scale = 1.0F / 255.0F;
        Mat inputBlob = Dnn.blobFromImage(im, scale, sz, new Scalar(0), false, false);
        net.setInput(inputBlob, "data");
//        Log.e("Yolo-runtime", net.getUnconnectedOutLayersNames().toString());

        Mat detectionMat = net.forward("yolo_16");
        List<float[]> result = new ArrayList<>();
        for (int i = 0; i < detectionMat.rows(); i++) {
            int probability_index = 5;
            int size = detectionMat.cols() * detectionMat.channels();

            float[] data = new float[size];
            detectionMat.get(i, 0, data);
            float confidence = -1;
            for (int j = 0; j < detectionMat.cols(); j++) {
                if (j >= probability_index && confidence < data[j]) {
                    confidence = data[j];
                }
            }

            if (confidence > 0.5) {
                float x = data[0];
                float y = data[1];
                float width = data[2];
                float height = data[3];
                float xLeftBottom = (x - width / 2) * frame.cols();
                float yLeftBottom = (y - height / 2) * frame.rows();
                float xRightTop = (x + width / 2) * frame.cols();
                float yRightTop = (y + height / 2) * frame.rows();

                float[] one = {xLeftBottom, yLeftBottom, xRightTop, yRightTop};
                result.add(one);
//                Imgproc.rectangle(frame, new Point(xLeftBottom, yLeftBottom),
//                        new Point(xRightTop, yRightTop), new Scalar(0, 255, 0), 3);
            }
        }

        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        Log.e("Yolo-runtime", "" + runTime / 1000.0 + "  " + result.size());
        return result;
    }
}
