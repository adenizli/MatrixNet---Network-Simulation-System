/**
 * Parses console commands and dispatches them to {@link Service}. Keeps I/O
 * independent from core logic (Network/Service).
 */
public class Controller {
    private final Service service;

    Controller(Service s) {
        this.service = s;
    }

    /**
     * Routes a tokenized command line to the corresponding service method.
     *
     * @param args tokenized input line (command name is args[0])
     * @return log line to be written to output
     */
    public String handleRequest(String[] args) {
        String endpoint = args[0];
        Response r = null;
        switch (endpoint) {
        case "spawn_host" -> {
            String hostId = args[1];
            int clearanceLevel = Integer.parseInt(args[2]);
            r = this.service.createHost(hostId, clearanceLevel);
        }

        case "link_backdoor" -> {
            String hostId1 = args[1];
            String hostId2 = args[2];
            int latency = Integer.parseInt(args[3]);
            int bandwidth = Integer.parseInt(args[4]);
            int firewallLevel = Integer.parseInt(args[5]);
            r = this.service.linkBackdoor(hostId1, hostId2, latency, bandwidth, firewallLevel);
        }

        case "seal_backdoor" -> {
            String hostId1 = args[1];
            String hostId2 = args[2];
            r = this.service.sealBackdoor(hostId1, hostId2);
        }

        case "trace_route" -> {
            String sourceId = args[1];
            String destId = args[2];
            int minBandwidth = Integer.parseInt(args[3]);
            int congestionFactor = Integer.parseInt(args[4]);
            r = this.service.traceRoute(sourceId, destId, minBandwidth, congestionFactor);
        }

        case "scan_connectivity" -> r = this.service.scanConnectivity();

        case "oracle_report" -> r = this.service.oracleReport();

        case "simulate_breach" -> {
            if (args.length == 2) {
                String hostId = args[1];
                r = this.service.simulateBreach(hostId);
            } else {
                String hostId1 = args[1];
                String hostId2 = args[2];
                r = this.service.simulateBreach(hostId1, hostId2);
            }
        }

        }

        return r == null ? Response.endpointNotFound() : r.generateResponseMessage();
    }
}
