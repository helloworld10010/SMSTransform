package com.example.guo.smstrans;


import java.io.UnsupportedEncodingException;

/**
 * @author Administrator
 */
public class Packet {

    private int id = AtomicIntegerUtil.getIncrementID();
    private byte[] data;

    public int getId() {
        return id;
    }

    public void pack(String txt) {

            data = txt.getBytes();

    }

    public byte[] getPacket() {
        return data;
    }
}
