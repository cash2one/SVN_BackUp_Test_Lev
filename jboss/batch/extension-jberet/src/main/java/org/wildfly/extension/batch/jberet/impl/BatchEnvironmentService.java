/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.batch.jberet.impl;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet.impl.ContextHandle.ChainedContextHandle;
import org.wildfly.extension.batch.jberet.impl.ContextHandle.Handle;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.jberet.BatchEnvironmentFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<BatchEnvironment> {

    // This can be removed after the getBatchConfigurationProperties() is removed from jBeret
    private static final Properties PROPS = new Properties();

    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<>();
    private final InjectedValue<TransactionManager> transactionManagerInjector = new InjectedValue<>();
    private final InjectedValue<RequestController> requestControllerInjector = new InjectedValue<>();
    private final InjectedValue<JobRepository> jobRepositoryInjector = new InjectedValue<>();

    private final ClassLoader classLoader;
    private final JobXmlResolver jobXmlResolver;
    private final String deploymentName;
    private BatchEnvironment batchEnvironment = null;
    private ControlPoint controlPoint;

    public BatchEnvironmentService(final ClassLoader classLoader, final JobXmlResolver jobXmlResolver, final String deploymentName) {
        this.classLoader = classLoader;
        this.jobXmlResolver = jobXmlResolver;
        this.deploymentName = deploymentName;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        BatchLogger.LOGGER.debugf("Creating batch environment; %s", classLoader);
        final RequestController requestController = requestControllerInjector.getOptionalValue();
        if (requestController != null) {
            // Create the entry point
            controlPoint = requestController.getControlPoint(deploymentName, "batch-executor-service");
        } else {
            controlPoint = null;
        }
        final BatchEnvironment batchEnvironment = new WildFlyBatchEnvironment(beanManagerInjector.getOptionalValue(),
                executorServiceInjector.getValue(), transactionManagerInjector.getValue(),
                jobRepositoryInjector.getValue(), jobXmlResolver, controlPoint);
        // Add the service to the factory
        BatchEnvironmentFactory.getInstance().add(classLoader, batchEnvironment);
        this.batchEnvironment = batchEnvironment;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        BatchLogger.LOGGER.debugf("Removing batch environment; %s", classLoader);
        BatchEnvironmentFactory.getInstance().remove(classLoader);
        batchEnvironment = null;
        if (controlPoint != null) {
            requestControllerInjector.getValue().removeControlPoint(controlPoint);
        }
    }

    @Override
    public synchronized BatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjector() {
        return executorServiceInjector;
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjector() {
        return transactionManagerInjector;
    }

    public InjectedValue<RequestController> getRequestControllerInjector() {
        return requestControllerInjector;
    }

    public InjectedValue<JobRepository> getJobRepositoryInjector() {
        return jobRepositoryInjector;
    }

    private class WildFlyBatchEnvironment implements BatchEnvironment {

        private final ArtifactFactory artifactFactory;
        private final ExecutorService executorService;
        private final TransactionManager transactionManager;
        private final JobRepository jobRepository;
        private final JobXmlResolver jobXmlResolver;
        private final ControlPoint controlPoint;

        WildFlyBatchEnvironment(final BeanManager beanManager,
                                final ExecutorService executorService,
                                final TransactionManager transactionManager,
                                final JobRepository jobRepository,
                                final JobXmlResolver jobXmlResolver,
                                final ControlPoint controlPoint) {
            this.jobXmlResolver = jobXmlResolver;
            artifactFactory = new WildFlyArtifactFactory(beanManager);
            this.executorService = executorService;
            this.transactionManager = transactionManager;
            this.controlPoint = controlPoint;
            this.jobRepository = jobRepository;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            return artifactFactory;
        }

        @Override
        public void submitTask(final Runnable task) {
            final ContextHandle contextHandle = createContextHandle();
            // Wrap the runnable to setup the context for the thread
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    final Handle handle = contextHandle.setup();
                    try {
                        task.run();
                    } finally {
                        handle.tearDown();
                    }
                }
            };
            if (controlPoint == null) {
                executorService.submit(r);
            } else {
                // Queue the task to run in the control point, if resume is executed the queued tasks will run
                controlPoint.queueTask(r, executorService, -1, null, false);
            }
        }

        @Override
        public TransactionManager getTransactionManager() {
            return transactionManager;
        }

        @Override
        public JobRepository getJobRepository() {
            return jobRepository;
        }

        @Override
        public JobXmlResolver getJobXmlResolver() {
            return jobXmlResolver;
        }

        @Override
        public Properties getBatchConfigurationProperties() {
            return PROPS;
        }

        // Note this method will likely go away when a better solution for JBERET-180 is done
        @Override
        public void jobExecutionFinished() {
            BatchLogger.LOGGER.trace("The jobExecutionFinishedMethod() is not implemented in WildFly");
        }

        private ContextHandle createContextHandle() {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            // If the TCCL is null, use the deployments ModuleClassLoader
            final ClassLoaderContextHandle classLoaderContextHandle = (tccl == null ? new ClassLoaderContextHandle(classLoader) : new ClassLoaderContextHandle(tccl));
            // Class loader handle must be first so the TCCL is set before the other handles execute
            return new ChainedContextHandle(classLoaderContextHandle, new NamespaceContextHandle(), new SecurityContextHandle());
        }
    }
}