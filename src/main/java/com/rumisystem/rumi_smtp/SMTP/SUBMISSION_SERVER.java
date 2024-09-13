package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

import java.security.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.net.ssl.*;

public class SUBMISSION_SERVER {
	private static int PORT = 25;
	private static int MAX_SIZE = 0;

	public static void Main() throws Exception {
		PORT = CONFIG_DATA.get("SUBMISSION").asInt("PORT");
		MAX_SIZE = CONFIG_DATA.get("SMTP").asInt("MAX_SIZE");
	}
}
