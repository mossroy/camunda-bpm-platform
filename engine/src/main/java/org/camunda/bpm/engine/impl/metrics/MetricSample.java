/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.camunda.bpm.engine.impl.metrics;

import java.util.List;

/**
 * Sample of metric values
 */
public class MetricSample {

  public final String name;
  public final List<String> labelNames;
  public final List<String> labelValues;  // Must have same length as labelNames.
  public long value;

  public MetricSample(String name, List<String> labelNames, List<String> labelValues, long value) {
    this.name = name;
    this.labelNames = labelNames;
    this.labelValues = labelValues;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public List<String> getLabelNames() {
    return labelNames;
  }

  public List<String> getLabelValues() {
    return labelValues;
  }

  public long getValue() {
    return value;
  }

  public void setValue(long value) {
    this.value = value;
  }
}
