// components/state-lab.jsx
// Component state showcases — slot 5-state, panel 6-state, talent 4-state.
// Each variant labeled with token + libGDX hint.

function SlotStateLab() {
  const states = [
    { state: "empty",     label: "EMPTY",     token: "slot.empty"   },
    { state: "equipped",  label: "EQUIPPED",  token: "eq.sword_t2"  },
    { state: "selected",  label: "SELECTED",  token: "eq.sword_t2"  },
    { state: "invalid",   label: "INVALID",   token: "eq.sword_t2"  },
    { state: "pending",   label: "PENDING",   token: "eq.sword_t2"  },
    { state: "disabled",  label: "DISABLED",  token: "eq.sword_t2"  },
  ];
  return (
    <LabRow title="SLOT · 状态合同(装备/背包/动作槽共用)">
      {states.map(s => (
        <LabCell key={s.state} label={s.label} hint={`tone+sigil dual-channel · ${s.state === "empty" ? "placeholder text" : ""}`}>
          <Slot size={64} state={s.state} token={s.state === "empty" ? null : s.token} placeholder="weapon" hint={s.state} />
        </LabCell>
      ))}
    </LabRow>
  );
}

function PanelStateLab() {
  const states = [
    { edge: "iron",        label: "DEFAULT",  hint: "iron-edge + low contrast"   },
    { edge: "cyan",        label: "FOCUSED",  hint: "cold-cyan rim · small area" },
    { edge: "cyan-strip",  label: "SELECTED", hint: "2px cyan left strip"        },
    { edge: "ember",       label: "RESERVE",  hint: "ember-dim hairline"         },
    { edge: "red",         label: "WARNING",  hint: "blood-red rim + glyph"      },
    { edge: "iron-strong", label: "MODAL",    hint: "2px iron + inner inset"     },
  ];
  return (
    <LabRow title="PANEL · 状态合同(面板/Modal/Tooltip)">
      {states.map(s => (
        <LabCell key={s.label} label={s.label} hint={s.hint} cellWidth={140}>
          <Panel padding={10} edge={s.edge} surface={s.edge === "iron-strong" ? "stone" : "charcoal"} style={{ width: 124, height: 64 }}>
            <div style={{ fontSize: 10, color: "var(--muted-text)", fontFamily: "var(--f-mono)" }}>panel_body</div>
            <div style={{ fontSize: 11, color: "var(--primary-text)" }}>Sample row</div>
          </Panel>
        </LabCell>
      ))}
    </LabRow>
  );
}

function TalentStateLab() {
  const rows = [
    { state: "LOCKED",          marker: "[x]", tone: "locked",    color: "var(--talent-locked)",    name: "战吼",     rank: "0/5" },
    { state: "LEARNABLE",       marker: "[+]", tone: "cyan",      color: "var(--talent-learnable)", name: "致命突袭", rank: "0/5" },
    { state: "LEARNED_RESERVE", marker: "[r]", tone: "gold",      color: "var(--talent-reserve)",   name: "守备姿态", rank: "2/5" },
    { state: "LEARNED_ACTIVE",  marker: "[*]", tone: "green",     color: "var(--talent-active)",    name: "猛击",     rank: "3/5" },
  ];
  return (
    <LabRow title="TALENT · 职业树四态(LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE)">
      {rows.map(r => (
        <LabCell key={r.state} label={r.state} hint="indent + connector + marker + icon + name + rank" cellWidth={210}>
          <div style={{
            width: 200, height: 32,
            display: "grid",
            gridTemplateColumns: "10px 14px 28px 1fr 32px",
            alignItems: "center",
            gap: 6,
            padding: "0 6px",
            background: "linear-gradient(180deg, #0c1118, #080B11)",
            border: `1px solid ${r.state === "LEARNABLE" ? "var(--cold-cyan)" : "var(--iron-edge)"}`,
            borderLeft: r.state === "LEARNABLE" ? "2px solid var(--cold-cyan)" : "1px solid var(--iron-edge)",
            borderRadius: 2,
          }}>
            <span style={{ color: r.color, fontFamily: "var(--f-mono)", fontSize: 11 }}>├</span>
            <span style={{ color: r.color, fontFamily: "var(--f-mono)", fontSize: 11, fontWeight: 700 }}>{r.marker}</span>
            <IconToken token={`talent.${r.name}`} size={22} tone={r.tone} fill="hollow" showLabel={false} />
            <span style={{ color: r.state === "LOCKED" ? "var(--talent-locked)" : "var(--primary-text)", fontSize: 12 }}>{r.name}</span>
            <span style={{ color: r.color, fontFamily: "var(--f-mono)", fontSize: 11, textAlign: "right" }}>{r.rank}</span>
          </div>
        </LabCell>
      ))}
    </LabRow>
  );
}

function BarStateLab() {
  return (
    <LabRow title="BAR · HP / SP / XP">
      <LabCell label="HP · FULL"    hint="blood-red · 152/152"><Bar kind="hp" current={152} max={152} width={180} /></LabCell>
      <LabCell label="HP · WARNING" hint="<25% threshold"      ><Bar kind="hp" current={31}  max={152} width={180} /></LabCell>
      <LabCell label="SP"           hint="stamina-green"        ><Bar kind="sp" current={84}  max={84}  width={180} /></LabCell>
      <LabCell label="XP"           hint="arcane-violet · thin" ><Bar kind="xp" current={1240} max={4000} width={180} height={10} /></LabCell>
    </LabRow>
  );
}

function KeyCapLab() {
  return (
    <LabRow title="KEYCAP · 命令芯片">
      <LabCell label="iron"><KeyCap>I</KeyCap></LabCell>
      <LabCell label="gold (hint)"><KeyCap tone="gold">Ctrl</KeyCap></LabCell>
      <LabCell label="cyan (action)"><KeyCap tone="cyan">5</KeyCap></LabCell>
      <LabCell label="composite"><div style={{ display: "flex", gap: 3, alignItems: "center" }}><KeyCap tone="gold">Ctrl</KeyCap><span className="t-muted">+</span><KeyCap tone="gold">S</KeyCap></div></LabCell>
    </LabRow>
  );
}

function LabRow({ title, children }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <div className="section-title" style={{ marginBottom: 12 }}>{title}</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 14 }}>{children}</div>
    </div>
  );
}

function LabCell({ label, hint, children, cellWidth = 96 }) {
  return (
    <div style={{
      display: "flex", flexDirection: "column", alignItems: "center", gap: 6,
      width: cellWidth,
    }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: 72 }}>{children}</div>
      <div style={{
        fontFamily: "var(--f-mono)",
        fontSize: 10,
        color: "var(--cold-cyan)",
        letterSpacing: 0.5,
      }}>{label}</div>
      {hint && (
        <div style={{
          fontFamily: "var(--f-mono)",
          fontSize: 9,
          color: "var(--muted-text)",
          textAlign: "center",
          lineHeight: 1.3,
          maxWidth: cellWidth,
        }}>{hint}</div>
      )}
    </div>
  );
}

window.SlotStateLab = SlotStateLab;
window.PanelStateLab = PanelStateLab;
window.TalentStateLab = TalentStateLab;
window.BarStateLab = BarStateLab;
window.KeyCapLab = KeyCapLab;
