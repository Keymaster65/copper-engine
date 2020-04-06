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
package org.copperengine.management.model;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class MeasurePointData implements Serializable {

    private static final long serialVersionUID = -3238490302037724442L;

    private String mpId;
    private long elementCount = 0L;
    private long elapsedTimeMicros = 0L;
    private long count = 0L;

    public MeasurePointData() {
    }

    @ConstructorProperties({ "mpId", "elementCount", "elapsedTimeMicros", "count" })
    public MeasurePointData(String mpId, long elementCount, long elapsedTimeMicros, long count) {
        super();
        this.mpId = mpId;
        this.elementCount = elementCount;
        this.elapsedTimeMicros = elapsedTimeMicros;
        this.count = count;
    }

    public String getMpId() {
        return mpId;
    }

    public void setMpId(String mpId) {
        this.mpId = mpId;
    }

    public long getElementCount() {
        return elementCount;
    }

    public void setElementCount(long elementCount) {
        this.elementCount = elementCount;
    }

    public long getElapsedTimeMicros() {
        return elapsedTimeMicros;
    }

    public void setElapsedTimeMicros(long elapsedTimeMicros) {
        this.elapsedTimeMicros = elapsedTimeMicros;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

}
