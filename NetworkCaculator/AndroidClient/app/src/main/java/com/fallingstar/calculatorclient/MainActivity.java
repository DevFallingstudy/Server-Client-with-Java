package com.fallingstar.calculatorclient;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    TextView txt_result;
    EditText edt_formula;
    Button[] numBtnList;
    Integer[] numBtnIdList;

    Button btn_plus, btn_minus, btn_div, btn_multi;
    Button btn_clear, btn_dot, btn_equal;
    Button btn_exit;

    Socket sock = new Socket();
    BufferedWriter outToServer;
    BufferedReader inFromServer;
    NetworkThread thread;

    boolean isReady = false;
    boolean isSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            initWidgets();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            if (!sock.isConnected()){
                socketConnect("192.9.82.67", 5508);
            }
            while (!sock.isConnected()) {
                //Wait until sock is connect
            }
            thread = new NetworkThread(inFromServer, outToServer);
            thread.start();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Server is down. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void initWidgets() throws IOException {
        int i;

        //Text View
        txt_result = (TextView)findViewById(R.id.txt_result);

        //Edit Text
        edt_formula = (EditText)findViewById(R.id.edt_formula);

        //Buttons
        numBtnIdList = new Integer[10];
        numBtnList = new Button[10];
        numBtnIdList[0] = R.id.btn_0;
        numBtnIdList[1] = R.id.btn_1;
        numBtnIdList[2] = R.id.btn_2;
        numBtnIdList[3] = R.id.btn_3;
        numBtnIdList[4] = R.id.btn_4;
        numBtnIdList[5] = R.id.btn_5;
        numBtnIdList[6] = R.id.btn_6;
        numBtnIdList[7] = R.id.btn_7;
        numBtnIdList[8] = R.id.btn_8;
        numBtnIdList[9] = R.id.btn_9;
        for (i = 0; i < 10; i++){
            numBtnList[i] = (Button)findViewById(numBtnIdList[i]);
            final int finalI = i;
            numBtnList[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    edt_formula.setText(edt_formula.getText().toString() + finalI);
                }
            });
        }

        btn_plus = (Button)findViewById(R.id.btn_plus);
        btn_minus = (Button)findViewById(R.id.btn_minus);
        btn_div = (Button)findViewById(R.id.btn_divide);
        btn_multi = (Button)findViewById(R.id.btn_multiple);
        btn_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText(edt_formula.getText().toString() + " + ");
            }
        });
        btn_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText(edt_formula.getText().toString() + " - ");
            }
        });
        btn_div.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText(edt_formula.getText().toString() + " / ");
            }
        });
        btn_multi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText(edt_formula.getText().toString() + " * ");
            }
        });

        btn_dot = (Button)findViewById(R.id.btn_dot);
        btn_clear = (Button)findViewById(R.id.btn_clear);
        btn_equal = (Button)findViewById(R.id.btn_equal);
        btn_exit = (Button)findViewById(R.id.btn_exit);

        btn_dot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText(edt_formula.getText().toString() + ".");
            }
        });
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edt_formula.setText("");
            }
        });
        btn_equal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isReady = true;
            }
        });
        btn_exit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                try {
                    sock.close();
                    thread.interrupt();
                    outToServer.close();
                    inFromServer.close();
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void socketConnect(final String IP, final int port){
        Thread sockThread = new Thread(){
            @Override
            public void run() {
                super.run();
                try{
                    sock = new Socket(IP, port);
                    outToServer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        sockThread.start();
    }

    private class NetworkThread extends Thread {
        BufferedReader br;
        BufferedWriter bw;
        Handler mHandler;
        SendingRunnable sendingRunnable;

        private NetworkThread(BufferedReader br, BufferedWriter bw){
            this.br = br;
            this.bw = bw;

            mHandler = new Handler();
            sendingRunnable = new SendingRunnable(bw);

            Log.d("YSTAG", "Connected with Reader "+br+"and Writer "+bw);
        }

        @Override
        public void run() {
            super.run();
            boolean isError = false;
            String result = "";
            String error = "";

            try{
                this.bw.write("CONNECTED\n");
                this.bw.flush();
            }catch (Exception e){
                e.printStackTrace();
            }
            while (true){
                try{
                    if (isReady){
                        mHandler.post(sendingRunnable);

                        while (true) {
                            try{
                                result = br.readLine();
                                if (result != null){
                                    if (result.startsWith("ERROR")){
                                        String[] result_arr = result.split(" ");
                                        error = result_arr[1].trim();
                                        isError = true;
                                    }else if (result.startsWith("RESULT")){
                                        String[] result_arr = result.split(" ");
                                        result = result_arr[1].trim();
                                        isError = false;
                                    }

                                    if (isError){
                                        if (error.equals("ZERO")){
                                            error = "Divided by zero.";
                                        }else if (error.equals("OP")){
                                            error = "Wrong operation or format";
                                        }else if (error.equals("LENGTH")){
                                            error = "Wrong format";
                                        }

                                        final String finalError = error;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                txt_result.setText("Error >> "+finalError);
                                            }
                                        });
                                        Log.d("YSTAG", "error from server : "+error);
                                        isReady = false;
                                        break;
                                    }else{
                                        final String finalResult = result;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d("YSTAG", "ASDF");
                                                txt_result.setText("Result >> "+finalResult);
                                            }
                                        });
                                        isReady = false;
                                        break;
                                    }
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private class SendingRunnable implements Runnable {
        BufferedWriter bw;

        private SendingRunnable(BufferedWriter bw){
            this.bw = bw;
        }

        @Override
        public void run() {
            String formula = edt_formula.getText().toString();
            try{
                bw.write(formula+"\n");
                bw.flush();
                Log.d("YSTAG", "sent : "+formula);
                edt_formula.setText("");
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error while sending formula : "+formula, Toast.LENGTH_LONG).show();
                Log.e("YSTAG", "Error while sending formula : "+formula);
            }
        }
    }
}

