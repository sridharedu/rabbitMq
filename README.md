Great challenge.

Here’s a very clear, easy-to-understand, but extremely difficult to implement problem that truly tests message queue mastery, especially RabbitMQ — with layers of complexity including reliability, ordering, backpressure, retries, deduplication, and dynamic routing.


---

🔥 Hardest Problem (Easy to Understand)

"Real-Time Order Processing System for a Global Flash Sale"

> You are building a real-time order processing system for a global e-commerce flash sale. Millions of users from different regions are placing orders simultaneously for limited-stock items. You must ensure every valid order is processed once and only once, in the right order per user, and stock counts must not be oversold. The system must stay resilient, scalable, and efficient under massive load and regional failures.




---

💡 Key Functional Requirements

1. Each order is sent to a RabbitMQ queue.


2. Orders must be processed in the exact sequence per user.


3. No order should be lost, duplicated, or processed twice.


4. A user’s order should not be processed if stock for that item is already sold out.


5. If any consumer crashes, processing must resume from where it left off — without reprocessing successful messages.


6. Regional queues should handle latency & scale, but final processing must be globally consistent.


7. There will be burst traffic: 100k+ orders per second. System must throttle, buffer, or degrade gracefully.


8. Payment failures and timeouts must be retried up to 3 times and then dead-lettered.


9. Monitoring should expose metrics like: pending orders, retry rate, queue lag, stock-levels, and consumer health.




---

🧠 What This Problem Really Tests

Skill/Concept	What’s Being Tested

RabbitMQ fundamentals	Exchanges, queues, bindings, ack/nack, prefetch, TTL
Message ordering	Per-user ordering in a distributed system
Exactly-once processing	Idempotency, deduplication, atomic DB + MQ handling
Backpressure	Handling overwhelming traffic without crashing
Retry + DLQ	Dead-lettering, exponential backoff, and poison message handling
Scalability	Horizontal scaling of consumers without race conditions
Monitoring & Observability	Tracking system lag, message flow, errors in real-time
Cross-region consistency	Merging orders from regional queues into a global processor correctly



---

🛠️ Bonus Constraints for Mastery-Level Difficulty

Orders are submitted in multiple currencies. Currency conversion rates must be fetched dynamically (another queue integration).

Customers can cancel orders within 2 minutes — if cancellation reaches before processing, order must be skipped cleanly.

Queue consumers must intelligently pause and resume based on CPU and DB load.

Dynamic routing: based on item category, order should be routed to different processing services.



---

🎯 Goal

Design, implement, and demonstrate this system with full resilience, observability, and correctness guarantees, using RabbitMQ as the core message queue.


---

Would you like:

A system architecture diagram for this problem?

A starter project plan or checklist?

A step-by-step implementation roadmap with key RabbitMQ features to use?


