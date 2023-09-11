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
package org.camunda.bpm.engine.impl.batch.removaltime;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.impl.batch.BatchJobContext;
import org.camunda.bpm.engine.impl.cfg.TransactionListener;
import org.camunda.bpm.engine.impl.db.DbEntity;
import org.camunda.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ByteArrayManager;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.engine.impl.persistence.entity.MessageEntity;
import org.camunda.bpm.engine.impl.util.ClockUtil;

public class ProcessSetRemovalTimeResultHandler implements TransactionListener {

  protected Map<Class<? extends DbEntity>, DbOperation> operations;
  protected Integer chunkSize;
  protected String jobId;
  protected SetRemovalTimeBatchConfiguration batchJobConfiguration;
  protected CommandExecutor commandExecutor;
  protected ProcessSetRemovalTimeJobHandler jobHandler;

  public ProcessSetRemovalTimeResultHandler(Map<Class<? extends DbEntity>, DbOperation> operations, Integer chunkSize, String jobId, SetRemovalTimeBatchConfiguration batchJobConfiguration, CommandExecutor commandExecutor, ProcessSetRemovalTimeJobHandler jobHandler) {
    this.operations = operations;
    this.chunkSize = chunkSize;
    this.jobId = jobId;
    this.batchJobConfiguration = batchJobConfiguration;
    this.commandExecutor = commandExecutor;
    this.jobHandler = jobHandler;
  }

  @Override
  public void execute(CommandContext commandContext) {
    // use the new command executor since the command context might already have been closed/finished
    commandExecutor.execute(context -> {
        JobEntity job = context.getJobManager().findJobById(jobId);
        Set<String> entitiesToUpdate = getEntitiesToUpdate(operations, chunkSize, commandContext);
        if (entitiesToUpdate.isEmpty()) {
          job.delete(true);
        } else {
          // save batch job configuration
          batchJobConfiguration.setEntities(entitiesToUpdate);
          ByteArrayEntity newConfiguration = saveConfiguration(context.getByteArrayManager(), batchJobConfiguration);
          ProcessSetRemovalTimeJobHandler.JOB_DECLARATION.reconfigure(new BatchJobContext(null, newConfiguration), (MessageEntity) job);
          // reschedule job
          context.getJobManager().reschedule(job, ClockUtil.getCurrentTime());
        }
        return null;
    });
  }

  protected ByteArrayEntity saveConfiguration(ByteArrayManager byteArrayManager, SetRemovalTimeBatchConfiguration jobConfiguration) {
    ByteArrayEntity configurationEntity = new ByteArrayEntity();
    configurationEntity.setBytes(jobHandler.writeConfiguration(jobConfiguration));
    byteArrayManager.insert(configurationEntity);
    return configurationEntity;
  }

  protected static Set<String> getEntitiesToUpdate(Map<Class<? extends DbEntity>, DbOperation> operations, int chunkSize, CommandContext commandContext) {
    return operations.entrySet().stream().filter(op -> op.getValue().getRowsAffected() == chunkSize).map(e -> e.getKey().getName()).collect(Collectors.toSet());
  }
}
