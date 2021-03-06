/*
 * Copyright (c) 2011-2018 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.rxjava.ext.sql;

import org.junit.Test;
import rx.Observable;

/**
 * @author Thomas Segismont
 */
public class InTransactionObservableTest extends SQLTestBase {

  @Test
  public void inTransactionSuccess() throws Exception {
    inTransaction(null).test()
      .awaitTerminalEvent()
      .assertCompleted()
      .assertValues(namesWithExtraFolks());
  }

  @Test
  public void inTransactionFailure() throws Exception {
    Exception error = new Exception();
    inTransaction(error).test()
      .awaitTerminalEvent()
      .assertError(error)
      .assertValues(namesWithExtraFolks());
    assertTableContainsInitDataOnly();
  }

  private Observable<String> inTransaction(Exception e) throws Exception {
    return client.rxGetConnection().flatMapObservable(conn -> {
      return rxInsertExtraFolks(conn)
        .andThen(uniqueNames(conn))
        .compose(upstream -> e == null ? upstream : upstream.concatWith(Observable.error(e)))
        .compose(SQLClientHelper.txObservableTransformer(conn))
        .concatWith(rxAssertAutoCommit(conn).toObservable())
        .doAfterTerminate(conn::close);
    });
  }
}
