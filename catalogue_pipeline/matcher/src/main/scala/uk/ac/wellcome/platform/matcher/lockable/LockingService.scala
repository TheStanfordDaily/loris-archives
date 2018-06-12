package uk.ac.wellcome.platform.matcher.lockable

import java.time.Instant

trait LockingService {
  def lockRow(id: Identifier): Either[LockFailure, RowLock]
  def unlockRow(id: Identifier): Either[UnlockFailure, Unit]
}

case class LockFailure(t: Identifier, message: String)
case class UnlockFailure(t: Identifier, message: String)

case class Identifier(id: String)
case class RowLock(
  id: String,
  created: Instant,
  expires: Instant
)

sealed trait LockingFailures
case class LockFailures[T](
  failed: Iterable[T],
  succeeded: Iterable[Locked[T]]
) extends LockingFailures

case class UnlockFailures[T](
  failed: Iterable[Locked[T]],
  succeeded: Iterable[T]
) extends LockingFailures