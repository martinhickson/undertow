/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.undertow.io;

import io.undertow.UndertowOptions;
import org.junit.Assert;
import org.junit.Test;

/**
 * A configured timeout of 0 or less means a blocking read waits without a limit.
 * When neither timeout is set, the 10 minute default still applies.
 */
public class BlockingReadTimeoutTestCase {

    @Test
    public void unsetTimeoutUsesDefault() {
        Assert.assertEquals(UndertowOptions.DEFAULT_READ_TIMEOUT, UndertowInputStream.blockingReadTimeout(null, null));
    }

    @Test
    public void zeroOrNegativeTimeoutIsUnlimited() {
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(0, null));
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(null, 0));
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(0, 0));
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(-1, null));
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(null, -1));
        Assert.assertEquals(0, UndertowInputStream.blockingReadTimeout(-1, -1));
    }

    @Test
    public void positiveIdleTimeoutStillApplies() {
        Assert.assertEquals(5000, UndertowInputStream.blockingReadTimeout(null, 5000));
        Assert.assertEquals(5000, UndertowInputStream.blockingReadTimeout(0, 5000));
        Assert.assertEquals(1000, UndertowInputStream.blockingReadTimeout(1000, 5000));
        Assert.assertEquals(1000, UndertowInputStream.blockingReadTimeout(1000, 0));
    }
}
