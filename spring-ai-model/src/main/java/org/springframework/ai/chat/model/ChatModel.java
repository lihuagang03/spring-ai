/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.ai.chat.model;

import java.util.Arrays;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Model;

/**
 * 对话模型，文本聊天交互模型。
 * <p></p>
 * 其工作原理是接收 Prompt 或部分对话作为输入，将输入发送给后端大模型，模型根据其训练数据和对自然语言的理解生成对话响应，
 * 应用程序可以将响应呈现给用户或用于进一步处理。
 */
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {

	default String call(String message) {
		Prompt prompt = new Prompt(new UserMessage(message));
		Generation generation = call(prompt).getResult();
		return (generation != null) ? generation.getOutput().getText() : "";
	}

	default String call(Message... messages) {
		Prompt prompt = new Prompt(Arrays.asList(messages));
		Generation generation = call(prompt).getResult();
		return (generation != null) ? generation.getOutput().getText() : "";
	}

	@Override
	ChatResponse call(Prompt prompt);

	default ChatOptions getDefaultOptions() {
		return ChatOptions.builder().build();
	}

	@Override
	default Flux<ChatResponse> stream(Prompt prompt) {
		throw new UnsupportedOperationException("streaming is not supported");
	}

}
