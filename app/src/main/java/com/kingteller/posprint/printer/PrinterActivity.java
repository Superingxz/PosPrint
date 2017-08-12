package com.kingteller.posprint.printer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kingteller.posprint.R;
import com.telpo.tps550.api.printer.ThermalPrinter;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PrinterActivity extends AppCompatActivity {
    private static String printVersion;
    private final int NOPAPER = 3;
    private final int LOWBATTERY = 4;
    private final int CANCELPROMPT = 10;
    private final int PRINTERR = 11;
    private final int OVERHEAT = 12;
    private final int PRINTPICTURE = 14;

    MyHandler handler;

    private EditText editTextPrintGray;

    private Button button_print_picture;

    private String Result;
    private Boolean nopaper = false;
    private boolean LowBattery = false;

    private int printGray;
    private ProgressDialog progressDialog;

    private String picturePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/111.bmp";

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(PrinterActivity.this);
                    alertDialog.setTitle(R.string.operation_result);
                    alertDialog.setMessage(getString(R.string.LowBattery));
                    alertDialog.setPositiveButton(getString(R.string.dialog_comfirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    alertDialog.show();
                    break;
                case PRINTPICTURE:
                    new printPicture().start();
                    break;
                case OVERHEAT:
                    AlertDialog.Builder overHeatDialog = new AlertDialog.Builder(PrinterActivity.this);
                    overHeatDialog.setTitle(R.string.operation_result);
                    overHeatDialog.setMessage(getString(R.string.overTemp));
                    overHeatDialog.setPositiveButton(getString(R.string.dialog_comfirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    overHeatDialog.show();
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer);
        savepic();
        handler = new MyHandler();

        IntentFilter pIntentFilter = new IntentFilter();
        pIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        pIntentFilter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        registerReceiver(printReceive, pIntentFilter);

        editTextPrintGray = (EditText) findViewById(R.id.set_printGray);

        button_print_picture = (Button) findViewById(R.id.button_print_picture);
        button_print_picture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String exditText = editTextPrintGray.getText().toString();
                if (exditText == null || exditText.length() < 1) {
                    Toast.makeText(PrinterActivity.this, getString(R.string.gray_level) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG).show();
                    return;
                }
                printGray = Integer.parseInt(exditText);
                if (printGray < 0 || printGray > 12) {
                    Toast.makeText(PrinterActivity.this, getString(R.string.outOfGray), Toast.LENGTH_LONG).show();
                    return;
                }
                if (LowBattery == true) {
                    handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
                } else {
                    if (!nopaper) {
                        progressDialog = ProgressDialog.show(PrinterActivity.this, getString(R.string.bl_dy), getString(R.string.printing_wait));
                        handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));
                    } else {
                        Toast.makeText(PrinterActivity.this, getString(R.string.ptintInit), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private final BroadcastReceiver printReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                //TPS390 can not print,while in low battery,whether is charging or not charging
                if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS390.ordinal()){
                    if (level * 5 <= scale) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                }else {
                    if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
                        if (level * 5 <= scale) {
                            LowBattery = true;
                        } else {
                            LowBattery = false;
                        }
                    } else {
                        LowBattery = false;
                    }
                }
            }
            //Only use for TPS550MTK devices
            else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                if(status == 0){
                    if(level < 1){
                        LowBattery = true;
                    }else {
                        LowBattery = false;
                    }
                }else {
                    LowBattery = false;
                }
            }
        }
    };

    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(PrinterActivity.this);
        dlg.setTitle(getString(R.string.noPaper));
        dlg.setMessage(getString(R.string.noPaperNotice));
        dlg.setCancelable(false);
        dlg.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ThermalPrinter.stop(PrinterActivity.this);
            }
        });
        dlg.show();
    }

    private class printPicture extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                ThermalPrinter.start(PrinterActivity.this);
                ThermalPrinter.reset();
                ThermalPrinter.setGray(printGray);
                ThermalPrinter.setAlgin(ThermalPrinter.ALGIN_MIDDLE);
                File file = new File(picturePath);
                if (file.exists()) {
                    ThermalPrinter.printLogo(BitmapFactory.decodeFile(picturePath));
                    ThermalPrinter.walkPaper(100);
                } else {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(PrinterActivity.this, getString(R.string.not_find_picture), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
                    nopaper = true;
                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper){
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
                ThermalPrinter.stop(PrinterActivity.this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && !PrinterActivity.this.isFinishing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        unregisterReceiver(printReceive);
        ThermalPrinter.stop();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void savepic() {
        File file = new File(picturePath);
        if (!file.exists()) {
            InputStream inputStream = null;
            FileOutputStream fos = null;
            byte[] tmp = new byte[1024];
            try {
                inputStream = getApplicationContext().getAssets().open("syhlogo.png");
                fos = new FileOutputStream(file);
                int length = 0;
                while((length = inputStream.read(tmp)) > 0){
                    fos.write(tmp, 0, length);
                }
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
