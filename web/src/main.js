import { createApp } from "vue";
import "ant-design-vue/dist/antd.css";
import "./style.scss";
import App from "./App.vue";
import router from "@/router/routes.js";
import { store } from "./store";

const app = createApp(App);

app.use(router);

app.use(store);

app.mount("#app");
