# CS416 Project 3 Work Distribution

## Project Roles & Responsibilities

| Person                   | Primary Responsibilities                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Key Files                                                   |
|:-------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------|
| **A: Mitchell Williams** | **Routing Algorithm Core**: Implements the logic for calculating the best paths (e.g., Distance Vector's Bellman-Ford or Link State's Dijkstra). Focuses on the `processRoutingUpdate` method and updating the routing table based on uniform costs of 1.                                                                                                                                                                                                                                                              | `src/Router.java`                                           |
| **B: Unset**             | **Concurrency & Scheduling**: Implements the `ScheduledExecutorService` in `Router.java` to handle periodic broadcasts (every 5-10s) and update timeouts. Ensures the routing table is thread-safe (using `ConcurrentHashMap` or synchronization) so Member A's logic doesn't crash during updates.                                                                                                                                                                                                                    | `src/Router.java`, `src/NetworkLayer.java`                  |
| **C: Unset**             | **Topology & Configuration**: Rewrites `resources/config.json` for the full 10-subnet, 6-router topology. Updates `Host.java` to support the new gateway logic.                                                                                                                                                                                                                                                                                                                                                        | `resources/config.json`, `src/Host.java`, `src/Config.java` |
| **D: Lucas Lowe**        | **Packet & Logging Specialist**: Standardizes the routing update payload format (e.g., JSON or CSV strings in the `Packet` payload). Implements "verbose" logging to show the routing table converging in real-time. Creates a validation script (or manual test plan) to verify that packets actually follow the shortest path. Creates the 12+ required run configurations for the IDE to ensure the whole network can be launched at once (IDE configuration optional, we can run it manually but it would be nice) | `src/Packet.java`, `src/Router.java` (Logging), IDE stuff   |

## Next Steps

### Member A
- Decide on the protocol (Distance Vector is recommended).
- Define the `RoutingTableEntry` structure to include a `cost` field.
- Implement the parsing logic for routing updates received from neighbors.

### Member B
- Add a `startPeriodicUpdates()` method to `Router.java` that runs in the background using `ScheduledExecutorService`.
- Ensure all `Map` operations on the routing table are thread-safe.

### Member C
- Map out the 10 subnets (Net 1-Net 10) and their IP ranges to match the PDF topology.
- Update `config.json` with 6 routers (R1-R6), 3 switches (S1-S3), and 3 hosts (A-C).

### Member D
- Define the exact string format for a routing update (e.g., `"Subnet1:1,Subnet2:2"`).
- Update the `Packet` class if any additional fields are needed for routing updates.
- Create a test plan to verify that the routing table converges to the shortest path.
