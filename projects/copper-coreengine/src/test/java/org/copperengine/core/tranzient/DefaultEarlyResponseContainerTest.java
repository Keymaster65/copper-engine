/*
 * Copyright 2002-2015 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.copperengine.core.tranzient;

import org.copperengine.core.tranzient.DefaultEarlyResponseContainer.EarlyResponse;
import org.junit.Test;

public class DefaultEarlyResponseContainerTest {

    @Test
    public void test() {
        EarlyResponse x = new DefaultEarlyResponseContainer.EarlyResponse(null, Long.MAX_VALUE - 100);
        org.junit.Assert.assertEquals(x.ts, Long.MAX_VALUE);
    }

}
