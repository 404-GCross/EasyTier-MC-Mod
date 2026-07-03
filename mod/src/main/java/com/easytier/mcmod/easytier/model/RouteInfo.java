package com.easytier.mcmod.easytier.model;

/**
 * Represents route information in the EasyTier network.
 */
public class RouteInfo {
    public String ipv4;
    public String hostname;
    public String proxyCidrs;
    public String nextHopIpv4;
    public String nextHopHostname;
    public double nextHopLat;
    public int pathLen;
    public int pathLatency;
    public String version;

    public RouteInfo() {}
}
