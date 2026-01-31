public class Backdoor extends Indexable {
    private final Host host1;
    private final Host host2;
    private final int latency;
    private final int bandwidth;
    private final int firewallLevel;
    private boolean isSealed;

    public Backdoor(String key, Host host1, Host host2, int latency, int bandwidth, int firewallLevel) {
        super(key);
        this.host1 = host1;
        this.host2 = host2;
        this.latency = latency;
        this.bandwidth = bandwidth;
        this.firewallLevel = firewallLevel;
    }

    public void toggleSeal() {
        this.isSealed = !this.isSealed;
    }

    public boolean isSealed() {
        return this.isSealed;
    }

    public Host getHost1() {
        return host1;
    }

    public Host getHost2() {
        return host2;
    }

    public int getLatency() {
        return latency;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getFirewallLevel() {
        return firewallLevel;
    }

    public static String generateBackdoorKey(Host host1, Host host2) {
        String bdKey;

        if (host1.key().compareTo(host2.key()) > 0) bdKey = host1.key() + host2.key();
        else bdKey = host2.key() + host1.key();

        return bdKey;
    }

}
