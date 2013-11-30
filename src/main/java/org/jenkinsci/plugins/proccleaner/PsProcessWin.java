package org.jenkinsci.plugins.proccleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: psrna
 * Date: 8/2/13
 */
public class PsProcessWin extends PsProcess {

    public PsProcessWin(int pid, int ppid, String args, PsBasedProcessTree ptree) {
        super(pid, ppid, args, ptree);
    }

    @Override
    public void kill(int signum) {

        try {
            Process p = Runtime.getRuntime().exec("cmd.exe /c \"taskkill /F /PID " + super.getPid() + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static String getUser(int pid) {

        String user = null;
        //regex for getowner
        Pattern USER_PATTERN = Pattern.compile("(\\s*User = \")(\\w+)(\";\\s*)");

        String cmd = "cmd.exe /c \"WMIC PROCESS where (processid=" + pid + ") call getowner\"";

        try{
            // Run Windows command
            Process process = Runtime.getRuntime().exec(cmd);
            // Get input stream
            BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // Read and parse command standard output
            String s;
            while((s = stdin.readLine()) != null){
                Matcher muser = USER_PATTERN.matcher(s);
                if(muser.matches()){
                    user = muser.group(2);
                }
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        return user;
    }
}
