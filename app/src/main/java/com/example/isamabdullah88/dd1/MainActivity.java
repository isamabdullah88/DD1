package com.example.isamabdullah88.dd1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.opencv.imgproc.Imgproc.cvtColor;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final float LEFT_PROB_THRESH = 0.6f;
    private static final float RIGHT_PROB_THRESH = 0.6f;

    public int start_processing = 0;

    public int num_left_close = 0;
    public int num_right_close = 0;

    static FaceDetector face_detector;
    JavaCameraView javaCameraView;
    Mat mRgba;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        face_detector = new FaceDetector.Builder(getApplicationContext())
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE).build();

        javaCameraView = findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.i(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
            Log.d(TAG, String.valueOf(OpenCVLoader.initDebug()));
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if (start_processing == 0) {
            return mRgba;
        } else {
//            Log.d(TAG,inputFrame.rgba().getClass().getName());
            Bitmap bmpInput = convertMat2BitMap(mRgba);
            Log.d(TAG, String.valueOf(bmpInput.getHeight()));
            Log.d(TAG, String.valueOf(bmpInput.getWidth()));
//            saveImage(mRgba);
            Frame frame = new Frame.Builder().setBitmap(bmpInput).build();
//            Toast.makeText(MainActivity.this, "Applying Detector!!!", Toast.LENGTH_SHORT).show();
//            NativeClass.LandmarkDetection(matInput.getNativeObjAddr(), matOutput.getNativeObjAddr());
            SparseArray<Face> faces = face_detector.detect(frame);
            Log.d(TAG, String.valueOf(faces.size()));

            if (!MainActivity.face_detector.isOperational()) {
                Log.d(TAG, "Face detector dependency not met!");
                return convertBitMap2Mat(bmpInput);
            }

            for (int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);

                float left_open_prob = face.getIsLeftEyeOpenProbability();
                float right_open_prob = face.getIsRightEyeOpenProbability();
                Imgproc.putText(mRgba, String.valueOf(left_open_prob), new Point(20, 50),
                        Core.FONT_HERSHEY_SIMPLEX , 1, new Scalar(0, 0, 0), 4);

                if (left_open_prob < LEFT_PROB_THRESH)
                    num_left_close += 1;
                else
                    num_left_close = 0;
                if (right_open_prob < RIGHT_PROB_THRESH)
                    num_right_close += 1;
                else
                    num_right_close = 0;

                if (num_left_close > 10 && num_right_close > 10)
                    Imgproc.putText(mRgba, "Warning!!!!", new Point(20, 100),
                            Core.FONT_ITALIC, 2, new Scalar(0, 0, 0), 5);

                for (Landmark landmark: face.getLandmarks()) {
                    double cx = landmark.getPosition().x;
                    double cy = landmark.getPosition().y;
                    Point p1 = new Point(cx, cy);
                    Point p2 = new Point(cx+20, cy+20);
                    Log.d(TAG, "reached here lay bawa");
                    Imgproc.rectangle(mRgba, p1, p2, new Scalar(0, 255, 0, 255));
                    Log.d(TAG, "Points:");
                    Log.d(TAG, String.valueOf(cx) + " , " + String.valueOf(cy));
                }
            }
//            Log.d(TAG, "Reached here!");
//            Log.d(TAG, convertBitMap2Mat(bmpInput).getClass().getName());
//            return convertBitMap2Mat(bmpInput);
            return mRgba;
        }
    }

    private void saveImage(Mat subImg) {
        Bitmap bmp = null;

        try {
            bmp = Bitmap.createBitmap(subImg.cols(), subImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(subImg, bmp);
        } catch (CvException e) {
            Log.d(TAG, e.getMessage());
        }

        subImg.release();

        FileOutputStream out = null;

        String filename = "frame.png";

        File sd = new File(Environment.getExternalStorageDirectory() + "/frames");

        File sd1 = new File(Environment.getExternalStorageDirectory() + "/shape_predictor_68_face_landmarks.dat");
        Log.d(TAG, String.valueOf(sd1.exists()));

        boolean success = true;

        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (success) {
            File dest = new File(sd, filename);

            try {
                out = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        Log.d(TAG, "OK!!!");
                    }
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage() + "Error");
                    e.printStackTrace();
                }
            }
        } else
            Log.d(TAG, "Failed to create directory on external storage");
    }

    public void onClickGo(View view) {
        start_processing = 1;
//        saveImage(mRgba);
//        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
//        startActivity(intent);
    }

    Bitmap convertMat2BitMap(Mat img) {
//        int width = img.width();
//        int height = img.height();
//
//        Bitmap bmp;
//        bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
//        Mat tmp;
//        tmp = img.channels() == 1 ? new Mat(height, width, CvType.CV_8UC4, new Scalar(1)) : new Mat(height, width, CvType.CV_8UC4);
//        try {
//            if (img.channels() == 3)
//                cvtColor(img, tmp, Imgproc.COLOR_RGB2BGRA);
//            else if (img.channels() == 1)
//                cvtColor(img, tmp, Imgproc.COLOR_GRAY2BGRA);
//
//            Utils.matToBitmap(tmp, bmp);
//        } catch (CvException e) {
//            Log.d(TAG, e.getMessage());
//        }
//        return bmp;

        Bitmap bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bmp);
        return bmp;
    }



    Mat convertBitMap2Mat(Bitmap rgbaImage) {
        // convert Java Bitmap into OpenCV Mat
        Mat rgbaMat = new Mat(rgbaImage.getHeight(), rgbaImage.getWidth(), CvType.CV_8UC3);
        Bitmap bmp32 = rgbaImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(rgbaImage, rgbaMat);
//        Log.d(TAG, "rgbaMat type:");
//        Log.d(TAG, rgbaMat.getClass().getName());
//        Mat rgbMat = new Mat(rgbaImage.getHeight(), rgbaImage.getWidth(), CvType.CV_8UC4);
//        cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2BGR, 3);
        return rgbaMat;
    }

}
