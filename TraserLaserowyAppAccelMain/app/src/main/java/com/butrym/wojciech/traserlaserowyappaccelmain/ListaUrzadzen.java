package com.butrym.wojciech.traserlaserowyappaccelmain;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Set;

public class ListaUrzadzen extends Activity {

    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.length() - 17);
            Intent data = new Intent();
            data.putExtra("adres", address);
            data.putExtra("nazwa", name);
            setResult(RESULT_OK, data);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_urzadzen);
        ArrayAdapter<String> sparowaneUrzadzeniaArray = new ArrayAdapter<>(this, R.layout.lista);
        ListView pairedListView = (ListView) findViewById(R.id.ListView);
        pairedListView.setAdapter(sparowaneUrzadzeniaArray);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                sparowaneUrzadzeniaArray.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getString(R.string.brsparurz);
            sparowaneUrzadzeniaArray.add(noDevices);
        }
    }
}