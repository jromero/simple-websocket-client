package codes.jromero.swsc.stream.graph

import akka.stream.{Attributes, FanInShape2, Inlet, Outlet}
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

class Bouncer[N, A, B](f: (A) => B, bounceDuration: FiniteDuration) extends GraphStage[FanInShape2[N, A, B]] {

  val shape = new FanInShape2[N, A, B]("BouncerShape")

  private val notificationIn: Inlet[N] = shape.in0
  private val in: Inlet[A] = shape.in1
  private val out: Outlet[B] = shape.out

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) {

      var open = true
      val timerKey = "timerKey"
      def resetTimer() = {
        scheduleOnce(timerKey, bounceDuration)
      }

      setHandler(notificationIn, new InHandler {
        override def onPush(): Unit = {
          resetTimer()
          pull(notificationIn)
        }
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          if (open) {
            push(out, f(grab(in)))
            open = false
            resetTimer()
          } else {
            pull(in)
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
          if (!hasBeenPulled(notificationIn)) pull(notificationIn)
        }
      })

      override protected def onTimer(timerKey: Any): Unit = {
        open = true
      }
    }
}