package com.example.fachprojekt.mindcuber;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.RequestBodyEntity;

import org.json.JSONException;
import org.json.JSONObject;
import org.kociemba.twophase.Search;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;
    Mat mRgbaBlur;

    private static final boolean testing = false;

    public static boolean WAITFORME = false;
    public static boolean stopped = false;

    private static final int RED = 1;
    private static final int YELLOW = 2;
    private static final int BLUE = 3;
    private static final int WHITE = 4;
    private static final int ORANGE = 5;
    private static final int GREEN = 6;

    private static final int INORDER = 7; // front, top, bottom, right
    private static final int REVERSE = 8; // backside, left

    private ArrayList<Point> listOfRectsFirst = new ArrayList<Point>();
    private ArrayList<Point> listOfRectsSecond = new ArrayList<Point>();
    private int x = 80;
    private int y = 80;

    private double widthRect = (380 - 80) / 3;
    private double heightRect = (365 - 80) / 3;

    private int[] side = new int[9];
    private int[] colors = new int[9];

    public boolean SCANNING = false;
    public char scanningSide = 'U';
    public boolean legal = false;

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);


        listOfRectsFirst.add(new Point(x, x));
        listOfRectsSecond.add(new Point(x + widthRect, x + heightRect));
        listOfRectsFirst.add(new Point(x + widthRect, x));
        listOfRectsSecond.add(new Point(x + widthRect * 2, x + heightRect));
        listOfRectsFirst.add(new Point(x + widthRect * 2, x));
        listOfRectsSecond.add(new Point(x + widthRect * 3, x + heightRect));
        listOfRectsFirst.add(new Point(x, x + heightRect));
        listOfRectsSecond.add(new Point(x + widthRect, x + heightRect * 2));
        listOfRectsFirst.add(new Point(x + widthRect, x + heightRect));
        listOfRectsSecond.add(new Point(x + widthRect * 2, x + heightRect * 2));
        listOfRectsFirst.add(new Point(x + widthRect * 2, x + heightRect));
        listOfRectsSecond.add(new Point(x + widthRect * 3, x + heightRect * 2));
        listOfRectsFirst.add(new Point(x, x + heightRect * 2));
        listOfRectsSecond.add(new Point(x + widthRect, x + heightRect * 3));
        listOfRectsFirst.add(new Point(x + widthRect, x + heightRect * 2));
        listOfRectsSecond.add(new Point(x + widthRect * 2, x + heightRect * 3));
        listOfRectsFirst.add(new Point(x + widthRect * 2, x + heightRect * 2));
        listOfRectsSecond.add(new Point(x + widthRect * 3, x + heightRect * 3));

        // Scanreihenfolge festlegen (Foto -> Analyse -> Drehen/Flippen -> repeat)
        // String fuer Algorithmus zusammenbauen
        Thread sideThread = new Thread() {
            public void run() {
                boolean end = false;
                SCANNING = true;
                int[][] cube = new int[6][9];
                int i = 0;
                while (!end) {
                    if (legal) {
                        SCANNING = false;
                        legal = false;
                        int status = (scanningSide == 'B') ? REVERSE : INORDER;
                        String str = "";
                        for (int x : colors) {
                            str += x;
                        }
                        String flipped = (scanningSide == 'L' || scanningSide == 'R') ? str : flipColors(colors, status);
                        for (int k = 0; k < flipped.length(); k++) {
                            if (i < 6) {
                                cube[i][k] = Integer.parseInt(String.valueOf(flipped.charAt(k)));
                            }
                        }
                        i++;
                        System.out.println(colors.toString());

                        String uri = "http://192.168.1.104:10001/mindcuber/";
                        JSONObject obj = new JSONObject();

                        try {
                            switch (scanningSide) {
                                case 'U': {
                                    scanningSide = 'B';
                                    // send flip
                                    obj.put("sequence", "F");
                                    uri += "scan";
                                    break;
                                }
                                case 'B': {
                                    scanningSide = 'D';
                                    // send flip
                                    obj.put("sequence", "F");
                                    uri += "scan";
                                    break;
                                }
                                case 'D': {
                                    scanningSide = 'F';
                                    // send flip
                                    obj.put("sequence", "F");
                                    uri += "scan";
                                    break;
                                }
                                case 'F': {
                                    scanningSide = 'L';
                                    // send rotate 270
                                    // send flip
                                    obj.put("sequence", "rF");
                                    uri += "scan";
                                    break;
                                }
                                case 'L': {
                                    scanningSide = 'R';
                                    // send flip
                                    // send flip
                                    obj.put("sequence", "FF");
                                    uri += "scan";
                                    break;
                                }
                                case 'R': {
                                    scanningSide = ' ';
                                    obj.put("sequence", "RFR");
                                    uri += "scan";
                                    break;
                                }
                                default: {
                                    end = true;
                                    // myCube -> cube und dann berechnen
                                    // U,R,F,D,L,B
                                    int[][] facelet = new int[6][9];
                                    // U
                                    for (int j = 0; j < cube[0].length; j++) {
                                        facelet[0][j] = cube[0][j];
                                    }
                                    // R
                                    for (int j = 0; j < cube[5].length; j++) {
                                        facelet[1][j] = cube[5][j];
                                    }
                                    // F
                                    for (int j = 0; j < cube[3].length; j++) {
                                        facelet[2][j] = cube[3][j];
                                    }
                                    // D
                                    for (int j = 0; j < cube[2].length; j++) {
                                        facelet[3][j] = cube[2][j];
                                    }
                                    // L
                                    for (int j = 0; j < cube[4].length; j++) {
                                        facelet[4][j] = cube[4][j];
                                    }
                                    // B
                                    for (int j = 0; j < cube[1].length; j++) {
                                        facelet[5][j] = cube[1][j];
                                    }

                                    int maxDepth = 24;
                                    int maxTime = 5;
                                    boolean useSeparator = false;

                                    StringBuffer s = new StringBuffer(54);

                                    // default initialization
                                    for (int j = 0; j < 54; j++) {
                                        s.insert(j, 'B');
                                    }

                                    // read the 54 facelets
                                    for (int l = 0; l < 6; l++) {
                                        for (int j = 0; j < 9; j++) {
                                            if (facelet[l][j] == facelet[0][4])
                                                s.setCharAt(9 * l + j, 'U');
                                            if (facelet[l][j] == facelet[1][4])
                                                s.setCharAt(9 * l + j, 'R');
                                            if (facelet[l][j] == facelet[2][4])
                                                s.setCharAt(9 * l + j, 'F');
                                            if (facelet[l][j] == facelet[3][4])
                                                s.setCharAt(9 * l + j, 'D');
                                            if (facelet[l][j] == facelet[4][4])
                                                s.setCharAt(9 * l + j, 'L');
                                            if (facelet[l][j] == facelet[5][4])
                                                s.setCharAt(9 * l + j, 'B');
                                        }
                                    }

                                    String cubeString = s.toString();
                                    // Algorithmus terminiert nicht (RAM?), daher nur Ausgabe und Abbruch
                                    // usgabe dann in Eclipse berechnen und mittels GET an den Webservice schicken
                                    System.out.println("CUBESTRING: " + cubeString);
                                    //String result = Search.solution(cubeString, maxDepth, maxTime, useSeparator);
                                    //result = replace(result);
                                    //System.out.println(result);
                                    //obj.put("sequence", result);
                                    uri += "solve";
                                    System.exit(-1);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            String body = obj.toString();
                            System.out.println("sending: " + body);
                            RequestBodyEntity requestBody = Unirest.post(uri).body(body);
                            HttpResponse<String> response = requestBody.asString();
                            String returnBody = response.getBody().toString();
                            System.out.println("returned: " + returnBody);
                        } catch (UnirestException e) {
                            e.printStackTrace();
                        }
                        SCANNING = !end;
                    }
                }
            }

            private String flipColors(int[] colors, int status) {
                switch (status) {
                    case INORDER: {
                        String colorsNew = String.valueOf(colors[6]) + String.valueOf(colors[3]) + String.valueOf(colors[0]) + String.valueOf(colors[7]) + String.valueOf(colors[4]) + String.valueOf(colors[1]) + String.valueOf(colors[8]) + String.valueOf(colors[5]) + String.valueOf(colors[2]);
                        return colorsNew;
                    }
                    case REVERSE: {
                        String colorsNew = String.valueOf(colors[2]) + String.valueOf(colors[5]) + String.valueOf(colors[8]) + String.valueOf(colors[1]) + String.valueOf(colors[4]) + String.valueOf(colors[7]) + String.valueOf(colors[0]) + String.valueOf(colors[3]) + String.valueOf(colors[6]);
                        return colorsNew;
                    }
                    default: {
                        System.out.println("SHOULD NOT HAPPEN");
                        break;
                    }
                }
                return "";
            }
        };
        sideThread.start();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //Receive Image Data when the camera preview starts on your screen
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(height, width, CvType.CV_8UC4);
        mRgbaBlur = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();


        int width = mRgba.width();
        int height = mRgba.height();
        int sum = 0;

        double gamma = 0.25;

        Imgproc.blur(mRgba, mRgbaBlur, new Size(height / 40, width / 40));
        LinkedList<Point> greenTokens = findTokens(mRgbaBlur, GREEN);
        LinkedList<Point> redTokens = findTokens(mRgbaBlur, RED);
        LinkedList<Point> yellowTokens = findTokens(mRgbaBlur, YELLOW);
        LinkedList<Point> whiteTokens = findTokens(mRgbaBlur, WHITE);
        LinkedList<Point> orangeTokens = findTokens(mRgbaBlur, ORANGE);
        LinkedList<Point> blueTokens = findTokens(mRgbaBlur, BLUE);

        Point point1 = new Point(80, 80);
        Point point2 = new Point(380, 365);

        Imgproc.rectangle(mRgba, point1, point2, new Scalar(0, 0, 255));

        Imgproc.rectangle(mRgba, point1, new Point(x + widthRect, y + heightRect), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect, y), new Point(x + widthRect * 2, y + heightRect), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect * 2, y), new Point(x + widthRect * 3, y + heightRect), new Scalar(0, 0, 255));

        Imgproc.rectangle(mRgba, new Point(x, y + heightRect), new Point(x + widthRect, y + heightRect * 2), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect, y + heightRect), new Point(x + widthRect * 2, y + heightRect * 2), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect * 2, y + heightRect), new Point(x + widthRect * 3, y + heightRect * 2), new Scalar(0, 0, 255));

        Imgproc.rectangle(mRgba, new Point(x, y + heightRect * 2), new Point(x + widthRect, y + heightRect * 3), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect, y + heightRect * 2), new Point(x + widthRect * 2, y + heightRect * 3), new Scalar(0, 0, 255));
        Imgproc.rectangle(mRgba, new Point(x + widthRect * 2, y + heightRect * 2), new Point(x + widthRect * 3, y + heightRect * 3), new Scalar(0, 0, 255));

        for (Point point : redTokens) {
            checkColor(point, RED);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(255, 0, 0), 3);
        }
        for (Point point : blueTokens) {
            checkColor(point, BLUE);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(0, 0, 255), 3);
        }
        for (Point point : yellowTokens) {
            checkColor(point, YELLOW);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(255, 255, 0), 3);
        }
        for (Point point : orangeTokens) {
            checkColor(point, ORANGE);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(255, 165, 0), 3);
        }
        for (Point point : greenTokens) {
            checkColor(point, GREEN);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(0, 255, 0), 3);
        }
        for (Point point : whiteTokens) {
            checkColor(point, WHITE);
            Imgproc.circle(mRgba, new Point(point.x, point.y), (25), new Scalar(0, 0, 0), 3);
        }

        // Rotate mRgba 90 degrees

        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, mRgba, 1);

        //Imgproc.resize(mRgbaBlur,mRgbaBlur, mRgba.size());

        if (SCANNING) {
            LinkedList<Point> combined = new LinkedList<>();
            combined.addAll(greenTokens);
            combined.addAll(redTokens);
            combined.addAll(yellowTokens);
            combined.addAll(whiteTokens);
            combined.addAll(orangeTokens);
            combined.addAll(blueTokens);
            legal = checkSide(combined);
            SCANNING = !legal;
            System.out.println(legal);
        }
        // This function must return
        return mRgba;
    }

    private static LinkedList<Point> findTokens(Mat sourceImage, int tokenColor) {
        Mat boardThreshold = performThresholdForColor(sourceImage, tokenColor);
        LinkedList<MatOfPoint> contours = new LinkedList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(boardThreshold, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        LinkedList<Point> minCircles = new LinkedList<Point>();
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > 4000) {
                Point center = new Point();
                float[] radius = new float[1];
                Imgproc.minEnclosingCircle(new MatOfPoint2f(contours.get(i)
                        .toArray()), center, radius);
                //globalradius = radius[0];
                minCircles.push(center);
            }
        }
        return minCircles;
    }

    // Returns a binary image of the board based on the specified color
    private static Mat performThresholdForColor(Mat image, int color) {
        Mat imageHSV = new Mat();
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_RGB2HSV);
        Mat threshold = new Mat();
        Mat threshold1 = new Mat();
        Mat threshold2 = new Mat();


        if (color == RED) {
            Core.inRange(imageHSV, new Scalar(141, 100, 100), new Scalar(179, 255, 255), threshold1);
            Core.inRange(imageHSV, new Scalar(0, 100, 100), new Scalar(5, 255, 255), threshold2);
            Core.bitwise_or(threshold1, threshold2, threshold);
        } else if (color == YELLOW) {
            Core.inRange(imageHSV, new Scalar(25, 100, 80), new Scalar(34, 255, 255), threshold);
        } else if (color == GREEN) {
            Core.inRange(imageHSV, new Scalar(39, 100, 100), new Scalar(100, 255, 255), threshold);
        } else if (color == BLUE) {
            Core.inRange(imageHSV, new Scalar(103, 160, 60), new Scalar(140, 255, 255), threshold);
        } else if (color == WHITE) {
            Core.inRange(imageHSV, new Scalar(0, 0, 150), new Scalar(255, 50, 255), threshold);
        } else if (color == ORANGE) {
            Core.inRange(imageHSV, new Scalar(9, 100, 100), new Scalar(20, 255, 255), threshold);
        }
        return threshold;
    }

    private boolean checkColor(Point p, int j) {
        for (int i = 0; i < listOfRectsFirst.size(); i++) {
            if (p.x > listOfRectsFirst.get(i).x && p.y > listOfRectsFirst.get(i).y &&
                    p.x < listOfRectsSecond.get(i).x && p.y < listOfRectsSecond.get(i).y) {
                colors[i] = j;
            }
        }
        return false;
    }

    public boolean checkBound(Point p) {
        double widthRect = (380 - 80) / 3;
        double heightRect = (365 - 80) / 3;
        int x = 80;

        for (int i = 0; i < listOfRectsFirst.size(); i++) {
            if (p.x > listOfRectsFirst.get(i).x && p.y > listOfRectsFirst.get(i).y &&
                    p.x < listOfRectsSecond.get(i).x && p.y < listOfRectsSecond.get(i).y) {
                side[i] = side[i] + 1;
            }
        }
        return true;
    }

    public boolean checkSide(LinkedList<Point> list) {
        //initiailsieren
        for (int i = 0; i < side.length; i++) {
            side[i] = 0;
            colors[i] = 0;
        }

        //check
        for (Point p : list) {
            checkBound(p);
        }

        // one entry per rectangle
        for (int i : side) {
            if (i != 1) {
                System.out.println("too many in one rect");
                return false;
            }
        }

        //sum
        int sum = 0;
        for (int i : side) {
            sum += i;
        }
        System.out.println("sum" + sum);

        return sum == 9;
    }


    private static String replace(String result) {
        result = result.replace("U2", "UU");
        result = result.replace("D2", "DD");
        result = result.replace("L2", "LL");
        result = result.replace("R2", "RR");
        result = result.replace("F2", "FF");
        result = result.replace("B2", "BB");

        result = result.replace("U'", "u");
        result = result.replace("D'", "d");
        result = result.replace("L'", "l");
        result = result.replace("R'", "r");
        result = result.replace("F'", "f");
        result = result.replace("B'", "b");

        result = result.replace(" ", "");

        return result;
    }
}
