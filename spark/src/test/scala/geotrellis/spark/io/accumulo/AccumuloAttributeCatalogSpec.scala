package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.raster.histogram._

import geotrellis.spark._

import geotrellis.spark.testfiles._
import geotrellis.spark.op.stats._

import spray.json._
import spray.json.DefaultJsonProtocol._
import org.joda.time.DateTime
import org.scalatest._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

class AccumuloAttributeCatalogSpec extends FunSpec
  with Matchers
  with TestFiles
  with TestEnvironment
  with OnlyIfCanRunSpark {

  describe("Accumulo Attribute Catalog") {
    ifCanRunSpark {

      val accumulo = new AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )

      val attribCatalog = new AccumuloAttributeCatalog(accumulo.connector, "attributes")
      val layerId = LayerId("test", 3)

      it("should save and pull out a histogram") {
        val histo = DecreasingTestFile.histogram

        attribCatalog.save(layerId, "histogram", histo)

        val loaded = attribCatalog.load[Histogram](layerId, "histogram")
        loaded.getMean should be (histo.getMean)
      }

      it("should save and load a random RootJsonReadable object") {
        case class Foo(x: Int, y: String)

        implicit object FooFormat extends RootJsonFormat[Foo] {
          def write(obj: Foo): JsObject =
            JsObject(
              "the_first_field" -> JsNumber(obj.x),
              "the_second_field" -> JsString(obj.y)
            )

          def read(json: JsValue): Foo =
            json.asJsObject.getFields("the_first_field", "the_second_field") match {
              case Seq(JsNumber(x), JsString(y)) =>
                Foo(x.toInt, y)
              case _ =>
                throw new DeserializationException(s"JSON reading failed.")
            }
        }

        val foo = Foo(1, "thing")

        attribCatalog.save(layerId, "foo", foo)
        attribCatalog.load[Foo](layerId, "foo") should be (foo)
      }
    }
  }
}
