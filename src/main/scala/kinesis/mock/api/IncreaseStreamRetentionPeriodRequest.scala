package kinesis.mock
package api

import scala.concurrent.duration._

import cats.data.Validated._
import cats.data._
import cats.kernel.Eq
import cats.syntax.all._
import io.circe._

import kinesis.mock.models._
import kinesis.mock.validations.CommonValidations

// https://docs.aws.amazon.com/kinesis/latest/APIReference/API_IncreaseStreamRetention.html
final case class IncreaseStreamRetentionPeriodRequest(
    retentionPeriodHours: Int,
    streamName: StreamName
) {
  def increaseStreamRetention(
      streams: Streams
  ): ValidatedNel[KinesisMockException, Streams] =
    CommonValidations
      .findStream(streamName, streams)
      .andThen(stream =>
        (
          CommonValidations.validateStreamName(streamName),
          CommonValidations.validateRetentionPeriodHours(retentionPeriodHours),
          CommonValidations.isStreamActive(streamName, streams),
          if (stream.retentionPeriod.toHours > retentionPeriodHours)
            InvalidArgumentException(
              s"Provided RetentionPeriodHours $retentionPeriodHours is less than the currently defined retention period ${stream.retentionPeriod.toHours}"
            ).invalidNel
          else Valid(())
        ).mapN((_, _, _, _) =>
          streams.updateStream(
            stream.copy(retentionPeriod = retentionPeriodHours.hours)
          )
        )
      )
}

object IncreaseStreamRetentionPeriodRequest {
  implicit val increaseStreamRetentionRequestEncoder
      : Encoder[IncreaseStreamRetentionPeriodRequest] =
    Encoder.forProduct2("RetentionPeriodHours", "StreamName")(x =>
      (x.retentionPeriodHours, x.streamName)
    )
  implicit val increaseStreamRetentionRequestDecoder
      : Decoder[IncreaseStreamRetentionPeriodRequest] = { x =>
    for {
      retentionPeriodHours <- x.downField("RetentionPeriodHours").as[Int]
      streamName <- x.downField("StreamName").as[StreamName]
    } yield IncreaseStreamRetentionPeriodRequest(
      retentionPeriodHours,
      streamName
    )
  }
  implicit val increaseStreamRetentionRequestEq
      : Eq[IncreaseStreamRetentionPeriodRequest] = Eq.fromUniversalEquals
}
