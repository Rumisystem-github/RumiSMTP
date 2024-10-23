package com.rumisystem.rumi_smtp.LOG_SYSTEM;

import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

public class LOG_SYSTEM {
	public static void LOG_PRINT(String TEXT, LOG_TYPE TYPE, LOG_LEVEL LEVEL) {
		switch (LEVEL) {
			case INFO: {
				LOG(TYPE, TEXT);
				break;
			}
			
			case DEBUG: {
				if (CONFIG_DATA.get("LOG").asInt("LEVEL") == 1) {
					LOG(TYPE, TEXT);
					break;
				}
			}
		}
	}
}
