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

package org.springframework.ai.chat.memory;

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

/**
 * The contract for storing and managing the memory of chat conversations.
 * <p></p>
 * 聊天记忆，表示聊天对话历史记录的存储。
 * 它提供向对话添加消息、从对话中检索消息以及清除对话历史记录的方法。
 * <p></p>
 * "大模型的对话记忆"这一概念，根植于人工智能与自然语言处理领域，特别是针对具有深度学习能力的大型语言模型而言，
 * 它指的是模型在与用户进行交互式对话过程中，能够追踪、理解并利用先前对话上下文的能力。
 * 此机制使得大模型不仅能够响应即时的输入请求，还能基于之前的交流内容能够在对话中记住先前的对话内容，并根据这些信息进行后续的响应。
 * 这种记忆机制使得模型能够在对话中持续跟踪和理解用户的意图和上下文，从而实现更自然和连贯的对话。
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 1.0.0
 */
public interface ChatMemory {

	String DEFAULT_CONVERSATION_ID = "default";

	/**
	 * The key to retrieve the chat memory conversation id from the context.
	 */
	String CONVERSATION_ID = "chat_memory_conversation_id";

	/**
	 * Save the specified message in the chat memory for the specified conversation.
	 */
	default void add(String conversationId, Message message) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(message, "message cannot be null");
		this.add(conversationId, List.of(message));
	}

	/**
	 * Save the specified messages in the chat memory for the specified conversation.
	 */
	void add(String conversationId, List<Message> messages);

	/**
	 * Get the messages in the chat memory for the specified conversation.
	 */
	List<Message> get(String conversationId);

	/**
	 * Clear the chat memory for the specified conversation.
	 */
	void clear(String conversationId);

}
