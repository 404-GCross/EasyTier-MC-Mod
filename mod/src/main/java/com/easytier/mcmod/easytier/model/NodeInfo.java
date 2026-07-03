package com.easytier.mcmod.easytier.model;

import java.util.List;

/**
 * Represents the local EasyTier node information.
 */
public class NodeInfo {
    public String virtualIp;
    public String hostname;
    public int peerId;
    public String stunType;
    public String publicIpv4;
    public String publicIpv6;
    public List<String> proxyCidrs;
    public List<String> listeners;
    public String version;
    public String config;

    public NodeInfo() {}
}
