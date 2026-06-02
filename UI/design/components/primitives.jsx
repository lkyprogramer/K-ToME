// components/primitives.jsx
// Atomic UI pieces used by the in-run shell.
//   - Panel (titled section frame)
//   - Bar (HP / SP / XP)
//   - KeyCap (small command chip)
//   - StatChip (icon + value)
//   - MapPlaceholder (tile-render target)

function Panel({ title, children, padding = 12, surface = "charcoal", edge = "iron", style = {}, headerRight = null }) {
  const surfaceClass =
    surface === "stone" ? "surface-stone" :
    surface === "void"  ? "surface-void"  : "surface-charcoal";
  const edgeClass =
    edge === "cyan"        ? "edge-cyan"
  : edge === "cyan-strip"  ? "edge-cyan-strip"
  : edge === "ember"       ? "edge-ember"
  : edge === "red"         ? "edge-red"
  : edge === "iron-strong" ? "edge-iron-strong"
  :                          "edge-iron";
  return (
    <div className={`${surfaceClass} ${edgeClass}`} style={{ borderRadius: "var(--r-md)", padding, ...style }}>
      {title && (
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
          <span style={{
            fontFamily: "var(--f-title)",
            color: "var(--ember-gold)",
            fontSize: 13,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            whiteSpace: "nowrap",
          }}>{title}</span>
          <span style={{ flex: 1, height: 1, background: "linear-gradient(to right, var(--ember-gold-dim), transparent 80%)" }} />
          {headerRight}
        </div>
      )}
      {children}
    </div>
  );
}

function Bar({ kind = "hp", current = 0, max = 100, width = 200, height = 14, label = true }) {
  const pct = Math.max(0, Math.min(1, current / max));
  const fillClass = kind === "hp" ? "bar-fill-hp" : kind === "sp" ? "bar-fill-sp" : "bar-fill-xp";
  return (
    <div style={{ position: "relative", width, height }}>
      <div className="bar-track" style={{ width: "100%", height: "100%" }}>
        <div className={fillClass} style={{ width: `${pct * 100}%` }} />
        {/* segment ticks every 25% — material grit */}
        <div style={{
          position: "absolute", inset: 0,
          background: "repeating-linear-gradient(90deg, rgba(0,0,0,0.4) 0 1px, transparent 1px 25%)",
          pointerEvents: "none",
        }}/>
      </div>
      {label && (
        <div style={{
          position: "absolute", inset: 0,
          display: "flex", alignItems: "center", justifyContent: "flex-end",
          paddingRight: 6,
          fontFamily: "var(--f-mono)",
          fontSize: 10,
          fontWeight: 600,
          color: "var(--primary-text)",
          textShadow: "0 1px 0 #000, 0 0 2px rgba(0,0,0,0.8)",
          letterSpacing: 0.2,
        }}>
          {current} / {max}
        </div>
      )}
    </div>
  );
}

function KeyCap({ children, tone = "iron", style = {} }) {
  const colorMap = {
    iron: { border: "var(--iron-edge)", bg: "rgba(5,7,10,0.7)", color: "var(--primary-text)" },
    gold: { border: "var(--ember-gold-dim)", bg: "rgba(5,7,10,0.7)", color: "var(--ember-gold)" },
    cyan: { border: "var(--cold-cyan-dim)", bg: "rgba(5,7,10,0.7)", color: "var(--cold-cyan)" },
  };
  const c = colorMap[tone] || colorMap.iron;
  return (
    <span style={{
      display: "inline-flex",
      alignItems: "center", justifyContent: "center",
      minWidth: 18, height: 18,
      padding: "0 5px",
      border: `1px solid ${c.border}`,
      background: c.bg,
      color: c.color,
      fontFamily: "var(--f-mono)",
      fontSize: 11,
      fontWeight: 600,
      borderRadius: 2,
      boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04), inset 0 -1px 0 rgba(0,0,0,0.5)",
      letterSpacing: 0.5,
      ...style,
    }}>{children}</span>
  );
}

function StatChip({ token, label, value, tone = "iron" }) {
  return (
    <div style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
      <IconToken token={token} size={20} tone={tone} fill="hollow" showLabel={false} />
      {/* hollow placeholder + token text */}
      <span style={{ color: "var(--muted-text)", fontSize: 12 }}>{label}</span>
      <span style={{ color: "var(--primary-text)", fontSize: 13, fontWeight: 600, fontFamily: "var(--f-mono)" }}>{value}</span>
    </div>
  );
}

function MapPlaceholder({ width, height }) {
  // A grid-textured void with token annotation showing it's the renderer target.
  return (
    <div style={{
      width, height,
      position: "relative",
      background:
        "radial-gradient(ellipse 70% 60% at 50% 50%, rgba(28,40,48,1), rgba(8,10,14,1) 80%), #05070A",
      border: "1px solid var(--iron-edge)",
      borderRadius: "var(--r-md)",
      overflow: "hidden",
      boxShadow: "inset 0 0 0 1px rgba(0,0,0,0.7), inset 0 2px 30px rgba(0,0,0,0.8)",
    }}>
      {/* tile grid */}
      <div style={{
        position: "absolute", inset: 0,
        background:
          "repeating-linear-gradient(90deg, rgba(255,255,255,0.025) 0 1px, transparent 1px 32px)," +
          "repeating-linear-gradient(0deg,  rgba(255,255,255,0.025) 0 1px, transparent 1px 32px)",
      }}/>
      {/* faint dungeon room outline */}
      <div style={{
        position: "absolute",
        top: "20%", left: "18%", right: "18%", bottom: "20%",
        border: "1px dashed rgba(255,200,140,0.10)",
        borderRadius: 2,
      }}/>
      {/* center label */}
      <div style={{
        position: "absolute", inset: 0,
        display: "flex", alignItems: "center", justifyContent: "center",
        flexDirection: "column", gap: 6, pointerEvents: "none",
      }}>
        <div style={{
          fontFamily: "var(--f-mono)",
          color: "rgba(28,183,200,0.55)",
          fontSize: 11,
          letterSpacing: "0.15em",
          textTransform: "uppercase",
        }}>[render target]</div>
        <div style={{
          fontFamily: "var(--f-mono)",
          color: "rgba(231,225,211,0.35)",
          fontSize: 13,
        }}>tile_map_render · 32px/tile</div>
        <div style={{
          fontFamily: "var(--f-mono)",
          color: "rgba(174,181,191,0.30)",
          fontSize: 10,
          marginTop: 4,
        }}>libGDX OrthographicCamera · tile_*, prop_*, actor_*</div>
      </div>
      {/* corner brackets */}
      {[
        { top: 6, left: 6, br: "none", bb: "none", bt: "1px", bl: "1px" },
        { top: 6, right: 6, bl: "none", bb: "none", bt: "1px", br: "1px" },
        { bottom: 6, left: 6, br: "none", bt: "none", bb: "1px", bl: "1px" },
        { bottom: 6, right: 6, bl: "none", bt: "none", bb: "1px", br: "1px" },
      ].map((s, i) => (
        <div key={i} style={{
          position: "absolute", width: 12, height: 12,
          borderTop:    s.bt    ? `${s.bt} solid var(--cold-cyan-dim)` : "none",
          borderBottom: s.bb    ? `${s.bb} solid var(--cold-cyan-dim)` : "none",
          borderLeft:   s.bl    ? `${s.bl} solid var(--cold-cyan-dim)` : "none",
          borderRight:  s.br    ? `${s.br} solid var(--cold-cyan-dim)` : "none",
          top: s.top, left: s.left, right: s.right, bottom: s.bottom,
        }}/>
      ))}
    </div>
  );
}

function NavRail({ width = 56, items, active = 0 }) {
  return (
    <div className="surface-charcoal edge-iron" style={{
      width,
      borderRadius: "var(--r-md)",
      padding: 8,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 4,
    }}>
      {items.map((it, i) => (
        <div key={i} style={{
          width: width - 16,
          height: width - 16,
          borderRadius: 2,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: i === active ? "rgba(28,183,200,0.07)" : "transparent",
          border: i === active ? "1px solid var(--cold-cyan)" : "1px solid transparent",
          position: "relative",
        }}>
          <IconToken token={it.token} size={width - 24} tone={i === active ? "cyan" : "iron"} fill="hollow" />
          {/* hotkey hint */}
          <span style={{
            position: "absolute", bottom: -2, right: -2,
            fontFamily: "var(--f-mono)",
            fontSize: 8,
            color: "var(--muted-text)",
            background: "var(--void-black)",
            padding: "0 2px",
            border: "1px solid var(--iron-edge)",
            borderRadius: 1,
            lineHeight: "10px",
          }}>{it.key}</span>
        </div>
      ))}
    </div>
  );
}

window.Panel = Panel;
window.Bar = Bar;
window.KeyCap = KeyCap;
window.StatChip = StatChip;
window.MapPlaceholder = MapPlaceholder;
window.NavRail = NavRail;
