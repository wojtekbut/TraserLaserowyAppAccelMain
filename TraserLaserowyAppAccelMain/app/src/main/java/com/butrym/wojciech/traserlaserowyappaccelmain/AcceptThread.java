package com.butrym.wojciech.traserlaserowyappaccelmain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;



class AcceptThread extends Thread {

    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private BluetoothServerSocket mmServerSocket;
    BluetoothDevice mDevice;
    InputStream mmInStream;
    OutputStream mmOutStream;
    BluetoothSocket socket;
    Handler mhandler;
    private String state;

    AcceptThread(){}

    AcceptThread(Handler handler) {
        mhandler = handler;
        BluetoothServerSocket tmp = null;
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            tmp = mAdapter.listenUsingRfcommWithServiceRecord("Klon",
                    MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmServerSocket = tmp;
    }

    public void run() {
        mhandler.obtainMessage(1, "Czekam na połączenie...").sendToTarget();
        socket = null;
        state = "waiting";
        try {
            socket = mmServerSocket.accept();
            mDevice = socket.getRemoteDevice();
            mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mhandler.obtainMessage(2, "Połączony z: ").sendToTarget();
        state = "connected";
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        czytaj();
    }

    private void write(byte[] buffer) {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isConnected()) {
                if (state.equals("connected")) {
                    socket.close();
                    mhandler.obtainMessage(3, "Rozłączony").sendToTarget();
                }
            } else {
                mmOutStream.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void writebs(String rozkaz, byte[] bity) {
        String poczatek, koniec;
        byte[] bpoczatek, bkoniec, brozkaz, wiadomosc;
        poczatek = "stArt:";
        koniec = ":koNiec";
        bpoczatek = poczatek.getBytes();
        bkoniec = koniec.getBytes();
        brozkaz = rozkaz.getBytes();
        byte[] objetosc = new byte[2];
        byte dlrozkazu = (byte) brozkaz.length;
        int dlugosc = bity.length;
        if (dlugosc < 256) {
            objetosc[0] = 0;
            objetosc[1] = (byte) dlugosc;
        } else if (dlugosc > 255 && dlugosc < 65535) {
            objetosc[0] = (byte) (dlugosc / 256);
            objetosc[1] = (byte) (dlugosc - ((int) objetosc[0] * 256));
        } else {
            Log.e("Write", "Wiadomość za długa.");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(bpoczatek);
            outputStream.write(dlrozkazu);
            outputStream.write(objetosc);
            outputStream.write(brozkaz);
            outputStream.write(bity);
            outputStream.write(bkoniec);
        } catch (IOException e) {
            e.printStackTrace();
        }
        wiadomosc = outputStream.toByteArray();
        write(wiadomosc);
    }


    void writebs(String rozkaz, String string) {
        String poczatek, koniec;
        byte[] bpoczatek, bkoniec, brozkaz, bstring, wiadomosc;
        poczatek = "stArt:";
        koniec = ":koNiec";
        bpoczatek = poczatek.getBytes();
        bkoniec = koniec.getBytes();
        brozkaz = rozkaz.getBytes();
        bstring = string.getBytes();
        byte[] objetosc = new byte[2];
        byte dlrozkazu = (byte) brozkaz.length;
        int dlugosc = bstring.length;
        if (dlugosc < 256) {
            objetosc[0] = 0;
            objetosc[1] = (byte) dlugosc;
        } else if (dlugosc > 255 && dlugosc < 65535) {
            objetosc[0] = (byte) (dlugosc / 256);
            objetosc[1] = (byte) (dlugosc - ((int) objetosc[0] * 256));
        } else {
            Log.e("Write", "Wiadomość za długa.");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(bpoczatek);
            outputStream.write(dlrozkazu);
            outputStream.write(objetosc);
            outputStream.write(brozkaz);
            outputStream.write(bstring);
            outputStream.write(bkoniec);
        } catch (IOException e) {
            e.printStackTrace();
        }
        wiadomosc = outputStream.toByteArray();
        write(wiadomosc);
    }

    void czytaj() {
        byte[] buffer = new byte[2];
        byte[] poczatek = new byte[6];
        byte dlrozkazu;
        byte[] rozkaz;
        byte[] objetosc = new byte[2];
        byte[] koniec = new byte[7];
        int dlugosc;
        while (socket.isConnected()) {
            try {
                poczatek[0] = (byte) mmInStream.read();
                if ((char) poczatek[0] == 's') {
                    for (int i = 1; i < 6; i++) {
                        poczatek[i] = (byte) mmInStream.read();
                    }
                    String spocz = new String(poczatek);
                    if (spocz.equals("stArt:")) {
                        dlrozkazu = (byte) mmInStream.read();
                        rozkaz = new byte[dlrozkazu];
                        for (int i = 0; i < 2; i++) {
                            objetosc[i] = (byte) mmInStream.read();
                        }
                        dlugosc = objetosc[0] * 256 + objetosc[1];
                        buffer = new byte[dlugosc];
                        for (int i = 0; i < dlrozkazu; i++) {
                            rozkaz[i] = (byte) mmInStream.read();
                        }
                        for (int i = 0; i < dlugosc; i++) {
                            buffer[i] = (byte) mmInStream.read();
                        }
                        for (int i = 0; i < 7; i++) {
                            koniec[i] = (byte) mmInStream.read();
                        }
                        String skoniec = new String(koniec);
                        if (skoniec.equals(":koNiec")) {
                            this.wiadomosc(new String(rozkaz), buffer);
                            Arrays.fill(buffer, (byte) 0);
                            Arrays.fill(poczatek, (byte) 0);
                            Arrays.fill(koniec, (byte) 0);
                            Arrays.fill(objetosc, (byte) 0);
                        } else {
                            Arrays.fill(buffer, (byte) 0);
                            Arrays.fill(poczatek, (byte) 0);
                            Arrays.fill(koniec, (byte) 0);
                            Arrays.fill(objetosc, (byte) 0);
                        }
                    } else {
                        Arrays.fill(buffer, (byte) 0);
                        Arrays.fill(poczatek, (byte) 0);
                        Arrays.fill(koniec, (byte) 0);
                        Arrays.fill(objetosc, (byte) 0);
                    }
                }
            } catch (IOException e) {
                mhandler.obtainMessage(3, getClass().getSimpleName()).sendToTarget();
                break;
            }
        }
    }

    protected void wiadomosc(String rozkaz, byte[] wiadomosc) {
        Log.d("wiadomość", rozkaz + " ");
        if (rozkaz.startsWith("run:")) {
            mhandler.obtainMessage(5, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("lista:")) {
            mhandler.obtainMessage(5, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("l:")) {
            mhandler.obtainMessage(5, wiadomosc).sendToTarget();
        } else if (rozkaz.startsWith("polArd")) {
            mhandler.obtainMessage(7, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("POCHYL:")) {
            mhandler.obtainMessage(12, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("PODNIES:")) {
            mhandler.obtainMessage(11, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("OBROC:")) {
            mhandler.obtainMessage(13, new String(wiadomosc)).sendToTarget();
        } else if (rozkaz.startsWith("KALIBRACJA:")) {
            mhandler.obtainMessage(14,"").sendToTarget();
        } else if (rozkaz.startsWith("RESETKALIBRACJI:")) {
            mhandler.obtainMessage(15,"").sendToTarget();
        } else if (rozkaz.startsWith("ZERUJ:")) {
            mhandler.obtainMessage(16,"").sendToTarget();
        } else if (rozkaz.startsWith("STOP:")) {
            mhandler.obtainMessage(17,"").sendToTarget();
        } else if (rozkaz.startsWith("ROWNOL:")) {
            mhandler.obtainMessage(18,"").sendToTarget();
        } else if (rozkaz.startsWith("ARDUINO:")) {
            mhandler.obtainMessage(19,new String(wiadomosc)).sendToTarget();
        }
    }

    void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
