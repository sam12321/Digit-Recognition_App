package com.example.digitrecognitionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.digitrecognitionapp.models.Classification;
import com.example.digitrecognitionapp.views.DrawModel;
import com.example.digitrecognitionapp.views.DrawView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by Bhavya Saxena (github-sam12321) on 8/8/20.
 */


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    Button predButton,clearButton;

    // List of all predictable labels
    public static final List<String> OUTPUT_LABELS = Collections.unmodifiableList(
            Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"));

    TextView resultTextView;    // text view which displays the result(i.e prediction made)

    // Views
    DrawModel drawModel;
    DrawView drawView;

    float mLastX,mLastY;     // for draw view

    private PointF mTmpPiont = new PointF();   // used for line drawing

    protected Interpreter tflite;
    //used for accessing and running our TensorFlow Lite model



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        predButton = (Button) findViewById(R.id.predButton);
        clearButton = (Button) findViewById(R.id.clearButton);
        resultTextView = (TextView) findViewById(R.id.resultTextView);

        drawView = (DrawView) findViewById(R.id.drawView);
        drawView.setOnTouchListener(this);
        drawModel = new DrawModel(28,28);    // because the input in our model is of 28x28 pixels
        drawView.setModel(drawModel);


        try {
             tflite= new Interpreter(loadModelFile(this));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }





    public void clear(View view){   // function for clear button

        drawView.reset();    // drawing is erased
        drawModel.clear();
        resultTextView.setText("");    // the previous result is cleared
        drawView.invalidate();

    }


    public void predict(View view){    // function for predict button

        Bitmap bitmap=drawView.getBitmap();

        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);

        float[][] result = new float[1][10];    // result of classification is stored here


        tflite.run(byteBuffer,result);


        List<Classification> recognitions =getSortedResult(result);
        resultTextView.setText(recognitions.toString());

    }


    public ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(3136);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[28 *28];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for(int i=0;i<pixels.length;i++){
        /* for (int pixel : pixels) {
            float rChannel = (pixel >> 16) & 0xFF;
            float gChannel = (pixel >> 8) & 0xFF;
            float bChannel = (pixel) & 0xFF;
            float pixelValue = (rChannel + gChannel + bChannel) / 3 / 255.f;

            byteBuffer.putFloat(pixelValue);
         */
            int pix = pixels[i];
            int b = pix & 0xff;
            byteBuffer.putFloat((float) ((0xff - b)/255.0));
        }
        return byteBuffer;
    }


// this method analyzes the result array and gets the predicted value
    private List<Classification> getSortedResult(float[][] resultsArray) {
        PriorityQueue<Classification> sortedResults = new PriorityQueue<>(
                2,
                new Comparator<Classification>() {
                    @Override
                    public int compare(Classification lhs, Classification rhs) {
                        return Float.compare(rhs.confidence, lhs.confidence);
                    }
                }
        );

        for (int i = 0; i < 10; ++i) {
            float confidence = resultsArray[0][i];
            if (confidence > 0.1f) {
                OUTPUT_LABELS.size();
                sortedResults.add(new Classification(OUTPUT_LABELS.get(i), confidence));
            }
        }

        return new ArrayList<>(sortedResults);
    }





    @Override
    // this method is called when the user receives a call or a message,
    protected void onPause() {
        drawView.onPause();
        super.onPause();
    }

    @Override
    // this method is called when the user resumes his Activity,
    protected void onResume() {
        drawView.onResume();
        super.onResume();
    }

    // this method loads our tflite model from assets folder

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("converted_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    // The following methods are used for drawing line on the draw view


    @Override
    // this detects which direction a user is moving their finger and draws a line accordingly in that
    // direction
    public boolean onTouch(View v, MotionEvent event) {
        //get the action and store it as an int
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        // now to detect, which direction the users finger is moving, and if they've stopped moving

        //if touched
        if (action == MotionEvent.ACTION_DOWN) {
            //begin drawing line
            processTouchDown(event);
            return true;
            //draw line in every direction the user moves
        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;
            //if finger is lifted, stop drawing
        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }

    //draw line down

    private void processTouchDown(MotionEvent event) {
        //calculate the x, y coordinates where the user has touched
        mLastX = event.getX();
        mLastY = event.getY();
        //user them to calcualte the position
        drawView.calcPos(mLastX, mLastY, mTmpPiont);
        //store them in memory to draw a line between the
        //difference in positions
        float lastConvX = mTmpPiont.x;
        float lastConvY = mTmpPiont.y;
        //and begin the line drawing
        drawModel.startLine(lastConvX, lastConvY);
    }

    // the main drawing function stores all the drawing position into the drawmodel object
    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        drawView.calcPos(x, y, mTmpPiont);
        float newConvX = mTmpPiont.x;
        float newConvY = mTmpPiont.y;
        drawModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        drawView.invalidate();

    }

    private void processTouchUp() {

        drawModel.endLine();

    }

}