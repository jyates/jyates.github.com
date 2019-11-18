# Confluent Post: IoT Challenges and Techniques

Going to give an overiew of some of the common IoT processing challenges and then some techniques that you can apply to your problem

Problem Statement
 - large number of devices
 - (some) low latency availabilty needs
 - thundering herds, ciradian
 - large messages
 - custom formats
 - providing access to the stream, timeseries

Handling Scale and Herds
 - random partitioning key
   -  ensure that data lands
 - partitioning canonical to group by
   - time bucket, UUID bucket
 - pinned groups to limit bucket access

Large Messages
 - message by reference
 - truncation
 - fast lane, slow lane, fat lane
 - intra-partition, record parallelism 

Custom Formats & schema evolution
 - provide simple interface to parsing
 - limit complexity exposed to users
 - avoid proliferation, focus on a couple of standard formats
   - if you are producing server-side, rarely a reason to not have schemaful messages
 - 

Looking forward
 - providing ownership to data owners to their own data flows
 - democratizing operational excellence
 - flow lineage and management
