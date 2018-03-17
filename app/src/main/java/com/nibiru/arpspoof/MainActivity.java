package com.nibiru.arpspoof;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nibiru.arpspoof.su.Shell;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private static volatile Shell.Interactive rootSession = null;
    private TextView tv_status;
    private boolean spoofing = false;
    /**************************************CLASS METHODS*******************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_status = (TextView) findViewById(R.id.tv_status);
        openRootShell();
    }
    // Used to load the native library on application startup.
    static {
        //System.loadLibrary("arp-spoof");
    }

    public void onClickBtn(View v) {
        spoofing = !spoofing;
        if (!spoofing){
            return;
        }
        sendArpSpoofCommand("192.168.12.1","192.168.12.94");
        final Handler checkStatus = new Handler();
        checkStatus.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendArpSpoofCheck();
            }
        }, 10000);
    }

    private void updateResultStatus(boolean suAvailable, List<String> suResult) {
        if (!suAvailable) finishAndRemoveTask();
        if (suResult != null) {
            for (String line : suResult) {
                Log.d("ROOT:", line);
            }
        }
    }

    private void sendArpSpoofCommand(String gw, String target) {
        String path = getApplicationInfo().dataDir;
        String cmd = String.format("LD_LIBRARY_PATH=%s/lib/ %s/lib/libarpspoof.so %s %s &",
                path, path, gw, target);
        Log.i("ROOT", cmd);
        rootSession.addCommand(new String[]{cmd, "pid_arp=$!"}, 0,
                new Shell.OnCommandLineListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode) {
                        switch (exitCode){
                            case 0:
                                tv_status.setText("Initializing...");
                                break;
                        }
                        Log.e("ROOT", "exit code: " + exitCode);
                    }
                    @Override
                    public void onLine(String line) {
                        Log.d("ROOT", line);
                    }
                });
    }

    private void sendArpSpoofCheck(){
        rootSession.addCommand(new String[]{"kill -0 $pid_arp", "if [ $? -ne 0 ]; then wait $pid_arp; else echo 0; fi"}, 0,
        //rootSession.addCommand(new String[]{"id"}, 0,
                new Shell.OnCommandLineListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode) {
                        switch (exitCode){
                            case 0:
                                tv_status.setText("Spoofing...");
                                break;
                            case 1:
                                Log.e("ROOT", "Wrong command usage: <LAN gateway> <LAN target>");
                                tv_status.setText("Wrong command usage");
                                break;
                            case 2:
                                Log.e("ROOT", "Invalid IP format");
                                tv_status.setText("Invalid IP format");
                                break;
                            case 7:
                                Log.e("ROOT", "Runtime error");
                                tv_status.setText("Runtime error");
                                break;
                        }
                        Log.e("ROOT", "exit code: " + exitCode);
                    }
                    @Override
                    public void onLine(String line) {
                        Log.d("ROOT", line);
                    }
                });
    }

    private void openRootShell() {
        if (rootSession != null) return;
        // start the shell in the background and keep it alive as long as the app is running
        rootSession = new Shell.Builder().
                useSU().
                setWantSTDERR(true).
                setWatchdogTimeout(0).
                setMinimalLogging(true).
                open(new Shell.OnCommandResultListener() {
                    // Callback to report whether the shell was successfully started up
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                            List<String> errorInfo = new ArrayList<String>();
                            errorInfo.add("Error opening root shell: exitCode " + exitCode);
                            updateResultStatus(false, errorInfo);
                            rootSession = null;
                        } else{
                            updateResultStatus(true, output);
                        }
                    }
                });
    }

    /*********************************** NATIVE FUNCTIONS *****************************************/
    //public native int toggleArpSpoofThread(String gateway, String target);

}
