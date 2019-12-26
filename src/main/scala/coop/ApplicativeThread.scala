/*
 * Copyright 2019 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package coop

import cats.{Applicative, InjectK, Monad, Monoid}
import cats.data.{EitherT, Kleisli, WriterT}
import cats.free.FreeT
import cats.implicits._

trait ApplicativeThread[F[_]] extends Serializable {
  val applicative: Applicative[F]

  def fork[A](left: A, right: A): F[A]

  def cede[A](results: A): F[A]

  val cede_ : F[Unit] = cede(())

  def done[A]: F[A]

  def start[A](child: F[A]): F[Unit]
}

// NB it doesn't really make sense to define this for WriterT or StateT due to the value loss in start/fork
object ApplicativeThread {

  def apply[F[_]](implicit F: ApplicativeThread[F]): ApplicativeThread[F] = F

  implicit def forThreadFCapable[
      F[_]: Applicative,
      S[_]](
      implicit S: InjectK[ThreadF, S])
      : ApplicativeThread[FreeT[S, F, ?]] =
    new ApplicativeThread[FreeT[S, F, ?]] {

      val applicative = Applicative[FreeT[S, F, ?]]

      def fork[A](left: A, right: A): FreeT[S, F, A] =
        FreeT.liftF(S(ThreadF.Fork(left, right)))

      def cede[A](results: A): FreeT[S, F, A] =
        FreeT.liftF(S(ThreadF.Cede(results)))

      def done[A]: FreeT[S, F, A] =
        FreeT.liftF(S(ThreadF.Done))

      def start[A](child: FreeT[S, F, A]): FreeT[S, F, Unit] =
        fork(false, true).ifM(child.void, ().pure[FreeT[S, F, ?]])
    }

  implicit def forKleisli[F[_]: Monad: ApplicativeThread, R]: ApplicativeThread[Kleisli[F, R, ?]] =
    new ApplicativeThread[Kleisli[F, R, ?]] {
      private val thread = ApplicativeThread[F]

      val applicative = Applicative[Kleisli[F, R, ?]]

      def fork[A](left: A, right: A): Kleisli[F, R, A] =
        Kleisli.liftF(thread.fork(left, right))

      def cede[A](results: A): Kleisli[F, R, A] =
        Kleisli.liftF(thread.cede(results))

      def done[A]: Kleisli[F, R, A] =
        Kleisli.liftF(thread.done[A])

      def start[A](child: Kleisli[F, R, A]): Kleisli[F, R, Unit] =
        Kleisli.ask[F, R] flatMapF { r =>
          thread.start(child.run(r))
        }
    }

  implicit def forEitherT[F[_]: Monad: ApplicativeThread, E]: ApplicativeThread[EitherT[F, E, ?]] =
    new ApplicativeThread[EitherT[F, E, ?]] {
      private val thread = ApplicativeThread[F]

      val applicative = Applicative[EitherT[F, E, ?]]

      def fork[A](left: A, right: A): EitherT[F, E, A] =
        EitherT.liftF(thread.fork(left, right))

      def cede[A](results: A): EitherT[F, E, A] =
        EitherT.liftF(thread.cede(results))

      def done[A]: EitherT[F, E, A] =
        EitherT.liftF(thread.done[A])

      def start[A](child: EitherT[F, E, A]): EitherT[F, E, Unit] =
        EitherT.liftF(thread.start(child.value))
    }
}
