// components/token-board.jsx
// Color, spacing, and typography token reference — the source-of-truth swatch board.

const COLOR_TOKENS = [
  { name: "void-black",        hex: "#05070A", usage: "主背景 · 地图外暗区"            },
  { name: "charcoal-panel",    hex: "#10151D", usage: "面板底色 · default surface"    },
  { name: "iron-edge",         hex: "#2B3542", usage: "面板边 · slot 边 · 分隔"        },
  { name: "cold-cyan",         hex: "#1CB7C8", usage: "可交互边缘 · 焦点 · 选中"        },
  { name: "ember-gold",        hex: "#D99A2B", usage: "标题 · 稀有 · 确认态"          },
  { name: "blood-red",         hex: "#B64242", usage: "生命 · 危险 · 警告"            },
  { name: "stamina-green",     hex: "#52C989", usage: "耐力 · 恢复 · 已激活"          },
  { name: "arcane-violet",     hex: "#7B5CE1", usage: "奥术 · 经验 · 稀有魔法"        },
  { name: "muted-text",        hex: "#AEB5BF", usage: "次级文本"                      },
  { name: "primary-text",      hex: "#E7E1D3", usage: "主文本 · warm off-white"      },
];

const TALENT_TOKENS = [
  { name: "talent-locked",     hex: "#59616C", usage: "LOCKED ·   锁定节点"           },
  { name: "talent-learnable",  hex: "#1CB7C8", usage: "LEARNABLE · 可学习"            },
  { name: "talent-reserve",    hex: "#D99A2B", usage: "LEARNED_RESERVE · 已学未激活"  },
  { name: "talent-active",     hex: "#52C989", usage: "LEARNED_ACTIVE · 已激活"      },
];

function TokenBoard() {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 24 }}>
      <div>
        <div className="section-title" style={{ marginBottom: 12 }}>COLOR · 主调色板</div>
        <SwatchList items={COLOR_TOKENS} />
      </div>
      <div>
        <div className="section-title" style={{ marginBottom: 12 }}>TALENT · 职业树四态 tone</div>
        <SwatchList items={TALENT_TOKENS} />

        <div className="section-title" style={{ marginTop: 24, marginBottom: 12 }}>SPACING · 4px 基线</div>
        <SpacingBoard />

        <div className="section-title" style={{ marginTop: 24, marginBottom: 12 }}>TYPOGRAPHY · 字阶</div>
        <TypeBoard />

        <div className="section-title" style={{ marginTop: 24, marginBottom: 12 }}>SLOT · 渲染尺寸合同</div>
        <SlotSizeBoard />
      </div>
    </div>
  );
}

function SwatchList({ items }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      {items.map(it => (
        <div key={it.name} style={{
          display: "grid",
          gridTemplateColumns: "32px 1fr auto",
          gap: 10,
          alignItems: "center",
          padding: "6px 10px",
          background: "var(--charcoal-panel)",
          border: "1px solid var(--iron-edge)",
          borderRadius: 2,
        }}>
          <div style={{
            width: 32, height: 32, borderRadius: 2,
            background: it.hex,
            border: "1px solid rgba(0,0,0,0.6)",
            boxShadow: "inset 0 1px 0 rgba(255,255,255,0.08)",
          }}/>
          <div>
            <div style={{ fontFamily: "var(--f-mono)", fontSize: 12, color: "var(--primary-text)" }}>--{it.name}</div>
            <div style={{ fontFamily: "var(--f-body)", fontSize: 11, color: "var(--muted-text)" }}>{it.usage}</div>
          </div>
          <div style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--cold-cyan)" }}>{it.hex}</div>
        </div>
      ))}
    </div>
  );
}

function SpacingBoard() {
  const scale = [
    { token: "--space-1", px: 4  },
    { token: "--space-2", px: 8  },
    { token: "--space-3", px: 12 },
    { token: "--space-4", px: 16 },
    { token: "--space-5", px: 20 },
    { token: "--space-6", px: 24 },
    { token: "--space-8", px: 32 },
  ];
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      {scale.map(s => (
        <div key={s.token} style={{ display: "grid", gridTemplateColumns: "100px 40px 1fr", alignItems: "center", gap: 10 }}>
          <span style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--primary-text)" }}>{s.token}</span>
          <span style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--cold-cyan)", textAlign: "right" }}>{s.px}px</span>
          <div style={{ height: 10, width: s.px * 4, background: "var(--ember-gold-dim)", border: "1px solid var(--ember-gold)" }}/>
        </div>
      ))}
    </div>
  );
}

function TypeBoard() {
  const items = [
    { token: "f-title · 24",  fs: 24, fw: 600, family: "var(--f-title)", sample: "K-ToME 破碎前哨", role: "Modal H1 · Section header" },
    { token: "f-title · 18",  fs: 18, fw: 600, family: "var(--f-title)", sample: "薇雄  Vyon",      role: "Hero name · Panel title" },
    { token: "f-title · 13",  fs: 13, fw: 600, family: "var(--f-title)", sample: "EQUIPMENT · 装备", role: "Panel header (uppercase)" },
    { token: "f-body  · 14",  fs: 14, fw: 400, family: "var(--f-body)",  sample: "你进入了地牢。",   role: "Body / log primary"     },
    { token: "f-body  · 12",  fs: 12, fw: 400, family: "var(--f-body)",  sample: "目标: 突破前哨。", role: "Body secondary · meta"  },
    { token: "f-body  · 11",  fs: 11, fw: 400, family: "var(--f-body)",  sample: "路线提示: 先沿钥匙口…", role: "Hint / dense rows" },
    { token: "f-mono  · 10",  fs: 10, fw: 600, family: "var(--f-mono)",  sample: "152 / 152",       role: "Numeric · HUD value"    },
  ];
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      {items.map(it => (
        <div key={it.token} style={{ display: "grid", gridTemplateColumns: "120px 1fr 1fr", gap: 10, alignItems: "baseline", padding: "4px 8px", background: "var(--charcoal-panel)", border: "1px solid var(--iron-edge)", borderRadius: 2 }}>
          <span style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--cold-cyan)" }}>{it.token}</span>
          <span style={{ fontFamily: it.family, fontSize: it.fs, fontWeight: it.fw, color: "var(--primary-text)" }}>{it.sample}</span>
          <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)" }}>{it.role}</span>
        </div>
      ))}
    </div>
  );
}

function SlotSizeBoard() {
  const sizes = [
    { token: "--slot-hud",    px: 32, label: "HUD · log glyph · 状态点" },
    { token: "--slot-inv",    px: 48, label: "Inventory cell"           },
    { token: "--slot-equip",  px: 64, label: "Equipment / Action"       },
  ];
  return (
    <div style={{ display: "flex", gap: 20, alignItems: "flex-end" }}>
      {sizes.map(s => (
        <div key={s.token} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
          <Slot size={s.px} state="equipped" token={`sample.${s.px}`} />
          <div style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--cold-cyan)" }}>{s.token}</div>
          <div style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)" }}>{s.px}px</div>
          <div style={{ fontFamily: "var(--f-body)", fontSize: 10, color: "var(--muted-text)", textAlign: "center" }}>{s.label}</div>
        </div>
      ))}
    </div>
  );
}

window.TokenBoard = TokenBoard;
