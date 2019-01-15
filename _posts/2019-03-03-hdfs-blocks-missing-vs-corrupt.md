---
layout: post
title: HDFS Block Metrics - Missing vs Corrupt
location: San Francisco, CA
subtitle: Understanding Namenode blocks
tags: hdfs, hadoop, blocks, metrics, big data
---

You are starting to move away from your Hadoop vendor - it was great for getting started, but you want to control your own destiny, reap huge saving money or institute advanced management. Once you start managing your own Hadoop cluster there are many metrics you will need to start collecting and monitoring.

Two of the most important metrics you **have to monitor** to ensure your HDFS cluster is happy are "MissingBlocks" and "CorruptBlocks".

**TL;DR:** Corrupt Blocks have at least one copy from which the file can be repaired, while Missing Blocks have no more "good" copies available.

First, where do these metrics come from?

Every NameNode exposes it statistics over JMX and even has its own small query language built in! The missing and corrupt blocks can be found at `http://mynamenode.host:50070/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem`:

```
 {
  "beans" : [ {
    "name" : "Hadoop:service=NameNode,name=FSNamesystem",
    "modelerType" : "FSNamesystem",
    "tag.Context" : "dfs",
    "tag.HAState" : "active",
    "tag.Hostname" : "mynamenode.host",
    "MissingBlocks" : 0,                     <------------
    "CorruptBlocks" : 10,                    <------------
    "MissingReplOneBlocks" : 0,
    "ExpiredHeartbeats" : 86,
    "TransactionsSinceLastCheckpoint" : 1354471,
    "TransactionsSinceLastLogRoll" : 159071,
    "LastWrittenTransactionId" : 14338027132,
    "LastCheckpointTime" : 1547515152261,
    "UnderReplicatedBlocks" : 0,
    ...
  } ]
}
```

Missing and Corrupt both sound pretty bad; when should you sound the alarm?

Let's look at an example to understand the difference. The example above reported 10 corrupt blocks, but 0 missing blocks.

You would think if you ran `fsck` that it would return 10 corrupt files.

```
$ hdfs fsck -list-corruptfileblocks
Connecting to namenode via http://mynamenode.host:50070/fsck?ugi=yarn&listcorruptfileblocks=1&path=%2F
The filesystem under path '/' has 0 CORRUPT files
```

Huh. But the metrics say we have corrupt blocks!

If you dig into [HDFS-8533](https://issues.apache.org/jira/browse/HDFS-8533) you will see that some blocks can be reported as bad by a DataNode but not actually be bad. Likely, the NameNode knows about the corruption and is actively working to copy a non-corrupt version of the file.

Chances are good, especially in large clusters, that you are going to see a corrupt blocks from time to time. And it turns out that users aren't going to even notice its an issue (you are alerting on [symptoms not causes, right?](https://www.datadoghq.com/blog/monitoring-101-alerting/)). So its probably something to warn about if its a persistent issue or really gets out of hand.

Instead, **MissingBlocks** are the real danger. Missing blocks can happen when all replicas of a block in the file are corrupted or all replicas go missing (i.e. don't take down more than 2 datanodes (or rather replication factor - 1) at a time). This is definitely something to alert on - if a user queries for that file they will get an error back. If this becomes a common issue you need to know ahead of time to maintain the quality of your data platform, rather than waiting for blocks to not be found, since the next missing file could be that mission critical one.

# But doesn't CDH/CDP/etc. handle this for me?

Sure does! But then your alerting is scattered around your infrastructure, making it hard to manage. With poor source embedded source control these tools often end up giving your relatively small bang for the non-trivial amount of bucks (especially for large clusters).

While great when getting started, these tools often end up hamstringing you. By ripping out these tools you can accelerate your development with more confidence and at lower costs.

# Bonus recommendation: prometheus exporter

If you are already using Prometheus for collecting metrics you can export JMX metrics from your NameNode and DataNodes with a simple javaagent - the [prometheus JMX exporter](https://github.com/prometheus/jmx_exporter). Its enabled with a simple java command line argument that points to the jar, prometheus scrape port and the configuration location. For instance, for a NameNode it would make sense to expose prometheus metrics on a port close to the client port, so the command line option would look something like

```
-javaagent:/opt/prometheu/jmx_exporter.jar=50076:/etc/hadoop/prometheus/jmx_exporter/namenode.yml
```

where your prometheus config would translate JMX into prometheus metrics with a config like:

```
---
lowercaseOutputName: true
lowercaseOutputLabelNames: true
whitelistObjectNames: ["Hadoop:*", "java.lang:*"]
rules:
  - pattern: Hadoop<service=NameNode, name=Rpc(Detailed)?ActivityForPort8020><>(\w+)
    name: hadoop_namenode_client_rpc_$2

  - pattern: Hadoop<service=NameNode, name=Rpc(Detailed)?ActivityForPort8022><>(\w+)
    name: hadoop_namenode_service_rpc_$2

  - pattern: Hadoop<service=NameNode, name=(\w+)><>(\w+)
    name: hadoop_namenode_$1_$2
```

Check out some of the other [example configurations](https://github.com/prometheus/jmx_exporter/tree/master/example_configs) for some more ideas.
