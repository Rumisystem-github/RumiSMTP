package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.TYPE.LOG_LEVEL;

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
