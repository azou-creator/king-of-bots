<template>
  <content-field>
    <div class="page-head">
      <h2 class="page-title">我的 Bots</h2>
      <p class="page-sub">管理你的 AI 战斗程序，匹配时可选择由 Bot 代为出战</p>
    </div>

    <a-row :gutter="28">
      <a-col :xs="24" :md="7">
        <div class="profile">
          <a-avatar :src="$store.state.user.avatar" :size="92" alt="头像" />
          <div class="profile-name">{{ $store.state.user.username }}</div>
          <div class="profile-sub">共 {{ pagination.totalCount }} 个 Bot</div>
        </div>
      </a-col>

      <a-col :xs="24" :md="17">
        <div class="bots-toolbar">
          <a-button type="primary" @click="openCreate">+ 创建 Bot</a-button>
        </div>
        <a-table
          :data-source="botList"
          :columns="columns"
          :loading="loading"
          :pagination="tablePagination"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <span class="bot-title">{{ record.title }}</span>
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="bot-desc">{{ record.description || "暂无描述" }}</span>
            </template>
            <template v-else-if="column.key === 'rating'">
              <span class="kob-num bot-rating">{{ record.rating }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span>
                <a @click="handleEditClick(record)">编辑</a>
                <a-divider type="vertical" />
                <a-popconfirm
                  title="删除后无法恢复，确定删除该 Bot 吗？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="handleRemoveBot(record.id)"
                >
                  <a class="danger-link">删除</a>
                </a-popconfirm>
              </span>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="还没有 Bot，点击右上角「创建 Bot」添加第一个" />
          </template>
        </a-table>
      </a-col>
    </a-row>

    <a-modal
      v-model:visible="visible"
      :title="bot.id ? '编辑 Bot' : '创建 Bot'"
      :confirm-loading="confirmLoading"
      :ok-text="bot.id ? '保存' : '创建'"
      cancel-text="取消"
      @ok="handleOk"
      width="800px"
      :afterClose="afterClose"
    >
      <bot-form v-model:value="bot" />
    </a-modal>
  </content-field>
</template>

<script setup>
import { onMounted, ref, reactive, computed } from "vue";
import { getBotList, addBot, removeBot, updateBot } from "@/api/bot";
import BotForm from "./BotForm.vue";
import _ from "lodash";
import { message } from "ant-design-vue";
import ContentField from "@/components/ContentField.vue";

const visible = ref(false);
const loading = ref(false);
const botList = ref([]);
const columns = ref([
  {
    title: "名称",
    dataIndex: "title",
    key: "title",
  },
  {
    title: "描述",
    dataIndex: "description",
    key: "description",
  },
  {
    title: "评分",
    dataIndex: "rating",
    key: "rating",
  },
  {
    title: "创建时间",
    dataIndex: "createTime",
    key: "createTime",
  },
  {
    title: "操作",
    key: "action",
  },
]);

// 后端 PageRequest 的 page 从 0 开始,antd 分页 current 从 1 开始
const pagination = reactive({
  page: 0,
  limit: 10,
  total: 0,
  totalCount: 0,
});

const tablePagination = computed(() => ({
  current: pagination.page + 1,
  pageSize: pagination.limit,
  total: pagination.totalCount,
  onChange: (current) => {
    pagination.page = current - 1;
    fetchBotList();
  },
}));

const confirmLoading = ref(false);
function afterClose() {
  bot.id = null;
  bot.title = "";
  bot.description = "";
  bot.content = "";
}
const bot = reactive({
  id: null,
  title: "",
  description: "",
  content: "",
});

const openCreate = () => {
  afterClose();
  visible.value = true;
};

const handleEditClick = (data) => {
  _.assign(bot, data);
  visible.value = true;
};

async function handleOk() {
  if (bot.id) {
    try {
      confirmLoading.value = true;
      const { status, message: msg } = await updateBot(bot);
      if (status == 200) {
        message.success(msg || "更新成功");
        fetchBotList();
      }
      visible.value = false;
    } catch (e) {
      message.error("更新失败");
      console.log(e);
    } finally {
      confirmLoading.value = false;
    }
  } else {
    const { title, description, content } = bot;
    try {
      confirmLoading.value = true;
      const { status, message: msg } = await addBot({
        title,
        description,
        content,
      });
      if (status === 200) {
        message.success(msg || "创建成功");
        fetchBotList();
        visible.value = false;
      }
    } catch (e) {
      message.error("添加失败");
      console.log(e);
    } finally {
      confirmLoading.value = false;
    }
  }
}

async function fetchBotList() {
  loading.value = true;
  try {
    const { page, limit } = pagination;
    const { data } = await getBotList({ page, limit });
    const { list, totalPage, totalCount } = data;
    botList.value = list;
    pagination.total = totalPage;
    pagination.totalCount = totalCount;
  } catch (e) {
    console.log(e);
  } finally {
    loading.value = false;
  }
}

async function handleRemoveBot(id) {
  if (id) {
    try {
      const { status, message: msg } = await removeBot(id);
      if (status === 200) {
        message.success(msg || "删除成功");
        fetchBotList();
      }
    } catch (e) {
      message.error("删除失败");
      console.log(e);
    }
  }
}

onMounted(() => {
  fetchBotList();
});
</script>

<style lang="scss" scoped>
.profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 12px 22px;
  border-radius: var(--kob-radius);
  background: rgba(31, 45, 39, 0.035);
  border: 1px solid var(--kob-border);
}

.profile-name {
  margin-top: 14px;
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--kob-text);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-sub {
  margin-top: 4px;
  font-size: 0.85rem;
  color: var(--kob-text-secondary);
}

.bots-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.bot-title {
  font-weight: 600;
  color: var(--kob-text);
}

.bot-desc {
  color: var(--kob-text-secondary);
}

.bot-rating {
  font-weight: 700;
}

.danger-link {
  color: var(--kob-b) !important;

  &:hover {
    color: #ff6b6b !important;
  }
}

@media (max-width: 768px) {
  .profile {
    margin-bottom: 20px;
  }
  .bots-toolbar {
    justify-content: flex-start;
  }
}
</style>
