package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.PrintWriter;

import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class BW_WRITEER {
	private PrintWriter BW;
	private String IP;
	private String SOURCE;

	public BW_WRITEER(PrintWriter BW, String IP, String SOURCE) {
		this.BW = BW;
		this.IP = IP;
		this.SOURCE = SOURCE;
	}
	
	public void SEND(String TEXT) {
		BW.print(TEXT + "\r\n");
		BW.flush();
		
		LOG(LOG_TYPE.INFO, SOURCE + "->" + IP + "|" + TEXT);
	}
}
