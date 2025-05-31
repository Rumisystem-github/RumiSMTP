package com.rumisystem.rumi_smtp.MODULE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TelnetParse {
	public static String[] Parse(byte[] Body, ByteArrayOutputStream BAOS) throws IOException {
		//コマンド受信
		BAOS.write(Body);
		BAOS.flush();

		byte[] All = BAOS.toByteArray();
		int Last = 0;
		List<String> CommandLineList = new ArrayList<String>();

		for (int I = 0; I < All.length - 1; I++) {
			//↓\r\nだよ
			if (All[I] == 0x0D && All[I+1] == 0x0A) {
				byte[] CommandBytes = Arrays.copyOfRange(All, Last, I);
				String CommandLine = new String(CommandBytes, StandardCharsets.UTF_8);

				CommandLineList.add(CommandLine);

				//次の行の先頭へ
				I++;//\n
				Last = I + 1;
			}
		}

		//未処理の残りを保持(全部処理したならresetする)
		if (Last > 0) {
			byte[] ReMain = Arrays.copyOfRange(All, Last, All.length);
			BAOS.reset();
			BAOS.write(ReMain);
		}

		return CommandLineList.toArray(new String[0]);
	}
}
