/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import java.util.List;

import org.springframework.xd.dirt.module.ModuleDependencyTracker;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * Default implementation of {@link StreamDeployer} that emits deployment request messages on a bus and relies on
 * {@link StreamDefinitionRepository} and {@link StreamRepository} for persistence.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class StreamDeployer extends AbstractInstancePersistingDeployer<StreamDefinition, Stream> {

	private ModuleDependencyTracker dependencyTracker;

	public StreamDeployer(StreamDefinitionRepository repository, DeploymentMessageSender messageSender,
			StreamRepository streamRepository, XDParser parser, ModuleDependencyTracker dependencyTracker) {
		super(repository, streamRepository, messageSender, parser, "stream");
		this.dependencyTracker = dependencyTracker;
	}

	@Override
	protected Stream makeInstance(StreamDefinition definition) {
		return new Stream(definition);
	}

	@Override
	protected StreamDefinition afterSave(StreamDefinition savedDefinition) {
		StreamDefinition definition = super.afterSave(savedDefinition);
		recordDependencies(definition);
		return definition;
	}

	private void recordDependencies(StreamDefinition definition) {
		List<ModuleDeploymentRequest> requests = streamParser.parse(definition.getName(), definition.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			dependencyTracker.record(request, "stream:" + definition.getName());
		}
	}

	@Override
	protected void beforeDelete(StreamDefinition definition) {
		removeDependencies(definition);

		super.beforeDelete(definition);
	}

	private void removeDependencies(StreamDefinition definition) {
		List<ModuleDeploymentRequest> requests = streamParser.parse(definition.getName(), definition.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			dependencyTracker.remove(request, "stream:" + definition.getName());
		}
	}

}
