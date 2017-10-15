package com.butrym.wojciech.traserlaserowyappaccelmain;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private static final float ALPHA = 0.20f;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String PREFERENCES_NAME = "daneKalibracji";
    private static final String PREFERENCES_FIELD_X = "Xcalib";
    private static final String PREFERENCES_FIELD_Y = "Ycalib";
    private static final String PREFERENCES_XY_EXIST = "XYexist";
    private AcceptThread mirror;
    private ConnectThread polaczenie;
    private float[] accelVals;
    private float[] magVals;
    private TextView xval;
    private TextView yval;
    private TextView status;
    private TextView statusard;
    private ToggleButton toggle;
    private Button arduino;
    private Button kalibracja;
    private Button kalibracjareset;
    private final float[] Rot = new float[9];
    private final float[] results = new float[3];
    private final float[] resultsdeg = new float[3];
    private BluetoothAdapter mBluetoothAdapter = null;
    private SensorManager sm;
    private Sensor accel, magnet;
    private SharedPreferences preferences;
    private Context context;
    private float xCalibrate;
    private float yCalibrate;
    private boolean calibrateExist;
    private AlertDialog alertDialog;
    private ProgressBar mProgressBar;
    private TextView odliczanie;

    private final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            String text = msg.obj.toString();
            switch (msg.what) {
                case 1: //czekam na połączenie
                    status.setText(text);
                    break;
                case 2: // połączyłem mirror
                    status.setText(String.format("%s%s", text, mirror.mDevice.getName()));
                    if (polaczenie != null) {
                        mirror.writebs("ARDUINO:", "Polaczony z " + polaczenie.mmDevice.getName());
                    } else {
                        mirror.writebs("ARDUINO:","rozlaczony");
                    }
                    if (calibrateExist) {
                        mirror.writebs("KALIBRACJA:", "on");
                    } else {
                        mirror.writebs("KALIBRACJA:", "off");
                    }
                    break;
                case 3: //rozłączony
                    if (text.equals("AcceptThread")) {
                        try {
                            status.setText(text);
                            mirror.cancel();
                            mirror = null;
                            mirror = new AcceptThread(mHandler);
                            mirror.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (text.equals("ConnectThread")) {
                        try {
                            statusard.setText(R.string.rozlaczony);
                            polaczenie.cancel();
                            polaczenie = null;
                            arduino.setEnabled(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mirror.writebs("ARDUINO:","rozlaczony");
                    }
                    break;
                case 4: //nie połączono z arduino
                    statusard.setText(String.format(getString(R.string.cantconn), text));
                    polaczenie = null;
                    if (mirror != null) {
                        mirror.writebs("ARDUINO:", "failed " + text);
                    }
                    break;
                case 5:  // rozkazy z mirrora
                    switch (text) {
                        case "on":
                            toggle.toggle();
                            break;
                        case "off":
                            toggle.toggle();
                            break;
                        case "getBonded":
                            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                            if (pairedDevices.size() > 0) {
                                ArrayList<String> mystring = new ArrayList<>();
                                for (BluetoothDevice mydevice : pairedDevices) {
                                    String name = mydevice.getName();
                                    if (name.length() > 20) {
                                        name = name.substring(0, 20);
                                    }
                                    mystring.add(mydevice.getAddress() + "\n" + name);
                                }
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                try {
                                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                                    oos.writeObject(mystring);
                                    oos.close();
                                    byte[] bondedbuff = baos.toByteArray();
                                    mirror.writebs("lista:", bondedbuff);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                    }
                    break;
                case 6:   //połączono z arduino
                    statusard.setText(String.format(getString(R.string.conned), text));
                    arduino.setEnabled(false);
                    mirror.writebs("ARDUINO:", "Polaczony z " + polaczenie.mmDevice.getName());
                    break;
                case 7:     //połącz z arduino;
                    polaczZArduino(text);
                    break;
                case 8:     //rozkazy z arduino
                    String dane;
                    switch (text) {
                        case "X:":
                            dane = xval.getText().toString();
                            wyslijDoArduino(text, dane);
                            break;
                        case "Y:":
                            dane = yval.getText().toString();
                            wyslijDoArduino(text, dane);
                            break;
                    }
                    break;
                case 9: //czekam na połączenie z arduino
                    statusard.setText(text);
                    break;
                case 10:
                    Toast.makeText(context, "Arduino zakończyło poziomowanie.", Toast.LENGTH_LONG).show();
                    mirror.writebs("TOAST:","Arduino zakończyło poziomowanie.");
                    break;
                case 11:  //dodatkowe rozkazy - w gore/dol
                    text = String.valueOf(Double.parseDouble(text));
                    Toast.makeText(context, "Przesyłam Podnieś o " + text + " st.", Toast.LENGTH_LONG).show();
                    wyslijDoArduino("PODNIES:",text);
                    break;
                case 12:  //dodatkowe rozkazy - lewo/prawo
                    text = String.valueOf(Double.parseDouble(text));
                    Toast.makeText(context, "Przesyłam Pochyl o " + text + " st.", Toast.LENGTH_LONG).show();
                    wyslijDoArduino("POCHYL:",text);
                    break;
                case 13:  // dodatkowe rozkazy - obrot
                    text = String.valueOf(Double.parseDouble(text));
                    Toast.makeText(context, "Przesyłam Obróć o " + text + " st.", Toast.LENGTH_LONG).show();
                    wyslijDoArduino("OBROC:",text);
                    break;
                case 14:  // dodatkowe rozkazy - kalibracja
                    if (kalibracja.isEnabled()) {
                        mirror.writebs("TOAST:","Czekaj zapisuję kalibrację...");
                        if(!toggle.isChecked()) {
                            toggle.setChecked(true);
                        }
                        zapiszKalibracje();
                        mirror.writebs("TOAST:","Kalibracja zapisana.");
                    } else {
                        mirror.writebs("TOAST:","Urządzenie skalibrowane.\nMusisz najpierw usunąć kalibrację.");
                    }
                    break;
                case 15:  // dodatkowe rozkazy - reset kalibracji
                    if (kalibracjareset.isEnabled()) {
                        czysckalibracje();
                        mirror.writebs("TOAST:","Kalibracja usunięta.");
                    } else {
                        mirror.writebs("TOAST:","Brak kalibracji, nie ma czego usunąć.");
                    }
                    break;
                case 16:
                    wyslijDoArduino("ZERUJ:","");
                    break;
                case 17:
                    wyslijDoArduino("STOP:","");
                    break;
                case 18:
                    wyslijDoArduino("ROWNOL:","");
                    break;
                case 19:
                    if (text.equals("cancel")) {
                        if (polaczenie!=null) {
                            polaczenie.cancel();
                            polaczenie=null;
                        }
                    }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        context = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        xval = (TextView) findViewById(R.id.xval);
        yval = (TextView) findViewById(R.id.yval);
        status = (TextView) findViewById(R.id.textView2);
        statusard = (TextView) findViewById(R.id.textView4);
        status.setText(R.string.notconn);
        statusard.setText(R.string.notconn);
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        arduino = (Button) findViewById(R.id.arduino);
        kalibracja = (Button) findViewById(R.id.kalibracja);
        kalibracjareset = (Button) findViewById(R.id.kalibracjareset);
        preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        xCalibrate = preferences.getFloat(PREFERENCES_FIELD_X, 0);
        yCalibrate = preferences.getFloat(PREFERENCES_FIELD_Y, 0);
        calibrateExist = preferences.getBoolean(PREFERENCES_XY_EXIST,false);
        if (calibrateExist) {
            kalibracja.setEnabled(false);
            kalibracjareset.setEnabled(true);
        } else {
            kalibracja.setEnabled(true);
            kalibracjareset.setEnabled(false);
        }
        toggle.setOnCheckedChangeListener(this);
        toggle.setTextColor(Color.RED);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Urządzenie nie obsługuje Bluetooth", Toast.LENGTH_LONG).show();
            this.finish();
        }
        onStart();
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mirror = new AcceptThread(mHandler);
        mirror.start();
        xval.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String roz = "x:";
                String msg = charSequence.toString();
                mirror.writebs(roz, msg);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        yval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String roz = "y:";
                String msg = charSequence.toString();
                mirror.writebs(roz, msg);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        arduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ListaUrzadzen.class);
                startActivityForResult(intent, 1);
            }
        });
        kalibracja.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!toggle.isChecked()){
                    toggle.setChecked(true);
                    Toast.makeText(getApplicationContext(), "Włączam sensory.", Toast.LENGTH_SHORT).show();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton(R.string.zapisz, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.zapiszKalibracje();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
                builder.setMessage(R.string.calibques);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        kalibracjareset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton(R.string.wyczysc, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.czysckalibracje();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
                builder.setMessage(R.string.resetcalques);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toggle.isChecked()) {
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (polaczenie != null) {
            try {
                polaczenie.cancel();
                polaczenie = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mirror != null) {
            try {
                mirror.cancel();
                mirror = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelVals = lowPass(event.values.clone(), accelVals);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magVals = lowPass(event.values.clone(), magVals);
        }
        if (accelVals != null && magVals != null) {
            SensorManager.getRotationMatrix(Rot, null, accelVals, magVals);
            SensorManager.getOrientation(Rot, results);
            resultsdeg[1] = (float) (((results[1] * 180 / Math.PI)));
            resultsdeg[2] = (float) (((results[2] * 180 / Math.PI)));
            if (resultsdeg[2] > 90) {
                resultsdeg[2] = 180 - resultsdeg[2];
            }
            if (resultsdeg[2] < -90) {
                resultsdeg[2] = -180 - resultsdeg[2];
            }
            String y = String.format(Locale.getDefault(),"%.01f", resultsdeg[1]-yCalibrate);
            String x = String.format(Locale.getDefault(),"%.01f", resultsdeg[2]-xCalibrate);
            if (y.equals("-0,0")) {
                y = "0,0";
            }
            if (x.equals("-0,0")) {
                x = "0,0";
            }
            xval.setText(x);
            yval.setText(y);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            toggle.setTextColor(0xff669900);
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            toggle.setTextColor(Color.RED);
            sm.unregisterListener(this);
            xval.setText("- - -");
            yval.setText("- - -");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String adresMac = data.getStringExtra("adres");
            polaczZArduino(adresMac);
        } else {
            statusard.setText("Nie wybrano adresu.\nPołącz jeszcze raz.");
        }
    }

    private void polaczZArduino(String address) {
        if (statusard.getText().toString().startsWith("Połączono")) {
            Toast.makeText(this, "Jastem już połączony z Arduino.", Toast.LENGTH_LONG).show();
            return;
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        String nazwa = device.getName();
        statusard.setText("Łączę z: \n" + nazwa + "\n" + address);
        polaczenie = new ConnectThread(device, mHandler);
        polaczenie.start();
    }

    private void wyslijDoArduino(String rozkaz, String wiadomosc) {
        if (polaczenie !=null) {
            polaczenie.writebs(rozkaz, wiadomosc);
        }
    }

    private void zapiszKalibracje() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.timer, null);
        dialogBuilder.setView(dialogView);
        mProgressBar = (ProgressBar) dialogView.findViewById(R.id.progressBar);
        mProgressBar.setMax(3000);
        odliczanie = (TextView) dialogView.findViewById(R.id.textView5);
        alertDialog = dialogBuilder.create();
        alertDialog.show();
        CountDownTimer countDownTimer = new CountDownTimer(3000, 100) {

            @Override
            public void onTick(long leftTimeInMilliseconds) {
                long seconds = leftTimeInMilliseconds / 1000 + 1;
                mProgressBar.setProgress((int) (leftTimeInMilliseconds));
                odliczanie.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss();
                xCalibrate = Float.parseFloat(xval.getText().toString().replace(',','.'));
                yCalibrate = Float.parseFloat(yval.getText().toString().replace(',','.'));
                calibrateExist = true;
                zapiszDane(xCalibrate,yCalibrate,calibrateExist);
                kalibracja.setEnabled(false);
                kalibracjareset.setEnabled(true);
                if (mirror != null) {
                    mirror.writebs("KALIBRACJA:", "on");
                }
            }

        }.start();
    }

    private void czysckalibracje() {
        xCalibrate = 0;
        yCalibrate = 0;
        calibrateExist = false;
        zapiszDane(xCalibrate,yCalibrate,calibrateExist);
        kalibracja.setEnabled(true);
        kalibracjareset.setEnabled(false);
        if (mirror != null) {
            mirror.writebs("KALIBRACJA:", "off");
        }

    }

    private void zapiszDane(float x, float y, boolean exist) {
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        preferencesEditor.putFloat(PREFERENCES_FIELD_X,x);
        preferencesEditor.putFloat(PREFERENCES_FIELD_Y,y);
        preferencesEditor.putBoolean(PREFERENCES_XY_EXIST,exist);
        preferencesEditor.commit();
    }
}
