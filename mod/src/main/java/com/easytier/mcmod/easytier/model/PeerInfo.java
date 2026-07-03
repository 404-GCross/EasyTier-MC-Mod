package com.easytier.mcmod.easytier.model;

/**
 * Represents a connected peer/node in the EasyTier network.
 */
public class PeerInfo {
    public String cidr;
    public String hostname;
    public String cost;
    public double latencyMs;
    public double lossRate;
    public long rxBytes;
    public long txBytes;
    public String tunnelProto;
    public String natType;
    public int peerId;
    public String version;

    public PeerInfo() {}

    public boolean isLocal() {
        return "Local".equals(cost);
    }
}
