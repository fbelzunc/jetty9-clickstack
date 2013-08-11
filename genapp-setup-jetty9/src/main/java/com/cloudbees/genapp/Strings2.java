/*
 * Copyright 2010-2013, the original author or authors
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
package com.cloudbees.genapp;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Strings2 {

    /**
     * @param str
     * @param searchFor
     * @return <code>null</code> if given <code>str</code> is <code>null</code>. return full <code>String</code> if <code>searchFor</code> is not found
     */
    @Nullable
    public static java.lang.String substringBeforeFirst(@Nullable java.lang.String str, char searchFor) {
        if (str == null) {
            return null;
        }
        int idx = str.indexOf(searchFor);
        if (idx == -1) {
            return str;
        }
        return str.substring(0, idx);
    }


    /**
     * @param str
     * @param searchFor
     * @return <code>null</code> if given <code>str</code> is <code>null</code> or if <code>searchFor</code> is not found
     */
    @Nullable
    public static java.lang.String substringAfterFirst(@Nullable java.lang.String str, char searchFor) {
        if (str == null) {
            return null;
        }
        int idx = str.indexOf(searchFor);
        if (idx == -1) {
            return null;
        }
        return str.substring(idx+1);
    }
}
