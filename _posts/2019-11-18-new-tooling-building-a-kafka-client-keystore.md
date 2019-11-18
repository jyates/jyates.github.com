---
layout: post
title: "New (Open Source!) Tooling: Kafka Keystore Building"
location: San Francisco, CA
subtitle:
tags: kafka, security, open source, java, rsa, x509, stream processing, big data
---

The easiest way to setup basic authentication with Kafka is to use x509 certificates. However, getting these certificates into a place where they are actually  by your Kafka client can be frustrating and error prone. That is why we recently released a [Kafka Certificates] tool to make your life easier.

Most intro guides ([example](https://docs.confluent.io/3.0.0/kafka/ssl.html#creating-your-own-ca)) have you creating your own Certificate Authority (CA) to sign keys. This can work in the small scale, for instance with just one or two clusters and/or clients. However, when you start having proper "corporate infrastructure", chances are you will want to have a central CA for the entire company that can issue certs. These certs need not even be for Kafka exclusively - x509 certificates can be used to prove individuals' identity across a range of services.

If you are using python, ruby, golang or any other language backed by [librdkafka] you can just drop these certificates into the client and move along with your life. Unfortunately, taking a private key, public key, signed certificate and a CA and making Java Keystores out of them is not so straightforward.

The usual process is something along the lines of using the command line to generate a PKCS12 certificate store with all the appropriate keys and certificate chains. Then you would need to create an empty Java Keystore. Finally, you would have to import each and every certificate that you want to add to the keystore. And unfortunately, none of this is really well documented anywhere.

Not a very simple process by any means, and something I personally have messed up a number of times.

That is why we created and open sourced the [Kafka Certificates] tool. You just pass it:
 * private key
 * signed certificate of the private key
 * the issuing CA's certificate
 * the issuing CA's certificate chain (ca_chain)

And it will generate you a password protected, Java Keystore formatted, keystore and atruststore for use with a Kafka client. It will also dump them to the console as base64 encoded values, which are great for adding directly to, say, Kubernetes configurations.

# Internals

There are two different Keystores that need to be created (pardon the overloaded terms, this is standard Java): the keystore and the truststore.

 > Here "K" Keytores are the format. So a trustore is a Keystore formated file, that holds certificates and/or key that the client use to determine which server certificates to trust.

The keystore stores the client's private key and the certificate chain for that key back up to the root CA. This allows it to cryptographically prove that it is who it says it is, along with "testimony" all the way back up root CA.

The truststore is the opposite - you add all the certificates for authorities that you trust to sign certificates (issuing CAs), so if you get a request you can check to see if their request certificate chain cryptographically matches any of the issuing CAs certificates in your truststore.

So if we have a Private Key (PK) with a certificate C, and a certificate chain of C1 -> C2 -> Cr, where Cr is the certificate for the root Certificate Authority (CA), then our keystore would look something like:

 * PK + C -> C1 -> C2 -> Cr

And then to trust any certificate signed by the CA, our truststore would just need

 * CR

Seems simple right? Too bad Java doesn't make it easy. Good thing that we did :)

Be sure to check out the [Kafka Certificates] tool next time you need to build Keystores for a client.

[Kafka Certificates]: https://github.com/teslamotors/kafka-helmsman/tree/master/kafka_certificates
[librdkafka]: https://github.com/edenhill/librdkafka
