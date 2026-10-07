<template>
  <div class="replay-page">
    <div class="replay-bar">
      <a-button size="small" class="back-btn" @click="goBack">
        <svg class="back-icon" viewBox="0 0 12 12" aria-hidden="true">
          <path d="M7.5 2.5 4 6l3.5 3.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        返回对局列表
      </a-button>
      <span class="replay-title">对局回放</span>
    </div>
    <!-- 数据按 recordId 加载完成后才渲染地图, 避免读到上一局的残留轨迹 -->
    <play-ground v-if="loaded" />
    <div class="replay-loading" v-else>
      <a-spin size="large" />
    </div>
  </div>
</template>

<script setup>
import PlayGround from "@/components/PlayGround.vue";
import { ref, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import { message } from "ant-design-vue";
import { getRecord } from "@/api/record";

const route = useRoute();
const router = useRouter();
const store = useStore();
const loaded = ref(false);

const goBack = () => router.push("/record");

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

onMounted(async () => {
  const recordId = route.params.recordId;
  try {
    const { data: record } = await getRecord(recordId);
    if (!record) {
      message.error("对局记录不存在");
      goBack();
      return;
    }
    const game = {
      map: stringTo2D(record.map),
      a_id: record.aId,
      b_id: record.bId,
      a_sx: record.aSx,
      a_sy: record.aSy,
      b_sx: record.bSx,
      b_sy: record.bSy,
    };
    store.commit("updateGame", game);
    store.commit("updateRecordLoser", record.loser);
    store.commit("updateSteps", {
      a_steps: record.aSteps,
      b_steps: record.bSteps,
    });
    store.commit("updateIsRecording", true);
    // 数据全部就位后再挂载 GameMap, 保证回放只读取本局数据
    loaded.value = true;
  } catch (e) {
    message.error("对局记录加载失败");
    goBack();
  }
});

onUnmounted(() => {
  store.commit("updateIsRecording", false);
});
</script>

<style lang="scss" scoped>
.replay-bar {
  width: min(720px, 92vw);
  margin: 20px auto 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 999px;
}

.back-icon {
  width: 11px;
  height: 11px;
}

.replay-title {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 600;
  font-size: 0.95rem;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.replay-loading {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}
</style>
