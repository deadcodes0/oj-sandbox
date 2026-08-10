<template>
  <div id="ragSearchView">
    <a-form :model="searchParams" layout="inline">
      <a-form-item field="keyword" label="关键词" style="min-width: 280px">
        <a-input
          v-model="searchParams.keyword"
          placeholder="请输入检索关键词"
          @press-enter="doSearch"
        />
      </a-form-item>
      <a-form-item field="topN" label="数量">
        <a-input-number v-model="searchParams.topN" :min="1" :max="50" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="doSearch">检索</a-button>
          <template v-if="isAdminUser">
            <a-tooltip
              content="重建题库检索索引：将题目内容重新写入向量库，供「题目检索」和「智能答疑」使用，一般新增题目后执行。"
            >
              <a-button @click="doRebuild">重建索引</a-button>
            </a-tooltip>
          </template>
        </a-space>
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
import { computed, ref } from "vue";
import {
  RagControllerService,
  RagSearchItem,
  RagSearchRequest,
} from "../../../generated/rag";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import ACCESS_ENUM from "@/access/accessEnum";

// 是否管理员：决定是否展示「重建索引」按钮
const isAdminUser = computed(
  () => useStore().state.user.loginUser?.userRole === ACCESS_ENUM.ADMIN
);

const searchParams = ref<RagSearchRequest>({
  keyword: "",
  topN: 10,
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

const doSearch = async () => {
  const res = await RagControllerService.searchUsingPost(searchParams.value);
  if (res.code === 0) {
    dataList.value = res.data ?? [];
  } else {
    message.error("检索失败，" + res.message);
  }
};

const doRebuild = async () => {
  // 重建索引仅管理员可操作（后端同样做了权限控制），非管理员直接忽略
  if (!isAdminUser.value) {
    message.warning("仅管理员可重建索引");
    return;
  }
  const res = await RagControllerService.rebuildUsingPost();
  if (res.code === 0) {
    message.success("重建索引成功");
  } else {
    message.error("重建索引失败，" + res.message);
  }
};

const toQuestionPage = (item: RagSearchItem) => {
  if (item.questionId != null) {
    router.push({
      path: `/view/question/${item.questionId}`,
    });
  }
};
</script>

<style scoped>
#ragSearchView {
  max-width: 1280px;
  margin: 0 auto;
}
</style>
