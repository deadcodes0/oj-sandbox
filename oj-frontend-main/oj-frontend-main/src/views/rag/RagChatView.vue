<template>
  <div id="ragChatView">
    <a-card title="智能答疑">
      <p class="desc">和 AI 自由对话：可多次追问，AI 会基于题目知识库回答。</p>
      <AiChatPanel
        :height="'calc(100vh - 260px)'"
        placeholder="和 AI 自由对话，可多次追问…"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import AiChatPanel from "@/components/AiChatPanel.vue";
</script>

<style scoped>
#ragChatView {
  max-width: 1280px;
  margin: 0 auto;
}
.desc {
  color: var(--color-text-3);
  margin-bottom: 12px;
}
</style>
