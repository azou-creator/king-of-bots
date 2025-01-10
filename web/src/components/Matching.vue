<template>
  <div class="matchground">
    <a-row :wrap="true" align="middle" justify="center" style="height: 100%">
      <a-col class="flex-center" :span="8">
        <a-avatar
          :src="$store.state.user.avatar"
          :size="{ xs: 124, sm: 132, md: 140, lg: 164, xl: 180, xxl: 200 }"
          alt="头像"
        />
        <div class="user-username">
          {{ $store.state.user.username }}
        </div>
      </a-col>
      <a-col class="select_container" :span="8">
        <a-select v-model:value="select_bot" :options="options" />
      </a-col>
      <a-col class="flex-center" :span="8">
        <a-avatar
          :src="$store.state.pk.opponent_photo"
          :size="{ xs: 124, sm: 132, md: 140, lg: 164, xl: 180, xxl: 200 }"
          alt="头像"
        />
        <div class="user-username">
          {{ $store.state.pk.opponent_username }}
        </div>
      </a-col>
      <a-col class="flex-center" :span="12">
        <a-button
          @click="click_match_btn"
          type="primary"
          class="btn btn-warning btn-lg"
        >
          {{ match_btn_info }}
        </a-button>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useStore } from "vuex";
import { getBotList } from "@/api/bot";
import { START_MATCH, STOP_MATCH, MATCH_SUCCESS } from "@/utils/constant";

const store = useStore();
const match_btn_info = ref("开始匹配");
const select_bot = ref("-1");
const options = ref([
  {
    value: "-1",
    label: "亲自出战",
  },
]);

const click_match_btn = () => {
  if (match_btn_info.value === "开始匹配") {
    match_btn_info.value = "取消";
    store.state.pk.socket.send(
      JSON.stringify({
        event: START_MATCH,
        botId: select_bot.value,
      })
    );
  } else {
    match_btn_info.value = "开始匹配";
    store.state.pk.socket.send(
      JSON.stringify({
        event: STOP_MATCH,
      })
    );
  }
};

const refresh_bots = async () => {
  const { data } = await getBotList({ page: 0, limit: 10 });
  const { content } = data;
  const t = content.map((bot) => {
    return {
      value: bot.id,
      label: bot.title,
    };
  });
  t.forEach((e) => {
    options.value.push(e);
  });
};

onMounted(() => {
  refresh_bots(); // 从云端动态获取bots
});
</script>

<style lang="scss" scoped>
.matchground {
  width: 60vw;
  height: 70vh;
  margin: 40px auto;
  background-color: rgba(50, 50, 50, 0.5);
}
.user-username {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: white;
  padding-top: 2vh;
}

.select_container {
  display: flex;
  align-items: center;
  justify-content: center;

  .ant-select {
    width: 200px;
  }
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}
</style>
