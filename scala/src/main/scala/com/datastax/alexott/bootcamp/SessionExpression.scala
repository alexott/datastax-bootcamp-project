/*
 * (C) 2018 DataStax, Inc, All rights reserved.
 */
package com.datastax.alexott.bootcamp

import io.gatling.commons.validation.Success
import io.gatling.core.session.{Session, _}

/**
  * TODO add javadoc explaining the purpose of this class/interface/annotation
  */
case class SessionExpression[T](f: (Session => T)) extends Expression[T] {
  def apply(session: Session) = Success(f.apply(session))
}
