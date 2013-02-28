package net.spy.memcached

import java.util.concurrent.{Future => JFuture, TimeUnit, CountDownLatch, ConcurrentHashMap}
import java.{util => jutil}
import scala.concurrent.{ExecutionContext, Future, Promise}
import net.spy.memcached.ops.{GetOperation, Operation, OperationStatus}
import net.spy.memcached.internal.{BulkGetFuture, SingleElementInfiniteIterator, GetFuture}
import net.spy.memcached.transcoders.Transcoder
import java.net.InetSocketAddress
import net.spy.memcached.util.StringUtils
import scala.util.Failure
import scala.util.Success
import scala.collection.JavaConverters._
import shifter.cache.errors.{NotFoundInCacheError, CacheError}

/**
 * Started by: Alexandru Nedelcu
 * Copyright @2013 Epigrams, Inc.
 */

class WrappedMemcachedClient(conn: ConnectionFactory, addrs: jutil.List[InetSocketAddress]) extends MemcachedClient(conn, addrs) {

  def realAsyncGetBulk[T](keyIter: jutil.Iterator[String]): Future[Map[String, T]] = {
    val tcIter = new SingleElementInfiniteIterator[Transcoder[T]](transcoder.asInstanceOf[Transcoder[T]])
    val m: jutil.Map[String, JFuture[T]] = new ConcurrentHashMap[String, JFuture[T]]
    val promise = Promise[Map[String, T]]()

    // This map does not need to be a ConcurrentHashMap
    // because it is fully populated when it is used and
    // used only to read the transcoder for a key.
    val tcMap: jutil.Map[String, Transcoder[T]] = new jutil.HashMap[String, Transcoder[T]]

    // Break the gets down into groups by key
    val chunks: jutil.Map[MemcachedNode, jutil.Collection[String]] = new jutil.HashMap[MemcachedNode, jutil.Collection[String]]

    val locator: NodeLocator = mconn.getLocator

    while (keyIter.hasNext && tcIter.hasNext) {
      val key: String = keyIter.next
      tcMap.put(key, tcIter.next)
      StringUtils.validateKey(key)
      val primaryNode: MemcachedNode = locator.getPrimary(key)
      var node: MemcachedNode = null
      if (primaryNode.isActive) {
        node = primaryNode
      }
      else {
        {
          val i: jutil.Iterator[MemcachedNode] = locator.getSequence(key)
          while (node == null && i.hasNext) {
            val n: MemcachedNode = i.next
            if (n.isActive) {
              node = n
            }
          }
        }
        if (node == null) {
          node = primaryNode
        }
      }
      assert(node != null, "Didn't find a node for " + key)
      var ks: jutil.Collection[String] = chunks.get(node)
      if (ks == null) {
        ks = new jutil.ArrayList[String]
        chunks.put(node, ks)
      }
      ks.add(key)
    }

    val latch: CountDownLatch = new CountDownLatch(chunks.size)
    val ops: jutil.Collection[Operation] = new jutil.ArrayList[Operation](chunks.size)
    val rv: BulkGetFuture[T] = new BulkGetFuture[T](m, ops, latch)

    val cb: GetOperation.Callback = new GetOperation.Callback {
      @SuppressWarnings(Array("synthetic-access"))
      def receivedStatus(status: OperationStatus) {
        rv.setStatus(status)
        if (!status.isSuccess && !promise.isCompleted && status.getMessage.toLowerCase.trim != "not found") {
          promise.complete(Failure(new CacheError(status.getMessage)))
        }
      }

      def gotData(k: String, flags: Int, data: Array[Byte]) {
        val tc: Transcoder[T] = tcMap.get(k)
        m.put(k, tcService.decode(tc, new CachedData(flags, data, tc.getMaxSize)))
      }

      def complete {
        latch.countDown

        if (latch.getCount == 0 && !promise.isCompleted) {
          val map = m.keySet().iterator().asScala.foldLeft(Map.empty[String, T]) { (acc, key) =>
            Option(m.get(key).get(50, TimeUnit.MILLISECONDS)) match {
              case Some(value) =>
                acc.updated(key, value)
              case None =>
                acc
            }
          }

          promise.complete(Success(map))
        }
      }
    }

    // Now that we know how many servers it breaks down into, and the latch
    // is all set up, convert all of these strings collections to operations
    val mops: jutil.Map[MemcachedNode, Operation] = new jutil.HashMap[MemcachedNode, Operation]
    import scala.collection.JavaConversions._
    for (me <- chunks.entrySet) {
      val op: Operation = opFact.get(me.getValue, cb)
      mops.put(me.getKey, op)
      ops.add(op)
    }

    assert(mops.size == chunks.size)
    mconn.checkState
    mconn.addOperations(mops)

    promise.future
  }

  def realAsyncGet[T](key: String)(implicit ec: ExecutionContext): Future[T] = {
    val latch = new CountDownLatch(1)
    val rv = new GetFuture[T](latch, operationTimeout, key)
    val promise = Promise[T]()

    val op: Operation = opFact.get(key, new GetOperation.Callback {
      def receivedStatus(status: OperationStatus) {
        rv.set(jfuture, status)
        if (!status.isSuccess)
          if (status.getMessage.toLowerCase.trim == "not found")
            promise.complete(Failure(new NotFoundInCacheError(key)))
          else
            promise.complete(Failure(new CacheError(status.getMessage)))
      }

      def gotData(k: String, flags: Int, data: Array[Byte]) {
        assert(key == k, "Wrong key returned")
        val tc = transcoder.asInstanceOf[Transcoder[T]]
        jfuture = tcService.decode(tc, new CachedData(flags, data, tc.getMaxSize))
        promise.complete(Success(jfuture.get))
      }

      def complete() {
        latch.countDown()
      }

      private var jfuture: JFuture[T] = null
    })

    rv.setOperation(op)
    mconn.enqueueOperation(key, op)
    promise.future
  }
}
