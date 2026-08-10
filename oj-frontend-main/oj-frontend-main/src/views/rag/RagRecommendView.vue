<template>
  <div id="ragRecommendView">
    <a-form :model="searchParams" layout="inline">
      <a-form-item field="questionId" label="题目ID" style="min-width: 240px">
        <a-input-number v-model="searchParams.questionId" :min="1" />
      </a-form-item>
      <a-form-item field="topN" label="推荐数量">
        <a-input-number v-model="searchParams.topN" :min="1" :max="50" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="doRecommend">推荐</a-button>
      </a-form-item>
    </a-form>
    <a-divider size="0" />
    <a-table :columns="columns" :data="dataList" :pagination="false">
      <template #score="{ record }">
        {{ record.score?.toFixed(4) }}
      </template>
      <template #optional="{ record }">
        <a-button type="link" @click="toQuestionPage(record.questionId)">
          去做题
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import {
  RagControllerService,
  RagRecommendRequest,
  RagSearchItem,
} from "../../../generated/rag";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";

const searchParams = ref<RagRecommendRequest>({
  questionId: undefined,
  topN: 5,
});

const dataList = ref<Array<RagSearchItem>>([]);

const columns = [
  {
    title: "题目ID",
    dataIndex: "questionId",
  },
  {
    title: "标题",
    dataIndex: "title",
  },
  {
    title: "相关度",
    slotName: "score",
  },
  {
    slotName: "optional",
  },
];

const router = useRouter();

const doRecommend = async () => {
  if (searchParams.value.questionId == null) {
    message.warning("请输入题目ID");
    return;
  }
  const res = await RagControllerService.recommendUsingPost(searchParams.value);
  if (res.code === 0) {
    dataList.value = res.data ?? [];
  } else {
    message.error("推荐失败，" + res.message);
  }
};

const toQuestionPage = (id?: number) => {
  if (id != null) {
    router.push({
      path: `/view/question/${id}`,
    });
  }
};
</script>

<style scoped>
#ragRecommendView {
  max-width: 1280px;
  margin: 0 auto;
}
</style>
