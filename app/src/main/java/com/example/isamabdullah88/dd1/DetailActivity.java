package com.example.isamabdullah88.dd1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.cvtColor;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";

    Canvas canvas;
    ImageView imageView;
    Button btnProcess;
    Bitmap bmpInput, bmpOutput;
    Mat matInput, matOutput;

//    static {
//        System.loadLibrary("MyLibs");
//    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        // get image view
        imageView = findViewById(R.id.imageView);
        btnProcess = findViewById(R.id.btnProcess);

        // get frame's path
        String photoPath = Environment.getExternalStorageDirectory() + "/frames/frame.png";

        // get the bitmap frame
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bmpInput = BitmapFactory.decodeFile(photoPath, options);

        imageView.setImageBitmap(bmpInput);

        // convert bitmap to mat for native functions
//        matInput = convertBitMap2Mat(bmpInput);
//        matOutput = new Mat(matInput.rows(), matInput.cols(), CvType.CV_8UC4);
        Log.d(TAG, "Ye bar bar call horaha hai ji");


        btnProcess.setOnClickListener(v -> {
            Frame frame = new Frame.Builder().setBitmap(bmpInput).build();
            Toast.makeText(DetailActivity.this, "Applying Detector!!!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Reached here!");
//            NativeClass.LandmarkDetection(matInput.getNativeObjAddr(), matOutput.getNativeObjAddr());
            SparseArray<Face> faces = MainActivity.face_detector.detect(frame);

            if (!MainActivity.face_detector.isOperational()) {
                Log.d(TAG, "Face detector dependency not met!");
                return;
            }

            // convert back mat to bitmap
//            bmpOutput = convertMat2BitMap(matOutput);

            // Drawing on the bitmap image
            Bitmap bmp_tmp = bmpInput.copy(Bitmap.Config.ARGB_8888, true);
            canvas = new Canvas(bmp_tmp);

            float scale = Math.min(imageView.getWidth()/bmpInput.getWidth(), imageView.getHeight()/bmpInput.getHeight());
            Log.d(TAG, String.valueOf(scale));
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(14);
//            int y = (int)(textHeight - metric.descent);
//            canvas.drawText("Hello There. It is convas drawn on Screen!!", 0, y, textPaint);
//            canvas.drawText("Hello There. It is convas drawn on Screen!!", 0,0, p);

            for (int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);
                int x = (int)face.getPosition().x;
                int y = (int)face.getPosition().y;
                int width = (int)face.getWidth() + x;
                int height = (int)face.getHeight() + y;
                canvas.drawRect(x, y, width, height, paint);

                for (Landmark landmark: face.getLandmarks()) {
                    float cx = landmark.getPosition().x * scale;
                    float cy = landmark.getPosition().y * scale;
                    canvas.drawPoint(cx, cy, paint);
                    Log.d(TAG, "Points:");
                    Log.d(TAG, String.valueOf(cx) + " , " + String.valueOf(cy));
                }
            }

            imageView.setImageBitmap(bmp_tmp);
            Log.d(TAG, "Reached here! too");
        });
    }

    Bitmap convertMat2BitMap(Mat img) {
        int width = img.width();
        int height = img.height();

        Bitmap bmp;
        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Mat tmp;
        tmp = img.channels() == 1 ? new Mat(width, height, CvType.CV_8UC4, new Scalar(1)) : new Mat(width, height, CvType.CV_8UC4);
        try {
            if (img.channels() == 3)
                cvtColor(img, tmp, Imgproc.COLOR_RGB2BGRA);
            else if (img.channels() == 1)
                cvtColor(img, tmp, Imgproc.COLOR_GRAY2BGRA);

            Utils.matToBitmap(tmp, bmp);
        } catch (CvException e) {
            Log.d(TAG, e.getMessage());
        }
        return bmp;
    }

    Mat convertBitMap2Mat(Bitmap rgbaImage) {
        // convert Java Bitmap into OpenCV Mat
        Mat rgbaMat = new Mat(rgbaImage.getHeight(), rgbaImage.getWidth(), CvType.CV_8UC4);
        Bitmap bmp32 = rgbaImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, rgbaMat);

        Mat rgbMat = new Mat(rgbaImage.getHeight(), rgbaImage.getWidth(), CvType.CV_8UC4);
        cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2BGR, 3);
        return rgbMat;
    }
}
