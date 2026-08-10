<template>
  <div id="questionSubmitView">
    <a-form :model="searchParams" layout="inline">
      <a-form-item field="questionId" label="题号" style="min-width: 240px">
        <a-input v-model="searchParams.questionId" placeholder="请输入" />
      </a-form-item>
      <a-form-item field="language" label="编程语言" style="min-width: 240px">
        <a-select
          v-model="searchParams.language"
          :style="{ width: '320px' }"
          placeholder="选择编程语言"
        >
          <a-option>java</a-option>
          <a-option>cpp</a-option>
          <a-option>go</a-option>
          <a-option>html</a-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="doSubmit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider size="0" />
    <a-table
      :ref="tableRef"
      :columns="columns"
      :data="dataList"
      :pagination="{
        showTotal: true,
        pageSize: searchParams.pageSize,
        current: searchParams.current,
        total,
      }"
      @page-change="onPageChange"
    >
      <template #questionId="{ record }">
        <a-link @click="toQuestionPage(record)">
          {{ record.questionVO?.title ?? record.questionId }}
        </a-link>
      </template>
      <template #userName="{ record }">
        {{ record.userVO?.userName ?? record.userId }}
      </template>
      <template #status="{ record }">
        <a-tag :color="statusColorMap[record.status]">
          {{ statusTextMap[record.status] }}
        </a-tag>
      </template>
      <template #judgeInfo="{ record }">
        <a-space v-if="record.judgeInfo" direction="vertical" size="mini">
          <span v-if="record.judgeInfo.message">{{
            record.judgeInfo.message
          }}</span>
          <span
            v-if="
              record.judgeInfo.time != null || record.judgeInfo.memory != null
            "
          >
            耗时 {{ record.judgeInfo.time ?? "-" }} ms，内存
            {{ record.judgeInfo.memory ?? "-" }} KB
          </span>
          <span
            v-if="
              record.judgeInfo.passedCase != null &&
              record.judgeInfo.totalCase != null
            "
          >
            用例 {{ record.judgeInfo.passedCase }} /
            {{ record.judgeInfo.totalCase }}
          </span>
        </a-space>
        <span v-else>-</span>
      </template>
      <template #createTime="{ record }">
        {{ moment(record.createTime).format("YYYY-MM-DD HH:mm") }}
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watchEffect } from "vue";
import {
  QuestionControllerService,
  QuestionSubmitQueryRequest,
  QuestionSubmitVO,
} from "../../../generated/question";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import moment from "moment";

const tableRef = ref();

const dataList = ref<Array<QuestionSubmitVO>>([]);
const total = ref(0);
const searchParams = ref<QuestionSubmitQueryRequest>({
  questionId: undefined,
  language: undefined,
  pageSize: 10,
  current: 1,
});

const loadData = async () => {
  const res = await QuestionControllerService.listQuestionSubmitByPageUsingPost(
    {
      ...searchParams.value,
      sortField: "createTime",
      sortOrder: "descend",
    }
  );
  if (res.code === 0) {
    dataList.value = res.data.records;
    total.value = res.data.total;
  } else {
    message.error("加载失败，" + res.message);
  }
};

/**
 * 监听 searchParams 变量，改变时触发页面的重新加载
 */
watchEffect(() => {
  loadData();
});

/**
 * 页面加载时，请求数据
 */
onMounted(() => {
  loadData();
});

const columns = [
  {
    title: "提交号",
    dataIndex: "id",
    width: 90,
  },
  {
    title: "题目",
    slotName: "questionId",
  },
  {
    title: "编程语言",
    dataIndex: "language",
    width: 90,
  },
  {
    title: "判题状态",
    slotName: "status",
    width: 100,
  },
  {
    title: "判题信息",
    slotName: "judgeInfo",
  },
  {
    title: "提交者",
    slotName: "userName",
    width: 120,
  },
  {
    title: "创建时间",
    slotName: "createTime",
    width: 150,
  },
];

// 判题状态 -> 文案 / 颜色
const statusTextMap: Record<number, string> = {
  0: "等待中",
  1: "判题中",
  2: "成功",
  3: "失败",
};
const statusColorMap: Record<number, string> = {
  0: "orange",
  1: "arcoblue",
  2: "green",
  3: "red",
};

const onPageChange = (page: number) => {
  searchParams.value = {
    ...searchParams.value,
    current: page,
  };
};

const router = useRouter();

/**
 * 跳转到做题页面
 * @param question
 */
const toQuestionPage = (record: QuestionSubmitVO) => {
  const id = record.questionVO?.id ?? record.questionId;
  if (id != null) {
    router.push({
      path: `/view/question/${id}`,
    });
  }
};

/**
 * 确认搜索，重新加载数据
 */
const doSubmit = () => {
  // 这里需要重置搜索页号
  searchParams.value = {
    ...searchParams.value,
    current: 1,
  };
};
</script>

<style scoped>
#questionSubmitView {
  max-width: 1280px;
  margin: 0 auto;
}
</style>
