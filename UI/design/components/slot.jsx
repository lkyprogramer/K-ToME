// components/slot.jsx
// Equipment / Inventory / Action slot — covers the five states:
//   empty | equipped | selected | invalid | pending
// State channel is dual: tone + sigil/badge — never color alone.

function Slot({
  size = 64,
  state = "empty",           // empty | equipped | selected | invalid | pending | disabled
  token = null,              // icon token to render inside when present
  shape = "square",
  hint = null,
  badge = null,              // {text, tone}
  hotkey = null,             // small bottom-right hotkey chip ("1", "Q")
  count = null,              // stack count bottom-right
  placeholder = null,        // text shown inside an empty slot (e.g. "weapon")
}) {
  const stateMeta = {
    empty:     { rim: "var(--iron-edge)",     glow: "none",                                                 badgeText: null,     badgeTone: "iron"  },
    equipped:  { rim: "var(--ember-gold-dim)", glow: "inset 0 0 0 1px rgba(217,154,43,0.18)",               badgeText: null,     badgeTone: "gold"  },
    selected:  { rim: "var(--cold-cyan)",      glow: "inset 0 0 0 1px rgba(28,183,200,0.25), 0 0 0 1px rgba(28,183,200,0.10)", badgeText: null, badgeTone: "cyan" },
    invalid:   { rim: "var(--blood-red)",      glow: "inset 0 0 0 1px rgba(182,66,66,0.30)",                badgeText: "!",      badgeTone: "red"   },
    pending:   { rim: "var(--ember-gold-dim)", glow: "inset 0 0 0 1px rgba(217,154,43,0.20), 0 0 0 1px var(--cold-cyan-dim)", badgeText: "↻", badgeTone: "cyan" },
    disabled:  { rim: "var(--talent-locked)",  glow: "none",                                                 badgeText: null,     badgeTone: "iron"  },
  };
  const m = stateMeta[state] || stateMeta.empty;

  const borderRadius = shape === "circle" ? "50%" : "var(--r-md)";

  return (
    <div data-slot-state={state} style={{
      position: "relative",
      width: size,
      height: size,
      borderRadius,
      background:
        state === "disabled"
          ? "linear-gradient(180deg, #0d1118, #070A0F)"
          : "radial-gradient(60% 60% at 50% 40%, rgba(255,235,200,0.018), transparent 80%), #0A0E14",
      border: `1px solid ${m.rim}`,
      boxShadow: `${m.glow}, inset 0 1px 0 rgba(255,255,255,0.025), inset 0 -1px 0 rgba(0,0,0,0.55)`,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      overflow: "visible",
      flex: "0 0 auto",
      opacity: state === "disabled" ? 0.55 : 1,
    }}>
      {/* corner ticks — render slot affordance even in empty state */}
      <CornerTicks size={size} color={m.rim} />

      {/* content */}
      {token ? (
        <IconToken token={token} size={size - 6} tone="iron" shape={shape} hint={hint} />
      ) : (
        <span style={{
          fontFamily: "var(--f-mono)",
          fontSize: size >= 64 ? 9 : 8,
          color: "var(--talent-locked)",
          opacity: 0.5,
          letterSpacing: 0,
          textAlign: "center",
          padding: 4,
        }}>{placeholder || "empty"}</span>
      )}

      {/* badge */}
      {(badge || m.badgeText) && (
        <div style={{
          position: "absolute",
          top: -5, right: -5,
          minWidth: 14, height: 14,
          padding: "0 3px",
          background: "var(--void-black)",
          border: `1px solid ${
            (badge?.tone || m.badgeTone) === "red"  ? "var(--blood-red)"
          : (badge?.tone || m.badgeTone) === "cyan" ? "var(--cold-cyan)"
          : (badge?.tone || m.badgeTone) === "gold" ? "var(--ember-gold)"
          : "var(--iron-edge)"
          }`,
          color:
            (badge?.tone || m.badgeTone) === "red"  ? "var(--blood-red)"
          : (badge?.tone || m.badgeTone) === "cyan" ? "var(--cold-cyan)"
          : (badge?.tone || m.badgeTone) === "gold" ? "var(--ember-gold)"
          : "var(--muted-text)",
          fontFamily: "var(--f-mono)",
          fontSize: 9,
          lineHeight: "12px",
          textAlign: "center",
          borderRadius: 2,
        }}>{badge?.text || m.badgeText}</div>
      )}

      {/* hotkey */}
      {hotkey && (
        <div style={{
          position: "absolute",
          top: 3, left: 3,
          minWidth: 12, height: 12,
          padding: "0 3px",
          background: "rgba(5,7,10,0.85)",
          border: "1px solid var(--iron-edge)",
          color: "var(--ember-gold)",
          fontFamily: "var(--f-mono)",
          fontSize: 9,
          lineHeight: "10px",
          textAlign: "center",
          borderRadius: 2,
        }}>{hotkey}</div>
      )}

      {/* count */}
      {count != null && (
        <div style={{
          position: "absolute",
          bottom: 2, right: 3,
          color: "var(--primary-text)",
          fontFamily: "var(--f-mono)",
          fontSize: size >= 48 ? 11 : 9,
          fontWeight: 600,
          textShadow: "0 1px 0 #000, 0 0 2px #000",
        }}>{count}</div>
      )}
    </div>
  );
}

function CornerTicks({ size, color }) {
  // 4 small L-shapes at slot corners — purely material affordance.
  const len = size >= 56 ? 7 : 5;
  const off = 2;
  const lineColor = "rgba(255,255,255,0.08)";
  const style = (pos) => ({ position: "absolute", ...pos, pointerEvents: "none" });
  return (
    <>
      <span style={style({ top: off, left: off, width: len, height: 1, background: lineColor })} />
      <span style={style({ top: off, left: off, width: 1, height: len, background: lineColor })} />
      <span style={style({ top: off, right: off, width: len, height: 1, background: lineColor })} />
      <span style={style({ top: off, right: off, width: 1, height: len, background: lineColor })} />
      <span style={style({ bottom: off, left: off, width: len, height: 1, background: lineColor })} />
      <span style={style({ bottom: off, left: off, width: 1, height: len, background: lineColor })} />
      <span style={style({ bottom: off, right: off, width: len, height: 1, background: lineColor })} />
      <span style={style({ bottom: off, right: off, width: 1, height: len, background: lineColor })} />
    </>
  );
}

window.Slot = Slot;
window.CornerTicks = CornerTicks;
