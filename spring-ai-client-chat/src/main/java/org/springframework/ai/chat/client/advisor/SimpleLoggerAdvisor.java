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

package org.springframework.ai.chat.client.advisor;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;

/**
 * A simple logger advisor that logs the request and response messages.
 * <p></p>
 * 日志记录顾问，一个用于记录对话客户端的请求入参和响应输出的消息数据，这对于调试和监控您的AI交互非常有用。
 *
 * @author Christian Tzolov
 */
public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {

	/**
	 * 将对话客户端请求转化为字符串
	 */
	public static final Function<ChatClientRequest, String> DEFAULT_REQUEST_TO_STRING = ChatClientRequest::toString;

	/**
	 * 将对话响应转化为JSON字符串
	 */
	public static final Function<ChatResponse, String> DEFAULT_RESPONSE_TO_STRING = ModelOptionsUtils::toJsonStringPrettyPrinter;

	private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);

	private final Function<ChatClientRequest, String> requestToString;

	private final Function<ChatResponse, String> responseToString;

	private final int order;

	public SimpleLoggerAdvisor() {
		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, 0);
	}

	public SimpleLoggerAdvisor(int order) {
		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, order);
	}

	public SimpleLoggerAdvisor(@Nullable Function<ChatClientRequest, String> requestToString,
			@Nullable Function<ChatResponse, String> responseToString, int order) {
		this.requestToString = requestToString != null ? requestToString : DEFAULT_REQUEST_TO_STRING;
		this.responseToString = responseToString != null ? responseToString : DEFAULT_RESPONSE_TO_STRING;
		this.order = order;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		// 记录请求输入日志
		logRequest(chatClientRequest);

		// 下一个调用顾问
		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

		// 记录响应输出日志
		logResponse(chatClientResponse);

		return chatClientResponse;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		logRequest(chatClientRequest);

		Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

		return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
	}

	private void logRequest(ChatClientRequest request) {
		// 记录请求输入日志
		logger.debug("request: {}", this.requestToString.apply(request));
	}

	private void logResponse(ChatClientResponse chatClientResponse) {
		// 记录响应输出日志
		logger.debug("response: {}", this.responseToString.apply(chatClientResponse.chatResponse()));
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String toString() {
		return SimpleLoggerAdvisor.class.getSimpleName();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private Function<ChatClientRequest, String> requestToString;

		private Function<ChatResponse, String> responseToString;

		private int order = 0;

		private Builder() {
		}

		public Builder requestToString(Function<ChatClientRequest, String> requestToString) {
			this.requestToString = requestToString;
			return this;
		}

		public Builder responseToString(Function<ChatResponse, String> responseToString) {
			this.responseToString = responseToString;
			return this;
		}

		public Builder order(int order) {
			this.order = order;
			return this;
		}

		public SimpleLoggerAdvisor build() {
			return new SimpleLoggerAdvisor(this.requestToString, this.responseToString, this.order);
		}

	}

}
