
const GAME_OBJECTS = []

export default class  GameObject{
  constructor(){
    GAME_OBJECTS.push(this)
    this.timedelta = 0
    this.hasStarted = false
  }

  start(){
  }

  update(){
  }

  onDestory(){
  }

  destroy(){
    this.onDestory()
    for(let i in GAME_OBJECTS){
      if(GAME_OBJECTS[i] == this){
        GAME_OBJECTS.splice(i, 1)
        break
      }
    }
  }
  
}

let last_timestamp = 0

const step = (timestamp) => {
  for(let obj of GAME_OBJECTS){
    if(!obj.hasStarted){
      obj.start()
      obj.hasStarted = true
    }
    // 浏览器后台会冻结 requestAnimationFrame, 恢复后与 last_timestamp 的差值
    // 可能长达数分钟; 不钳制的话 Snake.move 一步会把蛇头甩出棋盘外
    obj.timedelta = Math.min(timestamp - last_timestamp, 100)
    obj.update()
  }
  last_timestamp = timestamp
  requestAnimationFrame(step)
}

requestAnimationFrame(step)