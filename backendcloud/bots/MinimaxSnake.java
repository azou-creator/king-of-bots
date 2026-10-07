package com.kob.botruningsystem.reflect;

import java.io.File;
import java.util.Scanner;
import java.util.function.Supplier;

/**
 * Minimax 贪吃蛇 Bot(与 AlphaBeta-Snake 形成对比的另一种算法风格)。
 *
 * 与 AlphaBeta-Snake 的差异:
 *  - 纯 Minimax 全展开搜索, 无 Alpha-Beta 剪枝, 固定搜索深度(最多 4 层联合走子)
 *  - 评估函数偏"生存空间贪心": 单源 BFS 可达空间 + 机动性 + 场地中心倾向,
 *    不做双方领地争夺计算, 风格偏开阔地带游走
 *
 * 与后端裁判(Game.check_valid)语义保持一致: 双方同时移动、移动后身体判定、
 * 头对头不死、尾巴增长规则(步数<=10 每步增长, 之后 step%3==1)。
 * 输入/方向编码与 AlphaBeta-Snake 完全相同。
 */
public class Bot implements Supplier<Integer> {

    private static final int ROWS = 13, COLS = 14;
    private static final int[] DX = {-1, 0, 1, 0}, DY = {0, 1, 0, -1};
    private static final long TIME_BUDGET_MS = 1100;
    private static final int BIG = 10_000_000;
    private static final int MAX_LEN = ROWS * COLS;
    private static final int MINIMAX_DEPTH = 4; // 纯全展开, 固定深度

    private long deadline;
    private int nodes;

    private static class Timeout extends RuntimeException {
    }

    private static final class State {
        final int[] myBody = new int[MAX_LEN];
        final int[] oppBody = new int[MAX_LEN];
        final int[] block = new int[MAX_LEN];
        int myLen, oppLen;
        int myStepNo, oppStepNo;
    }

    @Override
    public Integer get() {
        try {
            File file = new File("input.txt");
            Scanner scanner = new Scanner(file);
            return nextMove(scanner.nextLine());
        } catch (Exception e) {
            return 0;
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

    // ==================== 解析与状态 ====================

    private State parse(String input) {
        State s = new State();
        String[] p = input.split("#");
        String map = p[0];
        for (int i = 0; i < MAX_LEN && i < map.length(); i++) {
            wallStatic[i] = map.charAt(i) == '1' ? 1 : 0;
            s.block[i] = wallStatic[i];
        }
        int aSx = Integer.parseInt(p[1]), aSy = Integer.parseInt(p[2]);
        int aSteps = p[3].length() - 2;
        int bSx = Integer.parseInt(p[4]), bSy = Integer.parseInt(p[5]);
        int bSteps = p[6].length() - 2;

        s.myLen = replay(s.myBody, aSx, aSy, p[3]);
        s.oppLen = replay(s.oppBody, bSx, bSy, p[6]);
        s.myStepNo = aSteps;
        s.oppStepNo = bSteps;

        for (int i = 0; i < s.myLen; i++) s.block[s.myBody[i]] = 1;
        for (int i = 0; i < s.oppLen; i++) s.block[s.oppBody[i]] = 1;
        if (aSteps > 0 && !increasing(aSteps + 1)) s.block[s.myBody[0]] = 0;
        if (bSteps > 0 && !increasing(bSteps + 1)) s.block[s.oppBody[0]] = 0;
        return s;
    }

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

    private final int[] wallStatic = new int[MAX_LEN];

    // ==================== Minimax 搜索 ====================

    private int searchBest(State root) {
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

        // 固定深度的小迭代: 2 -> MINIMAX_DEPTH, 逐级保留完成结果作为超时安全网
        for (int depth = 2; depth <= MINIMAX_DEPTH; depth++) {
            try {
                int[] res = rootSearch(root, depth);
                if (res[0] <= -BIG / 2) break; // 必败: 保留浅层活路
                bestMove = res[1];
                if (res[0] >= BIG / 2) break;
            } catch (Timeout t) {
                break;
            }
        }
        return bestMove;
    }

    private int[] rootSearch(State s, int depth) {
        int best = -BIG * 2, bestMove = 0;
        for (int m = 0; m < 4; m++) {
            int lo = BIG * 2;
            for (int o = 0; o < 4; o++) {
                checkTime();
                State c = copy(s);
                int outcome = applyJoint(c, m, o);
                int v = outcome == 0 ? search(c, depth - 1, 2) : outcomeValue(outcome, 1);
                if (v < lo) lo = v;
            }
            if (lo > best) {
                best = lo;
                bestMove = m;
            }
        }
        return new int[]{best, bestMove};
    }

    private int search(State s, int depth, int ply) {
        if (depth <= 0) return evaluate(s);
        int best = -BIG * 2;
        for (int m = 0; m < 4; m++) {
            int lo = BIG * 2;
            for (int o = 0; o < 4; o++) {
                checkTime();
                State c = copy(s);
                int outcome = applyJoint(c, m, o);
                int v = outcome == 0 ? search(c, depth - 1, ply + 1) : outcomeValue(outcome, ply);
                if (v < lo) lo = v;
            }
            if (lo > best) best = lo;
        }
        return best;
    }

    private int outcomeValue(int outcome, int ply) {
        if (outcome == 0) return 0;
        if (outcome == 1) return -(BIG - ply * 1000);
        if (outcome == 2) return (BIG - ply * 1000);
        return -(BIG / 2 - ply * 1000);
    }

    private void checkTime() {
        if ((++nodes & 511) == 0 && System.currentTimeMillis() > deadline) {
            throw new Timeout();
        }
    }

    // ==================== 评估(生存空间贪心风格) ====================

    private int evaluate(State s) {
        int[] vis = new int[MAX_LEN];
        int[] queue = new int[MAX_LEN];
        int myHead = s.myBody[s.myLen - 1];
        int qh = 0, qt = 0;
        queue[qt++] = myHead;
        vis[myHead] = 1;
        int space = 0;
        while (qh < qt) {
            int cell = queue[qh++];
            space++;
            int x = cell / COLS, y = cell % COLS;
            for (int i = 0; i < 4; i++) {
                int nx = x + DX[i], ny = y + DY[i];
                if (outOfBounds(nx, ny)) continue;
                int n = nx * COLS + ny;
                if (s.block[n] == 1 || vis[n] == 1) continue;
                vis[n] = 1;
                queue[qt++] = n;
            }
        }

        int myMob = 0;
        int x = myHead / COLS, y = myHead % COLS;
        for (int i = 0; i < 4; i++) {
            int nx = x + DX[i], ny = y + DY[i];
            if (!outOfBounds(nx, ny) && s.block[nx * COLS + ny] == 0) myMob++;
        }

        // 场地中心倾向: 离中心越近越不容易被边角围死
        int center = (ROWS / 2 - Math.abs(x - ROWS / 2)) + (COLS / 2 - Math.abs(y - COLS / 2));

        return space * 6 + myMob * 3 + center;
    }
}
