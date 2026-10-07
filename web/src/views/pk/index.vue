<template>
  <div class="pk-page">
    <play-ground v-if="$store.state.pk.status === 'playing'" />
    <matching v-else />
    <result-board v-if="$store.state.pk.loser != 'none'" />

    <!-- 对局中的阵营提示 + 操作说明,顶部居中悬浮 -->
    <div class="pk-turn-chip" v-if="$store.state.pk.status === 'playing'">
      <span class="dot" :class="isBlue ? 'blue' : 'red'"></span>
      <span>{{ isBlue ? "你是蓝方" : "你是红方" }}</span>
      <span class="divider"></span>
      <span class="hint">WASD / 方向键 控制移动</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from "vue";
import { useStore } from "vuex";
import ResultBoard from "@/components/ResultBoard.vue";
import PlayGround from "@/components/PlayGround.vue";
import Matching from "@/components/Matching.vue";
import {
  START_MATCH,
  STOP_MATCH,
  MATCH_SUCCESS,
  MOVE,
  RESULT,
} from "@/utils/constant";

const store = useStore();
const socketUrl = `ws://localhost:8080/websocket/${store.state.user.token}`;
let socket = null;

const isBlue = computed(
  () => parseInt(store.state.user.id) === store.state.pk.a_id
);

store.commit("updateLoser", "none");

onMounted(() => {
  store.commit("updateOpponent", {
    username: "我的对手",
    photo:
      "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
  });
  socket = new WebSocket(socketUrl);
  // 提前挂到 store，Matching 组件通过 readyState 判断是否可发送
  store.commit("updateSocket", socket);
  socket.onopen = () => {
    console.log("websocket open");
  };

  socket.onmessage = (e) => {
    const { data, event, game, a_dir, b_dir, loser } = JSON.parse(e.data);
    console.log(event, a_dir, b_dir, loser);
    if (event === MATCH_SUCCESS) {
      store.commit("updateOpponent", {
        username: data.username,
        photo: data.avatar,
      });
      store.commit("updateStatus", "playing");
      store.commit("updateGame", game);
    } else if (event === MOVE) {
      const gameObject = store.state.pk.gameObject;
      const [snake0, snake1] = gameObject.snakes;
      snake0.set_direction(a_dir);
      snake1.set_direction(b_dir);
    } else if (event === RESULT) {
      const [snake0, snake1] = store.state.pk.gameObject.snakes;
      if (loser === "all" || loser === "A") {
        snake0.set_status("dead");
      } else if (loser === "all" || loser === "B") {
        snake1.set_status("dead");
      }
      store.commit("updateLoser", loser);
    }
  };

  socket.onclose = () => {
    console.log("websocket close");
    store.commit("updateSocket", null);
    store.commit("updateStatus", "matching");
  };
});

onUnmounted(() => {
  if (socket) {
    socket.onclose = null; // 卸载触发的 close 不再重置 store 状态
    socket.close();
  }
  store.commit("updateSocket", null);
});
</script>

<style lang="scss" scoped>
.pk-turn-chip {
  position: fixed;
  top: 72px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 16px;
  border-radius: 999px;
  background: var(--kob-glass-dark);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.9rem;
  white-space: nowrap;
  box-shadow: var(--kob-shadow-sm);

  .dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    flex-shrink: 0;

    &.blue {
      background: var(--kob-a);
      box-shadow: 0 0 6px rgba(77, 114, 230, 0.9);
    }

    &.red {
      background: var(--kob-b);
      box-shadow: 0 0 6px rgba(234, 53, 73, 0.9);
    }
  }

  .divider {
    width: 1px;
    height: 12px;
    background: rgba(255, 255, 255, 0.18);
  }

  .hint {
    color: var(--kob-text-dim);
    font-size: 0.82rem;
  }
}

@media (max-width: 560px) {
  .pk-turn-chip .hint,
  .pk-turn-chip .divider {
    display: none;
  }
}
</style>
