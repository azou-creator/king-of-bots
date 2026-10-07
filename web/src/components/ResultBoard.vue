<template>
  <div class="result-mask">
    <div class="result-card" :class="tone">
      <div class="result-title">{{ result }}</div>
      <div class="result-sub">{{ subtitle }}</div>
      <a-button
        class="result-btn"
        block
        type="primary"
        size="large"
        @click="rematch"
        >再来一局</a-button
      >
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useStore } from "vuex";

const store = useStore();

const isDraw = computed(() => store.state.pk.loser === "all");
const iLose = computed(() => {
  const { loser } = store.state.pk;
  return (
    !isDraw.value &&
    ((loser === "A" && store.state.user.id == store.state.pk.a_id) ||
      (loser === "B" && store.state.user.id == store.state.pk.b_id))
  );
});

const result = computed(() => {
  if (isDraw.value) return "平局";
  return iLose.value ? "你输了" : "你获胜了";
});

const tone = computed(() => {
  if (isDraw.value) return "draw";
  return iLose.value ? "lose" : "win";
});

const subtitle = computed(() => {
  if (isDraw.value) return "不分伯仲，再战一回？";
  return iLose.value ? "胜败乃兵家常事，再接再厉" : "恭喜！这一局你更胜一筹";
});

function rematch() {
  store.commit("updateStatus", "matching");
  store.commit("updateLoser", "none");
  store.commit("updateOpponent", {
    username: "我的对手",
    photo:
      "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
  });
}
</script>

<style lang="scss" scoped>
.result-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(8, 15, 12, 0.45);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  animation: fade-in 0.25s ease;
}

.result-card {
  width: min(380px, 88vw);
  background: var(--kob-panel);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 20px;
  box-shadow: var(--kob-shadow);
  padding: 38px 32px 30px;
  text-align: center;
  animation: pop-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.result-title {
  font-size: 2.2rem;
  font-weight: 800;
  letter-spacing: 1px;
}

.result-card.win .result-title {
  color: var(--kob-accent-strong);
}
.result-card.lose .result-title {
  color: var(--kob-b);
}
.result-card.draw .result-title {
  color: var(--kob-text-secondary);
}

.result-sub {
  margin: 10px 0 26px;
  color: var(--kob-text-secondary);
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes pop-in {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
