package org.jenkinsci.plugins.proccleaner;

import static hudson.util.jna.GNUCLibrary.LIBC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents process tree obtain from ps utility. 
 * Should work on Linux and most Unixes (at least Sun, HP and AIX)
 * 
 * @author vjuranek
 *
 */
public class PsBasedProcessTree {
	
	private List<PsProcess> processList;
	private PrintStream log;
	
	public PsBasedProcessTree() {
		this.processList = new ArrayList<PsBasedProcessTree.PsProcess>();
	}
	
	public List<PsProcess> getProcessList() {
		return processList;
	}
	
	public PrintStream getLog() {
		return log;
	}
	
	public void setLog(PrintStream log) {
		this.log = log;
	}
	
	public void addProcess(String psLine) {
		processList.add(createFromString(psLine));
	}
	
	public PsProcess getByPid(int pid) {
		for(PsProcess p : PsBasedProcessTree.this.processList)
			if(pid == p.pid)
				return p;
		return null;
	}
	
	public PsProcess createFromString(String psLine) {
		//System.out.println("Creating ps:" + psLine.trim());
		String[] ps = psLine.trim().split(" +", 3);
		if(ps.length < 3)
			return null;
		return new PsProcess(s2i(ps[0]), s2i(ps[1]), ps[2]);
	}
	
	private int s2i(String str) {
		return new Integer(str.trim()).intValue();
	}
	
	public static PsBasedProcessTree createProcessTreeFor(String user) throws InterruptedException, IOException {
		String[] cmd = {"ps","-u",user,"-o","pid,ppid,args"};
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		Process proc = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		int ec = proc.waitFor();
		PsBasedProcessTree ptree = new PsBasedProcessTree();
		String line = reader.readLine(); // first line should be "PID  PPID COMMAND" - skip it
		while((line = reader.readLine()) != null)
			ptree.addProcess(line);
		return ptree;
	}
	
	
	/**
	 * Represent process created by parsing ps output line
	 * 
	 * @author vjuranek
	 *
	 */
	public class PsProcess {
		
		private final int pid;
		private final int ppid;
		private final String args;
		
		public PsProcess(int pid, int ppid, String args) {
			this.pid = pid;
			this.ppid = ppid;
			this.args = args;
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
			return PsBasedProcessTree.this.getByPid(ppid);
		}
		
		public PsBasedProcessTree getProcessTree() {
			return PsBasedProcessTree.this;
		}

		public List<PsProcess> getChildren() {
			List<PsProcess> children = new ArrayList<PsProcess>();
			for(PsProcess p : PsBasedProcessTree.this.processList) 
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
			System.out.println("Killing " + this);
			if(log != null)
				log.println("Killing " + this);
			killHard();
		}
		
		public void killHard() {
			kill(9);
		}
		
		public void kill(int signum) {
			LIBC.kill(pid, signum);
		}
		
		public void killAllExceptMe() {
			System.out.println("Proc kill");
			Map<Integer, PsProcess> ph = getParentHierarchy(this);
			for(PsProcess p : PsBasedProcessTree.this.processList) 
				if(this != p && !ph.containsKey(new Integer(p.pid))) // don't kill myself (and parent) //TODO should contain whole possible hierarchy
					p.kill();
		}
		/*
		public Map<Integer,PsProcess> getParentHierarchy() {
			Map<Integer,PsProcess> ph = new HashMap<Integer, PsBasedProcessTree.PsProcess>();
			return ph;
		}
		*/
		public Map<Integer, PsProcess> getParentHierarchy(PsProcess p) {
			Map<Integer, PsProcess> ph = new HashMap<Integer, PsBasedProcessTree.PsProcess>();
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
	
	
	/*
	  
   PID  PPID COMMAND
14833 14829 sshd: hudson@pts/0
14834 14833 -bash
14926 14834 /usr/bin/mc -P /tmp/mc-hudson/mc.pwd.14834
14928 14926 bash -rcfile .bashrc
15063 30851 /qa/tools/opt/amd64/jdk1.6.0_last/bin/java -Dqe.artifacts=/home/hudson/static_build_env/teiid-test-artifacts/scenario-deploy-artifacts -Dfile=scripts/soa/teiid_querytesting.groovy -Dserver.name=production -Dds.output.dir=testresults -Dctc.framework.version=/home/hudson/static_build_env/bqt_testing_framework/16 -Dbits.type=EMBEDDED -Dds.config.file=/home/hudson/static_build_env/bqt_testing_framework/16/ctc_tests/ctc-test.properties -Dds.result.mode=COMPARE -Dds.queryset.dir=/home/hudson/static_build_env/teiid-test-artifacts/ctc-tests/queries -classpath /qa/tools/opt/groovy-1.7.6/lib/groovy-1.7.6.jar -Dscript.name=/qa/tools/opt/groovy-1.7.6/bin/groovy -Dprogram.name=groovy -Dgroovy.starter.conf=/qa/tools/opt/groovy-1.7.6/conf/groovy-starter.conf -Dgroovy.home=/qa/tools/opt/groovy-1.7.6 -Dtools.jar=/qa/tools/opt/amd64/jdk1.6.0_last/lib/tools.jar org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain --conf /qa/tools/opt/groovy-1.7.6/conf/groovy-starter.conf --classpath . /mnt/hudson_workspace/workspace/soa-teiid-bqt-pass1_matrix/jdk/java16_default/label/RHEL6_x86_64/scripts/common/run.groovy
16403 16400 sshd: hudson@pts/2
16404 16403 -bash
16525 15063 /qa/tools/opt/x86_64/jdk1.6.0_30/bin/java -cp /mnt/hudson_workspace/workspace/soa-teiid-bqt-pass1_matrix/jdk/java16_default/label/RHEL6_x86_64/jbosssoa/jboss-as/bin/run.jar:/qa/tools/opt/amd64/jdk1.6.0_last/lib/tools.jar -Dhornetq.broadcast.bg-group1.address=231.125.28.156 -Dhornetq.broadcast.dg-group1.address=231.125.28.156 -Xmx1303m -XX:MaxPermSize=1024m -Djboss.server.log.threshold=INFO -Djboss.messaging.ServerPeerID=1 -Djava.endorsed.dirs=/mnt/hudson_workspace/workspace/soa-teiid-bqt-pass1_matrix/jdk/java16_default/label/RHEL6_x86_64/jbosssoa/jboss-as/lib/endorsed -Djgroups.udp.ip_ttl=0 org.jboss.Main -c production -b localhost -u 227.43.88.174
18120 15063 /qa/tools/opt/x86_64/jdk1.6.0_30/jre/bin/java -Xmx1024m -Dconfig=/home/hudson/static_build_env/bqt_testing_framework/16/ctc_tests/ctc-test.properties -Dscenariofile=/home/hudson/static_build_env/teiid-test-artifacts/scenario-deploy-artifacts/PassOne/scenarios/mysql50_bqt_push.properties -Dqueryset.artifacts.dir=/home/hudson/static_build_env/teiid-test-artifacts/ctc-tests/queries -Dquery.scenario.classname=org.jboss.bqt.test.client.ctc.CTCQueryScenario -Dserver.host.name=localhost -Dproj.dir=/home/hudson/static_build_env/bqt_testing_framework/16 -Doutput.dir=/mnt/hudson_workspace/workspace/soa-teiid-bqt-pass1_matrix/jdk/java16_default/label/RHEL6_x86_64/testresults/PassOne -Dresult.mode=COMPARE -Dexceedpercent=-1 -Dexectimemin=-1 -Dusername=user -Dpassword=user -classpath /home/hudson/static_build_env/bqt_testing_framework/16/lib/bqt-client-0.0.1-SNAPSHOT.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/bqt-core-0.0.1-SNAPSHOT.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/bqt-framework-0.0.1-SNAPSHOT.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/bqt-jdk-16-support-0.0.1-SNAPSHOT.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/commons-collections-3.2.1.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/jdom-1.0.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/junit-4.4.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/log4j-1.2.14.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/slf4j-api-1.5.6.jar:/home/hudson/static_build_env/bqt_testing_framework/16/lib/teiid-client.jar org.jboss.bqt.test.client.TestClient
18161 16404 ps -u hudson -o pid,ppid,args
30850 30846 sshd: hudson@notty
30851 30850 /qa/tools/opt/amd64/jdk1.6.0_last/bin/java -Djava.net.preferIPv4Stack=true -Djava.home=/qa/tools/opt/amd64/jdk1.6.0_last/jre -Xmx700m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -jar /home/hudson/hudson_release/WEB-INF/slave.jar
	 
	 */
	

}