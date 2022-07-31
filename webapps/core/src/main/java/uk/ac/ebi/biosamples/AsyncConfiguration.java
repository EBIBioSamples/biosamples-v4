/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {
  private final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

  @Override
  @Bean(name = "taskExecutor")
  public AsyncTaskExecutor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(25);
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }

  /** Configure async support for REST calls */
  @Bean
  public WebMvcConfigurer webMvcConfigurerConfigurer(
      AsyncTaskExecutor taskExecutor, CallableProcessingInterceptor callableProcessingInterceptor) {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer
            .setDefaultTimeout(300000)
            .setTaskExecutor(taskExecutor)
            .registerCallableInterceptors(callableProcessingInterceptor);
        super.configureAsyncSupport(configurer);
      }
    };
  }

  @Bean
  public CallableProcessingInterceptor callableProcessingInterceptor() {
    return new TimeoutCallableProcessingInterceptor() {
      @Override
      public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
        log.error("Bulk sample download timed out");
        return super.handleTimeout(request, task);
      }
    };
  }
}
