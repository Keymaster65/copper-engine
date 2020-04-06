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
package org.copperengine.core.batcher;

/**
 * Abstract base implementation of {@link BatchCommand}
 *
 * @param <E> type of the BatchExecutor
 * @param <T> type of the BatchCommand to be executed
 * @author austermann
 */
public abstract class AbstractBatchCommand<E extends BatchExecutor<E, T>, T extends AbstractBatchCommand<E, T>> implements BatchCommand<E, T> {

    final CommandCallback<T> callback;
    long targetTime;

    public AbstractBatchCommand(CommandCallback<T> callback) {
        this(callback, System.currentTimeMillis());
    }

    public AbstractBatchCommand(CommandCallback<T> callback, long targetTime) {
        this.callback = callback;
        this.targetTime = targetTime;
    }

    public CommandCallback<T> callback() {
        return callback;
    }

    public long targetTime() {
        return targetTime;
    }

}
