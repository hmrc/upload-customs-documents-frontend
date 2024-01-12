package uk.gov.hmrc.uploaddocuments.support

import com.codahale.metrics.MetricRegistry
import org.scalatest.Suite
import play.api.Application

import scala.jdk.CollectionConverters
import org.scalatest.matchers.should.Matchers

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[MetricRegistry]
    for (metric <- CollectionConverters.IteratorHasAsScala[String](registry.getMetrics.keySet().iterator()).asScala)
      registry.remove(metric)
    metricsRegistry = registry
  }

  def verifyTimerExistsAndBeenUpdated(metric: String): Unit = {
    val timers  = metricsRegistry.getTimers
    val metrics = timers.get(s"Timer-$metric")
    if (metrics == null)
      throw new Exception(s"Metric [$metric] not found, try one of ${timers.keySet()}")
    metrics.getCount should be >= 1L
  }

}
