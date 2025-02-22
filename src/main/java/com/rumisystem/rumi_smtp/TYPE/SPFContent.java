package com.rumisystem.rumi_smtp.TYPE;

import java.util.List;

public class SPFContent {
	private SPFAllowIP AllowIP = null;
	private List<String> IPList = null;

	public SPFContent(SPFAllowIP AllowIP, List<String> IPList) {
		this.AllowIP = AllowIP;
		this.IPList = IPList;
	}

	public SPFAllowIP GetAllowIP() {
		return AllowIP;
	}

	public List<String> GetIPList() {
		return IPList;
	}
}
