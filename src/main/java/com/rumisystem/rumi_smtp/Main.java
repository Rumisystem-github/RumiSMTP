package com.rumisystem.rumi_smtp;

import java.io.IOException;

import com.rumisystem.rumi_smtp.POP.POP;
import com.rumisystem.rumi_smtp.SMTP.SMTP;

public class Main {
	public static boolean develop = true;

	public static void main(String[] args) throws IOException {
		Config.load();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new SMTP(Config.SMTP.TransferPort, true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new SMTP(Config.SMTP.SubmissionPort, false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new POP(Config.POP.Port);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();
	}
}
