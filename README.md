# MatrixNet - Network Simulation System

> **Bogazici University - CMPE 250 Data Structures and Algorithms**  
> Programming Assignment 3

A Java-based network simulation system that models a resistance network with hosts and backdoor connections. The system supports pathfinding with congestion modeling, connectivity analysis, and network vulnerability assessment.

## Quick Start

```bash
# Clone and navigate to directory
cd 2023400309

# Option 1: Use the run script (recommended)
./run.sh testcases/inputs/example.txt output.txt

# Option 2: Manual compilation and run
javac -d out src/*.java
java -cp out Main testcases/inputs/example.txt output.txt
```

## Project Overview

MatrixNet simulates a network of interconnected hosts (access points) connected via backdoors (edges). Each host has a clearance level, and each backdoor has properties like latency, bandwidth, and firewall level. The system supports various operations including:

- **Host Management**: Create hosts with unique IDs and clearance levels
- **Backdoor Management**: Link hosts with backdoors, seal/unseal connections
- **Pathfinding**: Find optimal routes between hosts considering latency, bandwidth, and congestion
- **Network Analysis**: Analyze connectivity, detect cycles, find articulation points and bridges

## Project Structure

```
2023400309/
├── src/                          # Source files
│   ├── Main.java                 # Entry point
│   ├── Controller.java           # Command dispatcher
│   ├── Service.java              # Business logic layer
│   ├── Network.java              # Graph algorithms
│   ├── Host.java                 # Host entity
│   ├── Backdoor.java             # Edge entity
│   ├── Route.java                # Path result record
│   ├── Response.java             # Response formatting
│   ├── HashTable.java            # Hash table implementation
│   ├── PriorityQueue.java        # Priority queue implementation
│   ├── DoublyLinkedList.java     # Linked list implementation
│   ├── DoublyLinkedListNode.java # List node
│   └── Indexable.java            # Base class for indexed objects
├── testcases/
│   ├── inputs/                   # Test input files
│   │   ├── example.txt           # Simple example
│   │   ├── type1_small.txt
│   │   ├── type1_large.txt
│   │   └── ...
│   └── outputs/                  # Expected outputs
│       ├── example_out.txt
│       └── ...
├── run.sh                        # Build and run script
└── README.md
```

## Architecture

The project follows a layered MVC-like architecture:

```
Main.java           → Entry point, I/O handling
    ↓
Controller.java     → Command parsing and routing
    ↓
Service.java        → Business logic and validation
    ↓
Network.java        → Graph operations and algorithms
    ↓
Data Structures     → HashTable, PriorityQueue, DoublyLinkedList
```

### Core Classes

| Class              | Description                                                                           |
| ------------------ | ------------------------------------------------------------------------------------- |
| `Host`             | Represents a network access point with ID and clearance level                         |
| `Backdoor`         | Edge connecting two hosts with latency, bandwidth, and firewall properties            |
| `Network`          | Graph structure with pathfinding (Dijkstra/multi-objective) and connectivity analysis |
| `HashTable`        | Generic hash table with open hashing (separate chaining)                              |
| `PriorityQueue`    | Binary heap with multi-criteria comparison and index tracking                         |
| `DoublyLinkedList` | Generic doubly linked list for efficient insertions/deletions                         |

## Supported Commands

### 1. spawn_host

Creates a new host in the network.

```
spawn_host <host_id> <clearance_level>
```

- `host_id`: Unique identifier (uppercase letters, digits, underscore only)
- `clearance_level`: Integer representing access level

### 2. link_backdoor

Creates a backdoor connection between two hosts.

```
link_backdoor <host_id1> <host_id2> <latency> <bandwidth> <firewall_level>
```

- `latency`: Connection delay in milliseconds (must be > 0)
- `bandwidth`: Connection speed in Mbps (must be > 0)
- `firewall_level`: Minimum clearance required to traverse (must be >= 0)

### 3. seal_backdoor

Toggles the sealed state of a backdoor (sealed backdoors cannot be traversed).

```
seal_backdoor <host_id1> <host_id2>
```

### 4. trace_route

Finds the optimal path between two hosts.

```
trace_route <source_id> <dest_id> <min_bandwidth> <congestion_factor>
```

- `min_bandwidth`: Minimum required bandwidth for the route
- `congestion_factor` (λ): Additional latency per hop (models network congestion)

**Pathfinding Algorithm:**

- When λ = 0: Standard Dijkstra's algorithm
- When λ > 0: Multi-objective shortest path with dynamic edge costs
- Tie-breaking: (1) Total latency → (2) Number of hops → (3) Lexicographic order

### 5. scan_connectivity

Analyzes network connectivity status.

```
scan_connectivity
```

### 6. oracle_report

Generates a comprehensive network status report.

```
oracle_report
```

### 7. simulate_breach

Tests network vulnerability by simulating host or backdoor failure.

```
simulate_breach <host_id>              # Test if host is an articulation point
simulate_breach <host_id1> <host_id2>  # Test if backdoor is a bridge
```

## Example

### Input (testcases/inputs/example.txt)

```
spawn_host ALPHA 5
spawn_host BETA 3
spawn_host GAMMA 4
spawn_host DELTA 5
link_backdoor ALPHA BETA 10 100 2
link_backdoor BETA GAMMA 15 50 3
link_backdoor GAMMA DELTA 20 80 1
link_backdoor ALPHA DELTA 50 200 4
trace_route ALPHA DELTA 40 0
scan_connectivity
oracle_report
simulate_breach BETA
seal_backdoor ALPHA BETA
trace_route ALPHA DELTA 40 0
```

### Output

```
Spawned host ALPHA with clearance level 5.
Spawned host BETA with clearance level 3.
Spawned host GAMMA with clearance level 4.
Spawned host DELTA with clearance level 5.
Linked ALPHA <-> BETA with latency 10ms, bandwidth 100Mbps, firewall 2.
Linked BETA <-> GAMMA with latency 15ms, bandwidth 50Mbps, firewall 3.
Linked GAMMA <-> DELTA with latency 20ms, bandwidth 80Mbps, firewall 1.
Linked ALPHA <-> DELTA with latency 50ms, bandwidth 200Mbps, firewall 4.
Optimal route ALPHA -> DELTA: ALPHA -> BETA -> GAMMA -> DELTA (Latency = 45ms)
Network is fully connected.
--- Resistance Network Report ---
Total Hosts: 4
Total Unsealed Backdoors: 4
Network Connectivity: Connected
Connected Components: 1
Contains Cycles: Yes
Average Bandwidth: 107.5Mbps
Average Clearance Level: 4.3
Host BETA IS an articulation point.
Failure results in 2 disconnected components.
Backdoor ALPHA <-> BETA sealed.
Optimal route ALPHA -> DELTA: ALPHA -> DELTA (Latency = 50ms)
```

## Running Tests

```bash
# Run with example test case
./run.sh testcases/inputs/example.txt my_output.txt

# Compare with expected output
diff my_output.txt testcases/outputs/example_out.txt

# Run with larger test cases
./run.sh testcases/inputs/type1_small.txt result.txt
./run.sh testcases/inputs/type2_1_small.txt result.txt
```

## Algorithm Details

### Pathfinding with Congestion

The routing algorithm uses a modified Dijkstra's algorithm that accounts for congestion:

- **Edge cost at step i** = base_latency + λ × (i - 1)
- Uses Pareto-optimal frontier pruning for efficiency
- Supports multi-objective optimization (latency, hops, lexicographic path)

### Connectivity Analysis

- BFS-based connected component detection
- Articulation point detection via component counting after removal
- Bridge detection via component counting after edge removal
- Cycle detection during traversal

### Data Structure Optimizations

- **HashTable**: Polynomial rolling hash with automatic rehashing at 50% load factor
- **PriorityQueue**: Binary heap with O(log n) operations and index tracking for updates
- **HostIdentitySet**: Identity-based visited set for fast BFS traversals

## Complexity Analysis

| Operation         | Time Complexity                                     |
| ----------------- | --------------------------------------------------- |
| spawn_host        | O(1) amortized                                      |
| link_backdoor     | O(1) amortized                                      |
| seal_backdoor     | O(1)                                                |
| trace_route (λ=0) | O((V+E) log V)                                      |
| trace_route (λ>0) | O(k × (V+E) log(kV)) where k = Pareto frontier size |
| scan_connectivity | O(V+E)                                              |
| oracle_report     | O(V+E)                                              |
| simulate_breach   | O(V+E)                                              |

## Requirements

- Java 17 or higher (uses records and pattern matching)
- No external dependencies

## Course Information

- **University**: Bogazici University
- **Department**: Computer Engineering
- **Course**: CMPE 250 - Data Structures and Algorithms
- **Assignment**: Programming Assignment 3
