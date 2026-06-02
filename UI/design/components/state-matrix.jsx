// components/state-matrix.jsx
// Two things:
//   1) SlotStateMatrix — 8 slot kinds × 6 states (48 cells). The full
//      acceptance grid renderer engineers should screenshot for golden tests.
//   2) NinePatchSpec — 6 panel kinds × source-rect + content-padding + stretched render.
//      Atlas slice spec ready for libGDX NinePatchDrawable.

// ====== Slot State Matrix ======
const SLOT_KINDS = [
  { k: "weapon",  ch: "武器", token: "eq.sword_t2",   shape: "square", placeholder: "weapon" },
  { k: "shield",  ch: "盾",   token: "eq.shield_t1",  shape: "square", placeholder: "shield" },
  { k: "helmet",  ch: "头盔", token: "eq.helm_t1",    shape: "square", placeholder: "helmet" },
  { k: "armor",   ch: "胸甲", token: "eq.armor_t2",   shape: "square", placeholder: "armor"  },
  { k: "cloak",   ch: "斗篷", token: "eq.cloak_t1",   shape: "square", placeholder: "cloak"  },
  { k: "amulet",  ch: "项链", token: "eq.amulet_t1",  shape: "circle", placeholder: "amulet" },
  { k: "ring",    ch: "戒指", token: "eq.ring_t1",    shape: "circle", placeholder: "ring"   },
  { k: "boots",   ch: "靴子", token: "eq.boots_t1",   shape: "square", placeholder: "boots"  },
];

const SLOT_STATES = ["empty", "equipped", "selected", "invalid", "pending", "disabled"];

function SlotStateMatrix() {
  return (
    <div style={{ padding: 18 }}>
      <div className="section-title" style={{ marginBottom: 14 }}>
        SLOT STATE MATRIX · 8 kinds × 6 states · canonical golden grid
      </div>
      <div style={{ display: "grid",
        gridTemplateColumns: `90px repeat(${SLOT_KINDS.length}, 1fr)`,
        gap: 8,
        alignItems: "stretch",
      }}>
        {/* header row */}
        <div/>
        {SLOT_KINDS.map(k => (
          <div key={k.k} style={{ textAlign: "center" }}>
            <div style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 11, letterSpacing: "0.08em" }}>{k.ch}</div>
            <div style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 9 }}>{k.k}</div>
          </div>
        ))}
        {/* body */}
        {SLOT_STATES.map(state => (
          <React.Fragment key={state}>
            <div style={{
              display: "flex", flexDirection: "column", justifyContent: "center",
              padding: "0 6px",
              borderRight: "1px solid var(--iron-edge-dim)",
            }}>
              <div style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 11, letterSpacing: 0.4 }}>{state.toUpperCase()}</div>
              <div style={{ fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 9, lineHeight: 1.3 }}>
                {state === "empty"    ? "iron rim · placeholder text" :
                 state === "equipped" ? "ember dim rim + tone icon"   :
                 state === "selected" ? "cyan rim + glow"             :
                 state === "invalid"  ? "red rim + ‘!’ badge"          :
                 state === "pending"  ? "ember + cyan halo + ‘↻’"      :
                                        "locked gray + dim bg"        }
              </div>
            </div>
            {SLOT_KINDS.map(kind => (
              <div key={kind.k} style={{ display: "flex", justifyContent: "center", alignItems: "center", padding: 4 }}>
                <Slot
                  size={64}
                  state={state}
                  token={state === "empty" ? null : kind.token}
                  shape={kind.shape}
                  placeholder={kind.placeholder}
                  hint={state}
                />
              </div>
            ))}
          </React.Fragment>
        ))}
      </div>

      {/* legend strip */}
      <div style={{ marginTop: 14, padding: "8px 10px", border: "1px solid var(--iron-edge-dim)", background: "var(--charcoal-panel-2)", borderRadius: 2, fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)", lineHeight: 1.55 }}>
        <span className="t-gold">dual channel:</span> every state is rendered with rim color + a sigil/badge or placeholder, never color alone. shape (circle) preserves through all states. renderer must reach this grid in golden screenshot tests before PR can land.
      </div>
    </div>
  );
}

// ====== Nine-Patch Spec ======
// Each kind has: source-rect (px), content-padding (l/t/r/b), stretch markers, atlas key.

const NINEPATCH_KINDS = [
  {
    key:    "panel_iron_9p",
    name:   "Default Iron Panel",
    desc:   "默认面板。1px iron edge + 内嵌阴影。HUD、右栏装备/铭刻/背包栈用此 9p。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 12, t: 12, r: 12, b: 12 },
    minSize:{ w: 32, h: 32 },
    edge:   "iron",
    surface:"charcoal",
    target: "charcoal-panel · iron-edge",
  },
  {
    key:    "panel_stone_9p",
    name:   "Stone Panel",
    desc:   "Variant C 与 modal 用。石板纹理通过 background 实现,9p 仅控制 iron 边和 corner。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 14, t: 14, r: 14, b: 14 },
    minSize:{ w: 40, h: 40 },
    edge:   "iron",
    surface:"stone",
    target: "stone fill · iron-edge",
  },
  {
    key:    "panel_focus_cyan_9p",
    name:   "Focused (Cyan)",
    desc:   "焦点态 1px cyan rim,边缘外 1px 软光晕(由 box-shadow 模拟,9p 只切 rim)。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 12, t: 12, r: 12, b: 12 },
    minSize:{ w: 32, h: 32 },
    edge:   "cyan",
    surface:"charcoal",
    target: "cold-cyan rim",
  },
  {
    key:    "panel_strip_cyan_9p",
    name:   "Selected (Cyan Strip)",
    desc:   "选中态:左边 2px cyan 条 + 三边 1px iron。非对称 9p,左边 stretch 锁定在 2px。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 14, t: 12, r: 12, b: 12 },
    minSize:{ w: 32, h: 32 },
    edge:   "cyan-strip",
    surface:"charcoal",
    target: "iron-edge + 2px cold-cyan left strip",
    asymm: true,
  },
  {
    key:    "panel_reserve_ember_9p",
    name:   "Reserve (Ember)",
    desc:   "Reserve 态:1px ember-dim hairline,内嵌 0.10 ember 微光。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 12, t: 12, r: 12, b: 12 },
    minSize:{ w: 32, h: 32 },
    edge:   "ember",
    surface:"charcoal",
    target: "ember-gold-dim rim",
  },
  {
    key:    "panel_warn_red_9p",
    name:   "Warning (Red)",
    desc:   "警告态:1px blood-red rim + 0.18 内嵌红光。仅用于真实危险/无效目标。",
    src:    { w: 96, h: 64 },
    corner: 3,
    padding:{ l: 12, t: 12, r: 12, b: 12 },
    minSize:{ w: 32, h: 32 },
    edge:   "red",
    surface:"charcoal",
    target: "blood-red rim",
  },
];

function NinePatchSpec() {
  return (
    <div style={{ padding: 18 }}>
      <div className="section-title" style={{ marginBottom: 14 }}>
        NINE-PATCH SPEC · 6 panel kinds · atlas slice + content padding + stretch proof
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        {NINEPATCH_KINDS.map(np => (
          <NinePatchCard key={np.key} np={np} />
        ))}
      </div>

      <div style={{ marginTop: 18, padding: "10px 12px", border: "1px solid var(--iron-edge-dim)", background: "var(--charcoal-panel-2)", borderRadius: 2, fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--muted-text)", lineHeight: 1.55 }}>
          <div className="t-gold" style={{ marginBottom: 4 }}>atlas pipeline contract</div>
          1. source rect is the canonical baseline; the patch must be authored at that exact size.<br/>
          2. corner stretch zones are 3px × 3px, fixed; only edges and center stretch.<br/>
          3. content padding (l/t/r/b) drives <code style={{color:"var(--cold-cyan)"}}>NinePatchDrawable.setPadding</code>.<br/>
          4. min size = corner*2 + 2px buffer. enforce with <code style={{color:"var(--cold-cyan)"}}>Cell.minSize</code> in scene2d.<br/>
          5. focus/glow halos are <em>not</em> baked into the 9-patch — they are <code style={{color:"var(--cold-cyan)"}}>Stage</code>-level effects on top.
      </div>
    </div>
  );
}

function NinePatchCard({ np }) {
  const sourceScale = 3;   // visualize source @ 3x for clarity
  const sw = np.src.w * sourceScale;
  const sh = np.src.h * sourceScale;
  const corner = np.corner * sourceScale;

  return (
    <div style={{
      padding: 14,
      background: "var(--charcoal-panel)",
      border: "1px solid var(--iron-edge)",
      borderRadius: 2,
      display: "flex", flexDirection: "column", gap: 10,
    }}>
      {/* identity */}
      <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
        <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 14, letterSpacing: "0.06em" }}>{np.name}</span>
        <span style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 11 }}>{np.key}</span>
      </div>
      <div style={{ color: "var(--muted-text)", fontSize: 11, lineHeight: 1.55 }}>{np.desc}</div>

      {/* source + stretched render */}
      <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: 14, alignItems: "center", marginTop: 6 }}>
        {/* source */}
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
          <div style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 10 }}>source · {np.src.w}×{np.src.h}</div>
          <div style={{ position: "relative", width: sw, height: sh }}>
            {/* the actual panel at source size */}
            <Panel padding={0} surface={np.surface} edge={np.edge} style={{ width: "100%", height: "100%" }}/>
            {/* stretch overlay markers */}
            <div style={{ position: "absolute", inset: 0, pointerEvents: "none" }}>
              {/* corners (red squares) */}
              {[
                { top: 0, left: 0 }, { top: 0, right: 0 },
                { bottom: 0, left: 0 }, { bottom: 0, right: 0 },
              ].map((p, i) => (
                <div key={i} style={{
                  position: "absolute",
                  width: corner, height: corner,
                  border: "1px solid rgba(182,66,66,0.7)",
                  background: "rgba(182,66,66,0.10)",
                  ...p,
                }}/>
              ))}
              {/* stretch marker lines (top + left) — the 9-patch black pixels */}
              <div style={{ position: "absolute", top: -4, left: corner, right: corner, height: 2,
                background: "var(--cold-cyan)" }}/>
              <div style={{ position: "absolute", left: -4, top: corner, bottom: corner, width: 2,
                background: "var(--cold-cyan)" }}/>
              {/* content padding markers (right + bottom) */}
              <div style={{ position: "absolute", bottom: -4,
                left:  np.padding.l * sourceScale,
                right: np.padding.r * sourceScale,
                height: 2, background: "var(--ember-gold)" }}/>
              <div style={{ position: "absolute", right: -4,
                top:    np.padding.t * sourceScale,
                bottom: np.padding.b * sourceScale,
                width: 2, background: "var(--ember-gold)" }}/>
            </div>
          </div>
          <div style={{ fontFamily: "var(--f-mono)", fontSize: 9, color: "var(--muted-text)", textAlign: "center" }}>
            corner {np.corner}px · pad {np.padding.l}/{np.padding.t}/{np.padding.r}/{np.padding.b}
          </div>
        </div>

        {/* stretched render */}
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          <div style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 10 }}>stretched · 280×120</div>
          <Panel padding={np.padding.l} surface={np.surface} edge={np.edge} style={{ width: 280, height: 120 }}>
            <div style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", height: "100%" }}>
              <div style={{ fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 10 }}>content area</div>
              <div style={{ display: "flex", justifyContent: "space-between", fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 10 }}>
                <span>min: {np.minSize.w}×{np.minSize.h}</span>
                <span>target: {np.target}</span>
              </div>
            </div>
          </Panel>
        </div>
      </div>

      {/* spec table */}
      <div style={{
        display: "grid", gridTemplateColumns: "100px 1fr",
        gap: "2px 8px",
        padding: "8px 10px",
        background: "rgba(5,7,10,0.5)",
        border: "1px solid var(--iron-edge-dim)",
        borderRadius: 2,
        fontFamily: "var(--f-mono)", fontSize: 10,
      }}>
        <span style={{ color: "var(--cold-cyan)" }}>atlas.key</span>           <span className="t-primary">{np.key}</span>
        <span style={{ color: "var(--cold-cyan)" }}>source</span>              <span className="t-primary">{np.src.w}×{np.src.h}px</span>
        <span style={{ color: "var(--cold-cyan)" }}>splits l,t,r,b</span>      <span className="t-primary">{np.corner},{np.corner},{np.corner},{np.corner}</span>
        <span style={{ color: "var(--cold-cyan)" }}>padding l,t,r,b</span>     <span className="t-primary">{np.padding.l},{np.padding.t},{np.padding.r},{np.padding.b}</span>
        <span style={{ color: "var(--cold-cyan)" }}>min</span>                 <span className="t-primary">{np.minSize.w}×{np.minSize.h}px</span>
        <span style={{ color: "var(--cold-cyan)" }}>asymmetric</span>          <span className="t-primary">{np.asymm ? "yes (left strip locked)" : "no"}</span>
      </div>

      {/* legend */}
      <div style={{ display: "flex", gap: 12, fontFamily: "var(--f-mono)", fontSize: 9, color: "var(--muted-text)" }}>
        <span><span style={{ display: "inline-block", width: 8, height: 8, background: "rgba(182,66,66,0.4)", border: "1px solid rgba(182,66,66,0.7)", marginRight: 4 }}/>fixed corner</span>
        <span><span style={{ display: "inline-block", width: 8, height: 2, background: "var(--cold-cyan)", marginRight: 4, verticalAlign: "middle" }}/>split markers</span>
        <span><span style={{ display: "inline-block", width: 8, height: 2, background: "var(--ember-gold)", marginRight: 4, verticalAlign: "middle" }}/>content padding</span>
      </div>
    </div>
  );
}

window.SlotStateMatrix = SlotStateMatrix;
window.NinePatchSpec = NinePatchSpec;
