
/**
 * Application service layer: validates command semantics and delegates graph
 * work to {@link Network}. Produces spec-compliant log messages.
 */
public class Service {
    private final Network network;

    public Service(Network network) {
        this.network = network;
    }

    public Response createHost(String hostId, int clearanceLevel) {
        if (this.network.contains(hostId)) return Response.failure("spawn_host", "HOST_ALREADY_EXISTS");

        for (int i = 0; i < hostId.length(); i++) {
            char c = hostId.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';

            if (!ok) return Response.failure("spawn_host", "ILLEGAL_CHARACTER");
        }

        this.network.createHost(hostId, clearanceLevel);

        String responseMessage = "Spawned host " + hostId + " with clearance level " + clearanceLevel + ".";
        return Response.success("spawn_host", responseMessage);
    }

    public Response linkBackdoor(String hostId1, String hostId2, int latency, int bandwidth, int firewallLevel) {
        if (hostId1.equals(hostId2)) return Response.failure("link_backdoor", "HOSTS_ARE_THE_SAME");

        Host host1 = this.network.findHost(hostId1);
        Host host2 = this.network.findHost(hostId2);

        if (host1 == null || host2 == null) return Response.failure("link_backdoor", "HOST_NOT_FOUND");

        if (this.network.hasBackdoor(host1, host2)) return Response.failure("link_backdoor", "HOST_HAS_BACKDOOR");

        if (latency <= 0) return Response.failure("link_backdoor", "INVALID_LATENCY");
        if (bandwidth <= 0) return Response.failure("link_backdoor", "INVALID_BANDWIDTH");
        if (firewallLevel < 0) return Response.failure("link_backdoor", "INVALID_FIREWALL_LEVEL");

        this.network.createBackdoor(host1, host2, latency, bandwidth, firewallLevel);

        String responseMessage = "Linked " + hostId1 + " <-> " + hostId2 + " with latency " + latency + "ms, bandwidth " + bandwidth
                + "Mbps, firewall " + firewallLevel + ".";
        return Response.success("link_backdoor", responseMessage);
    }

    public Response sealBackdoor(String hostId1, String hostId2) {
        if (hostId1.equals(hostId2)) return Response.failure("seal_backdoor", "HOSTS_ARE_THE_SAME");

        Backdoor backdoor = this.network.findBackdoor(hostId1, hostId2);
        if (backdoor == null) return Response.failure("seal_backdoor", "HOSTS_HAS_NO_BACKDOOR");

        // optimization: keep Network's topology version in sync for cached reports.
        this.network.toggleBackdoorSeal(backdoor);

        String responseMessage = "Backdoor " + hostId1 + " <-> " + hostId2;

        if (backdoor.isSealed()) responseMessage += " sealed.";
        else responseMessage += " unsealed.";

        return Response.success("seal_backdoor", responseMessage);
    }

    public Response traceRoute(String sourceId, String destId, int minBandwidth, int congestionFactor) {
        Host source = this.network.findHost(sourceId);
        Host destination = this.network.findHost(destId);

        if (source == null || destination == null) {
            return Response.failure("trace_route", "HOST_NOT_FOUND");
        }

        Route route = this.network.findBackdoorPath(source, destination, minBandwidth, congestionFactor);

        boolean hasRoute = source.equals(destination)
                || (route.intersections().size() > 1 && route.intersections().getFirst().equals(source));

        if (!hasRoute) {
            String msg = "No route found from " + sourceId + " to " + destId;
            return Response.success("trace_route", msg);
        }

        int totalLatency = route.totalLatency();

        StringBuilder sb = new StringBuilder();
        sb.append("Optimal route ").append(sourceId).append(" -> ").append(destId).append(": ");

        for (int i = 0; i < route.intersections().size(); i++) {
            sb.append(route.intersections().get(i).getId());
            if (i < route.intersections().size() - 1) sb.append(" -> ");
        }

        sb.append(" (Latency = ").append(totalLatency).append("ms)");

        return Response.success("trace_route", sb.toString());
    }

    public Response scanConnectivity() {
        int components = this.network.scanConnectivityComponents();
        // Convention: |H| in {0,1} is considered fully connected (components treated as
        // 1)
        if (components <= 1) {
            return Response.success("scan_connectivity", "Network is fully connected.");
        }
        return Response.success("scan_connectivity", "Network has " + components + " disconnected components.");
    }

    public Response oracleReport() {
        return Response.success("oracle_report", this.network.oracleReport());
    }

    public Response simulateBreach(String hostId) {
        Host h = this.network.findHost(hostId);
        if (h == null) return Response.failure("simulate_breach", "HOST_NOT_FOUND");

        int baseComponents = this.network.scanConnectivityComponents();
        int afterComponents = this.network.countComponentsAfterHostRemoval(h);

        if (afterComponents > baseComponents) {
            String msg = "Host " + hostId + " IS an articulation point.\n" + "Failure results in " + afterComponents
                    + " disconnected components.";
            return Response.success("simulate_breach", msg);
        }

        return Response.success("simulate_breach", "Host " + hostId + " is NOT an articulation point. Network remains the same.");
    }

    public Response simulateBreach(String hostId1, String hostId2) {
        Host h1 = this.network.findHost(hostId1);
        Host h2 = this.network.findHost(hostId2);
        if (h1 == null || h2 == null) return Response.failure("simulate_breach", "HOST_NOT_FOUND");

        Backdoor bd = this.network.findBackdoor(h1, h2);
        if (bd == null) return Response.failure("simulate_breach", "NO_BACKDOOR");
        if (bd.isSealed()) return Response.failure("simulate_breach", "BACKDOOR_IS_SEALED");

        int baseComponents = this.network.scanConnectivityComponents();
        int afterComponents = this.network.countComponentsAfterBackdoorRemoval(bd);

        if (afterComponents > baseComponents) {
            String msg = "Backdoor " + hostId1 + " <-> " + hostId2 + " IS a bridge.\n" + "Failure results in " + afterComponents
                    + " disconnected components.";
            return Response.success("simulate_breach", msg);
        }

        return Response.success("simulate_breach",
                "Backdoor " + hostId1 + " <-> " + hostId2 + " is NOT a bridge. Network remains the same.");
    }

    public Response simulateBreach(String[] args) {
        // simulate_breach <hostId> OR simulate_breach <hostId1> <hostId2>
        if (args.length == 2) {
            return simulateBreach(args[1]);
        }

        if (args.length == 3) {
            return simulateBreach(args[1], args[2]);
        }

        return Response.failure("simulate_breach", "INVALID_ARGS");
    }

}