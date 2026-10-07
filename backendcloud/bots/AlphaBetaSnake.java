package com.kob.botruningsystem.reflect;

import java.io.File;
import java.util.Scanner;
import java.util.function.Supplier;

/**
 * Alpha-Beta 剪枝贪吃蛇 Bot。
 *
 * 输入格式: 地图(13x14 墙,182 字符)#我的起点x#我的起点y#(我的历史步)#对方起点x#对方起点y#(对方历史步)
 * 方向: 0=上 1=右 2=下 3=左, dx={-1,0,1,0}, dy={0,1,0,-1}
 *
 * 必须与后端裁判(Game.check_valid)语义一致:
 *  - 双方同时移动, 碰撞判定基于移动后的身体
 *  - 头进入: 墙 / 自己身体(不含头) / 对方身体(不含对方头) => 死亡
 *  - 头对头(两新头同格)不会死
 *  - 尾巴增长规则: 步数<=10 每步增长, 之后 step%3==1 才增长
 *
 * 策略: 迭代加深 Alpha-Beta(联合走子一层 = 我一步 + 对方一步),
 *       叶子评估 = Voronoi 领地差 + 机动性差, 死亡终局按深度惩罚。
 *       搜索用"每节点复制状态"而非共享可变状态, 避免递归中 undo 信息被内层覆盖。
 */
public class Bot implements Supplier<Integer> {

    private static final int ROWS = 13, COLS = 14;
    private static final int[] DX = {-1, 0, 1, 0}, DY = {0, 1, 0, -1};
    // Consumer.startTimeout 总预算 2000ms(含 joor 编译/IO), 这里只留一小半给搜索
    private static final long TIME_BUDGET_MS = 1100;
    private static final int BIG = 10_000_000;
    private static final int MAX_LEN = ROWS * COLS;

    private long deadline;
    private int nodes;

    private static class Timeout extends RuntimeException {
    }

    /** 搜索状态(蛇身 tail->head) */
    private static final class State {
        final int[] myBody = new int[MAX_LEN];
        final int[] oppBody = new int[MAX_LEN];
        final int[] block = new int[MAX_LEN]; // 墙 + 蛇身, 供 BFS 评估
        int myLen, oppLen;
        int myStepNo, oppStepNo; // 已走步数(增长判定用, 10 步后与长度不等)
    }

    @Override
    public Integer get() {
        try {
            File file = new File("input.txt");
            Scanner scanner = new Scanner(file);
            return nextMove(scanner.nextLine());
        } catch (Exception e) {
            return 0; // 任何异常也要返回一个方向, 避免上层 NPE
        }
    }

    public Integer nextMove(String input) {
        deadline = System.currentTimeMillis() + TIME_BUDGET_MS;
        nodes = 0;
        try {
            State s = parse(input);
            return searchBest(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 解析 ====================

    private State parse(String input) {
        State s = new State();
        String[] p = input.split("#");
        String map = p[0];
        for (int i = 0; i < MAX_LEN && i < map.length(); i++) {
            wallStatic[i] = map.charAt(i) == '1' ? 1 : 0;
            s.block[i] = wallStatic[i];
        }
        int aSx = Integer.parseInt(p[1]), aSy = Integer.parseInt(p[2]);
        int aSteps = p[3].length() - 2; // 去括号
        int bSx = Integer.parseInt(p[4]), bSy = Integer.parseInt(p[5]);
        int bSteps = p[6].length() - 2;

        s.myLen = replay(s.myBody, aSx, aSy, p[3]);
        s.oppLen = replay(s.oppBody, bSx, bSy, p[6]);
        s.myStepNo = aSteps;
        s.oppStepNo = bSteps;

        for (int i = 0; i < s.myLen; i++) s.block[s.myBody[i]] = 1;
        for (int i = 0; i < s.oppLen; i++) s.block[s.oppBody[i]] = 1;
        // 尾巴尖按"本步会腾出"留空(只影响评估的 BFS, 不影响判定)
        if (aSteps > 0 && !increasing(aSteps + 1)) s.block[s.myBody[0]] = 0;
        if (bSteps > 0 && !increasing(bSteps + 1)) s.block[s.oppBody[0]] = 0;
        return s;
    }

    /** 回放一条蛇的全部历史步, 返回最终长度 */
    private int replay(int[] body, int sx, int sy, String stepsStr) {
        String steps = stepsStr.length() >= 2 ? stepsStr.substring(1, stepsStr.length() - 1) : "";
        int x = sx, y = sy;
        int len = 0;
        body[len++] = x * COLS + y;
        for (int i = 0; i < steps.length(); i++) {
            int d = steps.charAt(i) - '0';
            x += DX[d];
            y += DY[d];
            body[len++] = x * COLS + y;
            if (!increasing(i + 1)) {
                len = removeTail(body, len);
            }
        }
        return len;
    }

    private static int removeTail(int[] body, int len) {
        for (int i = 1; i < len; i++) body[i - 1] = body[i];
        return len - 1;
    }

    private static boolean increasing(int step) {
        return step <= 10 || step % 3 == 1;
    }

    private static State copy(State s) {
        State n = new State();
        System.arraycopy(s.myBody, 0, n.myBody, 0, s.myLen);
        System.arraycopy(s.oppBody, 0, n.oppBody, 0, s.oppLen);
        System.arraycopy(s.block, 0, n.block, 0, MAX_LEN);
        n.myLen = s.myLen;
        n.oppLen = s.oppLen;
        n.myStepNo = s.myStepNo;
        n.oppStepNo = s.oppStepNo;
        return n;
    }

    // ==================== 联合走子 ====================

    /**
     * 双方同时走一步(就地修改 s)。返回值 bit0=我死, bit1=对方死。
     * 镜像 Game.check_valid: 移动后的身体, 不含各自的新头, 头对头不死。
     */
    private int applyJoint(State s, int m, int o) {
        int myHead = s.myBody[s.myLen - 1], oppHead = s.oppBody[s.oppLen - 1];
        int mx = myHead / COLS + DX[m], myy = myHead % COLS + DY[m];
        int ox = oppHead / COLS + DX[o], oy = oppHead % COLS + DY[o];

        boolean myGrow = increasing(s.myStepNo + 1);
        boolean oppGrow = increasing(s.oppStepNo + 1);
        if (!myGrow) s.myLen = removeTail(s.myBody, s.myLen);
        if (!oppGrow) s.oppLen = removeTail(s.oppBody, s.oppLen);

        int myHeadCell = mx * COLS + myy, oppHeadCell = ox * COLS + oy;
        boolean myOut = outOfBounds(mx, myy), oppOut = outOfBounds(ox, oy);
        if (!myOut) s.block[myHeadCell] = 1;
        if (!oppOut) s.block[oppHeadCell] = 1;
        s.myBody[s.myLen++] = myHeadCell;
        s.oppBody[s.oppLen++] = oppHeadCell;
        s.myStepNo++;
        s.oppStepNo++;

        int result = 0;
        if (myOut || wallAt(mx, myy)
                || containsExceptHead(s.myBody, s.myLen, myHeadCell)
                || containsExceptHead(s.oppBody, s.oppLen, myHeadCell)) {
            result |= 1;
        }
        if (oppOut || wallAt(ox, oy)
                || containsExceptHead(s.oppBody, s.oppLen, oppHeadCell)
                || containsExceptHead(s.myBody, s.myLen, oppHeadCell)) {
            result |= 2;
        }
        return result;
    }

    private boolean wallAt(int x, int y) {
        return x >= 0 && x < ROWS && y >= 0 && y < COLS && wallStatic[x * COLS + y] == 1;
    }

    private boolean outOfBounds(int x, int y) {
        return x < 0 || x >= ROWS || y < 0 || y >= COLS;
    }

    private boolean containsExceptHead(int[] body, int len, int cell) {
        for (int i = 0; i < len - 1; i++) {
            if (body[i] == cell) return true;
        }
        return false;
    }

    // 静态墙(与 block 分离, 判定不受蛇身影响)
    private final int[] wallStatic = new int[MAX_LEN];

    // ==================== Alpha-Beta ====================

    private int searchBest(State root) {
        // 根安全兜底: 先找一个不会立刻死的方向
        int bestMove = 0;
        outer:
        for (int m = 0; m < 4; m++) {
            for (int o = 0; o < 4; o++) {
                State c = copy(root);
                int outcome = applyJoint(c, m, o);
                if ((outcome & 1) == 0) {
                    bestMove = m;
                    break outer;
                }
            }
        }

        for (int depth = 1; depth <= 12; depth++) {
            try {
                int[] res = rootSearch(root, depth);
                // 全军必败: 保留上一层的 bestMove(浅层找到的活路/拖延走法), 不采用本层"第一个死法"
                if (res[0] <= -BIG / 2) break;
                bestMove = res[1];
                // 已确定必胜走势, 不再深挖
                if (res[0] >= BIG / 2) break;
            } catch (Timeout t) {
                break; // 保留上一层完整结果的 bestMove
            }
        }
        return bestMove;
    }

    /** 根节点: MAX 层。返回 {value, bestMove} */
    private int[] rootSearch(State s, int depth) {
        int best = -BIG * 2, bestMove = 0;
        int alpha = -BIG * 2, beta = BIG * 2;
        for (int m = 0; m < 4; m++) {
            int lo = BIG * 2;
            for (int o = 0; o < 4; o++) {
                checkTime();
                State c = copy(s);
                int outcome = applyJoint(c, m, o);
                int v = outcome == 0 ? search(c, depth - 1, alpha, beta, 2) : outcomeValue(outcome, 1);
                if (v < lo) lo = v;
                if (lo <= alpha) break;
            }
            if (lo > best) {
                best = lo;
                bestMove = m;
            }
            if (best > alpha) alpha = best;
        }
        return new int[]{best, bestMove};
    }

    /** MAX(我)层: 我选择使结果最大的走法, 对方随后作最坏应对; ply 用于死亡分值的深度惩罚 */
    private int search(State s, int depth, int alpha, int beta, int ply) {
        if (depth <= 0) return evaluate(s);
        int best = -BIG * 2;
        for (int m = 0; m < 4; m++) {
            int lo = BIG * 2;
            for (int o = 0; o < 4; o++) {
                checkTime();
                State c = copy(s);
                int outcome = applyJoint(c, m, o);
                int v = outcome == 0 ? search(c, depth - 1, alpha, beta, ply + 1) : outcomeValue(outcome, ply);
                if (v < lo) lo = v;
                if (lo <= alpha) break;
            }
            if (lo > best) best = lo;
            if (best > alpha) alpha = best;
            if (alpha >= beta) break;
        }
        return best;
    }

    /** 死亡结果转分值, ply 越深死亡分值损失越小(死得晚比死得早好) */
    private int outcomeValue(int outcome, int ply) {
        if (outcome == 0) return 0;
        if (outcome == 1) return -(BIG - ply * 1000);
        if (outcome == 2) return (BIG - ply * 1000);
        return -(BIG / 2 - ply * 1000); // 双方都死: 都判负, 但比单独送死好
    }

    private void checkTime() {
        if ((++nodes & 1023) == 0 && System.currentTimeMillis() > deadline) {
            throw new Timeout();
        }
    }

    // ==================== 评估 ====================

    /** 双源 BFS 的 Voronoi 领地差 + 机动性差 */
    private int evaluate(State s) {
        int[] dist = new int[MAX_LEN];
        int[] owner = new int[MAX_LEN];
        int[] queue = new int[MAX_LEN * 2];
        for (int i = 0; i < MAX_LEN; i++) dist[i] = -1;

        int myHead = s.myBody[s.myLen - 1], oppHead = s.oppBody[s.oppLen - 1];
        int qh = 0, qt = 0;
        queue[qt++] = myHead;
        dist[myHead] = 0;
        owner[myHead] = 1;
        queue[qt++] = oppHead;
        dist[oppHead] = 0;
        owner[oppHead] = 2;

        while (qh < qt) {
            int cell = queue[qh++];
            int d = dist[cell];
            int own = owner[cell];
            int x = cell / COLS, y = cell % COLS;
            for (int i = 0; i < 4; i++) {
                int nx = x + DX[i], ny = y + DY[i];
                if (outOfBounds(nx, ny)) continue;
                int n = nx * COLS + ny;
                if (s.block[n] == 1) continue;
                if (dist[n] == -1) {
                    dist[n] = d + 1;
                    owner[n] = own;
                    queue[qt++] = n;
                } else if (dist[n] == d + 1 && owner[n] != own && owner[n] != 0) {
                    owner[n] = 0; // 同层双方同时到达: 中立区
                }
            }
        }

        int territory = 0;
        for (int i = 0; i < MAX_LEN; i++) {
            if (owner[i] == 1) territory++;
            else if (owner[i] == 2) territory--;
        }

        int myMob = 0, oppMob = 0;
        for (int i = 0; i < 4; i++) {
            int nx = myHead / COLS + DX[i], ny = myHead % COLS + DY[i];
            if (!outOfBounds(nx, ny) && s.block[nx * COLS + ny] == 0) myMob++;
            nx = oppHead / COLS + DX[i];
            ny = oppHead % COLS + DY[i];
            if (!outOfBounds(nx, ny) && s.block[nx * COLS + ny] == 0) oppMob++;
        }

        return territory * 10 + (myMob - oppMob);
    }
}
