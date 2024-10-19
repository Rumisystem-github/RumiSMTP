package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_SYSTEM.LOG_PRINT;

import java.io.PrintWriter;

import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_LEVEL;

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

		LOG_PRINT(SOURCE + "->" + IP + "|" + TEXT, LOG_TYPE.INFO, LOG_LEVEL.DEBUG);
	}
}
