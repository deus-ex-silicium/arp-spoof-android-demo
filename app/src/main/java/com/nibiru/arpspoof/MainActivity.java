package com.nibiru.arpspoof;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
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
        sendGetArpPid();
    }

    public void onClickBtn(View v) {
        spoofing = !spoofing;
        if (!spoofing){
            sendGenericRootCommand("kill -2 $pid_arp");
            tv_status.setText(R.string.idle);
            return;
        }
        String gw = "192.168.12.1";
        String ip = "192.168.12.94";
        if (!Patterns.IP_ADDRESS.matcher(gw).matches()
                || !Patterns.IP_ADDRESS.matcher(ip).matches()){
            setStatus(R.string.bad_ip);
            return;
        }
        sendArpSpoofStart(gw, ip);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendArpSpoofCheck();
            }
        }, 10000);
    }

    private void setStatus(int s){
        switch (s){
            case R.string.idle:
            case R.string.bad_ip:
            case R.string.init:
            case R.string.err_runtime:
            case R.string.err_usage:
                spoofing = false;
                break;
            case R.string.spoofing:
                spoofing = true;
                break;
        }
        Log.e(TAG, getString(s));
        tv_status.setText(s);
    }

    private void updateResultStatus(boolean suAvailable, List<String> suResult) {
        if (!suAvailable) finishAndRemoveTask();
        if (suResult != null) {
            for (String line : suResult) {
                Log.d("ROOT:", line);
            }
        }
    }

    private void sendGetArpPid(){
        final String cmd = String.format("pid_arp=$(pgrep -f %s)", getApplicationInfo().dataDir);
        rootSession.addCommand(new String[]{cmd}, 0,
                new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        switch (exitCode){
                            case 0:
                                setStatus(R.string.spoofing);
                                break;
                            default:
                                setStatus(R.string.idle);
                                break;
                        }
                        Log.i("ROOT",  String.format("%s \n(exit code: %d)", cmd, exitCode));
                    }
                });
    }

    private void sendArpSpoofStart(String gw, String target) {
        String path = getApplicationInfo().dataDir;
        final String cmd = String.format("LD_LIBRARY_PATH=%s/lib/ %s/lib/libarpspoof.so %s %s &",
                path, path, gw, target);
        rootSession.addCommand(new String[]{cmd, "pid_arp=$!"}, 0,
                new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        switch (exitCode){
                            case 0:
                                setStatus(R.string.init);
                                break;
                        }
                        Log.i("ROOT",  String.format("%s \n(exit code: %d)", cmd, exitCode));
                    }
                });
    }

    private void sendArpSpoofCheck(){
        final String cmd = "kill -0 $pid_arp || wait $pid_arp";
        rootSession.addCommand(new String[]{cmd}, 0,
        //rootSession.addCommand(new String[]{"kill -0 $pid_arp", "if [ $? -ne 0 ]; then wait $pid_arp; else echo 0; fi"}, 0,
                new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        switch (exitCode){
                            case 0:
                                setStatus(R.string.spoofing);
                                break;
                            case 1:
                                setStatus(R.string.err_usage);
                                break;
                            case 2:
                                setStatus(R.string.bad_ip);
                                break;
                            case 7:
                                setStatus(R.string.err_runtime);
                                break;
                        }
                        Log.i("ROOT",  String.format("%s \n(exit code: %d)", cmd, exitCode));
                    }
                });
    }

    private void sendGenericRootCommand(final String cmd){
        rootSession.addCommand(new String[]{cmd}, 0,
                new Shell.OnCommandLineListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode) {
                        Log.i("ROOT",  String.format("%s \n(exit code: %d)", cmd, exitCode));
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
