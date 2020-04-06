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
package org.copperengine.core.common;

import java.util.UUID;

/**
 * Implementation of the {@link IdFactory} interface, using <code>UUID.randomUUID().toString()</code> for creating
 * UUIDs.
 *
 * @author austermann
 */
public class JdkRandomUUIDFactory implements IdFactory {

    @Override
    public String createId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void startup() {
    }

}
