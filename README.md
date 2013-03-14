# Shifter

Common infrastructure for our web-services built on top of the JVM and
Scala.

Warning: Highly unstable, sometimes broken.

## Quick Description

The project is made of several smaller projects, that can be used independently:

* **shifter-core** - some utilities for dealing with reflection, also
  used to contain language backports from Scala 2.10 to 2.9.2 that I
  pulled out  
* **shifter-geoip** - wrapper around Maxwind's GeoIP client, can
  download the
  [GeoLite City database](http://dev.maxmind.com/geoip/geolite) by
  itself  
* **shifter-http-client** - wrapper around
  [Ning's AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)
  for working with
  [Futures/Promises](http://docs.scala-lang.org/sips/pending/futures-promises.html)
* **shifter-db** - wrapper around JDBC
* **shifter-migrations** - a simple system for dealing with
  migrations, like database migrations, though usage isn't
  restricted to JDBC databases at all
* **shifter-cache** - wrapper around
  [SpyMemcached](http://code.google.com/p/spymemcached/) for working
  with
  [Futures/Promises](http://docs.scala-lang.org/sips/pending/futures-promises.html),
  also contains a stupidly simple in-memory cache implementation of
  the same interface  
* **shifter-web** - for servicing web requests, built over Servlets 3.0 and Jetty

The following dependencies are used:

* [Scala STM](http://nbronson.github.com/scala-stm/) for software transactional memory 
* [SpyMemcached](http://code.google.com/p/spymemcached/) as a Memcached client, as mentioned
* [Ning's AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client) for doing http requests, as mentioned
* [Maxmind's Java client](https://github.com/maxmind/geoip-api-java)
  for GeoIP, though the source-code has been imported directly into
  `shifter-geoip` because I couldn't find a Maven dependency to link to
* [Typesafe Config](https://github.com/typesafehub/config) for configuration files
* [Typesafe ScalaLogging](https://github.com/typesafehub/scalalogging) as an abstraction over SLF4J for logging
* [Logback](logback.qos.ch) as a SLF4J implementation for logging
* [Spray's Twirl](https://github.com/spray/twirl) templates rendering,
  which is an extraction of
  [Scala Templates from Play 2](http://www.playframework.org/documentation/2.0/ScalaTemplates)
* [Spray Json](https://github.com/spray/spray-json) for JSON processing
* [Commons Codec](http://commons.apache.org/proper/commons-codec/) and
  [Commons FileUpload](http://commons.apache.org/proper/commons-fileupload/)
  for processing requests
* [Yammer Metrics](http://metrics.codahale.com/) for gathering runtime stats from our servers
* [Jetty 8](http://jetty.codehaus.org/jetty/) as the web server

