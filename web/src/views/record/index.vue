<template>
  <content-field>
    <a-table
      :columns="columns"
      :data-source="listData"
      :pagination="pagination"
      :loading="loading"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userA'">
          <a-typography-text style="margin-right: 12px">
            {{ record.userA.username }}
          </a-typography-text>
          <a-avatar :src="record.userA.avatar" />
        </template>
        <template v-else-if="column.key === 'userB'">
          <a-typography-text style="margin-right: 12px">
            {{ record.userB.username }}
          </a-typography-text>
          <a-avatar :src="record.userB.avatar" />
        </template>
        <template v-else-if="column.key === 'loser'">
          <span>
            <a-tag color="green">
              {{ record.loser === "A" ? "B胜" : "A胜" }}
            </a-tag>
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <span>
            <a @click="handleWatchVideotape(record.id)">查看录像</a>
          </span>
        </template>
      </template>
    </a-table>
  </content-field>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from "vue";
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
    title: "action",
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

<style scoped></style>
