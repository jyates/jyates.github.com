# Confluent Post: IoT Challenges and Techniques

An overiew of some of the common IoT processing challenges and then some techniques that you can apply to your problem.

Problem Statement
 - large number of devices
 - (some) low latency needs
 - NO data loss - every message counts
 - thundering herds
 - large messages
 - custom formats

Handling Scale and Herds
 - random partitioning key
   -  ensure that data lands
 - partitioning canonical to group by
   - time bucket, UUID bucket

Large Messages
 - message by reference
 - truncation
 - fast lane, slow lane, fat lane
 - intra-partition/record parallelism

Formats and schema
 - provide simple interface to parsing
 - limit complexity exposed to users
 - avoid proliferation, focus on a couple of standard formats
   - if you are producing server-side, rarely a reason to not have schemaful messages

Looking forward
 - providing ownership to data owners to their own data flows
 - democratizing operational excellence
 - flow lineage and management


Things to include, if we have more space
-------------
 - schema evolution considerations, you probably want forward
 - circuadian load smearing
 - k8s integration
 - pinned groups to limit bucket writes
	 - semi-dynamic partitioning such that consumer's figure out bucket to partition mapping and then use that to claim buckets in race