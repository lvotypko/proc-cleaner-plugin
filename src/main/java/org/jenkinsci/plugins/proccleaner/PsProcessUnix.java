package org.jenkinsci.plugins.proccleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static hudson.util.jna.GNUCLibrary.LIBC;


/**
 * Author: psrna
 * Date: 8/2/13
 */
public class PsProcessUnix extends PsProcess{

    public PsProcessUnix(int pid, int ppid, String args, PsBasedProcessTree ptree) {
        super(pid, ppid, args, ptree);
    }

    @Override
    public void kill(int signum) {
        LIBC.kill(super.getPid(), signum);
    }

    public static String getUser(int pid){

        String[] cmd = {"ps","up", String.valueOf(pid)};
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        String line = null;

        try{
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            int ec = proc.waitFor();

            line = reader.readLine(); // first line should be "USER PID ..." - skip it
            line = reader.readLine(); //first word of second line is username

        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        return line.substring(0, line.indexOf(' '));
    }
}
