package test.nodes

import java.util.concurrent.TimeUnit
import javax.cache.expiry.CreatedExpiryPolicy

import org.apache.ignite.Ignition
import org.apache.ignite.cache.{CacheMode, CacheWriteSynchronizationMode}
import org.apache.ignite.configuration.{IgniteConfiguration, _}
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import java.util
import scala.util.Random

case class Device(id: String, metadata: String, lat: Double, lon: Double) {
  override def toString = s"Device[id = $id - metadata = $metadata - lat = $lat - lon = $lon}"
}


object TestNativePersistence extends App {


  val NativePersistence = "device_native"

  // create a new instance of TCP Discovery SPI// create a new instance of TCP Discovery SPI
  val spi = new TcpDiscoverySpi
  // create a new instance of tcp discovery multicast ip finder
  val tcMp = new TcpDiscoveryMulticastIpFinder
  tcMp.setAddresses(util.Arrays.asList("localhost")) // change your IP address here
  // set the multi cast ip finder for spi
  spi.setIpFinder(tcMp)

  // Apache Ignite node configuration.
  val config = new IgniteConfiguration()
  config.setDiscoverySpi(spi)

  val cacheCfg = new CacheConfiguration(NativePersistence)
  cacheCfg.setBackups(1)
  cacheCfg.setCacheMode(CacheMode.REPLICATED)
  cacheCfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC)
  cacheCfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new javax.cache.expiry.Duration(TimeUnit.SECONDS, 30)))

  // Ignite persistence configuration.
  val storageCfg = new DataStorageConfiguration()
  storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true)

  storageCfg.setStoragePath("/tmp/persistence")
  storageCfg.setWalPath("/tmp/wal")
  storageCfg.setWalArchivePath("/tmp/wal")
  config.setDataStorageConfiguration(storageCfg)
  config.setCacheConfiguration(cacheCfg)

  val ignition = Ignition.start(config)
  ignition.cluster().active(true)

  val deviceCache = ignition.getOrCreateCache[Int, Device](NativePersistence)

  def random(min: Int, max: Int): Double = {
    val r = new Random
    min + (max - min) * r.nextDouble
  }


  for (i <- 1 to 100) {
  deviceCache.put(i, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
  }

  for (i <- 1 to 100) {
    println(s"Get device ${deviceCache.get(i)}")
  }

  Thread.sleep(5000)

  for (i <- 1 to 100) {
    println(s"Get device ${deviceCache.get(i)}")
  }


}
