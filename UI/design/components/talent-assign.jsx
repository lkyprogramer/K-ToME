// components/talent-assign.jsx
// Talent Assign full-screen surface (1280×800).
// Per ART_STYLE_BIBLE §3 + ktome-dark-ui-design §6.3:
//   Talent row grammar: indent + connector + state marker + skill icon + talent name + rank
//   Four states: LOCKED [x] · LEARNABLE [+] · LEARNED_RESERVE [r] · LEARNED_ACTIVE [*]
// Layout: header(48) | [categories 280 | tree 540 | detail 360] | footer(40)

const TALENT_TREE = [
  { cat: "战斗 · Combat", catKey: "combat", talents: [
    { state: "LEARNED_ACTIVE",  name: "猛击",       en: "Smash",        rank: [3,5], token: "talent.smash"     },
    { state: "LEARNED_ACTIVE",  name: "盾击",       en: "Shield Bash",  rank: [2,5], token: "talent.shield_bash" },
    { state: "LEARNABLE",       name: "致命突袭",   en: "Onslaught",    rank: [0,5], token: "talent.onslaught" },
    { state: "LOCKED",          name: "终结",       en: "Execute",      rank: [0,5], token: "talent.execute", lockReason: "需 猛击 5/5" },
    { state: "LOCKED",          name: "破甲斩",     en: "Sunder",       rank: [0,3], token: "talent.sunder", lockReason: "需 致命突袭 3/5" },
  ]},
  { cat: "防御 · Defense", catKey: "defense", talents: [
    { state: "LEARNED_RESERVE", name: "守备姿态",   en: "Guard Stance", rank: [2,5], token: "talent.guard"     },
    { state: "LEARNABLE",       name: "反击",       en: "Riposte",      rank: [0,5], token: "talent.riposte"   },
    { state: "LOCKED",          name: "护盾屏障",   en: "Bulwark",      rank: [0,3], token: "talent.bulwark", lockReason: "需 守备姿态 5/5" },
  ]},
  { cat: "指挥 · Command", catKey: "command", talents: [
    { state: "LEARNED_RESERVE", name: "指挥姿态",   en: "Command",      rank: [1,3], token: "talent.command"   },
    { state: "LOCKED",          name: "鼓舞",       en: "Inspire",      rank: [0,3], token: "talent.inspire", lockReason: "需 指挥姿态 3/3" },
    { state: "LOCKED",          name: "横扫",       en: "Sweep",        rank: [0,3], token: "talent.sweep", lockReason: "需 反击 2/5" },
  ]},
];

const STATE_META = {
  LOCKED:          { marker: "[x]", color: "var(--talent-locked)",    label: "已锁定" },
  LEARNABLE:       { marker: "[+]", color: "var(--talent-learnable)", label: "可学习" },
  LEARNED_RESERVE: { marker: "[r]", color: "var(--talent-reserve)",   label: "已学·后备" },
  LEARNED_ACTIVE:  { marker: "[*]", color: "var(--talent-active)",    label: "已学·激活" },
};

function TalentAssign({ focusedTalent = "onslaught", focusedCat = "combat", showOverlay = false }) {
  return (
    <div data-screen-label="talent_assign" style={{
      width: 1280, height: 800,
      background: "var(--void-black)",
      display: "grid",
      gridTemplateRows: "48px 1fr 40px",
      fontFamily: "var(--f-body)",
      color: "var(--primary-text)",
      position: "relative",
    }}>
      {/* === Header bar === */}
      <div style={{
        background: "var(--charcoal-panel)",
        borderBottom: "1px solid var(--iron-edge)",
        display: "grid",
        gridTemplateColumns: "auto 1fr auto",
        alignItems: "center",
        padding: "0 16px",
        gap: 16,
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 18, letterSpacing: "0.10em" }}>天赋分配</span>
          <span style={{ color: "var(--talent-locked)" }}>·</span>
          <span style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 12 }}>TALENT ASSIGN</span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 12, color: "var(--muted-text)" }}>
          <span className="t-gold">薇雄 · Vyon</span>
          <span>›</span>
          <span>先锋 · Vanguard</span>
          <span>›</span>
          <span>Lv <span className="t-primary t-mono">7</span></span>
        </div>
        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
          <div style={{
            padding: "4px 10px",
            border: "1px solid var(--ember-gold-dim)",
            background: "rgba(217,154,43,0.05)",
            borderRadius: 2,
            fontFamily: "var(--f-mono)", fontSize: 11,
          }}>
            <span style={{ color: "var(--muted-text)" }}>可分配点 </span>
            <span style={{ color: "var(--ember-gold)", fontWeight: 700 }}>3</span>
          </div>
          <KeyCap>Esc</KeyCap>
        </div>
      </div>

      {/* === Body: 3 columns === */}
      <div style={{
        display: "grid",
        gridTemplateColumns: "280px 1fr 360px",
        gap: 6, padding: 6,
        minHeight: 0,
      }}>
        {/* --- Categories rail --- */}
        <Panel padding={10}>
          <div className="section-title" style={{ marginBottom: 10 }}>类目 · CATEGORIES</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
            {TALENT_TREE.map(c => {
              const active = c.catKey === focusedCat;
              const counts = c.talents.reduce((acc, t) => { acc[t.state]++; return acc; }, { LOCKED:0, LEARNABLE:0, LEARNED_RESERVE:0, LEARNED_ACTIVE:0 });
              return (
                <div key={c.catKey} style={{
                  padding: "10px 10px",
                  background: active ? "rgba(28,183,200,0.05)" : "transparent",
                  border: `1px solid ${active ? "var(--cold-cyan)" : "var(--iron-edge)"}`,
                  borderLeft: active ? "2px solid var(--cold-cyan)" : "1px solid var(--iron-edge)",
                  borderRadius: 2,
                  cursor: "default",
                }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                    <span style={{
                      fontFamily: "var(--f-title)",
                      color: active ? "var(--cold-cyan)" : "var(--primary-text)",
                      fontSize: 13,
                      letterSpacing: "0.06em",
                    }}>{c.cat}</span>
                    <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)" }}>{c.talents.length}</span>
                  </div>
                  <div style={{ display: "flex", gap: 6, marginTop: 4 }}>
                    <StateTally color="var(--talent-active)"    n={counts.LEARNED_ACTIVE}  glyph="●" />
                    <StateTally color="var(--talent-reserve)"   n={counts.LEARNED_RESERVE} glyph="◐" />
                    <StateTally color="var(--talent-learnable)" n={counts.LEARNABLE}       glyph="◇" />
                    <StateTally color="var(--talent-locked)"    n={counts.LOCKED}          glyph="✕" />
                  </div>
                </div>
              );
            })}
          </div>

          <div className="section-title" style={{ marginTop: 18, marginBottom: 10 }}>职业 · CLASS</div>
          <div style={{
            border: "1px solid var(--iron-edge)",
            background: "linear-gradient(180deg, #0c1118, #080B11)",
            padding: 10,
            display: "flex", gap: 10, alignItems: "center",
          }}>
            <IconToken token="portrait.vanguard" size={56} tone="gold" />
            <div>
              <div style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 14 }}>先锋</div>
              <div style={{ fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 10 }}>portrait.vanguard</div>
              <div style={{ fontFamily: "var(--f-body)", color: "var(--muted-text)", fontSize: 11, marginTop: 2 }}>持盾近战 · 指挥型</div>
            </div>
          </div>
        </Panel>

        {/* --- Tree list --- */}
        <Panel padding={0} style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ padding: "10px 14px 8px 14px", borderBottom: "1px solid var(--iron-edge-dim)" }}>
            <div className="section-title" style={{ margin: 0 }}>天赋树 · TREE</div>
          </div>
          <div style={{ padding: 8, flex: 1 }}>
            {TALENT_TREE.map(c => (
              <TreeCategory key={c.catKey} cat={c} focusedTalent={focusedTalent} />
            ))}
          </div>
          {showOverlay && (
            <div style={{ padding: 6, borderTop: "1px solid var(--iron-edge-dim)" }}>
              <SpecTag>row.h=28 · indent=12·24 · marker.w=14 · icon=20</SpecTag>
            </div>
          )}
        </Panel>

        {/* --- Detail --- */}
        <Panel padding={0} edge="cyan-strip" style={{ display: "flex", flexDirection: "column" }}>
          <TalentDetail focused={
            TALENT_TREE.find(c => c.catKey === focusedCat).talents.find(t => t.en.toLowerCase().includes(focusedTalent.toLowerCase())) ||
            TALENT_TREE[0].talents[2]
          } category={TALENT_TREE.find(c => c.catKey === focusedCat).cat}/>
        </Panel>
      </div>

      {/* === Footer === */}
      <div style={{
        background: "var(--charcoal-panel)",
        borderTop: "1px solid var(--iron-edge)",
        display: "flex", alignItems: "center", gap: 16,
        padding: "0 16px",
        fontSize: 11, color: "var(--muted-text)",
      }}>
        <LegendItem keys={["Enter"]} label="学习 / 升级" tone="gold" />
        <LegendItem keys={["R"]}     label="切换 后备/激活" tone="gold" />
        <LegendItem keys={["Tab"]}   label="切换类目" tone="gold" />
        <LegendItem keys={["↑","↓"]} label="移动" tone="cyan" />
        <LegendItem keys={["Esc"]}   label="关闭" tone="iron" />
        <div style={{ flex: 1 }}/>
        <StateLegend />
      </div>

      {showOverlay && (
        <>
          <SpecTag style={{ position: "absolute", top: 6, left: 300 }}>cat.col=280</SpecTag>
          <SpecTag style={{ position: "absolute", top: 6, right: 380 }}>detail.col=360</SpecTag>
          <SpecTag style={{ position: "absolute", top: 56, left: 8 }}>header.h=48</SpecTag>
          <SpecTag style={{ position: "absolute", bottom: 50, left: 8 }}>footer.h=40</SpecTag>
        </>
      )}
    </div>
  );
}

function StateTally({ color, n, glyph }) {
  return (
    <div style={{
      display: "inline-flex", alignItems: "center", gap: 3,
      padding: "0 4px", height: 14,
      background: "rgba(5,7,10,0.6)",
      border: `1px solid ${color}`,
      borderRadius: 1,
      fontFamily: "var(--f-mono)", fontSize: 9,
      color, opacity: n === 0 ? 0.35 : 1,
    }}>
      <span>{glyph}</span><span>{n}</span>
    </div>
  );
}

function TreeCategory({ cat, focusedTalent }) {
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{
        display: "flex", alignItems: "center", gap: 8,
        padding: "4px 6px",
        background: "rgba(217,154,43,0.04)",
        borderLeft: "2px solid var(--ember-gold-dim)",
        marginBottom: 4,
      }}>
        <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 12, letterSpacing: "0.08em" }}>{cat.cat}</span>
        <span style={{ flex: 1, height: 1, background: "linear-gradient(to right, var(--ember-gold-dim), transparent 80%)" }}/>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        {cat.talents.map((t, i) => (
          <TalentRow key={t.en} t={t} last={i === cat.talents.length - 1}
                     focused={t.en.toLowerCase().includes(focusedTalent.toLowerCase())}/>
        ))}
      </div>
    </div>
  );
}

function TalentRow({ t, last, focused }) {
  const meta = STATE_META[t.state];
  const connector = last ? "└" : "├";
  const rankFull = t.rank[0] >= t.rank[1];
  return (
    <div style={{
      display: "grid",
      gridTemplateColumns: "12px 14px 14px 22px 1fr 80px",
      alignItems: "center",
      gap: 8,
      padding: "5px 8px",
      height: 28,
      background: focused ? "rgba(28,183,200,0.05)" : "transparent",
      border: focused ? "1px solid var(--cold-cyan)" : "1px solid transparent",
      borderLeft: focused ? "2px solid var(--cold-cyan)" : "1px solid transparent",
      borderRadius: 2,
      opacity: t.state === "LOCKED" ? 0.7 : 1,
    }}>
      <span/>
      <span style={{ color: "var(--talent-locked)", fontFamily: "var(--f-mono)", fontSize: 11 }}>{connector}</span>
      <span style={{ color: meta.color, fontFamily: "var(--f-mono)", fontSize: 11, fontWeight: 700 }}>{meta.marker}</span>
      <IconToken token={t.token} size={20} tone={
        t.state === "LOCKED" ? "locked" :
        t.state === "LEARNABLE" ? "cyan" :
        t.state === "LEARNED_RESERVE" ? "gold" :
        "green"
      } fill="hollow" showLabel={false} />
      <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.1 }}>
        <span style={{
          color: t.state === "LOCKED" ? "var(--talent-locked)" : focused ? "var(--cold-cyan)" : "var(--primary-text)",
          fontSize: 12,
        }}>{t.name}</span>
        <span style={{ color: "var(--muted-text)", fontFamily: "var(--f-mono)", fontSize: 9 }}>
          {t.en}{t.lockReason ? ` · ${t.lockReason}` : ""}
        </span>
      </div>
      <div style={{
        display: "flex", alignItems: "center", gap: 4,
        justifyContent: "flex-end",
        fontFamily: "var(--f-mono)", fontSize: 11,
        color: meta.color,
      }}>
        <RankPips current={t.rank[0]} max={t.rank[1]} color={meta.color} />
        <span style={{ minWidth: 28, textAlign: "right" }}>{t.rank[0]}/{t.rank[1]}</span>
      </div>
    </div>
  );
}

function RankPips({ current, max, color }) {
  return (
    <div style={{ display: "flex", gap: 2 }}>
      {Array.from({ length: max }).map((_, i) => (
        <div key={i} style={{
          width: 5, height: 8,
          background: i < current ? color : "transparent",
          border: `1px solid ${color}`,
          opacity: i < current ? 1 : 0.4,
          borderRadius: 1,
        }}/>
      ))}
    </div>
  );
}

function TalentDetail({ focused, category }) {
  const meta = STATE_META[focused.state];
  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Top: identity */}
      <div style={{ padding: 14, borderBottom: "1px solid var(--iron-edge-dim)" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
          <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 20, letterSpacing: "0.06em" }}>{focused.name}</span>
          <span style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 11 }}>{focused.token}</span>
        </div>
        <div style={{ color: "var(--muted-text)", fontSize: 11, marginTop: 2 }}>
          {category} · <span style={{ color: meta.color }}>{meta.label}</span>
        </div>
      </div>

      {/* Body */}
      <div style={{ padding: 14, display: "flex", flexDirection: "column", gap: 14, flex: 1 }}>

        <DetailSection label="效果 · EFFECT">
          <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "4px 10px", fontSize: 12 }}>
            <span style={{ color: "var(--muted-text)" }}>当前 ({focused.rank[0]}/{focused.rank[1]})</span>
            <span className="t-primary">对目标造成 <span className="t-gold">{18 + focused.rank[0]*4}</span> 物理伤害,30% 几率眩晕 1 回合。</span>
            <span style={{ color: "var(--muted-text)" }}>下级 ({focused.rank[0]+1}/{focused.rank[1]})</span>
            <span className="t-cyan">对目标造成 <span className="t-cyan">{18 + (focused.rank[0]+1)*4}</span> 物理伤害,40% 几率眩晕 1 回合。</span>
          </div>
        </DetailSection>

        <DetailSection label="先决条件 · PREREQUISITES">
          <div style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 11 }}>
            <PrereqRow ok={true}  label="先锋 Lv 5"          actual="Lv 7" />
            <PrereqRow ok={true}  label="猛击 ≥ 2/5"          actual="3/5" />
            <PrereqRow ok={false} label="致命突袭 ≥ 1/5"      actual="0/5" />
          </div>
        </DetailSection>

        <DetailSection label="代价 · COST">
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <IconToken token="ui.talent_point" size={20} tone="gold" fill="hollow" showLabel={false}/>
              <span style={{ fontFamily: "var(--f-mono)", fontSize: 12, color: "var(--ember-gold)" }}>1 天赋点</span>
            </div>
            <span style={{ color: "var(--muted-text)", fontSize: 11 }}>剩余 <span className="t-gold t-mono">3</span></span>
          </div>
        </DetailSection>

        <div style={{
          marginTop: "auto",
          padding: 10,
          background: "rgba(28,183,200,0.04)",
          border: "1px solid var(--cold-cyan-dim)",
          borderRadius: 2,
          fontStyle: "italic",
          color: "var(--muted-text)",
          fontSize: 11,
          lineHeight: 1.55,
        }}>
          "敌人的步伐被斩断,胜负只在下一拍。" — Vyon's drill manual.
        </div>

      </div>

      {/* Footer actions */}
      <div style={{ padding: "10px 14px", borderTop: "1px solid var(--iron-edge-dim)", display: "flex", gap: 8 }}>
        <ActionButton tone="gold"  primary>Enter · 学习</ActionButton>
        <ActionButton tone="cyan"          >R · 后备</ActionButton>
      </div>
    </div>
  );
}

function DetailSection({ label, children }) {
  return (
    <div>
      <div style={{
        fontFamily: "var(--f-title)",
        color: "var(--ember-gold)",
        fontSize: 10,
        letterSpacing: "0.15em",
        marginBottom: 6,
      }}>{label}</div>
      {children}
    </div>
  );
}

function PrereqRow({ ok, label, actual }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <span style={{
        width: 14, height: 14, borderRadius: 1,
        border: `1px solid ${ok ? "var(--stamina-green)" : "var(--blood-red)"}`,
        background: "rgba(5,7,10,0.6)",
        display: "inline-flex", alignItems: "center", justifyContent: "center",
        color: ok ? "var(--stamina-green)" : "var(--blood-red)",
        fontFamily: "var(--f-mono)", fontSize: 10, fontWeight: 700,
        lineHeight: 1,
      }}>{ok ? "✓" : "✕"}</span>
      <span style={{ color: ok ? "var(--primary-text)" : "var(--muted-text)" }}>{label}</span>
      <span style={{ flex: 1 }}/>
      <span style={{ fontFamily: "var(--f-mono)", color: ok ? "var(--stamina-green)" : "var(--blood-red)" }}>{actual}</span>
    </div>
  );
}

function ActionButton({ children, tone = "iron", primary = false }) {
  const colors = {
    iron: { border: "var(--iron-edge)",        bg: "rgba(5,7,10,0.6)",  fg: "var(--primary-text)"   },
    gold: { border: "var(--ember-gold)",       bg: "rgba(217,154,43,0.08)", fg: "var(--ember-gold)" },
    cyan: { border: "var(--cold-cyan-dim)",    bg: "rgba(28,183,200,0.05)", fg: "var(--cold-cyan)"  },
  };
  const c = colors[tone];
  return (
    <button style={{
      flex: 1,
      height: 32,
      background: c.bg,
      border: `1px solid ${c.border}`,
      borderBottom: primary ? `2px solid ${c.border}` : `1px solid ${c.border}`,
      color: c.fg,
      fontFamily: "var(--f-title)",
      fontSize: 12,
      letterSpacing: "0.10em",
      cursor: "pointer",
      borderRadius: 2,
      boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04)",
    }}>{children}</button>
  );
}

function LegendItem({ keys, label, tone = "iron" }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      <div style={{ display: "flex", gap: 2 }}>
        {keys.map((k, i) => <KeyCap key={i} tone={tone}>{k}</KeyCap>)}
      </div>
      <span style={{ color: "var(--muted-text)", fontSize: 11 }}>{label}</span>
    </div>
  );
}

function StateLegend() {
  const items = [
    { k: "LOCKED",          meta: STATE_META.LOCKED          },
    { k: "LEARNABLE",       meta: STATE_META.LEARNABLE       },
    { k: "LEARNED_RESERVE", meta: STATE_META.LEARNED_RESERVE },
    { k: "LEARNED_ACTIVE",  meta: STATE_META.LEARNED_ACTIVE  },
  ];
  return (
    <div style={{ display: "flex", gap: 10 }}>
      {items.map(it => (
        <div key={it.k} style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <span style={{ color: it.meta.color, fontFamily: "var(--f-mono)", fontSize: 10, fontWeight: 700 }}>{it.meta.marker}</span>
          <span style={{ color: "var(--muted-text)", fontFamily: "var(--f-mono)", fontSize: 10 }}>{it.k}</span>
        </div>
      ))}
    </div>
  );
}

window.TalentAssign = TalentAssign;
