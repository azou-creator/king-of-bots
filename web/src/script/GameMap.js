import GameObject from "./GameObject.js";
import Snake from "./Snake.js";
import Wall from "./Wall.js";
import { store } from "@/store";
import { useRouter } from "vue-router";

// 游戏地图对象
export default class GameMap extends GameObject {
  constructor(ctx, parent) {
    super();
    this.ctx = ctx;
    this.parent = parent;

    this.L = 0;
    this.rows = 13;
    this.cols = 14;

    this.walls = [];
    this.inner_wall_counts = 20;

    this.snakes = [
      new Snake(
        { id: 0, color: "rgb(77, 114, 230)", r: this.rows - 2, c: 1 },
        this
      ),
      new Snake(
        { id: 1, color: "rgb(234, 53, 73)", r: 1, c: this.cols - 2 },
        this
      ),
    ];
    this.add_event_listener();
  }

  add_event_listener() {
    // 判断是否是录像
    const isRecording = () => {
      const router = useRouter();
      return (
        router.currentRoute.value.path.includes("videotape") ||
        store.state.record.is_recording
      );
    };
    if (isRecording()) {
      const [snake0, snake1] = this.snakes;
      
      const a_steps = store.state.record.a_steps || JSON.parse(localStorage.getItem('steps')).a_steps;
      const b_steps = store.state.record.b_steps || JSON.parse(localStorage.getItem('steps')).b_steps;
      
      const loser = (store.state.record.record_loser === 'none' ? false : store.state.record.record_loser ) || JSON.parse(localStorage.getItem('recordLoser'));
      
      let k = 0;
      const timer = setInterval(() => {
        if (k >= a_steps.length - 1) {
          if (loser == "all" || loser == "A") {
            snake0.status = "dead";
          } else if (loser == "all" || loser == "B") {
            snake1.status = "dead";
          }
          clearInterval(timer);
        } else {
          snake0.set_direction(parseInt(a_steps[k]));
          snake1.set_direction(parseInt(b_steps[k]));
        }
        k++;
      }, 300);
    } else {
      this.ctx.canvas.focus();
      this.ctx.canvas.addEventListener("keydown", (e) => {
        let d = -1;
        if (e.key == "w") {
          d = 0;
        } else if (e.key == "d") {
          d = 1;
        } else if (e.key == "s") {
          d = 2;
        } else if (e.key == "a") {
          d = 3;
        } else if (e.key == "ArrowUp") {
          d = 0;
        } else if (e.key == "ArrowRight") {
          d = 1;
        } else if (e.key == "ArrowDown") {
          d = 2;
        } else if (e.key == "ArrowLeft") {
          d = 3;
        }
        if (d > -1) {
          store.state.pk.socket.send(
            JSON.stringify({
              event: "move",
              direction: d,
            })
          );
        }
      });
    }
  }

  start() {
    this.create_wall();
  }

  check_ready() {
    for (const snake of this.snakes) {
      if (snake.status !== "idle") return false;
      if (snake.direction === -1) return false;
    }
    return true;
  }

  check_valid(cell) {
    for (const wall of this.walls) {
      if (wall.r == cell.r && wall.c == cell.c) {
        return false;
      }
    }
    for (const snake of this.snakes) {
      let k = snake.cells.length;
      if (!snake.check_tail_increasing()) {
        k--;
      }
      for (let i = 0; i < k; i++)
        if (snake.cells[i].r == cell.r && snake.cells[i].c == cell.c) {
          return false;
        }
    }
    return true;
  }

  update_size() {
    this.L = Math.min(
      this.parent.clientWidth / this.cols,
      this.parent.clientHeight / this.rows
    );
    this.ctx.canvas.width = this.L * this.cols;
    this.ctx.canvas.height = this.L * this.rows;
  }

  next_step() {
    for (const snake of this.snakes) {
      snake.next_step();
    }
  }

  update() {
    this.update_size();
    if (this.check_ready()) {
      this.next_step();
    }
    this.render();
  }

  render() {
    // 渲染网格矩形地图
    const even_color = "rgb(171, 214, 83)";
    const odd_color = "rgb(173, 228, 87)";

    for (let i = 0; i < this.rows; i++) {
      for (let j = 0; j < this.cols; j++) {
        this.ctx.fillStyle = (i + j) % 2 == 0 ? even_color : odd_color;
        this.ctx.fillRect(j * this.L, i * this.L, this.L, this.L);
      }
    }
  }

  create_wall() {
    let g = store.state.pk.map;
    if (!g)
      store.commit("updateGame", JSON.parse(localStorage.getItem("game")));
    g = store.state.pk.map;
    if (g[11][1] == 1) g[11][1] = 0;
    for (let i = 0; i < this.rows; i++)
      for (let j = 0; j < this.cols; j++)
        if (g[i][j]) this.walls.push(new Wall(i, j, this));
  }
}
