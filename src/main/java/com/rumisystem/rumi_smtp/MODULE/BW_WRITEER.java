package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.PrintWriter;

import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class BW_WRITEER {
	private PrintWriter BW;
	private String IP;

	public BW_WRITEER(PrintWriter BW, String IP) {
		this.BW = BW;
		this.IP = IP;
	}
	
	public void SEND(String TEXT) {
		BW.print(TEXT + "\r\n");
		BW.flush();
		
		LOG(LOG_TYPE.INFO, "->" + IP + "|" + TEXT);
	}
}
