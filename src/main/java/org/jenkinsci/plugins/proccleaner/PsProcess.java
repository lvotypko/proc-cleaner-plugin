package org.jenkinsci.plugins.proccleaner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class PsProcess {

    private final int pid;
    private final int ppid;
    private final String args;
    private PsBasedProcessTree ptree;


    public PsProcess(int pid, int ppid, String args, PsBasedProcessTree ptree) {
        this.pid = pid;
        this.ppid = ppid;
        this.args = args;
        this.ptree = ptree;
    }

    public int getPid() {
        return pid;
    }

    public int getPpid() {
        return ppid;
    }

    public String getArgs() {
        return args;
    }

    public PsProcess getParent() {
        return ptree.getByPid(ppid);
    }

    public List<PsProcess> getChildren() {
        List<PsProcess> children = new ArrayList<PsProcess>();
        for(PsProcess p : ptree.getProcessList())
            if(this == p.getParent())
                children.add(p);

        return children;
    }

    public void killRecursively() {
        for(PsProcess p : getChildren())
            p.killRecursively();
        kill();
    }

    public void kill() {
        System.out.println("Killing " + pid + ", args: " + args);
        if(ptree.getLog() != null)
            ptree.getLog().println("Killing " + this);
        killHard();
    }

    public void killHard() {
        kill(9);
    }

    public abstract void kill(int signum);

    public void killAllExceptMe() {
        Map<Integer, PsProcess> ph = getParentHierarchy(this);
        for(PsProcess p : ptree.getProcessList())
            if(this != p && !ph.containsKey(new Integer(p.pid))) // don't kill myself (and parent) //TODO should contain whole possible hierarchy
                p.kill();
    }

    public Map<Integer, PsProcess> getParentHierarchy(PsProcess p) {
        Map<Integer, PsProcess> ph = new HashMap<Integer, PsProcess>();
        while((p = p.getParent()) != null) {
            //p = p.getParent();
            ph.put(new Integer(p.pid), p);
        }
        return ph;
    }

    public String toString() {
        return "Process PID = " + pid + ", PPID = " + ppid + ", ARGS = " + args;
    }

    public boolean equals(Object o) {
        if(o instanceof PsProcess)
            if(pid == ((PsProcess)o).pid)
                return true;
        return false;
    }

    //TODO implement hashCode etc.

    // private static final long serialVersionUID = 1L;
}
