<template>
  <div class="matchground">
    <div class="match-panel">
      <div class="match-players">
        <div class="player">
          <a-avatar
            :src="$store.state.user.avatar"
            :size="88"
            alt="我的头像"
          />
          <div class="player-name" :title="$store.state.user.username">
            {{ $store.state.user.username }}
          </div>
        </div>

        <div class="vs-badge" aria-hidden="true">VS</div>

        <div class="player">
          <a-avatar
            :src="$store.state.pk.opponent_photo"
            :size="88"
            alt="对手头像"
          />
          <div class="player-name" :title="$store.state.pk.opponent_username">
            {{ $store.state.pk.opponent_username }}
          </div>
        </div>
      </div>

      <div class="match-config">
        <div class="config-label">出战方式</div>
        <a-select
          v-model:value="select_bot"
          :options="options"
          class="config-select"
        />
      </div>

      <div class="match-action">
        <a-button
          type="primary"
          size="large"
          class="match-btn"
          :class="{ matching: isMatching }"
          @click="click_match_btn"
        >
          {{ match_btn_info }}
        </a-button>
        <div class="error-message" v-if="errorMessage">{{ errorMessage }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { useStore } from "vuex";
import { getBotList } from "@/api/bot";
import { START_MATCH, STOP_MATCH, MATCH_SUCCESS } from "@/utils/constant";

const store = useStore();
const isMatching = ref(false);
const match_btn_info = computed(() =>
  isMatching.value ? "取消匹配" : "开始匹配"
);
const errorMessage = ref("");
const select_bot = ref("-1");
const options = ref([
  {
    value: "-1",
    label: "亲自出战",
  },
]);

const click_match_btn = () => {
  const socket = store.state.pk.socket;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    errorMessage.value = "连接未就绪，请稍候重试或刷新页面";
    return;
  }
  errorMessage.value = "";
  if (!isMatching.value) {
    isMatching.value = true;
    socket.send(
      JSON.stringify({
        event: START_MATCH,
        botId: select_bot.value,
      })
    );
  } else {
    isMatching.value = false;
    socket.send(
      JSON.stringify({
        event: STOP_MATCH,
      })
    );
  }
};

const refresh_bots = async () => {
  try {
    // /bot/list 返回 PageUtils: { list, totalCount, ... }
    const { data } = await getBotList({ page: 0, limit: 10 });
    (data.list || []).forEach((bot) => {
      options.value.push({
        value: bot.id,
        label: bot.title,
      });
    });
  } catch (e) {
    // Bot 列表拉取失败不阻塞匹配,保持只有「亲自出战」
  }
};

onMounted(() => {
  refresh_bots(); // 从云端动态获取bots
});
</script>

<style lang="scss" scoped>
.matchground {
  width: min(720px, 92vw);
  margin: 26px auto 32px;
}

.match-panel {
  background: var(--kob-glass-dark);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--kob-radius-lg);
  box-shadow: var(--kob-shadow);
  padding: 44px 32px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.match-players {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(20px, 7vw, 60px);
  width: 100%;
}

.player {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.player-name {
  color: #fff;
  font-weight: 600;
  font-size: 1.05rem;
  max-width: 10em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vs-badge {
  flex-shrink: 0;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-style: italic;
  letter-spacing: 1px;
  /* 渐变取自双方阵营色,蓝对红 */
  background: linear-gradient(135deg, var(--kob-a) 0%, var(--kob-b) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28),
    0 12px 24px -12px rgba(0, 0, 0, 0.55);
}

.match-config {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.config-label {
  color: var(--kob-text-dim);
  font-size: 0.9rem;
}

.config-select {
  width: 240px;
}

.match-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.match-btn {
  width: 240px;
  height: 46px;
  font-size: 1.05rem;
  font-weight: 600;
  border-radius: 999px;
}

/* 匹配中的呼吸脉冲,提示当前正在搜索对手 */
.match-btn.matching {
  animation: pulse 1.6s ease-out infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 var(--kob-accent-soft);
  }
  70% {
    box-shadow: 0 0 0 16px rgba(34, 181, 115, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 181, 115, 0);
  }
}

.error-message {
  color: #ff7875;
  font-size: 0.9rem;
}
</style>
