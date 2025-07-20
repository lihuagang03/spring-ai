/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.chat.client;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.micrometer.observation.ObservationRegistry;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Client to perform stateless requests to an AI Model, using a fluent API.
 * <p></p>
 * 对话客户端，向 AI 模型执行无状态请求，使用流畅的 API。
 *
 * Use {@link ChatClient#builder(ChatModel)} to prepare an instance.
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Josh Long
 * @author Arjen Poutsma
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ChatClient {

	// 对话模型-ChatModel
	// 对话客户端

	static ChatClient create(ChatModel chatModel) {
		return create(chatModel, ObservationRegistry.NOOP);
	}

	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry) {
		return create(chatModel, observationRegistry, null);
	}

	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention observationConvention) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		return builder(chatModel, observationRegistry, observationConvention).build();
	}

	// 创建对话客户端实例的构建者

	static Builder builder(ChatModel chatModel) {
		return builder(chatModel, ObservationRegistry.NOOP, null);
	}

	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
			@Nullable ChatClientObservationConvention customObservationConvention) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		// 创建对话客户端实例的构建者
		return new DefaultChatClientBuilder(chatModel, observationRegistry, customObservationConvention);
	}

	// 提示词-Prompt
	// 对话客户端的请求规范

	ChatClientRequestSpec prompt();

	ChatClientRequestSpec prompt(String content);

	ChatClientRequestSpec prompt(Prompt prompt);

	/**
	 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
	 * settings are replicated from the default {@link ChatClientRequestSpec} of this
	 * client.
	 */
	Builder mutate();

	/**
	 * 提示用户的规范
	 */
	interface PromptUserSpec {

		PromptUserSpec text(String text);

		PromptUserSpec text(Resource text, Charset charset);

		PromptUserSpec text(Resource text);

		PromptUserSpec params(Map<String, Object> p);

		PromptUserSpec param(String k, Object v);

		PromptUserSpec media(Media... media);

		PromptUserSpec media(MimeType mimeType, URL url);

		PromptUserSpec media(MimeType mimeType, Resource resource);

	}

	/**
	 * Specification for a prompt system.
	 * <p></p>
	 * 提示系统的规范
	 */
	interface PromptSystemSpec {

		PromptSystemSpec text(String text);

		PromptSystemSpec text(Resource text, Charset charset);

		PromptSystemSpec text(Resource text);

		PromptSystemSpec params(Map<String, Object> p);

		PromptSystemSpec param(String k, Object v);

	}

	/**
	 * 顾问的规范
	 * <p></p>
	 * 使用上下文数据附加或扩充 Prompt
	 * 用于扩充 Prompt 的上下文数据
	 * <pre>
	 * 您自己的数据
	 * 对话历史记录
	 * </pre>
	 */
	interface AdvisorSpec {

		AdvisorSpec param(String k, Object v);

		AdvisorSpec params(Map<String, Object> p);

		AdvisorSpec advisors(Advisor... advisors);

		AdvisorSpec advisors(List<Advisor> advisors);

	}

	/**
	 * 同步响应的规范
	 */
	interface CallResponseSpec {

		// 返回实体类-Entity

		@Nullable
		<T> T entity(ParameterizedTypeReference<T> type);

		@Nullable
		<T> T entity(StructuredOutputConverter<T> structuredOutputConverter);

		@Nullable
		<T> T entity(Class<T> type);

		// 返回 ChatResponse

		ChatClientResponse chatClientResponse();

		@Nullable
		ChatResponse chatResponse();

		@Nullable
		String content();

		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type);

		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type);

		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter);

	}

	/**
	 * 流式响应的规范
	 */
	interface StreamResponseSpec {

		Flux<ChatClientResponse> chatClientResponse();

		Flux<ChatResponse> chatResponse();

		Flux<String> content();

	}

	/**
	 * 同步提示词的响应规范
	 */
	interface CallPromptResponseSpec {

		String content();

		List<String> contents();

		ChatResponse chatResponse();

	}

	/**
	 * 流式提示词的响应规范
	 */
	interface StreamPromptResponseSpec {

		Flux<ChatResponse> chatResponse();

		Flux<String> content();

	}

	/**
	 * 对话客户端的请求规范
	 */
	interface ChatClientRequestSpec {

		/**
		 * Return a {@code ChatClient.Builder} to create a new {@code ChatClient} whose
		 * settings are replicated from this {@code ChatClientRequest}.
		 */
		Builder mutate();

		// 顾问链

		ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);

		ChatClientRequestSpec advisors(Advisor... advisors);

		ChatClientRequestSpec advisors(List<Advisor> advisors);

		// 对话记忆

		ChatClientRequestSpec messages(Message... messages);

		ChatClientRequestSpec messages(List<Message> messages);

		// AI模型的交互参数

		<T extends ChatOptions> ChatClientRequestSpec options(T options);

		// 工具调用

		ChatClientRequestSpec toolNames(String... toolNames);

		ChatClientRequestSpec tools(Object... toolObjects);

		ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);

		ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks);

		ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		ChatClientRequestSpec toolContext(Map<String, Object> toolContext);

		// 系统提示词

		ChatClientRequestSpec system(String text);

		ChatClientRequestSpec system(Resource textResource, Charset charset);

		ChatClientRequestSpec system(Resource text);

		ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);

		// 用户提示词

		ChatClientRequestSpec user(String text);

		ChatClientRequestSpec user(Resource text, Charset charset);

		ChatClientRequestSpec user(Resource text);

		ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);

		// 提示词模板

		ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer);

		// 同步响应

		CallResponseSpec call();

		// 流式响应

		StreamResponseSpec stream();

	}

	/**
	 * A mutable builder for creating a {@link ChatClient}.
	 * <p></p>
	 * 创建对话客户端实例的构建者
	 */
	interface Builder {

		// 定制 ChatClient 默认值
		// 默认的顾问链

		Builder defaultAdvisors(Advisor... advisors);

		Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);

		Builder defaultAdvisors(List<Advisor> advisors);

		// 默认的AI模型的交互参数

		Builder defaultOptions(ChatOptions chatOptions);

		// 默认的用户提示词

		Builder defaultUser(String text);

		Builder defaultUser(Resource text, Charset charset);

		Builder defaultUser(Resource text);

		Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);

		// 默认的系统提示词

		Builder defaultSystem(String text);

		Builder defaultSystem(Resource text, Charset charset);

		Builder defaultSystem(Resource text);

		Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);

		// 默认的提示词模板

		Builder defaultTemplateRenderer(TemplateRenderer templateRenderer);

		// 默认的工具调用

		Builder defaultToolNames(String... toolNames);

		Builder defaultTools(Object... toolObjects);

		Builder defaultToolCallbacks(ToolCallback... toolCallbacks);

		Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks);

		Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders);

		Builder defaultToolContext(Map<String, Object> toolContext);

		Builder clone();

		// 创建新的对话客户端实例

		ChatClient build();

	}

}
