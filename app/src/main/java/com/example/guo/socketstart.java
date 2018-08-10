package com.example.guo;

public class socketstart extends Thread {


    @Override
    public void run() {
        boolean isrun = false;
        if (!fun.isrun) {
            fun.isrun = true;
            while (fun.isrun) {
                try {

//                    if (fun.socket == null) {
//                        if (!isrun) {
//                            sleep(2 * 1000);
//                        }
//                        isrun = true;
////                        fun.socket = new socketth("183.196.13.28", 12307);
////                        fun.socket.start();
//                    }

                    sleep(5 * 1000);
                    fun.Log("loopcycle", "sleep:");
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
            }
        }
    }
}
