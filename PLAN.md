# CS416 Project 2 Implementation Plan

Based on the codebase analysis and project requirements, here is the roadmap to complete the implementation.

## 1. Config.java Updates
**Goal:** Support parsing of routing tables so routers aren't hardcoded.
- [ ] **Add Data Structure:** Create a `RoutingTableEntry` class (containing `subnet` and `nextHop`).
- [ ] **Update Parser:** Modify `parse()` to read a `"routingTables"` object from `config.json`.
  - Structure: `{"routerID": [{"subnet": "net1", "nextHop": "S1"}, ...]}`
- [ ] **Add Accessor:** Implement `List<RoutingTableEntry> getRoutingTable(String routerId)`.

## 2. Host.java Updates
**Goal:** Correctly address the default gateway at Layer 2.
- [ ] **Fix Gateway Parsing:**
  - Currently: `destMAC` is set to the full string (e.g., `"net1.R1"`).
  - **Required:** Parse the string to extract only the Router ID (e.g., `"R1"`) for the `destMAC`.

## 3. Router.java Updates
**Goal:** Enable packet forwarding logic.
- [ ] **Implement `loadRoutingTable`:**
  - Uncomment/Rewrite the method to fetch data from `Config.java`.
  - Populate the `routingTable` map.
- [ ] **Verify `handleFrame` Logic:**
  - Ensure `findPortByNeighborId` works with the values stored in the routing table (e.g., ensuring "S1" maps to the correct port).

## 4. Testing & Verification
1. **Local Delivery:** Host A -> Host B (Switch S1 only).
2. **One-Hop Routing:** Host A -> Router R1 (Verify MAC rewriting).
3. **End-to-End:** Host A (net1) -> Host C (net3).
   - Check R1 logs: Should receive frame from A, rewrite DestMAC to R2, forward to R2.
   - Check R2 logs: Should receive frame from R1, rewrite DestMAC to C, forward to S2.
