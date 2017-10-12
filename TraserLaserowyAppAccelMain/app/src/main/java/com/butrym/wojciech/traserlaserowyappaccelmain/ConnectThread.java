package com.butrym.wojciech.traserlaserowyappaccelmain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


class ConnectThread extends AcceptThread {

    public final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device, Handler handler) {
        mmDevice = device;
        mhandler = handler;
        BluetoothSocket tmp = null;
        try {
            tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = tmp;
    }

    @Override
    public void run() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        mhandler.obtainMessage(9, "Czekam na połączenie...").sendToTarget();
        try {
            socket.connect();
        } catch (IOException connectException) {
            mhandler.obtainMessage(4, mmDevice.getName()).sendToTarget();
            try {
                socket.close();
            } catch (IOException e) {
                connectException.printStackTrace();
                e.printStackTrace();
            }
            return;
        }
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        mhandler.obtainMessage(6, mmDevice.toString()).sendToTarget();
        czytaj();
    }

    protected void wiadomosc(String rozkaz, byte[] wiadomosc) {
        if (rozkaz.startsWith("Podaj")) {
            mhandler.obtainMessage(8, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("Koniec")) {
            mhandler.obtainMessage(10, new String(wiadomosc)).sendToTarget();
        }
    }
}
