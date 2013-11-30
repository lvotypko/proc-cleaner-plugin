package org.jenkinsci.plugins.proccleaner;

import hudson.DescriptorExtensionList;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.lang.management.ManagementFactory;

public abstract class ProcCleaner implements Callable<Void,Exception>, Describable<ProcCleaner>, ExtensionPoint {

	private static final long serialVersionUID = 1L;
	private BuildListener log;

    /**
     *
     * @return pid of the ProcCleaner process
     */
    public static int getpid(){
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');
        return Integer.parseInt(jvmName.substring(0,index));
    }

    /**
     * @return  'User' of the ProcCleaner process
     */
    public static String getUser(){
        String user;
        if(Functions.isWindows()){
             user = PsProcessWin.getUser(getpid());
        }else{
             user = PsProcessUnix.getUser(getpid());
        }
        return user;
    }

	public BuildListener getLog() {
		return log;
	}
	
	public void setLog(BuildListener log) {
		this.log = log;
	}
	
	public void setup(BuildListener log) {
		this.log = log;
	}
	
	public void tearDown() {
		this.log = null;
	}
	
	public ProcCleanerDescriptor getDescriptor() {
		return (ProcCleanerDescriptor) Jenkins.getInstance().getDescriptor(getClass());
	}

	public static ExtensionList<ProcCleaner> all() {
		return Jenkins.getInstance().getExtensionList(ProcCleaner.class);
	}

	public static DescriptorExtensionList<ProcCleaner,Descriptor<ProcCleaner>> getCleanerDescriptors() {
		return Jenkins.getInstance().<ProcCleaner,Descriptor<ProcCleaner>>getDescriptorList(ProcCleaner.class);
	}
	
	public static class ProcCleanerDescriptor extends Descriptor<ProcCleaner> {
	
		protected ProcCleanerDescriptor(Class<? extends ProcCleaner> clazz) {
			super(clazz);
		}
		
		protected ProcCleanerDescriptor() {
		}
		
		public String getDisplayName() {
            return clazz.getSimpleName();
        }
	}

}
