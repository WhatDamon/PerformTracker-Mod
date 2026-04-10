/*
 * Copyright 2026 Damon Lu and open-source contributors
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

package org.damon233.performtrackermod.utils;

import java.util.function.Supplier;

public class CachedValue<T> {
    private volatile T cached;
    private final Supplier<T> initializer;

    public CachedValue(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public T get() {
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cached == null) {
                cached = initializer.get();
            }
        }
        return cached;
    }

    public void invalidate() {
        synchronized (this) {
            cached = null;
        }
    }
}
