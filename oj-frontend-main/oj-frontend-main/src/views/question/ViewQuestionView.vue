<template>
  <div id="viewQuestionView">
    <a-row :gutter="[24, 24]">
      <a-col :md="12" :xs="24">
        <a-tabs default-active-key="question">
          <a-tab-pane key="question" title="题目">
            <a-card v-if="question" :title="question.title">
              <a-descriptions
                title="判题条件"
                :column="{ xs: 1, md: 2, lg: 3 }"
              >
                <a-descriptions-item label="时间限制">
                  {{ question.judgeConfig.timeLimit ?? 0 }}
                </a-descriptions-item>
                <a-descriptions-item label="内存限制">
                  {{ question.judgeConfig.memoryLimit ?? 0 }}
                </a-descriptions-item>
                <a-descriptions-item label="堆栈限制">
                  {{ question.judgeConfig.stackLimit ?? 0 }}
                </a-descriptions-item>
              </a-descriptions>
              <MdViewer :value="question.content || ''" />
              <template #extra>
                <a-space wrap>
                  <a-tag
                    v-for="(tag, index) of question.tags"
                    :key="index"
                    color="green"
                    >{{ tag }}
                  </a-tag>
                </a-space>
              </template>
            </a-card>
          </a-tab-pane>
          <a-tab-pane key="comment" title="评论" disabled> 评论区</a-tab-pane>
          <a-tab-pane key="answer" title="答案"> 暂时无法查看答案</a-tab-pane>
        </a-tabs>
      </a-col>
      <a-col :md="12" :xs="24">
        <a-form :model="form" layout="inline">
          <a-form-item
            field="language"
            label="编程语言"
            style="min-width: 240px"
          >
            <a-select
              v-model="form.language"
              :style="{ width: '320px' }"
              placeholder="选择编程语言"
              @change="handleLanguageChange"
            >
              <a-option value="java">Java</a-option>
              <a-option value="c">C</a-option>
              <a-option value="cpp">C++</a-option>
            </a-select>
          </a-form-item>
        </a-form>
        <CodeEditor
          ref="codeEditorRef"
          :value="form.code as string"
          :language="form.language"
          :handle-change="changeCode"
        />
        <a-divider size="0" />
        <a-button type="primary" style="min-width: 200px" @click="doSubmit">
          提交代码
        </a-button>
        <a-divider size="0" />
        <a-button @click="openAiChat"> AI 智能解析 </a-button>
      </a-col>
    </a-row>

    <a-drawer
      :visible="aiDrawerVisible"
      title="AI 智能问答"
      :width="560"
      :footer="false"
      @cancel="aiDrawerVisible = false"
    >
      <AiChatPanel
        ref="aiPanelRef"
        :question-id="question?.id"
        :height="'calc(100vh - 220px)'"
        :initial-message="'请解析这道题目，说明考点、解题思路并给出参考。'"
      />
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import message from "@arco-design/web-vue/es/message";
import CodeEditor from "@/components/CodeEditor.vue";
import MdViewer from "@/components/MdViewer.vue";
import AiChatPanel from "@/components/AiChatPanel.vue";
import {
  QuestionControllerService,
  QuestionSubmitAddRequest,
  QuestionVO,
} from "../../../generated/question";
import { useStore } from "vuex";
import { useRoute, useRouter } from "vue-router";
import ACCESS_ENUM from "@/access/accessEnum";

const store = useStore();
const router = useRouter();
const route = useRoute();
// 从路由参数取题目 id（/view/question/:id）
const questionIdParam = route.params.id as string;

const question = ref<QuestionVO>();
const aiDrawerVisible = ref(false);
const aiPanelRef = ref();

const loadData = async () => {
  const res = await QuestionControllerService.getQuestionVoByIdUsingGet(
    questionIdParam as any
  );
  if (res.code === 0) {
    question.value = res.data;
  } else {
    message.error("加载失败，" + res.message);
  }
};

const form = ref<QuestionSubmitAddRequest>({
  language: "java",
  code: "",
});

/**
 * 提交代码
 */
const doSubmit = async () => {
  if (!question.value?.id) {
    return;
  }

  // 提交代码需要登录，未登录先引导登录
  const userRole = store.state.user.loginUser?.userRole;
  if (!userRole || userRole === ACCESS_ENUM.NOT_LOGIN) {
    message.info("请先登录后再提交");
    await router.push({
      path: "/user/login",
      query: { redirect: `/view/question/${question.value.id}` },
    });
    return;
  }

  const res = await QuestionControllerService.doQuestionSubmitUsingPost({
    ...form.value,
    questionId: question.value.id,
  });
  if (res.code === 0) {
    message.success("提交成功");
  } else {
    message.error("提交失败," + res.message);
  }
};

/**
 * 打开 AI 侧栏；首次打开由共享组件自动发一条预填解析请求
 */
const openAiChat = () => {
  if (!question.value?.id) {
    message.warning("题目未就绪");
    return;
  }
  aiDrawerVisible.value = true;
  nextTick(() => aiPanelRef.value?.start());
};

/**
 * 页面加载时，请求数据
 */
onMounted(() => {
  loadData();
});

const changeCode = (value: string) => {
  form.value.code = value;
};

// 语言下拉选项对应的固定代码模板：值必须是沙箱 LanguageEnum 认的 c / cpp（不能写 c++）
const DEFAULT_CODE_TEMPLATES: Record<string, string> = {
  c:
    "#include <stdio.h>\n" +
    "int main() {\n" +
    "    // 在此编写你的代码\n" +
    "    return 0;\n" +
    "}\n",
  cpp:
    "#include <iostream>\n" +
    "using namespace std;\n" +
    "int main() {\n" +
    "    // 在此编写你的代码\n" +
    "    return 0;\n" +
    "}\n",
};

const codeEditorRef = ref();

const handleLanguageChange = (lang: string) => {
  const tpl = DEFAULT_CODE_TEMPLATES[lang];
  if (tpl !== undefined) {
    form.value.code = tpl; // 同步给提交体
    codeEditorRef.value?.setEditorValue(tpl); // 覆盖 Monaco 编辑内容
  }
};
</script>

<style>
#viewQuestionView {
  max-width: 1400px;
  margin: 0 auto;
}

#viewQuestionView .arco-space-horizontal .arco-space-item {
  margin-bottom: 0 !important;
}
</style>
