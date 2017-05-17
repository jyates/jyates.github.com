---
layout: post
title: Fineo Architecture Lessons
location: San Francisco, CA
tags: aws, architecture, scale, iot, database, hadoop
---

 * Go slow to go fast
   * Invest upfront on CI/CD tools and architecture to help make iteration & improvement faster later. It's a balance though.
 * Leverage managed services whenever possible
   * it helps minimize internal ops
 * Serverless/Lambda based design helps scale
   * lower complexity than traditional infra, so engineers don't need as much training/knowledge
 * Design for failure
   * Not just of individual services, but of the entire data center/region. It happens. It also makes everything more robust
 * Design for testability
   * In production-like environments, in the cloud (how else to test the managed services).
   * With real production data (i.e. shadow testing)
 * Backups as part of the system design.
   * Ideally at multiple places - you never know where bugs will manifest. Recovery should be part of natural processes (and exercised regularly).
 * Integration over modularization (within reason)
   * Integrated services help you dev faster and create higher performance products. Know where you need to be better, where you can use generic solutions defined at the interface/module level.
 * Cloud does not necessarily mean multi-tenant as independent stacks. More VMs doesn't always scale
   * Can get really expensive really fast.