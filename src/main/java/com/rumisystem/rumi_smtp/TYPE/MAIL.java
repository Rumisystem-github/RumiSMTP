package com.rumisystem.rumi_smtp.TYPE;

import java.util.ArrayList;
import java.util.List;

public class MAIL {
	public String DOMAIN = null;
	public String FROM = null;
	public List<String> TO = new ArrayList<String>();
	public StringBuilder TEXT = new StringBuilder();
}
