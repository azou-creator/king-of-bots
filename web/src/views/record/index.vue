<template>
  <content-field>
    <div class="page-head">
      <h2 class="page-title">对局记录</h2>
      <p class="page-sub">历史对战一览，点击「观看回放」复盘每一场对局</p>
    </div>
    <a-table
      :columns="columns"
      :data-source="listData"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userA'">
          <div class="player-cell">
            <a-avatar :src="record.userA.avatar" :size="26" />
            <span class="player-name">{{ record.userA.username }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'userB'">
          <div class="player-cell">
            <a-avatar :src="record.userB.avatar" :size="26" />
            <span class="player-name">{{ record.userB.username }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'loser'">
          <!-- 胜方阵营着色:A 为蓝方,B 为红方 -->
          <a-tag :color="record.loser === 'A' ? 'red' : 'blue'">
            {{ record.loser === "A" ? "B 胜" : "A 胜" }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleWatchVideotape(record.id)">
            观看回放
          </a-button>
        </template>
      </template>
    </a-table>
  </content-field>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { getRecordList } from "@/api/record";
import ContentField from "@/components/ContentField.vue";
import { useRouter } from "vue-router";
import { useStore } from "vuex";

const store = useStore();
const router = useRouter();

const current = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const pagination = computed(() => {
  return {
    current: current.value,
    pageSize: pageSize.value,
    total: total.value,
    onChange: (page) => {
      current.value = page;
      getRecordListData({ page, limit: pageSize.value });
    },
  };
});

const listData = ref([]);

const columns = ref([
  {
    title: "玩家A",
    dataIndex: "userA",
    key: "userA",
  },
  {
    title: "玩家B",
    dataIndex: "userB",
    key: "userB",
  },
  {
    title: "对局结果",
    dataIndex: "loser",
    key: "loser",
  },
  {
    title: "对战时间",
    dataIndex: "createTime",
    key: "createTime",
  },
  {
    title: "操作",
    key: "action",
  },
]);

function handleWatchVideotape(recordId) {
  store.commit("updateIsRecording", true);
  for (let record of listData.value) {
    if (record.id === recordId) {
      let game = {
        map: stringTo2D(record.map),
        a_id: record.aId,
        b_id: record.bId,
        a_sx: record.aSx,
        a_sy: record.aSy,
        b_sx: record.bSx,
        b_sy: record.bSy,
      }
      store.commit("updateGame", game);
      store.commit("updateRecordLoser", record.loser);
      let steps = {
        a_steps: record.aSteps,
        b_steps: record.bSteps,
      }
      store.commit("updateSteps", steps);
      localStorage.setItem('game', JSON.stringify(game))
      localStorage.setItem('recordLoser', JSON.stringify(record.loser))
      localStorage.setItem('steps', JSON.stringify(steps))

      break;
    }
  }

  router.push({
    name: "videotape",
    params: {
      recordId,
    },
  });
}

function stringTo2D(str) {
  let g = [];
  for (let i = 0, k = 0; i < 13; i++) {
    let line = [];
    for (let j = 0; j < 14; j++, k++) {
      if (str[k] === "0") line.push(0);
      else line.push(1);
    }
    g.push(line);
  }
  return g;
}

async function getRecordListData({ page, limit }) {
  loading.value = true;
  try {
    const { data } = await getRecordList({ page, limit });
    const { list, currPage, pageSize, totalPage, totalCount } = data;
    listData.value = list;
    total.value = totalCount;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  getRecordListData({ page: current.value, limit: pageSize.value });
});
</script>

<style lang="scss" scoped>
.player-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.player-name {
  color: var(--kob-text);
}
</style>
