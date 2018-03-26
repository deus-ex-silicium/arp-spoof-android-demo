package com.nibiru.arpspoof;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nibiru.arpspoof.db.DatabaseManager;
import com.nibiru.arpspoof.db.LogDbHelper;
import com.nibiru.arpspoof.su.Shell;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private static volatile Shell.Interactive rootSession = null;
    private static volatile Shell.Interactive progSession = null;
    private static DatabaseManager manDb;
    private TextView tv_status;
    private EditText et_gateway;
    private EditText et_target;
    private boolean spoofing = false;
    private boolean sniffing = false;
    /**************************************CLASS METHODS*******************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_status = (TextView) findViewById(R.id.tv_status);
        et_gateway = (EditText) findViewById(R.id.et_gateway);
        et_target = (EditText) findViewById(R.id.et_target);
        rootSession = openRootShell(rootSession);
        progSession = openRootShell(progSession);
        sendGetArpPid();
        DatabaseManager.initializeInstance(new LogDbHelper(this));
    }

    public void onClickBtn(View v) {
        spoofing = !spoofing;
        if (!spoofing){
            sendGenericRootCommand("kill -2 $pid_arp");
            sendGenericRootCommand("sysctl -w net.ipv4.ip_forward=0");
            tv_status.setText(R.string.idle);
            return;
        }
        String gw = et_gateway.getText().toString();
        String ip = et_target.getText().toString();
        //String gw = "192.168.12.1";
        //String ip = "192.168.12.84";
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

    public void onLogShowClick(View v) {
        sendDnsSniffToggle();
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
                sniffing = false;
                break;
            case R.string.spoofNsniff:
                spoofing = true;
                sniffing = true;
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

    private void sendDnsSniffToggle(){
        Log.e("DNS", String.format("spoof=%s sniff=%s", spoofing, sniffing));
        if (!sniffing && spoofing) {
            String path = getApplicationInfo().dataDir;
            final String cmd = String.format("LD_LIBRARY_PATH=%s/lib/ %s/lib/libdnssniff.so wlan0", path, path);
            setStatus(R.string.spoofNsniff);
            progSession.addCommand(new String[]{cmd}, 0,
                    new Shell.OnCommandLineListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode) {
                            Log.i("DNS", String.format("%s \n(exit code: %d)", cmd, exitCode));
                            if (spoofing) setStatus(R.string.spoofing);
                            else setStatus(R.string.idle);
                        }
                        @Override
                        public void onLine(String line) {
                            Log.i("DNS", line);
                        }
                    });
        } else if (!sniffing) {
            Toast.makeText(this,"Start spoofing first!", Toast.LENGTH_SHORT).show();
        }else{
            if (spoofing) setStatus(R.string.spoofing);
            else setStatus(R.string.idle);
            String cmd = "pid_dns=$(pgrep -f libdnssniff.so)";
            sendGenericRootCommand(cmd);
            cmd = "kill -2 $pid_dns";
            sendGenericRootCommand(cmd);
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
        rootSession.addCommand(new String[]{"sysctl -w net.ipv4.ip_forward=1 ",cmd, "pid_arp=$!"}, 0,
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

    private Shell.Interactive openRootShell(Shell.Interactive session) {
        if (session != null) return session;
        // start the shell in the background and keep it alive as long as the app is running
        session = new Shell.Builder().
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
                        } else{
                            updateResultStatus(true, output);
                        }
                    }
                });
        return session;
    }

    /*********************************** NATIVE FUNCTIONS *****************************************/
    //public native int toggleArpSpoofThread(String gateway, String target);

}
