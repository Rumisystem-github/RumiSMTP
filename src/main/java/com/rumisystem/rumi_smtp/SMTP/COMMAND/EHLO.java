package com.rumisystem.rumi_smtp.SMTP.COMMAND;

import java.io.PrintWriter;

import com.rumisystem.rumi_smtp.MODULE.BW_WRITEER;

public class EHLO {
	public static void Main(BW_WRITEER BWW, String DOMAIN, int MAX_SIZE) {
		BWW.SEND("250-rumiserver.com Hello " + DOMAIN);
		//BWW.SEND("250-STARTTLS");
		BWW.SEND("250 SIZE " + MAX_SIZE);
	}
}
