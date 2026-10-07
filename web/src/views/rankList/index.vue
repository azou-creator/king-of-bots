<template>
  <content-field>
    <div class="page-head">
      <h2 class="page-title">排行榜</h2>
      <p class="page-sub">全服玩家的天梯积分排名</p>
    </div>
    <a-list :data-source="data" :pagination="pagination" :loading="loading">
      <template #renderItem="{ index, item }">
        <a-list-item :key="item.id" class="rank-item">
          <div class="rank-left">
            <span class="rank-badge kob-num" :class="topClass(index)">{{
              index + 1
            }}</span>
            <a-avatar :src="item.avatar" :size="42" class="rank-avatar" />
            <div class="rank-info">
              <div class="rank-name">{{ item.username }}</div>
              <div class="rank-desc">
                {{ item.description || "这位玩家很神秘，什么都没留下" }}
              </div>
            </div>
          </div>
          <div class="rank-rating kob-num">
            {{ item.rating }}
            <span class="rating-unit">分</span>
          </div>
        </a-list-item>
      </template>
    </a-list>
  </content-field>
</template>

<script setup>
import ContentField from "@/components/ContentField.vue";
import { computed, onMounted, ref } from "vue";
import { getRankList } from "@/api/rankList";

const data = ref([]);
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
      fetchData({ page, limit: pageSize.value });
    },
  };
});

// 前三名使用金银铜徽章
const topClass = (index) => {
  return ["gold", "silver", "bronze"][index] || "";
};

const fetchData = async ({ page, limit }) => {
  try {
    loading.value = true;
    const { data: pageList } = await getRankList({ page, limit });
    data.value = pageList.list;
  } catch (e) {
    console.log(e);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchData({ page: current.value, limit: pageSize.value });
});
</script>

<style lang="scss" scoped>
.rank-item {
  padding: 14px 6px;
}

.rank-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.rank-badge {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--kob-text-secondary);
  border: 1px solid var(--kob-border);
  background: rgba(31, 45, 39, 0.04);

  &.gold {
    background: linear-gradient(160deg, #ffd479, #f0a418);
    border: none;
    color: #6b4a00;
    box-shadow: 0 6px 14px -6px rgba(240, 164, 24, 0.65);
  }

  &.silver {
    background: linear-gradient(160deg, #e8edf4, #b9c3d0);
    border: none;
    color: #4d5a6b;
    box-shadow: 0 6px 14px -6px rgba(154, 168, 186, 0.65);
  }

  &.bronze {
    background: linear-gradient(160deg, #eec6a7, #cd8f5c);
    border: none;
    color: #5d3a17;
    box-shadow: 0 6px 14px -6px rgba(205, 143, 92, 0.65);
  }
}

.rank-avatar {
  flex-shrink: 0;
}

.rank-info {
  min-width: 0;
}

.rank-name {
  font-weight: 600;
  color: var(--kob-text);
  line-height: 1.35;
}

.rank-desc {
  font-size: 0.85rem;
  color: var(--kob-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 46vw;
}

.rank-rating {
  flex-shrink: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--kob-text);
}

.rating-unit {
  font-size: 0.78rem;
  font-weight: 400;
  color: var(--kob-text-secondary);
  margin-left: 2px;
}
</style>
