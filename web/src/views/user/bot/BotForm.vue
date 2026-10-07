<template>
  <a-form
    :model="bot"
    :label-col="{ span: 3 }"
    :wrapper-col="{ span: 21 }"
    autocomplete="off"
  >
    <a-form-item
      label="名称"
      name="title"
      :rules="[{ required: true, message: '请输入 Bot 名称' }]"
    >
      <a-input v-model:value="title" placeholder="给 Bot 起个名字" />
    </a-form-item>
    <a-form-item label="描述" name="description">
      <a-input v-model:value="description" placeholder="简单介绍一下它的策略（可选）" />
    </a-form-item>
    <a-form-item
      label="代码"
      name="content"
      :rules="[{ required: true, message: '请输入 Bot 代码' }]"
    >
      <VAceEditor
        v-model:value="content"
        @init="editorInit"
        lang="java"
        theme="textmate"
        style="height: 340px"
        :options="editorOptions"
      />
      <div class="code-hint">
        实现 <code>Integer nextMove(String input)</code> 方法，返回 0/1/2/3
        分别表示 上 / 右 / 下 / 左
      </div>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { VAceEditor } from "vue3-ace-editor";
import { reactive, ref, computed } from "vue";
import ace from "ace-builds";

ace.config.set(
  "basePath",
  "https://cdn.jsdelivr.net/npm/ace-builds@" + ace.version + "/src-noconflict/"
);

const props = defineProps({
  value: {
    type: Object,
    default: () => {
      return {};
    },
  },
});

const emit = defineEmits(["update:value"]);
const bot = reactive(props.value);

const editorOptions = reactive({
  fontSize: 13,
  tabSize: 4,
  showPrintMargin: false,
});

const title = computed({
  get() {
    return bot.title;
  },
  set(value) {
    bot.title = value;
    emit("update:value", bot);
  },
});

const description = computed({
  get() {
    return bot.description;
  },
  set(value) {
    bot.description = value;
    emit("update:value", bot);
  },
});

const content = computed({
  get() {
    return bot.content;
  },
  set(value) {
    bot.content = value;
    emit("update:value", bot);
  },
});

const editorInit = reactive({});
</script>

<style lang="scss" scoped>
.code-hint {
  margin-top: 8px;
  font-size: 0.82rem;
  color: var(--kob-text-secondary);

  code {
    padding: 1px 5px;
    border-radius: 4px;
    background: rgba(31, 45, 39, 0.06);
    font-size: 0.8rem;
  }
}
</style>
