// components/shell.jsx
// In-run shell — the main K-ToME gameplay screen.
// Parameterized for 3 layout variants (A/B/C), spec-overlay toggle, and demo state.
//
// libGDX alignment notes:
//   • All dimensions integer px on a 4px subgrid (8px module)
//   • Panel borders ninepatch-friendly: 1px outer + 1px inner; corners 3px
//   • Slots fixed pixel: 64 equip, 48 inventory, 64 action, 32 HUD
//   • Right column scene2d.Table with fixed colWidth, fill_x rows
//   • Bottom HUD scene2d.Table with 5 columns (emblem | actions | log | tips)

// === Variant specs ===
const VARIANT_SPEC = {
  A: {
    name: "Standard Iron Frame",
    navW: 56,
    rightW: 320,
    hudH: 200,
    surface: "charcoal",
    panelEdge: "iron",
    rightStack: ["equipment", "inscription", "inventory"],
    desc: "参考实现的忠实译写。左侧 56px 导航,右侧 320px 装备-铭刻-背包,底部 200px HUD。",
    libgdx: "Root: Table(3 cols) → nav(56) | center | right(320). center: VerticalGroup → map(fill) + hud(200).",
  },
  B: {
    name: "Inscription-Priority",
    navW: 56,
    rightW: 360,
    hudH: 180,
    surface: "charcoal",
    panelEdge: "iron",
    rightStack: ["inscription", "equipment", "inventory"],
    desc: "铭刻为构筑核心,右栏顺序倒置。右栏 +40px,HUD -20px,腾给铭刻 2×6 视野。",
    libgdx: "Same Table skeleton, right column = 360. Inscription row gets weight=2, equipment weight=1.5, inventory weight=1.",
  },
  C: {
    name: "Wide-Map Tactical",
    navW: 48,
    rightW: 280,
    hudH: 160,
    surface: "stone",
    panelEdge: "iron",
    rightStack: ["equipment", "inscription", "inventory"],
    desc: "地图战术优先。导航 -8、右栏 -40、HUD -40,地图 +88×40。面板换石板纹。",
    libgdx: "Map cell expand+fill. nav=48, right=280. Stone bg via NinePatchDrawable('panel_stone_9p').",
  },
};

function InRunShell({ variant = "A", showOverlay = false, demoStates = {}, narrow = false }) {
  const spec = VARIANT_SPEC[variant];
  const W = 1280, H = 800;
  const gap = 6;

  // Compute regions
  const navW   = spec.navW;
  const rightW = spec.rightW;
  const hudH   = spec.hudH;
  const mapW   = W - navW - rightW - gap * 4;
  const mapH   = H - hudH - gap * 3;

  return (
    <div data-screen-label={`in_run_shell_${variant}`} style={{
      position: "relative",
      width: W, height: H,
      background: "var(--void-black)",
      padding: gap,
      display: "grid",
      gridTemplateColumns: `${navW}px 1fr ${rightW}px`,
      gridTemplateRows:    `1fr ${hudH}px`,
      gridTemplateAreas:   '"nav map right" "nav hud right"',
      gap: gap,
      fontFamily: "var(--f-body)",
    }}>
      {/* dungeon vignette under everything */}
      <div style={{
        position: "absolute", inset: 0, pointerEvents: "none",
        background: "radial-gradient(120% 80% at 50% 60%, transparent 40%, rgba(0,0,0,0.6) 100%)",
      }}/>

      {/* === Nav === */}
      <div style={{ gridArea: "nav", display: "flex", flexDirection: "column", gap }}>
        <NavRail width={navW} active={0} items={[
          { token: "nav.compass",   key: "M" },
          { token: "nav.backpack",  key: "I" },
          { token: "nav.quest",     key: "Q" },
          { token: "nav.talent",    key: "T" },
          { token: "nav.settings",  key: "Esc" },
        ]}/>
        {showOverlay && <SpecTag>{`nav.w=${navW}`}</SpecTag>}
      </div>

      {/* === Map === */}
      <div style={{ gridArea: "map", position: "relative" }}>
        <MapPlaceholder width="100%" height="100%" />
        {showOverlay && (
          <SpecTag style={{ position: "absolute", top: 6, left: 6 }}>
            {`map ${mapW}×${mapH}`}
          </SpecTag>
        )}
      </div>

      {/* === Right column === */}
      <div style={{ gridArea: "right", display: "flex", flexDirection: "column", gap }}>
        {spec.rightStack.map((section, i) => (
          <RightPanel key={section} section={section} variant={variant} demoStates={demoStates} flex={
            section === "inscription" ? (variant === "B" ? 2 : 1.6) :
            section === "equipment"   ? 1.5 :
                                        0.9
          }/>
        ))}
        {showOverlay && <SpecTag>{`right.w=${rightW}`}</SpecTag>}
      </div>

      {/* === Bottom HUD === */}
      <div style={{ gridArea: "hud", position: "relative" }}>
        <BottomHUD variant={variant} demoStates={demoStates} />
        {showOverlay && (
          <SpecTag style={{ position: "absolute", top: -10, left: 6 }}>
            {`hud.h=${hudH}`}
          </SpecTag>
        )}
      </div>

      {/* === Overlay grid lines === */}
      {showOverlay && <RegionOverlay W={W} H={H} navW={navW} rightW={rightW} hudH={hudH} mapW={mapW} mapH={mapH} gap={gap} />}
    </div>
  );
}

// ====== Right column section ======
function RightPanel({ section, variant, demoStates, flex }) {
  const eqStates = demoStates.equipment || {};
  const inscStates = demoStates.inscription || {};
  const invStates = demoStates.inventory || {};

  if (section === "equipment") {
    return (
      <Panel title="装备  ·  EQUIPMENT" surface="charcoal" edge="iron" padding={10} style={{ flex }}>
        <EquipmentGrid states={eqStates} variant={variant} />
      </Panel>
    );
  }
  if (section === "inscription") {
    return (
      <Panel title="铭刻栏  ·  INSCRIPTION" surface="charcoal" edge="iron" padding={10} style={{ flex }}>
        <InscriptionGrid states={inscStates} variant={variant} />
      </Panel>
    );
  }
  return (
    <Panel title="背包  ·  INVENTORY" surface="charcoal" edge="iron" padding={10} style={{ flex }}>
      <InventoryGrid states={invStates} variant={variant} />
    </Panel>
  );
}

// ====== Equipment grid ======
function EquipmentGrid({ states, variant }) {
  const slotSize = variant === "C" ? 56 : 60;
  const items = [
    { id: "weapon",   token: "eq.sword_t2",   placeholder: "weapon",  state: states.weapon   || "equipped" },
    { id: "shield",   token: "eq.shield_t1",  placeholder: "shield",  state: states.shield   || "equipped" },
    { id: "helmet",   token: "eq.helm_t1",    placeholder: "helmet",  state: states.helmet   || "equipped" },
    { id: "armor",    token: "eq.armor_t2",   placeholder: "armor",   state: states.armor    || "equipped" },
    { id: "cloak",    token: "eq.cloak_t1",   placeholder: "cloak",   state: states.cloak    || "equipped" },
    { id: "gloves",   token: "eq.gloves_t1",  placeholder: "gloves",  state: states.gloves   || "equipped" },
    { id: "amulet",   token: "eq.amulet_t1",  placeholder: "amulet",  state: states.amulet   || "empty",   shape: "circle" },
    { id: "ring",     token: "eq.ring_t1",    placeholder: "ring",    state: states.ring     || "selected",shape: "circle" },
  ];
  return (
    <div>
      <div style={{ display: "grid", gridTemplateColumns: `repeat(2, ${slotSize}px)`, gap: 8, justifyContent: "space-between" }}>
        {items.map(it => (
          <Slot key={it.id} size={slotSize} state={it.state} token={it.state === "empty" ? null : it.token}
                placeholder={it.placeholder} shape={it.shape || "square"} hint={it.id} />
        ))}
      </div>
      {/* boots — single centered row */}
      <div style={{ display: "flex", justifyContent: "center", marginTop: 8 }}>
        <Slot size={slotSize} state="equipped" token="eq.boots_t1" placeholder="boots" hint="boots" />
      </div>
    </div>
  );
}

// ====== Inscription grid — 2 col × 6 row ======
function InscriptionGrid({ states, variant }) {
  // Each cell: index + state-toned icon + name; 2 col × 6 row
  const inscriptions = [
    { i: 1, name: "战吼之印",  token: "insc.battlecry",   tone: "red"    },
    { i: 2, name: "格挡之印",  token: "insc.parry",       tone: "iron"   },
    { i: 3, name: "猛击之印",  token: "insc.bash",        tone: "iron"   },
    { i: 4, name: "守备姿态",  token: "insc.guard",       tone: "iron"   },
    { i: 5, name: "治疗之印",  token: "insc.heal",        tone: "cyan"   },
    { i: 6, name: "撕裂门",    token: "insc.rift",        tone: "violet" },
    { i: 7, name: "迅捷之印",  token: "insc.swift",       tone: "iron"   },
    { i: 8, name: "净化之印",  token: "insc.cleanse",     tone: "violet" },
    { i: 9, name: "凶暴之印",  token: "insc.savage",      tone: "red"    },
    { i: 10, name: "生命虹吸", token: "insc.lifesteal",   tone: "green"  },
    { i: 11, name: "迅捷之印", token: "insc.swift_2",     tone: "iron"   },
    { i: 12, name: "轨道光环", token: "insc.orbit",       tone: "cyan"   },
  ];
  const cellH = variant === "C" ? 28 : 32;
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6 }}>
      {inscriptions.map((insc) => {
        const stateKey = `slot${insc.i}`;
        const state = states[stateKey] || "equipped";
        const rim =
          state === "selected" ? "var(--cold-cyan)" :
          state === "invalid"  ? "var(--blood-red)" :
          state === "disabled" ? "var(--talent-locked)" :
                                  "var(--iron-edge)";
        return (
          <div key={insc.i} style={{
            display: "flex", alignItems: "center", gap: 6,
            height: cellH,
            padding: "0 6px",
            background: "linear-gradient(180deg, #0c1118, #080B11)",
            border: `1px solid ${rim}`,
            borderRadius: 2,
            opacity: state === "disabled" ? 0.5 : 1,
          }}>
            <IconToken token={insc.token} size={cellH - 8} tone={insc.tone} showLabel={false} />
            <span style={{
              fontFamily: "var(--f-mono)",
              color: "var(--ember-gold)",
              fontSize: 10,
              minWidth: 14,
              textAlign: "right",
            }}>{insc.i}.</span>
            <span style={{
              fontFamily: "var(--f-body)",
              color: state === "selected" ? "var(--cold-cyan)" : "var(--primary-text)",
              fontSize: 11,
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
              flex: 1,
            }}>{insc.name}</span>
          </div>
        );
      })}
    </div>
  );
}

// ====== Inventory grid (consumable resources) ======
function InventoryGrid({ states, variant }) {
  const slotSize = variant === "C" ? 44 : 48;
  const items = [
    { id: "potion_hp",  token: "inv.potion_hp",  count: 2,   tone: "red"    },
    { id: "scroll",     token: "inv.scroll",     count: 3,   tone: "iron"   },
    { id: "key",        token: "inv.key",        count: 1,   tone: "gold"   },
    { id: "gold",       token: "inv.gold",       count: 215, tone: "gold"   },
    { id: "ruby",       token: "inv.ruby",       count: 5,   tone: "red"    },
    { id: "herb",       token: "inv.herb",       count: 2,   tone: "green"  },
    { id: "potion_mp",  token: "inv.potion_mp",  count: 3,   tone: "cyan"   },
    { id: "crystal",    token: "inv.crystal",    count: 1,   tone: "violet" },
  ];
  return (
    <div style={{ display: "grid", gridTemplateColumns: `repeat(4, ${slotSize}px)`, gap: 8, justifyContent: "space-between" }}>
      {items.map(it => (
        <div key={it.id} style={{ position: "relative" }}>
          <Slot
            size={slotSize}
            state={states[it.id] || "equipped"}
            token={it.token}
            count={it.count}
            hint={it.id}
          />
        </div>
      ))}
    </div>
  );
}

// ====== Bottom HUD ======
function BottomHUD({ variant, demoStates }) {
  const hudH = VARIANT_SPEC[variant].hudH;
  const tight = variant === "C";
  return (
    <div style={{ display: "grid", gridTemplateColumns: "300px 240px 1fr 240px", gap: 6, height: "100%" }}>
      {/* Hero panel */}
      <Panel padding={tight ? 8 : 10} style={{ display: "flex", gap: 10 }}>
        <HeroEmblem level={7} />
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: tight ? 4 : 6, minWidth: 0 }}>
          <div>
            <div style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: tight ? 16 : 18, letterSpacing: "0.06em" }}>薇雄  <span style={{ fontSize: 10, color: "var(--muted-text)" }}>[name]</span></div>
            <div style={{ fontFamily: "var(--f-body)", color: "var(--muted-text)", fontSize: 11 }}>破碎前哨  ·  层 <span className="t-primary" style={{ fontFamily: "var(--f-mono)" }}>1/2</span></div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
            <Bar kind="hp" current={152} max={152} width={"100%"} height={tight ? 12 : 14} />
            <Bar kind="sp" current={84}  max={84}  width={"100%"} height={tight ? 12 : 14} />
            {!tight && <Bar kind="xp" current={120} max={400} width={"100%"} height={8} label={false} />}
          </div>
          <div style={{ display: "flex", gap: 10, marginTop: 2 }}>
            <StatChip token="stat.atk" label="攻击" value={38} tone="iron" />
            <StatChip token="stat.def" label="防御" value={13} tone="iron" />
          </div>
        </div>
      </Panel>

      {/* Action bar */}
      <Panel padding={tight ? 8 : 10}>
        <div style={{ display: "flex", flexDirection: "column", height: "100%", gap: 4 }}>
          <div style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 10, letterSpacing: "0.15em" }}>主动技能 · ACTIONS</div>
          <div style={{ display: "flex", gap: 8, justifyContent: "space-between", flex: 1, alignItems: "center" }}>
            {[
              { hk: "1", name: "猛击",     token: "act.smash",   state: demoStates.actions?.[0] || "equipped" },
              { hk: "2", name: "盾击",     token: "act.shield_bash", state: demoStates.actions?.[1] || "selected" },
              { hk: "3", name: "指挥姿态", token: "act.command", state: demoStates.actions?.[2] || "equipped" },
            ].map((a, i) => (
              <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 3 }}>
                <Slot size={tight ? 52 : 60} state={a.state} token={a.token} hotkey={a.hk} hint={a.name} />
                <div style={{ fontSize: 10, color: a.state === "selected" ? "var(--cold-cyan)" : "var(--primary-text)" }}>
                  <span style={{ color: "var(--ember-gold)", fontFamily: "var(--f-mono)" }}>{a.hk}</span> {a.name}
                </div>
              </div>
            ))}
          </div>
        </div>
      </Panel>

      {/* Log */}
      <Panel padding={tight ? 8 : 10}>
        <div style={{ display: "flex", flexDirection: "column", gap: 2, fontSize: 11, lineHeight: 1.5 }}>
          <div className="t-cyan">你进入了地牢。</div>
          <div className="t-cyan"><span className="t-gold">破碎前哨:</span> 废弃边境哨所,窒屑的泥泞。</div>
          <div className="t-primary">目标: <span className="t-gold">突破前哨。</span></div>
          <div className="t-muted">路线提示: 先沿钥匙口周边的交互点激活开局,再去...</div>
          <div className="t-muted">路线提示: 如果在 Boss 战外探索太久,递还...</div>
        </div>
      </Panel>

      {/* Key hints */}
      <Panel padding={tight ? 8 : 10}>
        <div style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 10, letterSpacing: "0.15em", marginBottom: 6 }}>操作提示 · HINTS</div>
        <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "3px 8px", fontSize: 11 }}>
          <div><KeyCap tone="gold">I</KeyCap></div><div className="t-muted">背包</div>
          <div><KeyCap tone="gold">G</KeyCap></div><div className="t-muted">拾取</div>
          <div><KeyCap tone="gold">Ctrl</KeyCap>+<KeyCap tone="gold">S</KeyCap></div><div className="t-muted">保存</div>
          <div><KeyCap tone="gold">L</KeyCap></div><div className="t-muted">调整装备</div>
          <div style={{ display: "flex", gap: 2 }}>
            <KeyCap tone="cyan">1</KeyCap><span style={{ color: "var(--muted-text)" }}>-</span><KeyCap tone="cyan">4</KeyCap>
          </div><div className="t-muted">使用药水</div>
          <div style={{ display: "flex", gap: 2 }}>
            <KeyCap tone="cyan">5</KeyCap><span style={{ color: "var(--muted-text)" }}>-</span><KeyCap tone="cyan">8</KeyCap>
          </div><div className="t-muted">使用铭刻</div>
        </div>
      </Panel>
    </div>
  );
}

function HeroEmblem({ level }) {
  return (
    <div style={{
      width: 56, height: 72,
      position: "relative",
      flex: "0 0 56px",
    }}>
      <div style={{
        width: "100%", height: "100%",
        background:
          "radial-gradient(60% 50% at 50% 25%, rgba(217,154,43,0.25), transparent 60%), " +
          "linear-gradient(180deg, #2a1112 0%, #4a1c1c 40%, #2a1112 100%)",
        border: "1px solid var(--ember-gold-dim)",
        clipPath: "polygon(0 0, 100% 0, 100% 80%, 50% 100%, 0 80%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow: "inset 0 0 0 1px rgba(0,0,0,0.7)",
      }}>
        <span style={{
          fontFamily: "var(--f-mono)",
          fontSize: 9,
          color: "var(--ember-gold)",
          opacity: 0.7,
          letterSpacing: 0,
          textAlign: "center",
        }}>[crest<br/>vanguard]</span>
      </div>
      {/* level badge */}
      <div style={{
        position: "absolute",
        bottom: -2, left: "50%",
        transform: "translateX(-50%)",
        width: 22, height: 22, borderRadius: "50%",
        background: "var(--void-black)",
        border: "1px solid var(--ember-gold)",
        display: "flex", alignItems: "center", justifyContent: "center",
        color: "var(--ember-gold)",
        fontFamily: "var(--f-title)",
        fontSize: 12,
        fontWeight: 700,
      }}>{level}</div>
    </div>
  );
}

// ====== Overlay grid annotations ======
function RegionOverlay({ W, H, navW, rightW, hudH, mapW, mapH, gap }) {
  const line = "1px dashed rgba(28,183,200,0.4)";
  return (
    <div style={{ position: "absolute", inset: 0, pointerEvents: "none", zIndex: 50 }}>
      {/* vertical guides */}
      <div style={{ position: "absolute", top: 0, bottom: 0, left: navW + gap*2, borderLeft: line }}/>
      <div style={{ position: "absolute", top: 0, bottom: 0, right: rightW + gap*2, borderRight: line }}/>
      {/* horizontal */}
      <div style={{ position: "absolute", left: 0, right: 0, bottom: hudH + gap*2, borderBottom: line }}/>
      {/* axis labels */}
      <div style={{ position: "absolute", top: 2, left: gap + 2 }}><SpecTag>{`x=${navW + gap}`}</SpecTag></div>
      <div style={{ position: "absolute", top: 2, right: gap + 2 }}><SpecTag>{`x=${W - rightW - gap}`}</SpecTag></div>
      <div style={{ position: "absolute", bottom: hudH + gap*2 + 2, left: gap*2 + navW + 6 }}><SpecTag>{`y=${H - hudH - gap}`}</SpecTag></div>
    </div>
  );
}

function SpecTag({ children, style = {} }) {
  return (
    <span style={{
      display: "inline-block",
      padding: "1px 4px",
      background: "rgba(28,183,200,0.12)",
      border: "1px solid var(--cold-cyan)",
      color: "var(--cold-cyan)",
      fontFamily: "var(--f-mono)",
      fontSize: 9,
      lineHeight: "12px",
      borderRadius: 1,
      letterSpacing: 0,
      pointerEvents: "none",
      ...style,
    }}>{children}</span>
  );
}

window.InRunShell = InRunShell;
window.VARIANT_SPEC = VARIANT_SPEC;
window.SpecTag = SpecTag;
