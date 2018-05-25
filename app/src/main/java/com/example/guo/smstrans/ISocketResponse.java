package com.example.guo.smstrans;

/**
 * socket回调
 */
public interface ISocketResponse {
    public abstract void onSocketResponse(String text);

    public abstract void onSocketState(int Flags);
}
