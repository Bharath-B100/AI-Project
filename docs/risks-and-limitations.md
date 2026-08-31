# Technical Risks, Edge Cases, and Limitations

This document lists potential edge cases, system limits, and architectural risks identified during design, along with strategies to mitigate them.

---

## Edge Cases

### 1. Cyclic Dependencies in Task Networks
* **Risk**: PM adds a task dependency that forms a cycle (e.g., Task A depends on Task B, Task B depends on Task C, and Task C is updated to depend on Task A).
* **Consequence**: The Critical Path Method (CPM) scheduling algorithm will enter an infinite loop or throw a stack overflow.
* **Mitigation**: Before saving any new or updated dependency link, the backend runs a Directed Acyclic Graph (DAG) cycle detection check using Depth-First Search (DFS). If a loop is found, the transaction is rejected, and a `400 Bad Request` is returned.

### 2. Over-Allocation Across Multiple Projects
* **Risk**: A team member is assigned to Project A (60% allocation) and Project B (50% allocation). The total allocation exceeds 100% (capacity overload).
* **Consequence**: Overworking employees, resulting in delays.
* **Mitigation**: The workload calculator aggregates assignments globally across all active projects. The system flags a "Resource Overload" risk and highlights the user on the dashboard.

### 3. AI Planning Input with Low Quality / Vague Prompts
* **Risk**: The PM inputs a prompt like *"Build a website"* or gibberish.
* **Consequence**: The LLM generates a poor or completely invalid list of tasks.
* **Mitigation**: The system sets minimal character length validations on the prompt. The LLM prompt template includes explicit instructions to return a structured fallback response if the prompt is nonsensical, and the PM has full editing rights in the staging screen before approving.

---

## Technical Risks & Mitigations

### 1. LLM Non-Determinism & Hallucination
* **Risk**: The LLM returns structural JSON that fails parsing (missing fields, invalid datatypes, or references to task dependency IDs that do not exist).
* **Mitigation**: 
  * Strict JSON Schema enforcement during backend deserialization.
  * Validation rules ensuring all dependency IDs map to items present in the same JSON payload.
  * Staging design: nothing is saved to production tables until the PM verifies the output.

### 2. API Latency and Outages
* **Risk**: The LLM API is slow (over 10-15 seconds) or goes offline.
* **Mitigation**:
  * Set backend connection and read timeouts on HTTP calls to the LLM (e.g., max 15 seconds).
  * Inform the user via clear UI spinner messages.
  * If the service fails, return a clean error page with a retry button, ensuring the application itself does not crash.

### 3. Database Integrity and Concurrency
* **Risk**: Multiple users update task statuses simultaneously, leading to scheduling discrepancies.
* **Mitigation**: 
  * Apply optimistic locking (`@Version`) on `Project` and `Task` entities.
  * Run recalculations within serializable transaction blocks when necessary.
