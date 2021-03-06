/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.execution;

import com.dangdang.ddframe.job.api.executor.ShardingContexts;
import com.dangdang.ddframe.job.api.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.api.type.dataflow.api.DataflowJobConfiguration;
import com.dangdang.ddframe.job.lite.api.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.fixture.TestDataflowJob;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodeStorage;
import com.dangdang.ddframe.job.util.env.LocalHostService;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.unitils.util.ReflectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class ExecutionContextServiceTest {
    
    @Mock
    private JobNodeStorage jobNodeStorage;
    
    @Mock
    private LocalHostService localHostService;
    
    @Mock
    private ConfigurationService configService;
    
    private final ExecutionContextService executionContextService = new ExecutionContextService(null, "test_job");
    
    @Before
    public void setUp() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);
        ReflectionUtils.setFieldValue(executionContextService, "jobNodeStorage", jobNodeStorage);
        ReflectionUtils.setFieldValue(executionContextService, "configService", configService);
        when(localHostService.getIp()).thenReturn("mockedIP");
        when(localHostService.getHostName()).thenReturn("mockedHostName");
    }
    
    @Test
    public void assertGetShardingContextWhenNotAssignShardingItem() {
        when(configService.load(false)).thenReturn(LiteJobConfiguration.newBuilder(new DataflowJobConfiguration(JobCoreConfiguration.newBuilder("test_job", "0/1 * * * * ?", 3).build(), 
                TestDataflowJob.class.getCanonicalName(), true)).monitorExecution(false).build());
        ShardingContexts expected = new ShardingContexts("test_job", 3, "", Collections.<Integer, String>emptyMap());
        assertThat(executionContextService.getJobShardingContext(Collections.<Integer>emptyList()), new ReflectionEquals(expected));
        verify(configService).load(false);
    }
    
    @Test
    public void assertGetShardingContextWhenAssignShardingItems() {
        when(configService.load(false)).thenReturn(LiteJobConfiguration.newBuilder(new DataflowJobConfiguration(JobCoreConfiguration.newBuilder("test_job", "0/1 * * * * ?", 3)
                .shardingItemParameters("0=A,1=B,2=C").build(), TestDataflowJob.class.getCanonicalName(), true)).monitorExecution(false).build());
        Map<Integer, String> map = new HashMap<>(3);
        map.put(0, "A");
        map.put(1, "B");
        ShardingContexts expected = new ShardingContexts("test_job", 3, "", map);
        assertShardingContext(executionContextService.getJobShardingContext(Arrays.asList(0, 1)), expected);
        verify(configService).load(false);
    }
    
    @Test
    public void assertGetShardingContextWhenHasRunningItems() {
        when(configService.load(false)).thenReturn(LiteJobConfiguration.newBuilder(new DataflowJobConfiguration(JobCoreConfiguration.newBuilder("test_job", "0/1 * * * * ?", 3)
                .shardingItemParameters("0=A,1=B,2=C").build(), TestDataflowJob.class.getCanonicalName(), true)).monitorExecution(true).build());
        when(jobNodeStorage.isJobNodeExisted("execution/0/running")).thenReturn(false);
        when(jobNodeStorage.isJobNodeExisted("execution/1/running")).thenReturn(true);
        Map<Integer, String> map = new HashMap<>(1, 1);
        map.put(0, "A");
        ShardingContexts expected = new ShardingContexts("test_job", 3, "", map);
        assertShardingContext(executionContextService.getJobShardingContext(Lists.newArrayList(0, 1)), expected);
        verify(configService).load(false);
        verify(jobNodeStorage).isJobNodeExisted("execution/0/running");
        verify(jobNodeStorage).isJobNodeExisted("execution/1/running");
    }
    
    private void assertShardingContext(final ShardingContexts actual, final ShardingContexts expected) {
        assertThat(actual.getJobName(), is(expected.getJobName()));
        assertThat(actual.getShardingTotalCount(), is(expected.getShardingTotalCount()));
        assertThat(actual.getJobParameter(), is(expected.getJobParameter()));
        assertThat(actual.getShardingItemParameters().size(), is(expected.getShardingItemParameters().size()));
        for (int i = 0; i < expected.getShardingItemParameters().size(); i++) {
            assertThat(actual.getShardingItemParameters().get(i), is(expected.getShardingItemParameters().get(i)));
        }
    }
}
