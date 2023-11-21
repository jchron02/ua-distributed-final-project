# ua-distributed-final-project
Final Project for CS 3003 - Distributed Systems. Fitting Room

For this final project, our task is to implement a simulated fitting room system using Java and
threads. The primary goal is to model a retail clothing store with a waiting area, limited
chairs, and fitting rooms, all accessed by customers. You must use threads, semaphores,
and mutexes to accomplish this task.

Our goal is to model a clothing store, which includes a waiting area with a limited number
of chairs and a set of fitting rooms, all of which are accessed by customers. This system
operates as follows: when a customer arrives, they first check the waiting area for an
available chair to try on clothes. If a fitting room is vacant, they proceed directly to it.
However, if all fitting rooms are occupied, the customer waits in the waiting area if there are
available chairs. As soon as a fitting room becomes available, the customer moves to it. If all
fitting rooms and waiting area chairs are occupied, the customer leaves in frustration. After
trying on clothes, customers exit the fitting room, pass through the waiting area, and leave
the store, either returning the clothes or making a purchase. To implement this system,
you must utilize threads, semaphores, and mutexes to manage customer interactions and
resource allocation effectively. Additionally, a distributed approach with socket connections
and a client-server architecture will be employed to enhance scalability and fault tolerance.
This client-server architecture must incorporate a minimum of three servers and support
multiple clients. The distribution of fitting rooms should be decentralized across the servers.
Clients should be able to connect to any server with available fitting rooms. The system
should be scalable to accommodate varying numbers of fitting rooms across three distinct
servers, as specified by the second command-line argument.
