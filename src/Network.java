import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;

public class Network {
    private final HashTable<Host> hosts;

    // We keep explicit lists so we can iterate hosts/backdoors when needed (type3).
    private final ArrayList<Host> allHosts;
    private final ArrayList<Backdoor> allBackdoors;

    // -----------------------
    // optimizations (type3)
    // -----------------------
    // We cache connectivity info for the *current* graph (no ignores) and we reuse
    // queue/visited structures across repeated traversals to avoid allocations.
    private long topologyVersion = 0;
    private long cachedConnectivityVersion = -1;
    private ConnectivityInfo cachedConnectivityInfo = null;

    // Reused BFS queue storage (avoids java.util.LinkedList allocations)
    private Host[] bfsQueueHosts;
    private Host[] bfsQueueParents;

    /**
     * Identity-based visited set for Hosts.
     *
     * Motivation: In large type3 cases, doing String-key HashTable lookups for
     * every visited check is expensive. Hosts are unique objects in memory; we can
     * use reference equality (==) safely.
     */
    private static class HostIdentitySet {
        private Host[] table;
        private int[] slotToken;
        private int tokenCounter;
        private int activeToken;

        HostIdentitySet(int initialCapacity) {
            int cap = 1;
            while (cap < initialCapacity) cap <<= 1;
            this.table = new Host[Math.max(1024, cap)];
            this.slotToken = new int[this.table.length];
            this.tokenCounter = 1;
            this.activeToken = 1;
        }

        void ensureCapacityFor(int expectedElementCount) {
            // Keep load factor comfortably low for linear probing.
            int requiredCapacity = 1;
            while (requiredCapacity < expectedElementCount * 4) requiredCapacity <<= 1;

            if (requiredCapacity <= table.length) return;

            this.table = new Host[requiredCapacity];
            this.slotToken = new int[requiredCapacity];
            this.tokenCounter = 1;
            this.activeToken = 1;
        }

        void resetForNewTraversal() {
            tokenCounter++;
            if (tokenCounter == Integer.MAX_VALUE) {
                // rare overflow: clear slot tokens
                for (int i = 0; i < slotToken.length; i++) slotToken[i] = 0;
                tokenCounter = 1;
            }
            activeToken = tokenCounter;
        }

        boolean contains(Host host) {
            int mask = table.length - 1;
            int position = System.identityHashCode(host) & mask;

            while (true) {
                if (slotToken[position] != activeToken) return false;
                if (table[position] == host) return true;
                position = (position + 1) & mask;
            }
        }

        void add(Host host) {
            int mask = table.length - 1;
            int position = System.identityHashCode(host) & mask;

            while (true) {
                if (slotToken[position] != activeToken) {
                    slotToken[position] = activeToken;
                    table[position] = host;
                    return;
                }
                if (table[position] == host) return;
                position = (position + 1) & mask;
            }
        }
    }

    private final HostIdentitySet visitedHostsIdentitySet;

    public Network() {
        this.hosts = new HashTable<>(HashTable.HashingMethods.OpenHashing);
        this.allHosts = new ArrayList<>();
        this.allBackdoors = new ArrayList<>();

        this.bfsQueueHosts = new Host[1024];
        this.bfsQueueParents = new Host[1024];

        this.visitedHostsIdentitySet = new HostIdentitySet(1024);
    }

    public void createHost(String hostId, int clearanceLevel) {
        Host h = new Host(hostId, clearanceLevel);
        this.hosts.insert(hostId, h);
        this.allHosts.add(h);
        this.topologyVersion++;
    }

    public boolean hasBackdoor(Host host1, Host host2) {
        return host1.hasBackdoor(host2);
    }

    public Host findHost(String hostId) {
        return this.hosts.find(hostId);
    }

    public Backdoor findBackdoor(Host host1, Host host2) {
        return host1.findBackdoor(host2);
    }

    public Backdoor findBackdoor(String hostId1, String hostId2) {
        Host host1 = this.findHost(hostId1);
        Host host2 = this.findHost(hostId2);

        if (host1 == null || host2 == null) return null;

        return this.findBackdoor(host1, host2);
    }

    public void createBackdoor(Host host1, Host host2, int latency, int bandwidth, int firewallLevel) {
        String bdKey = Backdoor.generateBackdoorKey(host1, host2);
        Backdoor bd = new Backdoor(bdKey, host1, host2, latency, bandwidth, firewallLevel);
        host1.addBackdoor(bd);
        host2.addBackdoor(bd);
        this.allBackdoors.add(bd);
        this.topologyVersion++;
    }

    public void toggleBackdoorSeal(Backdoor backdoor) {
        backdoor.toggleSeal();
        this.topologyVersion++;
    }

    // =============================
    // Type-2: traceroute pathfinding
    // =============================

    /**
     * A label/state in the multi-objective shortest path search.
     *
     * Spec ordering: (1) smallest total dynamic latency (2) then fewer segments
     * (hops) (3) then lexicographically smaller full host sequence
     */
    private static class PathState extends Indexable {
        final Host host;
        final int totalLatency;
        final int hops;
        final PathState parent;

        // lazily computed sequence key for lex tie-break
        private String lexoKeyCache;

        PathState(Host host, int totalLatency, int hops, PathState parent) {
            // Using host.id avoids per-state unique-string allocations.
            // Lex tie-break is handled by getLexoCompareKey() below.
            super(host.getId());
            this.host = host;
            this.totalLatency = totalLatency;
            this.hops = hops;
            this.parent = parent;
        }

        /**
         * Dominance relation to prune Pareto frontier. We prune only on (totalLatency,
         * hops); lex is used only when those two tie.
         */
        boolean dominates(PathState other) {
            if (other == null) return true;

            if (this.totalLatency > other.totalLatency) return false;
            if (this.hops > other.hops) return false;

            if (this.totalLatency < other.totalLatency) return true;
            if (this.hops < other.hops) return true;

            // same (latency,hops) => keep lexicographically smallest path
            return this.getLexoCompareKey().compareTo(other.getLexoCompareKey()) <= 0;
        }

        @Override
        public String getLexoCompareKey() {
            if (lexoKeyCache != null) return lexoKeyCache;

            if (parent == null) {
                lexoKeyCache = host.getId();
                return lexoKeyCache;
            }

            // Use '!' because it is lexicographically smaller than allowed hostId chars.
            // This makes string comparison behave like element-wise sequence comparison.
            lexoKeyCache = parent.getLexoCompareKey() + "!" + host.getId();
            return lexoKeyCache;
        }
    }

    /**
     * Compare two states with the exact spec ordering.
     *
     * @return negative if a is better, positive if b is better, 0 if equal
     */
    private static int compareStates(PathState a, PathState b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        if (a.totalLatency != b.totalLatency) return Integer.compare(a.totalLatency, b.totalLatency);
        if (a.hops != b.hops) return Integer.compare(a.hops, b.hops);
        return a.getLexoCompareKey().compareTo(b.getLexoCompareKey());
    }

    /**
     * λ = 0 case: classic Dijkstra. Because edge costs are static, we only need one
     * best label per host.
     */
    private Route findBackdoorPathNoCongestion(Host origin, Host destination, int minBandwidth) {
        PriorityQueue<PathState> open = new PriorityQueue<>(2, false);
        HashTable<PathState> bestByHost = new HashTable<>(HashTable.HashingMethods.OpenHashing, 262144);

        PathState start = new PathState(origin, 0, 0, null);
        bestByHost.insert(origin.getId(), start);
        open.insert(new int[] { 0, 0 }, start);

        while (!open.isEmpty()) {
            PathState state = open.extract();

            // stale-state check
            PathState best = bestByHost.find(state.host.getId());
            if (best != state && compareStates(best, state) <= 0) continue;

            if (state.host == destination) {
                LinkedList<Host> route = new LinkedList<>();
                for (PathState cur = state; cur != null; cur = cur.parent) {
                    route.addFirst(cur.host);
                }
                return new Route(state.totalLatency, route);
            }

            Host currentHost = state.host;
            for (Backdoor backdoor : currentHost.getBackdoors()) {
                if (backdoor.isSealed()) continue;
                if (backdoor.getBandwidth() < minBandwidth) continue;
                if (currentHost.getClearanceLevel() < backdoor.getFirewallLevel()) continue;

                Host nextHost = (backdoor.getHost1() == currentHost) ? backdoor.getHost2() : backdoor.getHost1();
                int newTotalLatency = state.totalLatency + backdoor.getLatency();
                int newHops = state.hops + 1;

                PathState nextState = new PathState(nextHost, newTotalLatency, newHops, state);
                PathState prev = bestByHost.find(nextHost.getId());

                if (prev == null || compareStates(nextState, prev) < 0) {
                    // Actively remove the previous best label from the queue if it is still there.
                    // This reduces stale extractions significantly in large routing searches.
                    if (prev != null) {
                        int idx = prev.getCurrentIndex();
                        if (idx >= 1) open.removeByIndex(idx);
                    }
                    bestByHost.insert(nextHost.getId(), nextState);
                    open.insert(new int[] { newTotalLatency, newHops }, nextState);
                }
            }
        }

        return new Route(Integer.MAX_VALUE, new LinkedList<>());
    }

    /**
     * Multi-objective shortest path with congestion: dynamic edge cost at step i is
     * baseLatency + λ*(i-1).
     */
    public Route findBackdoorPath(Host origin, Host destination, int minBandwidth, int congestionFactor) {
        if (origin == destination) {
            LinkedList<Host> route = new LinkedList<>();
            route.add(origin);
            return new Route(0, route);
        }

        // λ = 0 => Dijkstra fast path (minimal code, big speedup)
        if (congestionFactor == 0) {
            return findBackdoorPathNoCongestion(origin, destination, minBandwidth);
        }

        // Min-heap by (totalLatency, hops, lexKey)
        PriorityQueue<PathState> open = new PriorityQueue<>(2, false);

        // Host -> list of non-dominated states for that host
        HashTable<DoublyLinkedList<PathState>> frontierByHost = new HashTable<>(HashTable.HashingMethods.OpenHashing, 262144);

        PathState start = new PathState(origin, 0, 0, null);
        DoublyLinkedList<PathState> startFrontier = new DoublyLinkedList<>();
        startFrontier.add(start);
        frontierByHost.insert(origin.getId(), startFrontier);
        open.insert(new int[] { 0, 0 }, start);

        // Branch-and-bound: once we found *any* destination candidate, we use its
        // latency as an upper bound to prune expansions.
        PathState bestDest = null;
        int bestDestLatency = Integer.MAX_VALUE;

        while (!open.isEmpty()) {
            PathState state = open.extract();

            // If PQ already reached the upper bound, no later state can improve it.
            if (bestDest != null && state.totalLatency >= bestDestLatency) break;

            Host currentHost = state.host;

            for (Backdoor backdoor : currentHost.getBackdoors()) {
                // Constraints from spec
                if (backdoor.isSealed()) continue;
                if (backdoor.getBandwidth() < minBandwidth) continue;
                if (currentHost.getClearanceLevel() < backdoor.getFirewallLevel()) continue;

                Host nextHost = (backdoor.getHost1() == currentHost) ? backdoor.getHost2() : backdoor.getHost1();

                // step index i = state.hops + 1
                // extra congestion term λ*(i-1) = λ*state.hops
                int newTotalLatency = state.totalLatency + backdoor.getLatency() + congestionFactor * state.hops;
                int newHops = state.hops + 1;

                PathState nextState = new PathState(nextHost, newTotalLatency, newHops, state);

                // Branch-and-bound prune (latency primary objective)
                if (bestDest != null && newTotalLatency > bestDestLatency) continue;

                // If we can reach destination, update the best candidate and DO NOT
                // push it further (destination has no outgoing relevance).
                if (nextHost == destination) {
                    if (bestDest == null || compareStates(nextState, bestDest) < 0) {
                        bestDest = nextState;
                        bestDestLatency = nextState.totalLatency;
                    }
                    continue;
                }

                DoublyLinkedList<PathState> frontier = frontierByHost.find(nextHost.getId());
                if (frontier == null) {
                    frontier = new DoublyLinkedList<>();
                    frontier.add(nextState);
                    frontierByHost.insert(nextHost.getId(), frontier);
                    open.insert(new int[] { newTotalLatency, newHops }, nextState);
                    continue;
                }

                // 1) If dominated, ignore.
                boolean dominated = false;
                for (DoublyLinkedListNode<PathState> node = frontier.getHead(); node != null; node = node.next) {
                    if (node.getPayload().dominates(nextState)) {
                        dominated = true;
                        break;
                    }
                }
                if (dominated) continue;

                // 2) Remove states dominated by this candidate.
                DoublyLinkedListNode<PathState> node = frontier.getHead();
                while (node != null) {
                    DoublyLinkedListNode<PathState> nextNode = node.next;
                    if (nextState.dominates(node.getPayload())) {
                        // If the dominated state is still in the priority queue, remove it to
                        // prevent future stale extractions.
                        int idx = node.getPayload().getCurrentIndex();
                        if (idx >= 1) open.removeByIndex(idx);
                        frontier.remove(node);
                    }
                    node = nextNode;
                }

                frontier.add(nextState);
                open.insert(new int[] { newTotalLatency, newHops }, nextState);
            }
        }

        if (bestDest != null) {
            LinkedList<Host> route = new LinkedList<>();
            for (PathState cur = bestDest; cur != null; cur = cur.parent) {
                route.addFirst(cur.host);
            }
            return new Route(bestDest.totalLatency, route);
        }

        return new Route(Integer.MAX_VALUE, new LinkedList<>());
    }

    // =============================
    // Type-3: scan / simulate / report
    // =============================

    private record ConnectivityInfo(int totalHosts, int components, boolean hasCycle) {
    }

    /**
     * Ensures internal arrays can hold at least {@code requiredSize} elements.
     */
    private void ensureBfsQueueCapacity(int requiredSize) {
        if (bfsQueueHosts.length >= requiredSize) return;

        int newSize = Math.max(bfsQueueHosts.length * 2, requiredSize);
        Host[] newHosts = new Host[newSize];
        Host[] newParents = new Host[newSize];
        for (int i = 0; i < bfsQueueHosts.length; i++) {
            newHosts[i] = bfsQueueHosts[i];
            newParents[i] = bfsQueueParents[i];
        }
        bfsQueueHosts = newHosts;
        bfsQueueParents = newParents;
    }

    /**
     * Connectivity analysis over UNSEALED backdoors only.
     *
     * - ignoreHostIds: host IDs to pretend removed - ignoreBackdoorKeys: backdoor
     * keys to pretend removed (Backdoor.generateBackdoorKey)
     */
    private ConnectivityInfo analyzeConnectivity(ArrayList<String> ignoreHostIds, ArrayList<String> ignoreBackdoorKeys) {
        // optimizations:
        // - Convert ignore lists to Host / key values once (no repeated lookups).
        // - Use identity-based visited set (HostIdentitySet) for very fast membership.
        // - Use array-backed BFS queue (no LinkedList node allocations).
        // - Use parent Host reference directly (no parent HashTable allocations).

        ensureBfsQueueCapacity(allHosts.size());
        visitedHostsIdentitySet.ensureCapacityFor(allHosts.size());
        visitedHostsIdentitySet.resetForNewTraversal();

        // Convert ignore host IDs -> Host references (usually size 0 or 1)
        Host ignoredHost = null;
        if (ignoreHostIds != null && !ignoreHostIds.isEmpty()) {
            ignoredHost = this.findHost(ignoreHostIds.get(0));
        }

        // Convert ignore backdoor keys -> a single key (usually size 0 or 1)
        String ignoredBackdoorKey = null;
        if (ignoreBackdoorKeys != null && !ignoreBackdoorKeys.isEmpty()) {
            ignoredBackdoorKey = ignoreBackdoorKeys.get(0);
        }

        int totalHostsConsidered = allHosts.size() - (ignoredHost == null ? 0 : 1);

        if (totalHostsConsidered <= 1) {
            return new ConnectivityInfo(totalHostsConsidered, totalHostsConsidered == 0 ? 0 : 1, false);
        }

        int components = 0;
        boolean hasCycle = false;

        for (Host startHost : allHosts) {
            if (startHost == ignoredHost) continue;
            if (visitedHostsIdentitySet.contains(startHost)) continue;

            components++;

            // BFS queue contains (node, parent) pairs.
            int queueHeadIndex = 0;
            int queueTailIndex = 0;
            bfsQueueHosts[queueTailIndex] = startHost;
            bfsQueueParents[queueTailIndex] = null;
            queueTailIndex++;
            visitedHostsIdentitySet.add(startHost);

            while (queueHeadIndex < queueTailIndex) {
                Host currentHostNode = bfsQueueHosts[queueHeadIndex];
                Host parentHostNode = bfsQueueParents[queueHeadIndex];
                queueHeadIndex++;

                for (Backdoor backdoorEdge : currentHostNode.getBackdoors()) {
                    if (backdoorEdge.isSealed()) continue;
                    if (ignoredBackdoorKey != null && backdoorEdge.key().equals(ignoredBackdoorKey)) continue;

                    Host neighborHostNode = (backdoorEdge.getHost1() == currentHostNode) ? backdoorEdge.getHost2()
                            : backdoorEdge.getHost1();
                    if (neighborHostNode == ignoredHost) continue;

                    if (!visitedHostsIdentitySet.contains(neighborHostNode)) {
                        visitedHostsIdentitySet.add(neighborHostNode);
                        bfsQueueHosts[queueTailIndex] = neighborHostNode;
                        bfsQueueParents[queueTailIndex] = currentHostNode;
                        queueTailIndex++;
                    } else if (neighborHostNode != parentHostNode) {
                        hasCycle = true;
                    }
                }
            }
        }

        return new ConnectivityInfo(totalHostsConsidered, components, hasCycle);
    }

    private ConnectivityInfo getCachedConnectivityInfo() {
        if (cachedConnectivityInfo != null && cachedConnectivityVersion == topologyVersion) {
            return cachedConnectivityInfo;
        }

        cachedConnectivityInfo = analyzeConnectivity(new ArrayList<>(), new ArrayList<>());
        cachedConnectivityVersion = topologyVersion;
        return cachedConnectivityInfo;
    }

    /** Returns number of connected components using only unsealed backdoors. */
    public int scanConnectivityComponents() {
        ConnectivityInfo info = getCachedConnectivityInfo();
        return info.totalHosts <= 1 ? 1 : info.components;
    }

    public int countComponentsAfterHostRemoval(Host removedHost) {
        ArrayList<String> ignoreHosts = new ArrayList<>();
        ignoreHosts.add(removedHost.getId());
        ConnectivityInfo info = analyzeConnectivity(ignoreHosts, new ArrayList<>());
        return info.totalHosts <= 1 ? 1 : info.components;
    }

    public int countComponentsAfterBackdoorRemoval(Backdoor removedBackdoor) {
        ArrayList<String> ignoreBackdoors = new ArrayList<>();
        ignoreBackdoors.add(removedBackdoor.key());
        ConnectivityInfo info = analyzeConnectivity(new ArrayList<>(), ignoreBackdoors);
        return info.totalHosts <= 1 ? 1 : info.components;
    }

    public String oracleReport() {
        int totalHosts = this.allHosts.size();

        int unsealedBackdoors = 0;
        long sumBandwidth = 0;
        for (Backdoor bd : this.allBackdoors) {
            if (!bd.isSealed()) {
                unsealedBackdoors++;
                sumBandwidth += bd.getBandwidth();
            }
        }

        long sumClearance = 0;
        for (Host h : this.allHosts) sumClearance += h.getClearanceLevel();

        ConnectivityInfo info = getCachedConnectivityInfo();
        boolean connected = info.totalHosts <= 1 || info.components == 1;

        double avgBandwidth = (unsealedBackdoors == 0) ? 0.0 : (double) sumBandwidth / unsealedBackdoors;
        double avgClearance = (totalHosts == 0) ? 0.0 : (double) sumClearance / totalHosts;

        String avgBandwidthStr = BigDecimal.valueOf(avgBandwidth).setScale(1, RoundingMode.HALF_UP).toPlainString();
        String avgClearanceStr = BigDecimal.valueOf(avgClearance).setScale(1, RoundingMode.HALF_UP).toPlainString();

        StringBuilder sb = new StringBuilder();
        sb.append("--- Resistance Network Report ---\n");
        sb.append("Total Hosts: ").append(totalHosts).append('\n');
        sb.append("Total Unsealed Backdoors: ").append(unsealedBackdoors).append('\n');
        sb.append("Network Connectivity: ").append(connected ? "Connected" : "Disconnected").append('\n');
        sb.append("Connected Components: ").append(info.totalHosts <= 1 ? 1 : info.components).append('\n');
        sb.append("Contains Cycles: ").append(info.hasCycle ? "Yes" : "No").append('\n');
        sb.append("Average Bandwidth: ").append(avgBandwidthStr).append("Mbps\n");
        sb.append("Average Clearance Level: ").append(avgClearanceStr);

        return sb.toString();
    }

    public boolean contains(String hostId) {
        Host h = this.hosts.find(hostId);
        return h != null;
    }
}
