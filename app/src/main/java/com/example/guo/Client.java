package com.example.guo;


import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;

public class Client {
    Socket soc;
    boolean con = false;// 网络连接情况 true连接 false断开
    public String ip = "127.0.0.1";
    public int port = 9991;
    InetSocketAddress add;

    DataInputStream InputStream;

    public Client(String host, int port_) {
        if (!con) {
            soc = new Socket();
            this.ip = host;
            this.port = port_;
            add = new InetSocketAddress(ip, port);
            try {
                soc.connect(add, 5000);
                soc.setSoTimeout(5000);

                con = soc.isConnected();
                InputStream = new DataInputStream(soc.getInputStream());
                fun.receTime = new Date();
//                if(fun.loop!=null)
//                fun.loop.start();
            } catch (IOException e) {
                con = false;
                fun.Log("client connect", e.getMessage());
                Close();
            }

            if (soc.isConnected()) {
                new Thread() {
                    @Override
                    public void run() {
                        Receive();
                    }
                }.start();
                fun.Log("client", "connect ok");
            } else {
                fun.Log("client", "connect is fails");
                Close();
            }
        } else {
            fun.Log("client", "connect is over");
        }
    }


    public void Receive() {


        while (true) {
            try {
                if (!soc.isConnected() || soc.isClosed()) {
                    Close();
                    break;
                }

                int len = 0;

                //   do {
                byte[] sdata = new byte[1024];
                len = InputStream.read(sdata);
                if (len > 0) {
                    String id = new String(sdata, 0, len);
                    String s[] = id.split("/n");
                    for (int j = 0; j < s.length; j++) {
                        fun.Log("ret", s[j]);
                        String id1 = s[j];
//                        if (!id1.equals("9999"))
//                            fun.sms.DelSMS(s[j]);
                    }

                }
                //  } while (len > 0);

            } catch (SocketTimeoutException e) {
                try {
                    Thread.sleep(100);
//                    Date ntime = new Date();
//                    long kk = ntime.getTime() - fun.receTime.getTime();
//                    if (kk > (fun.HeartCycle + 20) * 1000) {
//                        fun.Log("Client", "over HeartCycle:");
//                        Close();
//                        break;
//                    }
                    fun.Log("Client", "timeout-sleep:");
                    InputStream = new DataInputStream(soc.getInputStream());

                } catch (InterruptedException e1) {
                    Close();
                    break;
                } catch (IOException e1) {
                    Close();
                    break;
                }
                continue;
            } catch (StringIndexOutOfBoundsException e) {
                fun.Log("Client:", e.getMessage());
                Close();
                break;
            } catch (IOException e) {
                fun.Log("Client:", e.getMessage());
                Close();
                break;
            }
        }
    }

    public void Close() {
        if (soc.isConnected()) {
            try {
                soc.close();
                con = false;
                fun.Log("Client", "disconnect");
            } catch (IOException e) {
//                fun.socket = null;
            }

        }

        fun.isrun = true;
//        fun.socket = null;

    }

    public void sendbyte(byte[] data) {
        if (soc.isConnected()) {

            try {
                OutputStream os = soc.getOutputStream();
                //	os.flush();
                os.write(data);
                os.flush(); // 发送图片流，继续等待 结束指令
                fun.Log("Client", "sendbyte");
            } catch (Exception e) {
//				e.printStackTrace();
                fun.Log("Client", "SendByte Error:" + e.getMessage());
                Close();

            }

        } else {
            Close();
        }


    }

    public String send(String data) {
        if (con) {
            try {
                OutputStream os = soc.getOutputStream();
                os.write(data.getBytes());
                os.flush(); // 发送图片流，继续等待 结束指令
                fun.Log("send", data);
            } catch (IOException e) {
                //	e.printStackTrace();
                fun.Log("Client", "SendByte Error:" + e.getMessage());
                Close();
            }

        }

        return "OK";
    }
}
