package com.example.foser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Timer;
import java.util.TimerTask;

public class MyForegroundService extends Service {

    //1. Kanał notyfikacji
    public static final String CHANNEL_ID = "MyForegroundServiceChannel";
    public static final String CHANNEL_NAME = "FoSer service channel";

    //2. Odczyt danych zapisanych w Intent
    public static final String MESSAGE = "message";
    public static final String TIME = "time";
    public static final String WORK = "work";
    public static final String WORK_DOUBLE = "work_double";
    public static final String INCREMENTATION_TIME = "incrementation_time";
    public static final String WITHOUT_RESET = "without_reset";

    //3. Wartości ustawień
    private String message, time;
    private Boolean show_time, do_work, double_speed, without_reset;
    private long period = 2000; //2s

    //4.
    private Context ctx;
    private Intent notificationIntent;
    private PendingIntent pendingIntent;

    //5.
    private int counter;
    private Timer timer;
    private TimerTask timerTask;
    final Handler handler = new Handler();

    SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        ctx = this;
        notificationIntent = new Intent(ctx, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        timer = new Timer();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        message = intent.getStringExtra(MESSAGE);
        show_time = intent.getBooleanExtra(TIME, false);
        do_work = intent.getBooleanExtra(WORK, false);
        double_speed = intent.getBooleanExtra(WORK_DOUBLE, false);

        time = intent.getStringExtra(INCREMENTATION_TIME);
        without_reset = intent.getBooleanExtra(WITHOUT_RESET, false);
        switch(time){
            case "2s":
                period = 2000;  //zmieniamy period w zależności od wybranego czasu
                break;
            case "5s":
                period = 5000;
                break;
            case "10s":
                period = 10000;
                break;
        }

        if(without_reset){
            counter = sharedPreferences.getInt("counter",0); //pobieranie zapisanej wartosci licznika
        }else {
            counter = 0 ; //wyzerowanie wartosci licznika
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                counter++;
                handler.post(runnable);
            }
        };

        createNotificationChannel();
        /**
         * Notification.Builder zamieniono na NotificationCompat.Builder
         * aby aplikacje można było uruchomić na urządzeniu z Android 7.1 i niżej
         * telefon na którym testuje aplikacji ma android 7.1
         **/
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_my_icon)
                .setContentTitle(getString(R.string.ser_title))
                .setShowWhen(show_time)
                .setContentText(message)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.circle))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        doWork();
        return START_NOT_STICKY;
    }

    private void doWork() {
        if(do_work) {
            timer.schedule(timerTask, 0L, double_speed ? period / 2L : period);
        }

        String info = "show_time=" + show_time.toString()
                + "\n do_work=" + do_work.toString()
                + "\n double_speed=" + double_speed.toString()
                + "\n period= " + period
                + "\n restart= " + without_reset.toString();

        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        /**
         *Dodano sprawdzenie na wersje API urządzenia, dlatego, aby aplikacje można było uruchomić na urządzeniu z Android 7.1 i niżej.
         **/
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Notification notification = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_my_icon)
                    .setContentTitle(getString(R.string.ser_title))
                    .setShowWhen(show_time)
                    .setContentText(message + " " + String.valueOf(counter))
                    .setLargeIcon(BitmapFactory.decodeResource (getResources() , R.drawable.circle ))
                    .setContentIntent(pendingIntent)
                    .build();

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.notify(1,notification);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        timer.cancel();
        timer.purge();
        timer = null;
        sharedPreferences.edit().putInt("counter" ,counter).commit(); //zapisywanie wartosci licznika
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

