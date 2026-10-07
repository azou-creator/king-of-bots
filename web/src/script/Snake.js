import GameObject from "./GameObject";
import Cell from "./Cell";

export default class Snake extends GameObject {
  constructor(info, gameMap) {
    super();
    this.info = info;
    this.gameMap = gameMap;

    this.id = info.id;
    this.color = info.color;

    this.cells = [new Cell(info.r, info.c)];

    // 每一秒走五个格子
    this.speed = 5;

    this.direction = -1; // 0: up, 1: right, 2: down, 3: left -1:没有方向
    this.status = "idle"; // idle静止, moving移动, dead死亡

    this.next_cell = null; // 下一个要走的格子

    // 偏移量
    this.dr = [-1, 0, 1, 0];
    this.dc = [0, 1, 0, -1];

    this.step = 0; //回合数
    this.eps = 1e-2; // 误差

    // 蛇眼睛的方向
    this.eye_direction = 0;
    if (this.id == 2) {
      this.eye_direction = 2;
    }
    this.eye_dx = [
      [-1, 1],
      [1, 1],
      [1, -1],
      [-1, -1],
    ];
    this.eye_dy = [
      [-1, -1],
      [-1, 1],
      [1, 1],
      [1, -1],
    ];
  }

  strat() {}

  //  将蛇的状态变为走下一步
  next_step() {
    const d = this.direction;
    this.next_cell = new Cell(
      this.cells[0].r + this.dr[d],
      this.cells[0].c + this.dc[d]
    );
    this.direction = -1;
    this.eye_direction = d;
    this.status = "moving";
    this.step++;

    let k = this.cells.length;
    for (let i = k; i > 0; i--)
      this.cells[i] = JSON.parse(JSON.stringify(this.cells[i - 1]));
  }

  /**
   * 创建一个新的蛇头，新的蛇头的向前移动，尾巴向倒数第二个身体移动一格
   */
  move() {
    const dx = this.next_cell.x - this.cells[0].x;
    const dy = this.next_cell.y - this.cells[0].y;
    // 欧几里得距离
    const distance = Math.sqrt(dx * dx + dy * dy);
    if (distance < this.eps) {
      this.cells[0] = this.next_cell;
      this.next_cell = null;
      this.status = "idle";

      if (!this.check_tail_increasing()) {
        this.cells.pop();
      }
    } else {
      const move_distance = (this.speed * this.timedelta) / 1000;
      this.cells[0].x += (move_distance * dx) / distance;
      this.cells[0].y += (move_distance * dy) / distance;
      if (!this.check_tail_increasing()) {
        const k = this.cells.length;
        const tail = this.cells[k - 1],
          tail_target = this.cells[k - 2];
        const tail_dx = tail_target.x - tail.x;
        const tail_dy = tail_target.y - tail.y;
        tail.x += (move_distance * tail_dx) / distance;
        tail.y += (move_distance * tail_dy) / distance;
      }
    }
  }

  set_direction(d) {
    this.direction = d;
  }

  set_status(status) {
    this.status = status;
  }

  check_tail_increasing() {
    if (this.step <= 10) {
      return true;
    }
    if (this.step % 3 == 1) return true;
    return false;
  }

  update() {
    if (this.status === "moving") {
      this.move();
    }
    this.render();
  }

  render() {
    const L = this.gameMap.L;
    const ctx = this.gameMap.ctx;

    ctx.save();
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.lineWidth = L * 0.8;
    ctx.strokeStyle = this.color;
    if (this.status === "dead") {
      ctx.strokeStyle = "rgba(235, 238, 235, 0.92)";
    }

    // 身体:把所有格子中心连成一条圆头折线,转折处自然圆润
    ctx.beginPath();
    let hasSegment = false;
    ctx.moveTo(this.cells[0].x * L, this.cells[0].y * L);
    for (let i = 1; i < this.cells.length; i++) {
      const a = this.cells[i - 1],
        b = this.cells[i];
      if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps)
        continue;
      ctx.lineTo(b.x * L, b.y * L);
      hasSegment = true;
    }
    if (hasSegment) {
      ctx.stroke();
    } else {
      // 只有一颗头时的圆形
      ctx.beginPath();
      ctx.arc(this.cells[0].x * L, this.cells[0].y * L, L * 0.4, 0, 2 * Math.PI);
      ctx.fillStyle = ctx.strokeStyle;
      ctx.fill();
    }

    // 眼睛:白眼球 + 黑瞳孔,朝向随移动方向
    for (let i = 0; i < 2; i++) {
      let eye_x =
        (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.2) * L;
      let eye_y =
        (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.2) * L;
      ctx.beginPath();
      ctx.fillStyle = "#ffffff";
      ctx.arc(eye_x, eye_y, L * 0.16, 0, Math.PI * 2, false);
      ctx.fill();
      ctx.beginPath();
      ctx.fillStyle = "#142019";
      ctx.arc(eye_x, eye_y, L * 0.09, 0, Math.PI * 2, false);
      ctx.fill();
    }
    ctx.restore();
  }
}
