import GameObject from "./GameObject";

/**
 * 地图墙体对象
 */
export default class Wall extends GameObject {
  constructor(r, c, gameMap) {
    super();
    this.r = r;
    this.c = c;
    this.gameMap = gameMap;
  }

  start() {}

  update() {
    this.render();
  }

  render() {
    const L = this.gameMap.L;
    const ctx = this.gameMap.ctx;
    const pad = L * 0.05;
    const x = this.c * L + pad;
    const y = this.r * L + pad;
    const size = L - pad * 2;
    const radius = L * 0.22;

    // 落影让墙体浮出棋盘
    ctx.fillStyle = "rgba(20, 40, 20, 0.22)";
    this.roundRect(ctx, x, y + L * 0.07, size, size, radius);
    ctx.fill();

    // 墙体主体:上亮下暗的纵向渐变
    const gradient = ctx.createLinearGradient(x, y, x, y + size);
    gradient.addColorStop(0, "#8a5a4a");
    gradient.addColorStop(1, "#6f463a");
    ctx.fillStyle = gradient;
    this.roundRect(ctx, x, y, size, size, radius);
    ctx.fill();

    // 顶部一道细高光,增强体积感
    ctx.fillStyle = "rgba(255, 255, 255, 0.14)";
    this.roundRect(ctx, x + size * 0.14, y + size * 0.1, size * 0.72, size * 0.16, radius * 0.5);
    ctx.fill();
  }

  // 兼容不支持 ctx.roundRect 的浏览器
  roundRect(ctx, x, y, w, h, r) {
    r = Math.min(r, w / 2, h / 2);
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }
}
