<template>
  <div ref="parent" class="gameMap">
    <canvas ref="canvas" tabindex="0"></canvas>
  </div>
</template>

<script setup>
import GameMap from "@/script/GameMap.js";
import { ref, onMounted, onUnmounted } from "vue";
import { useStore } from "vuex";

const canvas = ref(null);
const parent = ref(null);
const store = useStore();
let gameMap = null;

onMounted(() => {
  gameMap = new GameMap(canvas.value.getContext("2d"), parent.value);
  store.commit("updateGameObject", gameMap);
});

onUnmounted(() => {
  // 从全局渲染池移除并触发 onDestory(清理回放定时器), 防止旧实例残留
  if (gameMap) {
    gameMap.destroy();
    gameMap = null;
  }
  store.commit("updateGameObject", null);
});
</script>

<style lang="scss" scoped>
/* 地图为 13 行 x 14 列,用 aspect-ratio 让画布随宽度等比缩放 */
.gameMap {
  width: 100%;
  aspect-ratio: 14 / 13;
  display: flex;
  align-items: center;
  justify-content: center;

  canvas {
    border-radius: 8px;
    outline: none;
  }
}
</style>
