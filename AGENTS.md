# AGENTS.md

## Project

King of Bots (KOB) — real-time online snake battle game. Two players (keyboard input or user-written Java bot code) play Snake on a 13×14 grid in the browser. UI text and code comments are mostly Chinese; keep that style.

## Structure

- `web/` — Vue 3 + Vite SPA (ant-design-vue 3, Vuex, vue-router 4, SCSS, ace editor for bot code)
- `backendcloud/` — Maven multi-module, group `com.kob`, Spring Boot 3.1 / Spring Cloud. Three **independently run** services:
  - `backend/` — port **8080**: REST API, JWT auth, WebSocket, game engine (`com.kob.backend.comsumer.Game`)
  - `matchingsystem/` — port **8081**: `MatchingPool` thread pairs waiting players
  - `botruningsystem/` — port **8082**: compiles/runs user bot code via joor
- MySQL: `kob` database @ localhost:3306 (root/123456 in `application.yml`), Spring Data JPA with `ddl-auto: update`; entities `User`, `Bot`, `Record` in `backend/.../entity/`

## Commands

- Frontend (`cd web`, pnpm preferred; both pnpm-lock.yaml and yarn.lock committed):
  - `pnpm install`, `pnpm dev`, `pnpm build`
- Backend: no `mvnw` script — use system Maven. Build from `backendcloud/` root: `mvn package`. Run each Application class separately (`KingOfBotsApplication`, `MatchingSystemApplication`, `BotRunningSystemApplication`); **all three must run for a match to work**.
- No tests except a placeholder context-loads test; no linters configured.

## Request flow (a match)

1. Browser opens WebSocket `ws://localhost:8080/websocket/{token}` (JWT as path param; permitted without auth).
2. start-match → backend POSTs to matchingsystem; on pairing, matchingsystem POSTs `http://localhost:8080/startGame/start`.
3. `Game` thread builds the map, sends start data to both clients, then for each turn sends bot input to botruningsystem, which `Reflect.compile`s the user code, writes input to `input.txt` **in the service's working directory**, runs it, and POSTs the move to `http://127.0.0.1:8080/receiveBotMove/move`.
4. On finish the `Record` is saved (map string + both step strings); replays are re-simulated client-side in `web/src/views/record/RecordVideotape.vue`.

## Critical rules

- **Game logic is duplicated on purpose — keep in sync**: backend `comsumer/Game.java` + `Cell.java`, frontend `web/src/script/Snake.js` + `Cell.js` + `GameMap.js`, and bot-side `botruningsystem/.../reflect/Bot.java`. Any rule change (tail growth every 3 turns, move encoding, map) needs all copies updated.
- Move encoding: digits 0–3 = up/right/down/left with `dx={-1,0,1,0}, dy={0,1,0,-1}`; steps stored as `(0120...)`.
- Internal service-to-service endpoints (`/startGame/start`, `/receiveBotMove/move`, `/matching/player/*`) are NOT JWT-protected; each module's `IpAddressAuthorizationManager` allows only `127.0.0.1`. Internal calls MUST use explicit `http://127.0.0.1:...` — `localhost` can resolve to IPv6 `::1` and get 403'd. All internal calls are POST (`MatchingController /player/remove` must stay `@PostMapping` — callers send POST); keep them try/caught so a down service doesn't kill the WebSocket. Keep that guard when touching SecurityConfig.
- Hardcoded URLs to update together if ports/hosts change: `web/src/utils/http.js` (axios baseURL `http://127.0.0.1:8080/`), `web/src/views/pk/index.vue` (WebSocket URL), `WebSocketServer.java` (`http://localhost:8081`), `MatchingPool.java` (`:8080/startGame/start`), `Consumer.java` (`:8080/receiveBotMove/move`).
- `WebSocketServer` injects repositories into **static** fields via `@Autowired` setters (instance fields `session`/`user` are per connection). Follow the same pattern for new dependencies there.
- `@ServerEndpoint("/websocket/{token}")` — do not end the path with `/`.
- Package `com.kob.backend.comsumer` is a typo of "consumer" but is load-bearing; don't rename casually.
- User bot code is compiled at runtime with joor; `addUid` appends a random suffix to the class name to defeat classloader caching — keep that mechanism intact. Bots implement `Integer nextMove(String input)`; sample input in `backendcloud/input.txt` (runtime artifact, format `map#aSx#aSy#aSteps#bSx#bSy#bSteps`).
- Match lifecycle gotchas (all fixed, keep them fixed): `MatchingPool.addPlayer` dedupes by userId (duplicate start-match used to self-pair); `WebSocketServer.onClose` POSTs remove to the matching queue (disconnects used to leave ghost entries); frontend `Matching.vue` must check `socket.readyState === WebSocket.OPEN` before send — `pk/index.vue` puts the socket into the store synchronously on mount and clears it on close, so `store.state.pk.socket` can be null.
- Pagination: `/bot/list` and `/rankList/toplist` return `PageUtils` (`{list, totalCount, totalPage, currPage}`) — frontend destructures `data.list`, NOT Spring's `content`. Page param semantics differ per page: bot page (incl. matching dropdown) sends **0-based** page; rankList page sends **1-based** (service converts with `page - 1`). Values from `@RequestParam Map` are Strings, parse with hutool `Convert.toInt`. Past bug: `BotServiceImplService.page` filtered by `root.get("id")` instead of `userId` (bots never appeared); `BotController.update` lacked ownership checks.
- User bot protocol (`botruningsystem`): bot code is a class named exactly `Bot` with `package com.kob.botruningsystem.reflect;` implementing `Supplier<Integer>`; `Consumer.addUid` inserts the uid right before `" implements Supplier<Integer>"`, so the declared class name MUST be `Bot`. Bot reads its input from `input.txt` in the service working directory: `map(13x14 walls,182 chars)#mySx#mySy#(mySteps)#oppSx#oppSy#(oppSteps)` — the map has walls only, snake bodies are replayed from start positions + steps; output is direction 0-3 (up/right/down/left, dx={-1,0,1,0}, dy={0,1,0,-1}). Budget is 2000ms per move total (joor compile + IO + think).
- Referee semantics a bot must mirror (Game.check_valid): collision is judged on **post-move bodies** excluding each snake's own new head; head-to-head overlap does NOT kill; tail grows every move for steps ≤10, then when step%3==1. Since both snakes step synchronously, their lengths stay equal.
- joor-java-8 0.9.14 silently fails on JDK 16+ (deep reflection blocked, type()==null) — original tutorial bots could never compile. Replaced by `botruningsystem/core/BotCompiler.java` (javax.tools in-memory compiler, supports nested classes, logs compile diagnostics). `bot.content` is TEXT (was varchar(255), too small for any real bot code).
- Charset gotcha: inserting Chinese via `docker exec kob-mysql mysql ...` without `--default-character-set=utf8mb4` double-encodes UTF-8 (reads back fine in latin1 CLI but shows mojibake through JDBC). Always pass that flag when hand-writing SQL with Chinese.
- Backend responses use envelope `{status, message, data}` (status 200/500/403); `common/GlobalExceptionHandler.java` converts all controller exceptions into it (HTTP 200 + business status), so controller/service code should just throw exceptions — never let them escape raw, or Spring Security turns them into 403/500 empty bodies. Frontend `utils/http.js` rejects with `new Error(message)` on business failure; new APIs must return the envelope (`common/R.java`).
- `User.username` is unique (DB index `uk_username` + `@Column(unique=true)`); register checks duplicates in code and catches `DataIntegrityViolationException` as the concurrency fallback.
- Parent pom's spring-boot-maven-plugin `mainClass` (`com.kob.backendcloud.BackendcloudApplication`) doesn't exist, so `mvn package` still fails at repackage; compiler pins are 17 (aligned with Spring Boot 3).
- Both a MyBatis-Plus config (`MybatisPlusConfig`, `MyMetaObjectHandler`) and JPA exist; persistence is JPA — treat the MyBatis-Plus pieces as vestigial.
- Frontend: `@` alias → `src/`; ant-design-vue components are auto-imported via unplugin-vue-components (`importStyle: "scss"` — required, see vite.config.js). Auth state lives in Vuex root state `store.state.user`; `pk` and `record` are modules. Token in localStorage via `web/src/utils/token.js`.
- Git commits follow conventional style (`feat:`, `fix:`).
