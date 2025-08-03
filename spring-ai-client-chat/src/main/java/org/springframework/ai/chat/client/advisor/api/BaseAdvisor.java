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

package org.springframework.ai.chat.client.advisor.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.AdvisorUtils;
import org.springframework.util.Assert;

/**
 * Base advisor that implements common aspects of the {@link CallAdvisor} and
 * {@link StreamAdvisor}, reducing the boilerplate code needed to implement an advisor.
 * <p></p>
 * 基础顾问，实现调用顾问和流式顾问的共同方面，从而减少实现顾问所需的样板代码。
 * <p>
 * It provides default implementations for the
 * {@link #adviseCall(ChatClientRequest, CallAdvisorChain)} and
 * {@link #adviseStream(ChatClientRequest, StreamAdvisorChain)} methods, delegating the
 * actual logic to the {@link #before(ChatClientRequest, AdvisorChain advisorChain)} and
 * {@link #after(ChatClientResponse, AdvisorChain advisorChain)} methods.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface BaseAdvisor extends CallAdvisor, StreamAdvisor {

	/**
	 * 默认的调度器
	 */
	Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();

	// 调用顾问，CallAdvisor

	@Override
	default ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
		Assert.notNull(callAdvisorChain, "callAdvisorChain cannot be null");

		// 在调用其余顾问链之前要执行的逻辑
		ChatClientRequest processedChatClientRequest = before(chatClientRequest, callAdvisorChain);
		// 下一个调用顾问
		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(processedChatClientRequest);
		// 在调用其余顾问链之后要执行的逻辑
		return after(chatClientResponse, callAdvisorChain);
	}

	// 流式调用顾问，StreamAdvisor

	@Override
	default Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
		Assert.notNull(streamAdvisorChain, "streamAdvisorChain cannot be null");
		Assert.notNull(getScheduler(), "scheduler cannot be null");

		Flux<ChatClientResponse> chatClientResponseFlux = Mono.just(chatClientRequest)
			.publishOn(getScheduler())
			// 在调用其余顾问链之前要执行的逻辑
			.map(request -> this.before(request, streamAdvisorChain))
			// 下一个流式顾问
			.flatMapMany(streamAdvisorChain::nextStream);

		return chatClientResponseFlux.map(response -> {
			if (AdvisorUtils.onFinishReason().test(response)) {
				// 在调用其余顾问链之后要执行的逻辑
				response = after(response, streamAdvisorChain);
			}
			return response;
		}).onErrorResume(error -> Flux.error(new IllegalStateException("Stream processing failed", error)));
	}

	// 顾问的名称

	@Override
	default String getName() {
		// 类的简单名称
		return this.getClass().getSimpleName();
	}

	// 顾问环绕切点，AOP

	/**
	 * Logic to be executed before the rest of the advisor chain is called.
	 * <p></p>
	 * 在调用其余顾问链之前要执行的逻辑。
	 */
	ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain);

	/**
	 * Logic to be executed after the rest of the advisor chain is called.
	 * <p></p>
	 * 在调用其余顾问链之后要执行的逻辑。
	 */
	ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain);

	/**
	 * Scheduler used for processing the advisor logic when streaming.
	 */
	default Scheduler getScheduler() {
		return DEFAULT_SCHEDULER;
	}

}
