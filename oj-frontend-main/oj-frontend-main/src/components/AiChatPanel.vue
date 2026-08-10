<template>
  <div class="ai-chat-panel" :style="{ height }">
    <div ref="chatBodyRef" class="ai-chat-body">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="ai-msg"
        :class="msg.role === 'user' ? 'ai-msg-user' : 'ai-msg-assistant'"
      >
        <MdViewer v-if="msg.role === 'assistant'" :value="msg.content" />
        <div v-else class="ai-msg-content">{{ msg.content }}</div>
        <template
          v-if="
            msg.role === 'assistant' && msg.references && msg.references.length
          "
        >
          <div class="ai-refs">
            <span>相关题目：</span>
            <a-link
              v-for="(ref, ri) in msg.references"
              :key="ri"
              class="ai-ref-link"
              @click="toQuestionPage(ref.questionId)"
            >
              {{ ref.title }}
            </a-link>
          </div>
        </template>
      </div>
      <div v-if="loading" class="ai-thinking">AI 思考中…</div>
    </div>
    <div class="ai-chat-input">
      <a-textarea
        v-model="input"
        :placeholder="placeholder"
        :auto-size="{ minRows: 2, maxRows: 5 }"
        @keydown="onKeydown"
      />
      <a-button
        type="primary"
        :loading="loading"
        :disabled="!input.trim()"
        @click="send(input)"
      >
        发送
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue";
import { useRouter } from "vue-router";
import MdViewer from "@/components/MdViewer.vue";
import { RagControllerService } from "../../generated/rag";

interface AiMsg {
  role: "user" | "assistant";
  content: string;
  references?: { questionId?: number; title?: string }[];
}

const props = withDefaults(
  defineProps<{
    questionId?: number;
    initialMessage?: string;
    placeholder?: string;
    height?: string;
  }>(),
  {
    placeholder: "输入你的问题，Enter 发送，Shift+Enter 换行",
    height: "100%",
  }
);

const messages = ref<AiMsg[]>([]);
const loading = ref(false);
const pending = ref(0); // 尚未得到回答的用户消息条数
const input = ref("");
const chatBodyRef = ref<HTMLElement>();

const router = useRouter();

const send = async (raw: string) => {
  const text = (raw ?? "").trim();
  if (!text) {
    return;
  }
  // 用户消息立刻先上屏（乐观渲染），不被“思考中”挡掉；加载中再发则排队补发
  messages.value.push({ role: "user", content: text });
  input.value = "";
  pending.value++;
  await scrollToBottom();
  if (!loading.value) {
    await drain();
  }
};

const drain = async () => {
  while (pending.value > 0) {
    // 处理最早的未回答用户消息
    const fromIdx = messages.value.length - pending.value;
    const userMsg = messages.value[fromIdx];
    const history = messages.value
      .slice(0, fromIdx)
      .map((m) => ({ role: m.role, content: m.content }));
    loading.value = true;
    try {
      const res = await RagControllerService.chatUsingPost({
        message: userMsg.content,
        questionId: props.questionId,
        messages: history,
      });
      if (res.code === 0) {
        messages.value.push({
          role: "assistant",
          content: res.data?.content ?? "",
          references: Array.isArray(res.data?.references)
            ? res.data.references
            : [],
        });
      } else {
        messages.value.push({
          role: "assistant",
          content: "解析失败：" + (res.message ?? "未知错误"),
        });
      }
    } catch (e) {
      messages.value.push({
        role: "assistant",
        content: "解析异常：" + ((e as any)?.message ?? e),
      });
    } finally {
      loading.value = false;
      pending.value--;
      await scrollToBottom();
    }
  }
};

/**
 * 首次打开时若还没有任何消息，自动发一条预填问题（供侧栏“AI 智能解析”入口使用）
 */
const start = () => {
  if (messages.value.length === 0 && props.initialMessage) {
    send(props.initialMessage);
  }
};

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    send(input.value);
  }
};

const scrollToBottom = () => {
  return nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
};

const toQuestionPage = (id?: number) => {
  if (id != null) {
    router.push({ path: `/view/question/${id}` });
  }
};

defineExpose({ send, start });
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.ai-chat-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 8px;
}
.ai-msg-user {
  align-self: flex-end;
  max-width: 100%;
  background: var(--color-primary-6);
  color: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-msg-assistant {
  align-self: flex-start;
  width: 100%;
  background: var(--color-fill-2);
  border-radius: 8px;
  padding: 10px 12px;
}
.ai-refs {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-2);
  font-size: 13px;
}
.ai-ref-link {
  margin-right: 12px;
}
.ai-thinking {
  color: var(--color-text-3);
  font-size: 13px;
}
.ai-chat-input {
  display: flex;
  gap: 8px;
  padding-top: 12px;
}
.ai-chat-input .arco-textarea-wrapper {
  flex: 1;
}
</style>
