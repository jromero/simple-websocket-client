package codes.jromero.swsc

import java.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws._
import akka.stream._

import collection.JavaConverters._
import akka.stream.scaladsl._
import codes.jromero.swsc.stream.graph.Bouncer
import org.docopt.Docopt

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {

  implicit val actorSystem = ActorSystem("main")
  implicit val actorMaterializer = ActorMaterializer()

  val OPT_URI = "<uri>"
  val OPT_HEADER_NAME = "--headern"
  val OPT_HEADER_VALUE = "--headerv"

  val DOC =
    s"""
       |A simple WebSocket client.
       |
       |Usage:
       |  ${BuildInfo.assemblyJarName} [(${OPT_HEADER_NAME}=<hn> ${OPT_HEADER_VALUE}=<hv>)...] ${OPT_URI}
       |
       |Example:
       |  ${BuildInfo.assemblyJarName} ${OPT_HEADER_NAME}="Cookie" ${OPT_HEADER_VALUE}="USER=X" ws://echo.websocket.org
       |
       |Options:
       |  ${OPT_HEADER_NAME}=<hn>  Header Name (note: Header Value must be present)
       |  ${OPT_HEADER_VALUE}=<hv>  Header Value (note: Header Name must be present)
    """.stripMargin

  val ops = new Docopt(DOC).parse(args: _*)
  val rawHeaders = extractListPairs(OPT_HEADER_NAME, OPT_HEADER_VALUE).map { case (n, v) => RawHeader(n, v) }.toList

  val webSocketRequest = WebSocketRequest(
    ops.get(OPT_URI).asInstanceOf[String],
    immutable.Seq(rawHeaders: _*)
  )

  val printer: BidiFlow[Message, Message, Message, Message, _] = {
    def _printer(prefix: String): (Message) => Message = {
      case m@TextMessage.Strict(text) =>
        println(s"$prefix$text")
        m
      case m: Message =>
        val className = m.getClass.getSimpleName
        println(s"$prefix<$className>")
        m
    }

    BidiFlow.fromFunctions(
      _printer("sending  >>> "),
      _printer("received <<< ")
    )
  }

  val prompter = new Bouncer[Message, Unit, Message](m => {
    print("Enter message: ")
    val input = StdIn.readLine()
    TextMessage.Strict(input)
  }, 1.seconds)

  val flow = Flow.fromGraph(GraphDSL.create[FlowShape[Message, Message]]() { implicit builder =>
    import GraphDSL.Implicits._

    val source = Source.tick(1.seconds, 10.milliseconds, ())
    val sink = Sink.ignore
    val bcast = builder.add(Broadcast[Message](2))
    val bouncer = builder.add(prompter)

    bcast.out(0) ~> sink
    bcast.out(1) ~> bouncer.in0
    source ~> bouncer.in1

    FlowShape.of(bcast.in, bouncer.out)
  }).join(printer)

  val (upgradeResponse, closed) = Http().singleWebSocketRequest(webSocketRequest, flow)

  upgradeResponse.map {
    case r if r.response.status == StatusCodes.SwitchingProtocols =>
      println("connected!")
    case r =>
      println(s"invalid status code returned: ${r.response.status}!")
      System.exit(-1)
  }

  def extractListPairs(nameKey: String, valueKey: String): Map[String, String] =
    extractOpsList(OPT_HEADER_NAME).zip(extractOpsList(OPT_HEADER_VALUE)).toMap

  def extractOpsList(key: String): List[String] =
    ops.getOrDefault(key, new util.ArrayList[String]()).asInstanceOf[util.List[String]].asScala.toList
}
