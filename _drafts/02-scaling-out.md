---
layout: post
title: Scaling Out Fineo
tags: scale, big data, aws, kinesis, lambda, api gateway
excerpt_separator: <!--more-->
---

A deeper look into how Fineo manages its seamless scalability across the multi-layer architecture. By enabling each layer to scale independently and leaning on existing, fully-managed services we can enable wildly scalable infrastructure without notably increasing operations effort, and often times decreasing it.
<!-- more -->

I had a couple of questions come up from the [Fineo ingest post] on how we are managing to make things scalable, what happens when throughput limits are hit, etc. that I thought would be interesting to explore. For a refresher, here is the high-level view of Fineo's architecture:

<img src="/images/posts/scaling-out-fineo/highlevel.png">

# Scalable API

At the top layer, we leverage AWS API Gateway to manage the external REST endpoints and user/device authentication. Its transparently scalable and integrates well with IAM credentials (device auth) and AWS Cognito (user auth); it's our 'hard shell'. For a relatively low amount of requests (like those seen in a startup), API Gateway is very cost effective at $3.50 per million API calls received + data transfer costs, saving us the burden of writing, running and managing another service. Eventually, we will need to move off of API Gateway, both from a cost perspective and to get around its 30 second request/response timeouts.

# Managing buffers

The core of the Fineo write architecture is a series of AWS Kinesis stream, aka a fully managed, 24hr data buffer. Each buffer shard has a write limit of 1MB/sec and read limit of 2MB/sec and costs $0.015/hr and $0.014 per 1M 'put' requests. Again, for a startup this is significantly cheaper and easier than trying to run Apache Kafka on our own. The question is then, "what do we do when deman increases?"

The writes for each tenant are hashed into the Kinesis shards based on the tenant id and the timestamp, approximately uniformly distributing the events across all the shards. Then we just need to ensure that capacity stays ahead of the demand. Writes per Fineo user are limited to 200 events/sec, so as long as events stay below 5KB each, we can approximately allocate one Kinesis shard per tenant.

However, this misses a couple of things.

First, we support multi-put requests, making it relatively easy to go above 1KB/sec for many use cases. At the same time, many users aren't aren't always going to be using the full capacity, so by keeping 1 shard per user, we are wasting capacity (and money). Finally, we still need to ability to scale up the number of shards to support demand spikes.

Enter the [AWS Kinesis Autoscaling Util]. A tool and standalone service that manages the amount of shard capacity based on the monitored PUT and GET rates.

As long as we scale up when PUTs exceed a fixed percent (e.g. 50% of capacity), we can quickly respond to user demand shifts, while remaining cost effective. Its not 100% perfect as very fast demand bursts will overwhelm the system, but it captures more than 80% of our needs.

# Stream Processing

All of our stream processing is handled via AWS Lambda. Again, it has the same properties of being auto-scalable and fully managed, while remaining cost effective at relatively small scales. It starts to make more sense to move off of Lambda as the number of events increases, instead moving those functions into a standalone EC2 instance that leverages the AWS Kinesis client library to access the streams.

Fast, easy, simple to test. Check!

# Storage

We have two main storage engines - S3 and DynamoDB. Fully, managed, scalable, etc. etc. - all the things we are looking for above.

## Firehose Streams to S3

S3 is fed by AWS Firehose, which lets us buffer data for between 60 seconds and 900 seconds (15 minutes), and by default 2,000 transactions/second, 5,000 records/second, and 5 MB/second. This handles much of our early scale and can be scaled up either by opening an AWS case or managing a set of Firehoses and distributing the writes across them. We then periodically batch process the Firehosed records into a partitioned, columnar format for use with client reads. 

Because the Firehose copies are done at every stage of the stream processing, we have between 4 and 6 copies of the data at all times, across the Firehoses themselves, S3 and DynamoDB. This makes it very unlikely to lose data and easy to recover because its already formated in the stream processing layout.

## DynamoDB Storage

We spent a bit of time thinking about the DynamoDB schema to ensure that its going to be reasonably scalable and avoid 'hot spots' (the explanation of which is left to another post). Assuming that we have relatively uniform writes and reads, the remaining overhead is then to just ensure that our DynamoDB shard allocation is appropriate to our workload.

We leverage time-range grouped tables to partition groups of writes in DynamoDB, allowing us to quickly 'age-off' older data and economically adjust the allocated capacity. The key assumption here is that more recently written data is also the most frequently accessed data. Its easy to go overboard and do something like a table per day, but at a limit of 256 tables, we can quickly run out of tables for production and test environments. At the same time, too few tables means allocating extra capacity to data that is rarely accessed, effectively wasting money. We settles on a table per week.

Now, just like with Kinesis, we need to be able to turn up and down the capacity of the cluster. There are a couple of tools to do this: [dynamic DynamoDB], that runs a server, or one of many [lambda] [based] [implementations]. I'm partial to lambda functions for ease of deployment, but really its up what fits your deployment model. The only kicker is that DynamoDB can only be _scaled down four times a day_, so you have to be a little judicious in allocating capacity. Since we rely on lambda functions and an idempotent write model, we can support retries and being a little bit slower to scale, saving us money but at the cost of a slightly higher latency for users.

# Query Execution

We have two main components to query execution: a query server and an Apache Drill cluster. The query server runs as a simple AWS Elastic Beanstalk Java application. Because it is essentially stateless, we can transparently scale up and down the number of servers behind the load balancer based on user demand using standard AWS rules. Upcoming work includes adding client pinning to servers so we can support larger queries that take multiple round-trips.

Apache Drill similarly supports a dynamically scalable cluster. As resource demands grow, we can just add another node to the cluster to pick up the extra work. We trust to decent AWS network architecture to avoid major data locality issues (its all stored in DynamoDB and S3 anyways, so its not going to be local regardless). Similarly, as work drops below a given level, you can decommission. This is more of a manual process or driven by a custom watcher, and can be down via a separate monitoring server or in AWS Lambda, just like with Kinesis and DynamoDB.

# Wrap Up

At Fineo we designed for scalability from the beginning, while still remaining cost effective. By thinking about not only how we are going to scale now, but also what that is going to cost and how to support the same (or better) scalability down the road _at a lower cost_, we can move at startup speed and cost. By separating out the architecture into different layers and ensuring that they are independently scalable ensured that we have no bottlenecks. Since we are a small shop, it was imperative that we cut down on operations work, so we could focus on building new features and growing the business. In leveraging full-managed services and auto-scalability monitors we not only freed up our time, but run a better service at a lower cost.


[Fineo ingest post]: /2017/05/01/fineo-internals-ingest.mg.html
[AWS Kinesis Autoscaling Util]: https://github.com/awslabs/amazon-kinesis-scaling-utils
[dynamic DynamoDB]: https://aws.amazon.com/blogs/aws/auto-scale-dynamodb-with-dynamic-dynamodb
[lambda]: https://github.com/rockeee/dynamic-dynamodb-lambda
[based]: https://github.com/channl/dynamodb-lambda-autoscale
[implementations]: https://github.com/trek10inc/dynamodb-autoscaling