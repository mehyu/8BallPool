package com.letsgo.goated;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import java.util.Objects;

import static com.letsgo.goated.App.CHANNEL_ID;

import myproject.R;

public class ViewService extends Service {
    private WindowManager windowManager;
    private SensorManager sensorManager;
    private View view;
    private RelativeLayout board;
    private Normal normal;
    private Trickshot trickshot;
    private NineBall nineBall;
    private Button btn_normal, btn_trickshot, btn_trickshot_second_line, btn_nineBall;
    private MediaPlayer mediaPlayer;

    private float accel, accelCurrent, accelLast;

    boolean secondLine = false;
    private boolean isGuideVisible = false;

    private SharedPreferences prefs;
    private int boardWidth, boardHeight, canvasWidth, canvasHeight, canvasMarginTop;
    private int stepSize = 5;
    private int opacityPercent = 100;
    private int colorIndex = 0;
    private final int[] colors = {0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFE000, 0xFFFF00FF};
    private final String[] colorNames = {"WHITE", "RED", "GREEN", "BLUE", "YELLOW", "MAGENTA"};

    private LinearLayout layoutAdjustPanel;
    private TextView txtBoardW, txtBoardH, txtCanvasW, txtCanvasH, txtMargin, txtOpacity;
    private Button btnCycleColor;
    private Button btnStep1, btnStep5, btnStep20;

    public ViewService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loadDimensions() {
        prefs = getSharedPreferences("StealthPrefs", MODE_PRIVATE);
        boardWidth = prefs.getInt("boardWidth", (int) getResources().getDimension(R.dimen.boardWidth));
        boardHeight = prefs.getInt("boardHeight", (int) getResources().getDimension(R.dimen.boardHeight));
        canvasWidth = prefs.getInt("canvasWidth", (int) getResources().getDimension(R.dimen.canvasWidth));
        canvasHeight = prefs.getInt("canvasHeight", (int) getResources().getDimension(R.dimen.canvasHeight));
        canvasMarginTop = prefs.getInt("canvasMarginTop", (int) getResources().getDimension(R.dimen.canvasMarginTop));
        stepSize = prefs.getInt("stepSize", 5);
        opacityPercent = prefs.getInt("opacityPercent", 100);
        colorIndex = prefs.getInt("colorIndex", 0);
    }

    private void saveDimensions() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("boardWidth", boardWidth);
        editor.putInt("boardHeight", boardHeight);
        editor.putInt("canvasWidth", canvasWidth);
        editor.putInt("canvasHeight", canvasHeight);
        editor.putInt("canvasMarginTop", canvasMarginTop);
        editor.putInt("stepSize", stepSize);
        editor.putInt("opacityPercent", opacityPercent);
        editor.putInt("colorIndex", colorIndex);
        editor.apply();
    }

    private void resetDimensions() {
        boardWidth = (int) getResources().getDimension(R.dimen.boardWidth);
        boardHeight = (int) getResources().getDimension(R.dimen.boardHeight);
        canvasWidth = (int) getResources().getDimension(R.dimen.canvasWidth);
        canvasHeight = (int) getResources().getDimension(R.dimen.canvasHeight);
        canvasMarginTop = (int) getResources().getDimension(R.dimen.canvasMarginTop);
        stepSize = 5;
        opacityPercent = 100;
        colorIndex = 0;
        saveDimensions();
        updateOverlayDimensions();
        updateGuidelinesProperties();
        updateStepButtonStyles();
    }

    private void updateOverlayDimensions() {
        ViewGroup.LayoutParams boardParams = board.getLayoutParams();
        if (boardParams != null) {
            boardParams.width = boardWidth;
            boardParams.height = boardHeight;
            board.setLayoutParams(boardParams);
        }

        updateCanvasParams(normal);
        updateCanvasParams(trickshot);
        updateCanvasParams(nineBall);

        txtBoardW.setText(String.valueOf(boardWidth));
        txtBoardH.setText(String.valueOf(boardHeight));
        txtCanvasW.setText(String.valueOf(canvasWidth));
        txtCanvasH.setText(String.valueOf(canvasHeight));
        txtMargin.setText(String.valueOf(canvasMarginTop));
    }

    private void updateGuidelinesProperties() {
        int activeColor = colors[colorIndex];
        normal.setGuideColor(activeColor);
        trickshot.setGuideColor(activeColor);
        nineBall.setGuideColor(activeColor);

        normal.setGuideOpacity(opacityPercent);
        trickshot.setGuideOpacity(opacityPercent);
        nineBall.setGuideOpacity(opacityPercent);

        txtOpacity.setText(opacityPercent + "%");
        btnCycleColor.setText(colorNames[colorIndex]);
    }

    private void updateStepButtonStyles() {
        btnStep1.setTextColor(stepSize == 1 ? 0xFF00FF00 : 0xFFFFFFFF);
        btnStep5.setTextColor(stepSize == 5 ? 0xFF00FF00 : 0xFFFFFFFF);
        btnStep20.setTextColor(stepSize == 20 ? 0xFF00FF00 : 0xFFFFFFFF);
    }

    private void updateCanvasParams(View canvasView) {
        RelativeLayout.LayoutParams canvasParams = (RelativeLayout.LayoutParams) canvasView.getLayoutParams();
        if (canvasParams != null) {
            canvasParams.width = canvasWidth;
            canvasParams.height = canvasHeight;
            canvasParams.topMargin = canvasMarginTop;
            canvasView.setLayoutParams(canvasParams);
        }
    }

    private void setupAdjustmentButton(int buttonId, final int direction, final String type) {
        view.findViewById(buttonId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                int actualDelta = direction * stepSize;
                if (type.equals("board_w")) {
                    boardWidth += actualDelta;
                } else if (type.equals("board_h")) {
                    boardHeight += actualDelta;
                } else if (type.equals("canvas_w")) {
                    canvasWidth += actualDelta;
                } else if (type.equals("canvas_h")) {
                    canvasHeight += actualDelta;
                } else if (type.equals("margin")) {
                    canvasMarginTop += actualDelta;
                }
                updateOverlayDimensions();
            }
        });
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();

        view = LayoutInflater.from(this).inflate(R.layout.main, null);

        board = view.findViewById(R.id.board);
        normal = view.findViewById(R.id.normal);
        trickshot = view.findViewById(R.id.trickshot);
        nineBall = view.findViewById(R.id.nineBall);

        btn_normal = view.findViewById(R.id.btn_normal);
        btn_trickshot = view.findViewById(R.id.btn_trickshot);
        btn_trickshot_second_line = view.findViewById(R.id.btn_trickshot_second_ine);
        btn_nineBall = view.findViewById(R.id.btn_nineBall);

        Button btn_hide = view.findViewById(R.id.btn_hide);

        mediaPlayer = MediaPlayer.create(this, R.raw.touch);

        btn_normal.setOnClickListener(showNormal);
        btn_trickshot.setOnClickListener(showTrickshot);
        btn_trickshot_second_line.setOnClickListener(showSecondLine);
        btn_nineBall.setOnClickListener(showNineBall);
        btn_hide.setOnClickListener(hide);

        loadDimensions();

        layoutAdjustPanel = view.findViewById(R.id.layout_adjust_panel);
        txtBoardW = view.findViewById(R.id.txt_board_w);
        txtBoardH = view.findViewById(R.id.txt_board_h);
        txtCanvasW = view.findViewById(R.id.txt_canvas_w);
        txtCanvasH = view.findViewById(R.id.txt_canvas_h);
        txtMargin = view.findViewById(R.id.txt_margin);
        txtOpacity = view.findViewById(R.id.txt_opacity);
        btnCycleColor = view.findViewById(R.id.btn_cycle_color);
        btnStep1 = view.findViewById(R.id.btn_step_1);
        btnStep5 = view.findViewById(R.id.btn_step_5);
        btnStep20 = view.findViewById(R.id.btn_step_20);

        updateOverlayDimensions();
        updateGuidelinesProperties();
        updateStepButtonStyles();

        Button btnAdjust = view.findViewById(R.id.btn_adjust);
        btnAdjust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                if (layoutAdjustPanel.getVisibility() == View.VISIBLE) {
                    layoutAdjustPanel.setVisibility(View.GONE);
                } else {
                    layoutAdjustPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        setupAdjustmentButton(R.id.btn_dec_board_w, -1, "board_w");
        setupAdjustmentButton(R.id.btn_inc_board_w, 1, "board_w");
        setupAdjustmentButton(R.id.btn_dec_board_h, -1, "board_h");
        setupAdjustmentButton(R.id.btn_inc_board_h, 1, "board_h");
        setupAdjustmentButton(R.id.btn_dec_canvas_w, -1, "canvas_w");
        setupAdjustmentButton(R.id.btn_inc_canvas_w, 1, "canvas_w");
        setupAdjustmentButton(R.id.btn_dec_canvas_h, -1, "canvas_h");
        setupAdjustmentButton(R.id.btn_inc_canvas_h, 1, "canvas_h");
        setupAdjustmentButton(R.id.btn_dec_margin, -1, "margin");
        setupAdjustmentButton(R.id.btn_inc_margin, 1, "margin");

        btnStep1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                stepSize = 1;
                updateStepButtonStyles();
            }
        });

        btnStep5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                stepSize = 5;
                updateStepButtonStyles();
            }
        });

        btnStep20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                stepSize = 20;
                updateStepButtonStyles();
            }
        });

        view.findViewById(R.id.btn_dec_opacity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                if (opacityPercent > 10) {
                    opacityPercent -= 10;
                    updateGuidelinesProperties();
                }
            }
        });

        view.findViewById(R.id.btn_inc_opacity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                if (opacityPercent < 100) {
                    opacityPercent += 10;
                    updateGuidelinesProperties();
                }
            }
        });

        btnCycleColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                colorIndex = (colorIndex + 1) % colors.length;
                updateGuidelinesProperties();
            }
        });

        view.findViewById(R.id.btn_toggle_guide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                isGuideVisible = !isGuideVisible;
                if (isGuideVisible) {
                    board.setBackgroundColor(0x60FFFFFF);
                } else {
                    board.setBackgroundColor(0x00000000);
                }
            }
        });

        view.findViewById(R.id.btn_save_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                saveDimensions();
                isGuideVisible = false;
                board.setBackgroundColor(0x00000000);
                layoutAdjustPanel.setVisibility(View.GONE);
            }
        });

        view.findViewById(R.id.btn_reset_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                resetDimensions();
            }
        });

        view.findViewById(R.id.btn_close_panel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                isGuideVisible = false;
                board.setBackgroundColor(0x00000000);
                layoutAdjustPanel.setVisibility(View.GONE);
            }
        });

        layoutParams();
        sensorManager();
    }

    private void showNormal() {
        trickshot.setVisibility(View.GONE);
        nineBall.setVisibility(View.GONE);
        normal.setVisibility(View.VISIBLE);

        btn_trickshot_second_line.setVisibility(View.GONE);

        float widthCanvas = canvasWidth;
        float heightCanvas = canvasHeight;

        normal.setPositionCircle((widthCanvas / 2f), (heightCanvas / 2f));
        normal.setRotation(0);
    }

    private void showTrickshot() {
        normal.setVisibility(View.GONE);
        nineBall.setVisibility(View.GONE);
        trickshot.setVisibility(View.VISIBLE);

        btn_trickshot_second_line.setVisibility(View.VISIBLE);

        float widthCanvas = canvasWidth;
        float heightCanvas = canvasHeight;

        // Bug fix
        trickshot.resetLines();

        trickshot.setPositionCircleOne((widthCanvas / 2f) - 200, (heightCanvas / 2f));
        trickshot.setPositionCircleTwo((widthCanvas / 2f) + 200, (heightCanvas / 2f));

        trickshot.setPositionControls(widthCanvas - 200, 200);
        trickshot.setRotation(0);
    }

    private void showSecondLine() {
        secondLine =! secondLine;

        if (!secondLine) {
            btn_trickshot_second_line.setBackgroundResource(R.drawable.button_trickshot_second_line);
        }

        trickshot.secondLine(secondLine);
    }

    private void showNineBall() {
        normal.setVisibility(View.GONE);
        trickshot.setVisibility(View.GONE);
        nineBall.setVisibility(View.VISIBLE);

        btn_trickshot_second_line.setVisibility(View.GONE);

        float widthCanvas = canvasWidth;
        float heightCanvas = canvasHeight;

        // Start line
        float left = widthCanvas - 327;
        float top = heightCanvas - 300;

        // End line
        float right = widthCanvas - 290;
        float bottom = heightCanvas - 282.5f;

        nineBall.setPositionCircleOne((widthCanvas / 2f) + 345.5f, (heightCanvas / 2f) + 39f);
        nineBall.setPositionCircleTwo((widthCanvas / 2f) - 254, (heightCanvas / 2f) - 136.5f);

        nineBall.setPositionLine(left, top, right, bottom);
        nineBall.setRotation(0);
    }

    private final View.OnClickListener showNormal = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.start();

            btn_normal.setBackgroundResource(R.drawable.button_normal_clicked);
            btn_trickshot.setBackgroundResource(R.drawable.button_trickshot);
            btn_nineBall.setBackgroundResource(R.drawable.button_nineball);

            showNormal();
        }
    };

    private final View.OnClickListener showTrickshot = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.start();

            btn_trickshot.setBackgroundResource(R.drawable.button_trickshot_clicked);
            btn_normal.setBackgroundResource(R.drawable.button_normal);
            btn_nineBall.setBackgroundResource(R.drawable.button_nineball);

            showTrickshot();
        }
    };

    private final View.OnClickListener showSecondLine = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.start();

            btn_trickshot_second_line.setBackgroundResource(R.drawable.button_trickshot_second_line_clicked);

            showSecondLine();
        }
    };

    private final View.OnClickListener showNineBall = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.start();

            btn_normal.setBackgroundResource(R.drawable.button_normal);
            btn_trickshot.setBackgroundResource(R.drawable.button_trickshot);
            btn_nineBall.setBackgroundResource(R.drawable.button_nineball_clicked);

            showNineBall();
        }
    };

    private final View.OnClickListener hide = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.start();
            board.setVisibility(View.GONE);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Objects.equals(intent.getAction(), "STOP")) {
                stopForegroundService();
            }

            Intent stopNotificationIntent = new Intent(this, ViewService.class);
            stopNotificationIntent.setAction("STOP");

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent notificationIntent = new Intent(this, MainActivity.class);

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .addAction(0, getString(R.string.close), stopPendingIntent)
                .build();

            startForeground(1, notification);
        }

        return START_NOT_STICKY;
    }

    private void stopForegroundService() {
        Control.stop(this);

        stopForeground(true);
        stopSelf();
    }

    private void layoutParams() {
        float boardMarginBottom = getResources().getDimension(R.dimen.boardMarginBottom);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        params.verticalMargin = boardMarginBottom;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(view, params);
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float y = event.values[1];
            float z = event.values[2];

            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt(y * y + z * z);

            float delta = accelCurrent - accelLast;

            accel = accel * 0.9f + delta;

            if (accel > 8 && accel < 15) {
                board.setVisibility(View.VISIBLE);
                board.setAlpha(0.0f);
                board.animate().setDuration(1000).alpha(1.0f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        board.animate().setListener(null);
                    }
                });
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    public void sensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Objects.requireNonNull(sensorManager)
            .registerListener(
                mSensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST
            );

        accel = 10f;
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager.unregisterListener(mSensorListener);

        if (view != null) {
            windowManager.removeView(view);
        }
    }
}
