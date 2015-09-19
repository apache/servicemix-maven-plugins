/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.tooling.depends;

import java.util.Comparator;

import org.apache.maven.model.Dependency;

final class DependencyComparator implements Comparator<Dependency> {
    public int compare(Dependency o1, Dependency o2) {
        int result = o1.getGroupId().compareTo( o2.getGroupId() );
        if ( result == 0 ) {
            result = o1.getArtifactId().compareTo( o2.getArtifactId() );
            if ( result == 0 ) {
                result = o1.getType().compareTo( o2.getType() );
                if ( result == 0 ) {
                    if ( o1.getClassifier() == null ) {
                        if ( o2.getClassifier() != null ) {
                            result = 1;
                        }
                    } else {
                        if ( o2.getClassifier() != null ) {
                            result = o1.getClassifier().compareTo( o2.getClassifier() );
                        } else {
                            result = -1;
                        }
                    }
                    if ( result == 0 ) {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = o1.getVersion().compareTo( o2.getVersion() );
                    }
                }
            }
        }
        return result;
    }
}